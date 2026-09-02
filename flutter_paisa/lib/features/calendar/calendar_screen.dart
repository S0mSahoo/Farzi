import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:table_calendar/table_calendar.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/currency_formatter.dart';
import '../../core/utils/date_formatter.dart';
import '../../data/models/transaction_item.dart';
import '../../state/finance_notifier.dart';

class CalendarScreen extends ConsumerStatefulWidget {
  const CalendarScreen({super.key});

  @override
  ConsumerState<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends ConsumerState<CalendarScreen> {
  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay;

  @override
  void initState() {
    super.initState();
    _selectedDay = _focusedDay;
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(financeProvider);
    final theme = Theme.of(context);
    final symbol = state.userProfile.currencySymbol;

    // Filter transactions for the selected day
    final dayTransactions = state.transactions.where((t) {
      if (_selectedDay == null) return false;
      final d = DateTime.fromMillisecondsSinceEpoch(t.timestamp);
      return d.year == _selectedDay!.year && d.month == _selectedDay!.month && d.day == _selectedDay!.day;
    }).toList();

    final dayIncome = dayTransactions.where((t) => t.type == TransactionType.INCOME).fold(0.0, (s, t) => s + t.amount);
    final dayExpense = dayTransactions.where((t) => t.type == TransactionType.EXPENSE).fold(0.0, (s, t) => s + t.amount);

    return Scaffold(
      appBar: AppBar(title: const Text('Calendar')),
      body: SafeArea(
        child: Column(
          children: [
            // Calendar Widget
            TableCalendar(
              firstDay: DateTime(2020),
              lastDay: DateTime(2030),
              focusedDay: _focusedDay,
              selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
              onDaySelected: (selectedDay, focusedDay) {
                setState(() {
                  _selectedDay = selectedDay;
                  _focusedDay = focusedDay;
                });
              },
              calendarStyle: CalendarStyle(
                selectedDecoration: BoxDecoration(
                  color: theme.colorScheme.primary,
                  shape: BoxShape.circle,
                ),
                todayDecoration: BoxDecoration(
                  color: theme.colorScheme.primary.withOpacity(0.35),
                  shape: BoxShape.circle,
                ),
              ),
              headerStyle: const HeaderStyle(
                formatButtonVisible: false,
                titleCentered: true,
              ),
            ),
            const Divider(height: 1),

            // Daily Summary Bar
            if (_selectedDay != null)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                color: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      DateFormatter.toDateString(_selectedDay!),
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                    ),
                    Row(
                      children: [
                        Text('+$symbol${dayIncome.toStringAsFixed(0)}', style: const TextStyle(color: AppColors.incomeGreen, fontWeight: FontWeight.bold, fontSize: 13)),
                        const SizedBox(width: 8),
                        Text('-$symbol${dayExpense.toStringAsFixed(0)}', style: const TextStyle(color: AppColors.expenseRed, fontWeight: FontWeight.bold, fontSize: 13)),
                      ],
                    ),
                  ],
                ),
              ),

            // Daily Transactions List
            Expanded(
              child: dayTransactions.isEmpty
                  ? Center(
                      child: Text('No records on this date', style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.5))),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.only(left: 20, right: 20, top: 12, bottom: 96),
                      itemCount: dayTransactions.length,
                      itemBuilder: (context, index) {
                        final tx = dayTransactions[index];
                        final isInc = tx.type == TransactionType.INCOME;

                        return Container(
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: theme.colorScheme.surface,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
                          ),
                          child: Row(
                            children: [
                              Container(
                                width: 36,
                                height: 36,
                                decoration: BoxDecoration(
                                  color: isInc ? AppColors.incomeGreen.withOpacity(0.12) : AppColors.expenseRed.withOpacity(0.12),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Icon(isInc ? Icons.arrow_downward : Icons.shopping_bag_outlined, color: isInc ? AppColors.incomeGreen : AppColors.expenseRed, size: 18),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Text(tx.title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                              ),
                              Text(
                                '${isInc ? '+' : '-'} ${CurrencyFormatter.format(tx.amount, symbol: symbol)}',
                                style: TextStyle(
                                  fontWeight: FontWeight.w900,
                                  fontSize: 14,
                                  color: isInc ? AppColors.incomeGreen : AppColors.expenseRed,
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
