import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/models/backup_payload.dart';
import '../data/models/budget_model.dart';
import '../data/models/recurring_rule.dart';
import '../data/models/transaction_item.dart';
import '../data/models/user_profile.dart';
import '../data/repositories/finance_repository.dart';
import '../data/services/google_auth_service.dart';
import '../data/services/json_portability_service.dart';
import 'finance_state.dart';

final authServiceProvider = Provider<GoogleAuthService>((ref) => GoogleAuthService());
final financeRepositoryProvider = Provider<FinanceRepository>((ref) => FinanceRepository());

final financeProvider = StateNotifierProvider<FinanceNotifier, FinanceState>((ref) {
  final authService = ref.watch(authServiceProvider);
  final repo = ref.watch(financeRepositoryProvider);
  return FinanceNotifier(authService, repo);
});

class FinanceNotifier extends StateNotifier<FinanceState> {
  final GoogleAuthService _authService;
  final FinanceRepository _repo;

  FinanceNotifier(this._authService, this._repo)
      : super(FinanceState(selectedMonth: DateTime(DateTime.now().year, DateTime.now().month))) {
    _init();
  }

  Future<void> _init() async {
    final profile = await _authService.signInSilently();
    if (profile != null) {
      state = state.copyWith(userProfile: profile);
      await _loadLocalAndSyncWithCloud();
    }
  }

  Future<void> signIn() async {
    state = state.copyWith(isLoading: true);
    try {
      final profile = await _authService.signIn();
      if (profile != null) {
        state = state.copyWith(userProfile: profile, isLoading: false);
        await _loadLocalAndSyncWithCloud();
      } else {
        state = state.copyWith(isLoading: false);
      }
    } catch (e) {
      state = state.copyWith(isLoading: false, syncError: e.toString());
    }
  }

  Future<void> signOut() async {
    final email = state.userProfile.email;
    await _authService.signOut();
    if (email.isNotEmpty) {
      await _repo.clearLocalData(email);
    }
    state = FinanceState(selectedMonth: DateTime(DateTime.now().year, DateTime.now().month));
  }

  void selectMonth(DateTime date) {
    state = state.copyWith(selectedMonth: DateTime(date.year, date.month));
  }

  Future<void> _loadLocalAndSyncWithCloud() async {
    final email = state.userProfile.email;
    if (email.isEmpty) return;

    // 1. Load local cache first for instant rendering
    final txs = await _repo.getLocalTransactions(email);
    final bgs = await _repo.getLocalBudgets(email);
    final rcs = await _repo.getLocalRecurringRules(email);
    state = state.copyWith(
      transactions: txs,
      budgets: bgs,
      recurringRules: rcs,
    );

    // 2. Automatically sync with Google Drive
    await syncWithCloud();
  }

  Future<void> syncWithCloud({bool force = false}) async {
    final account = await _authService.getAuthenticatedAccount();
    if (account == null) return;

    state = state.copyWith(syncStatus: CloudSyncStatus.syncing, syncError: null);

    try {
      final cloudPayload = await _repo.fetchFromCloud(account);
      if (cloudPayload != null) {
        // Idempotently merge cloud data with local data
        final merged = JsonPortabilityService.mergeData(
          currentTransactions: state.transactions,
          currentBudgets: state.budgets,
          currentRecurringRules: state.recurringRules,
          importBackup: cloudPayload,
        );

        state = state.copyWith(
          transactions: merged.transactions,
          budgets: merged.budgets,
          recurringRules: merged.recurringRules,
          lastSyncTimestamp: cloudPayload.exportTimestamp,
        );

        await _repo.saveLocalData(
          email: account.email,
          transactions: merged.transactions,
          budgets: merged.budgets,
          recurringRules: merged.recurringRules,
        );
      } else {
        // No cloud backup yet, push local records to Drive
        final ts = await _repo.pushToCloud(
          account: account,
          profile: state.userProfile,
          transactions: state.transactions,
          budgets: state.budgets,
          recurringRules: state.recurringRules,
        );
        state = state.copyWith(lastSyncTimestamp: ts);
      }

      state = state.copyWith(syncStatus: CloudSyncStatus.success);
    } catch (e) {
      state = state.copyWith(
        syncStatus: CloudSyncStatus.error,
        syncError: e.toString(),
      );
    }
  }

