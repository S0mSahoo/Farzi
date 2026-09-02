import 'dart:convert';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/backup_payload.dart';
import '../models/budget_model.dart';
import '../models/recurring_rule.dart';
import '../models/transaction_item.dart';
import '../models/user_profile.dart';
import '../services/google_drive_service.dart';

class FinanceRepository {
  final GoogleDriveService _driveService = GoogleDriveService();

  Future<SharedPreferences> get _prefs => SharedPreferences.getInstance();

  String _cacheKey(String email, String type) => 'paisa_${email.trim().toLowerCase()}_$type';

  // --- Local Scoped Cache ---
  Future<List<TransactionItem>> getLocalTransactions(String email) async {
    final prefs = await _prefs;
    final jsonStr = prefs.getString(_cacheKey(email, 'transactions'));
    if (jsonStr == null) return [];
    final list = jsonDecode(jsonStr) as List<dynamic>;
    return list.map((e) => TransactionItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<BudgetModel>> getLocalBudgets(String email) async {
    final prefs = await _prefs;
    final jsonStr = prefs.getString(_cacheKey(email, 'budgets'));
    if (jsonStr == null) return [];
    final list = jsonDecode(jsonStr) as List<dynamic>;
    return list.map((e) => BudgetModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<RecurringRule>> getLocalRecurringRules(String email) async {
    final prefs = await _prefs;
    final jsonStr = prefs.getString(_cacheKey(email, 'recurring'));
    if (jsonStr == null) return [];
    final list = jsonDecode(jsonStr) as List<dynamic>;
    return list.map((e) => RecurringRule.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<void> saveLocalData({
    required String email,
    required List<TransactionItem> transactions,
    required List<BudgetModel> budgets,
    required List<RecurringRule> recurringRules,
  }) async {
    final prefs = await _prefs;
    await prefs.setString(_cacheKey(email, 'transactions'), jsonEncode(transactions.map((e) => e.toJson()).toList()));
    await prefs.setString(_cacheKey(email, 'budgets'), jsonEncode(budgets.map((e) => e.toJson()).toList()));
    await prefs.setString(_cacheKey(email, 'recurring'), jsonEncode(recurringRules.map((e) => e.toJson()).toList()));
  }

  Future<void> clearLocalData(String email) async {
    final prefs = await _prefs;
    await prefs.remove(_cacheKey(email, 'transactions'));
    await prefs.remove(_cacheKey(email, 'budgets'));
    await prefs.remove(_cacheKey(email, 'recurring'));
  }

  // --- Google Drive Sync ---
  Future<BackupPayload?> fetchFromCloud(GoogleSignInAccount account) async {
    return await _driveService.loadCloudData(account);
  }

  Future<int> pushToCloud({
    required GoogleSignInAccount account,
    required UserProfile profile,
    required List<TransactionItem> transactions,
    required List<BudgetModel> budgets,
    required List<RecurringRule> recurringRules,
  }) async {
    final payload = BackupPayload(
      schemaVersion: 1,
      appName: 'Paisa',
      version: '3.0.0',
      exportTimestamp: DateTime.now().millisecondsSinceEpoch,
      userProfile: profile,
      account: PaisaExportAccount(name: profile.name, email: profile.email),
      transactions: transactions,
      budgets: budgets,
      recurringTransactions: recurringRules,
    );
    return await _driveService.saveCloudData(account, payload);
  }
}
