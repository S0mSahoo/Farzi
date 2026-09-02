import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/currency_formatter.dart';
import '../../core/utils/date_formatter.dart';
import '../../state/finance_notifier.dart';

class BudgetScreen extends ConsumerWidget {
  const BudgetScreen({super.key});

  void _showSetBudgetDialog(BuildContext context, WidgetRef ref) {
    final state = ref.read(financeProvider);
    final monthKey = DateFormatter.toMonthKey(state.selectedMonth);
    final currentBudget = state.currentMonthBudget;
    final controller = TextEditingController(text: currentBudget != null && currentBudget.totalBudget > 0 ? currentBudget.totalBudget.toStringAsFixed(0) : '');

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Set Budget for ${DateFormatter.toShortMonthYear(state.selectedMonth)}'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: 'Monthly Limit (₹)',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () {
              final amount = double.tryParse(controller.text.trim()) ?? 0.0;
              ref.read(financeProvider.notifier).setMonthBudget(monthKey, amount, {});
              Navigator.pop(ctx);
            },
            child: const Text('Save Budget'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(financeProvider);
    final theme = Theme.of(context);
    final symbol = state.userProfile.currencySymbol;

    final budget = state.currentMonthBudget;
    final totalLimit = budget?.totalBudget ?? 0.0;
    final spent = state.currentMonthExpense;
    final remaining = (totalLimit - spent).clamp(0.0, double.infinity);
    final percent = totalLimit > 0 ? (spent / totalLimit).clamp(0.0, 1.0) : 0.0;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Budgets & Limits'),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit_outlined),
            onPressed: () => _showSetBudgetDialog(context, ref),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.only(left: 20, right: 20, top: 16, bottom: 96),
          children: [
            // Overall Month Budget Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: theme.colorScheme.surface,
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(DateFormatter.toMonthYear(state.selectedMonth), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: (spent > totalLimit && totalLimit > 0) ? AppColors.expenseRed.withOpacity(0.15) : AppColors.incomeGreen.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          totalLimit > 0 ? '${(percent * 100).toStringAsFixed(0)}% Used' : 'No Limit',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: (spent > totalLimit && totalLimit > 0) ? AppColors.expenseRed : AppColors.incomeGreen,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  LinearProgressIndicator(
                    value: percent,
                    minHeight: 10,
                    borderRadius: BorderRadius.circular(8),
                    backgroundColor: theme.colorScheme.surfaceContainerHighest,
                    color: (spent > totalLimit && totalLimit > 0) ? AppColors.expenseRed : theme.colorScheme.primary,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Spent', style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.6))),
                          Text(CurrencyFormatter.format(spent, symbol: symbol), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
                        ],
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text('Limit', style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.6))),
                          Text(CurrencyFormatter.format(totalLimit, symbol: symbol), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Advice Banner
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.minimalBlue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.minimalBlue.withOpacity(0.2)),
              ),
              child: Row(
                children: [
                  const Icon(Icons.info_outline_rounded, color: AppColors.minimalBlue, size: 22),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      totalLimit > 0
                          ? 'You have ${CurrencyFormatter.format(remaining, symbol: symbol)} left for this month.'
                          : 'Tap the edit icon at top right to configure a monthly spending budget.',
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