  // Transaction Operations
  Future<void> addTransaction(TransactionItem item) async {
    final updated = List<TransactionItem>.from(state.transactions)..insert(0, item);
    updated.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    state = state.copyWith(transactions: updated);
    await _persist();
  }

  Future<void> updateTransaction(TransactionItem item) async {
    final updated = state.transactions.map((t) => t.id == item.id ? item : t).toList();
    updated.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    state = state.copyWith(transactions: updated);
    await _persist();
  }

  Future<void> deleteTransaction(int id) async {
    final updated = state.transactions.where((t) => t.id != id).toList();
    state = state.copyWith(transactions: updated);
    await _persist();
  }

  // Budget Operations
  Future<void> setMonthBudget(String monthKey, double totalBudget, Map<TransactionCategory, double> categories) async {
    final existingIndex = state.budgets.indexWhere((b) => b.monthKey == monthKey);
    final updatedBudgets = List<BudgetModel>.from(state.budgets);
    if (existingIndex >= 0) {
      updatedBudgets[existingIndex] = updatedBudgets[existingIndex].copyWith(
        totalBudget: totalBudget,
        categoryBudgets: categories,
        updatedAt: DateTime.now().millisecondsSinceEpoch,
      );
    } else {
      updatedBudgets.add(BudgetModel(
        monthKey: monthKey,
        totalBudget: totalBudget,
        categoryBudgets: categories,
      ));
    }
    state = state.copyWith(budgets: updatedBudgets);
    await _persist();
  }

  // Recurring Rules Operations
  Future<void> addRecurringRule(RecurringRule rule) async {
    final updated = List<RecurringRule>.from(state.recurringRules)..add(rule);
    state = state.copyWith(recurringRules: updated);
    await _persist();
  }

  Future<void> toggleRecurringRule(int id) async {
    final updated = state.recurringRules.map((r) => r.id == id ? r.copyWith(isActive: !r.isActive) : r).toList();
    state = state.copyWith(recurringRules: updated);
    await _persist();
  }

  Future<void> deleteRecurringRule(int id) async {
    final updated = state.recurringRules.where((r) => r.id != id).toList();
    state = state.copyWith(recurringRules: updated);
    await _persist();
  }

  // JSON Import & Portability
  Future<void> applyImport(BackupPayload backup) async {
    final merged = JsonPortabilityService.mergeData(
      currentTransactions: state.transactions,
      currentBudgets: state.budgets,
      currentRecurringRules: state.recurringRules,
      importBackup: backup,
    );

    state = state.copyWith(
      transactions: merged.transactions,
      budgets: merged.budgets,
      recurringRules: merged.recurringRules,
    );
    await _persist();
  }

  Future<void> clearAllRecords() async {
    state = state.copyWith(
      transactions: [],
      budgets: [],
      recurringRules: [],
    );
    await _persist();
  }

  Future<void> _persist() async {
    final email = state.userProfile.email;
    if (email.isEmpty) return;

    await _repo.saveLocalData(
      email: email,
      transactions: state.transactions,
      budgets: state.budgets,
      recurringRules: state.recurringRules,
    );

    final account = await _authService.getAuthenticatedAccount();
    if (account != null) {
      try {
        final ts = await _repo.pushToCloud(
          account: account,
          profile: state.userProfile,
          transactions: state.transactions,
          budgets: state.budgets,
          recurringRules: state.recurringRules,
        );
        state = state.copyWith(lastSyncTimestamp: ts);
      } catch (_) {}
    }
  }
}
