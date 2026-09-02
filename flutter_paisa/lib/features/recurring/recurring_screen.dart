import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/currency_formatter.dart';
import '../../core/utils/date_formatter.dart';
import '../../data/models/recurring_rule.dart';
import '../../data/models/transaction_item.dart';
import '../../state/finance_notifier.dart';

class RecurringScreen extends ConsumerWidget {
  const RecurringScreen({super.key});

  void _showAddRuleDialog(BuildContext context, WidgetRef ref) {
    final titleController = TextEditingController();
    final amountController = TextEditingController();
    var type = TransactionType.EXPENSE;
    var interval = RecurrenceInterval.MONTHLY;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: const Text('Add Recurring Rule'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: titleController,
                decoration: const InputDecoration(labelText: 'Title (e.g. Netflix, Rent)'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: amountController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Amount (₹)'),
              ),
              const SizedBox(height: 10),
              DropdownButtonFormField<RecurrenceInterval>(
                value: interval,
                decoration: const InputDecoration(labelText: 'Frequency'),
                items: RecurrenceInterval.values.map((i) => DropdownMenuItem(value: i, child: Text(i.name))).toList(),
                onChanged: (v) {
                  if (v != null) setDialogState(() => interval = v);
                },
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
            ElevatedButton(
              onPressed: () {
                final amount = double.tryParse(amountController.text.trim()) ?? 0.0;
                final title = titleController.text.trim();
                if (title.isNotEmpty && amount > 0) {
                  ref.read(financeProvider.notifier).addRecurringRule(RecurringRule(
                    id: DateTime.now().microsecondsSinceEpoch % 1000000,
                    title: title,
                    amount: amount,
                    type: type,
                    category: TransactionCategory.BILLS,
                    interval: interval,
                    startDate: DateTime.now().millisecondsSinceEpoch,
                    lastProcessedDate: DateTime.now().millisecondsSinceEpoch,
                  ));
                  Navigator.pop(ctx);
                }
              },
              child: const Text('Add Rule'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(financeProvider);
    final theme = Theme.of(context);
    final symbol = state.userProfile.currencySymbol;
    final rules = state.recurringRules;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Recurring Automations'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => _showAddRuleDialog(context, ref),
          ),
        ],
      ),
      body: SafeArea(
        child: rules.isEmpty
            ? Center(
                child: Text('No recurring rules active', style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.5))),
              )
            : ListView.builder(
                padding: const EdgeInsets.only(left: 20, right: 20, top: 16, bottom: 96),
                itemCount: rules.length,
                itemBuilder: (context, index) {
                  final rule = rules[index];
                  return Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.surface,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
                    ),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppColors.primary.withOpacity(0.12),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(Icons.repeat_rounded, color: AppColors.primary, size: 22),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(rule.title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                              const SizedBox(height: 2),
                              Text(
                                '${rule.interval.name} • ${CurrencyFormatter.format(rule.amount, symbol: symbol)}',
                                style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.6)),
                              ),
                            ],
                          ),
                        ),
                        Switch(
                          value: rule.isActive,
                          onChanged: (_) => ref.read(financeProvider.notifier).toggleRecurringRule(rule.id),
                        ),
                      ],
                    ),
                  );
                },
              ),
      ),
    );
  }
}
