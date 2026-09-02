import '../data/models/budget_model.dart';
import '../data/models/recurring_rule.dart';
import '../data/models/transaction_item.dart';
import '../data/models/user_profile.dart';

enum CloudSyncStatus {
  idle,
  syncing,
  success,
  error,
}

class FinanceState {
  final UserProfile userProfile;
  final List<TransactionItem> transactions;
  final List<BudgetModel> budgets;
  final List<RecurringRule> recurringRules;
  final DateTime selectedMonth;
  final CloudSyncStatus syncStatus;
  final String? syncError;
  final int? lastSyncTimestamp;
  final bool isLoading;

  const FinanceState({
    this.userProfile = const UserProfile(),
    this.transactions = const [],
    this.budgets = const [],
    this.recurringRules = const [],
    required this.selectedMonth,
    this.syncStatus = CloudSyncStatus.idle,
    this.syncError,
    this.lastSyncTimestamp,
    this.isLoading = false,
  });

  FinanceState copyWith({
    UserProfile? userProfile,
    List<TransactionItem>? transactions,
    List<BudgetModel>? budgets,
    List<RecurringRule>? recurringRules,
    DateTime? selectedMonth,
    CloudSyncStatus? syncStatus,
    String? syncError,
    int? lastSyncTimestamp,
    bool? isLoading,
  }) {
    return FinanceState(
      userProfile: userProfile ?? this.userProfile,
      transactions: transactions ?? this.transactions,
      budgets: budgets ?? this.budgets,
      recurringRules: recurringRules ?? this.recurringRules,
      selectedMonth: selectedMonth ?? this.selectedMonth,
      syncStatus: syncStatus ?? this.syncStatus,
      syncError: syncError,
      lastSyncTimestamp: lastSyncTimestamp ?? this.lastSyncTimestamp,
      isLoading: isLoading ?? this.isLoading,
    );
  }

  // Monthly filtered transactions
  List<TransactionItem> get currentMonthTransactions {
    final y = selectedMonth.year;
    final m = selectedMonth.month;
    return transactions.where((t) {
      final d = DateTime.fromMillisecondsSinceEpoch(t.timestamp);
      return d.year == y && d.month == m;
    }).toList();
  }

  double get currentMonthIncome {
    return currentMonthTransactions
        .where((t) => t.type == TransactionType.INCOME)
        .fold(0.0, (sum, t) => sum + t.amount);
  }

  double get currentMonthExpense {
    return currentMonthTransactions
        .where((t) => t.type == TransactionType.EXPENSE)
        .fold(0.0, (sum, t) => sum + t.amount);
  }

  double get currentMonthSavings => currentMonthIncome - currentMonthExpense;

  BudgetModel? get currentMonthBudget {
    final key = '${selectedMonth.year}-${selectedMonth.month.toString().padLeft(2, '0')}';
    try {
      return budgets.firstWhere((b) => b.monthKey == key);
    } catch (_) {
      return null;
    }
  }
}
