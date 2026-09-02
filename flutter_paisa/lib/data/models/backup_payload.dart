import 'budget_model.dart';
import 'recurring_rule.dart';
import 'transaction_item.dart';
import 'user_profile.dart';

class PaisaExportAccount {
  final String name;
  final String email;

  const PaisaExportAccount({this.name = '', this.email = ''});

  Map<String, dynamic> toJson() => {'name': name, 'email': email};

  factory PaisaExportAccount.fromJson(Map<String, dynamic> json) {
    return PaisaExportAccount(
      name: json['name'] as String? ?? '',
      email: json['email'] as String? ?? '',
    );
  }
}

class BackupPayload {
  final int schemaVersion;
  final String appName;
  final String version;
  final String exportedAt;
  final int exportTimestamp;
  final PaisaExportAccount account;
  final UserProfile userProfile;
  final List<TransactionItem> transactions;
  final List<BudgetModel> budgets;
  final List<RecurringRule> recurringTransactions;

  BackupPayload({
    this.schemaVersion = 1,
    this.appName = 'Paisa',
    this.version = '3.0.0',
    this.exportedAt = '',
    required this.exportTimestamp,
    this.account = const PaisaExportAccount(),
    this.userProfile = const UserProfile(),
    this.transactions = const [],
    this.budgets = const [],
    this.recurringTransactions = const [],
  });

  Map<String, dynamic> toJson() {
    return {
      'schemaVersion': schemaVersion,
      'appName': appName,
      'version': version,
      'exportedAt': exportedAt,
      'exportTimestamp': exportTimestamp,
      'account': account.toJson(),
      'userProfile': userProfile.toJson(),
      'transactions': transactions.map((e) => e.toJson()).toList(),
      'budgets': budgets.map((e) => e.toJson()).toList(),
      'recurringTransactions': recurringTransactions.map((e) => e.toJson()).toList(),
    };
  }

  factory BackupPayload.fromJson(Map<String, dynamic> json) {
    final txList = (json['transactions'] as List<dynamic>? ?? [])
        .map((e) => TransactionItem.fromJson(e as Map<String, dynamic>))
        .toList();

    final bgList = (json['budgets'] as List<dynamic>? ?? [])
        .map((e) => BudgetModel.fromJson(e as Map<String, dynamic>))
        .toList();

    final rcList = (json['recurringTransactions'] as List<dynamic>? ?? [])
        .map((e) => RecurringRule.fromJson(e as Map<String, dynamic>))
        .toList();

    final acc = json['account'] != null
        ? PaisaExportAccount.fromJson(json['account'] as Map<String, dynamic>)
        : const PaisaExportAccount();

    final prof = json['userProfile'] != null
        ? UserProfile.fromJson(json['userProfile'] as Map<String, dynamic>)
        : UserProfile(name: acc.name, email: acc.email);

    return BackupPayload(
      schemaVersion: json['schemaVersion'] is int ? json['schemaVersion'] : 1,
      appName: json['appName'] as String? ?? 'Paisa',
      version: json['version'] as String? ?? '3.0.0',
      exportedAt: json['exportedAt'] as String? ?? '',
      exportTimestamp: json['exportTimestamp'] is int ? json['exportTimestamp'] : DateTime.now().millisecondsSinceEpoch,
      account: acc,
      userProfile: prof,
      transactions: txList,
      budgets: bgList,
      recurringTransactions: rcList,
    );
  }
}
