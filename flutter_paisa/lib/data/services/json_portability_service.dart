import 'dart:convert';
import 'dart:io';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import '../models/backup_payload.dart';
import '../models/budget_model.dart';
import '../models/recurring_rule.dart';
import '../models/transaction_item.dart';
import '../models/user_profile.dart';

class ValidationSummary {
  final int transactionCount;
  final int budgetCount;
  final int recurringCount;
  final String accountName;
  final String accountEmail;
  final String exportDate;

  ValidationSummary({
    required this.transactionCount,
    required this.budgetCount,
    required this.recurringCount,
    required this.accountName,
    required this.accountEmail,
    required this.exportDate,
  });
}

class JsonValidationResult {
  final bool isSuccess;
  final BackupPayload? backup;
  final ValidationSummary? summary;
  final String? errorMessage;

  JsonValidationResult.success(this.backup, this.summary)
      : isSuccess = true,
        errorMessage = null;

  JsonValidationResult.error(this.errorMessage)
      : isSuccess = false,
        backup = null,
        summary = null;
}

class JsonPortabilityService {
  static Future<File> exportToJsonFile({
    required UserProfile profile,
    required List<TransactionItem> transactions,
    required List<BudgetModel> budgets,
    required List<RecurringRule> recurringRules,
  }) async {
    final now = DateTime.now();
    final dateStamp = DateFormat('yyyy-MM-dd').format(now);
    final timeStamp = DateFormat('yyyyMMdd_HHmmss').format(now);
    final isoDateStr = DateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(now);

    final backup = BackupPayload(
      schemaVersion: 1,
      appName: 'Paisa',
      version: '3.0.0',
      exportedAt: isoDateStr,
      exportTimestamp: now.millisecondsSinceEpoch,
      account: PaisaExportAccount(name: profile.name, email: profile.email),
      userProfile: profile,
      transactions: transactions,
      budgets: budgets,
      recurringTransactions: recurringRules,
    );

    final jsonString = const JsonEncoder.withIndent('  ').convert(backup.toJson());
    final tempDir = await getTemporaryDirectory();
    final file = File('${tempDir.path}/Paisa_Backup_${dateStamp}_$timeStamp.json');
    await file.writeAsString(jsonString, encoding: utf8);
    return file;
  }

  static Future<void> shareJsonFile(File file) async {
    await Share.shareXFiles(
      [XFile(file.path)],
      subject: 'Paisa Financial Data Backup',
      text: 'Here is my machine-readable Paisa financial records backup.',
    );
  }

  static Future<JsonValidationResult> validateImportString(String jsonContent) async {
    try {
      if (jsonContent.trim().isEmpty) {
        return JsonValidationResult.error('The selected file is empty.');
      }

      final dynamic parsed = jsonDecode(jsonContent);
      if (parsed is! Map<String, dynamic>) {
        return JsonValidationResult.error("This doesn't appear to be a valid Paisa data file.");
      }

      final backup = BackupPayload.fromJson(parsed);

      if (backup.schemaVersion < 1) {
        return JsonValidationResult.error('Unsupported schema version (${backup.schemaVersion}).');
      }

      // Validate transactions
      for (final tx in backup.transactions) {
        if (tx.title.trim().isEmpty) {
          return JsonValidationResult.error('File contains transactions with empty titles.');
        }
        if (tx.amount.isNaN || tx.amount.isInfinite || tx.amount < 0) {
          return JsonValidationResult.error('File contains invalid transaction amounts.');
        }
      }

      final summary = ValidationSummary(
        transactionCount: backup.transactions.sizeSafe,
        budgetCount: backup.budgets.length,
        recurringCount: backup.recurringTransactions.length,
        accountName: backup.account.name.isNotEmpty ? backup.account.name : 'Unspecified',
        accountEmail: backup.account.email.isNotEmpty ? backup.account.email : 'Unspecified',
        exportDate: backup.exportedAt.isNotEmpty ? backup.exportedAt : 'Unknown Date',
      );

      return JsonValidationResult.success(backup, summary);
    } catch (e) {
      return JsonValidationResult.error('Invalid JSON structure: ${e.toString()}');
    }
  }

  /// Merges imported transactions, budgets, and recurring rules idempotently.
  static ({
    List<TransactionItem> transactions,
    List<BudgetModel> budgets,
    List<RecurringRule> recurringRules,
  }) mergeData({
    required List<TransactionItem> currentTransactions,
    required List<BudgetModel> currentBudgets,
    required List<RecurringRule> currentRecurringRules,
    required BackupPayload importBackup,
  }) {
    // 1. Transactions merge
    final existingSignatures = <String, TransactionItem>{};
    for (final tx in currentTransactions) {
      final sig = '${tx.title.trim().toLowerCase()}_${tx.amount}_${tx.type.name}_${tx.category.name}_${tx.timestamp}';
      existingSignatures[sig] = tx;
    }

    final mergedTxs = List<TransactionItem>.from(currentTransactions);
    for (final incoming in importBackup.transactions) {
      final sig = '${incoming.title.trim().toLowerCase()}_${incoming.amount}_${incoming.type.name}_${incoming.category.name}_${incoming.timestamp}';
      if (!existingSignatures.containsKey(sig)) {
        mergedTxs.add(incoming.copyWith(id: DateTime.now().microsecondsSinceEpoch % 1000000));
        existingSignatures[sig] = incoming;
      }
    }
    mergedTxs.sort((a, b) => b.timestamp.compareTo(a.timestamp));

    // 2. Budgets merge
    final budgetMap = <String, BudgetModel>{};
    for (final b in currentBudgets) {
      budgetMap[b.monthKey] = b;
    }
    for (final incoming in importBackup.budgets) {
      final existing = budgetMap[incoming.monthKey];
      if (existing == null) {
        budgetMap[incoming.monthKey] = incoming;
      } else {
        final mergedCat = Map<TransactionCategory, double>.from(existing.categoryBudgets)
          ..addAll(incoming.categoryBudgets);
        final newerTotal = (incoming.updatedAt >= existing.updatedAt && incoming.totalBudget > 0)
            ? incoming.totalBudget
            : existing.totalBudget;
        budgetMap[incoming.monthKey] = existing.copyWith(
          totalBudget: newerTotal,
          categoryBudgets: mergedCat,
          updatedAt: incoming.updatedAt > existing.updatedAt ? incoming.updatedAt : existing.updatedAt,
        );
      }
    }

    // 3. Recurring Rules merge
    final ruleSigs = <String, RecurringRule>{};
    for (final r in currentRecurringRules) {
      final sig = '${r.title.trim().toLowerCase()}_${r.amount}_${r.type.name}_${r.interval.name}';
      ruleSigs[sig] = r;
    }
    final mergedRules = List<RecurringRule>.from(currentRecurringRules);
    for (final incoming in importBackup.recurringTransactions) {
      final sig = '${incoming.title.trim().toLowerCase()}_${incoming.amount}_${incoming.type.name}_${incoming.interval.name}';
      if (!ruleSigs.containsKey(sig)) {
        mergedRules.add(incoming);
        ruleSigs[sig] = incoming;
      }
    }

    return (
      transactions: mergedTxs,
      budgets: budgetMap.values.toList(),
      recurringRules: mergedRules,
    );
  }
}

extension on List<TransactionItem> {
  int get sizeSafe => length;
}
