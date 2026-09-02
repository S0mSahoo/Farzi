import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/currency_formatter.dart';
import '../../core/utils/date_formatter.dart';
import '../../data/models/transaction_item.dart';
import '../../state/finance_notifier.dart';
import 'add_edit_transaction_dialog.dart';

class TransactionsScreen extends ConsumerStatefulWidget {
  const TransactionsScreen({super.key});

  @override
  ConsumerState<TransactionsScreen> createState() => _TransactionsScreenState();
}

class _TransactionsScreenState extends ConsumerState<TransactionsScreen> {
  String _searchQuery = '';
  TransactionType? _filterType;

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(financeProvider);
    final theme = Theme.of(context);
    final symbol = state.userProfile.currencySymbol;

    var filtered = state.currentMonthTransactions;
    if (_searchQuery.isNotEmpty) {
      filtered = filtered.where((t) => t.title.toLowerCase().contains(_searchQuery.toLowerCase()) || t.category.name.toLowerCase().contains(_searchQuery.toLowerCase())).toList();
    }
    if (_filterType != null) {
      filtered = filtered.where((t) => t.type == _filterType).toList();
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Transactions'),
      ),
      body: SafeArea(
        child: Column(
          children: [
            // Search & Filter Bar
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      onChanged: (v) => setState(() => _searchQuery = v),
                      decoration: InputDecoration(
                        hintText: 'Search records...',
                        prefixIcon: const Icon(Icons.search, size: 20),
                        contentPadding: const EdgeInsets.symmetric(vertical: 0, horizontal: 16),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
                        filled: true,
                        fillColor: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  // Filter Type Popup
                  PopupMenuButton<TransactionType?>(
                    initialValue: _filterType,
                    icon: Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.surfaceContainerHighest.withOpacity(0.4),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(Icons.filter_list_rounded, color: _filterType != null ? theme.colorScheme.primary : null),
                    ),
                    onSelected: (val) => setState(() => _filterType = val),
                    itemBuilder: (ctx) => const [
                      PopupMenuItem(value: null, child: Text('All Types')),
                      PopupMenuItem(value: TransactionType.EXPENSE, child: Text('Expenses Only')),
                      PopupMenuItem(value: TransactionType.INCOME, child: Text('Income Only')),
                    ],
                  ),
                ],
              ),
            ),

            // List of Transactions
            Expanded(
              child: filtered.isEmpty
                  ? Center(
                      child: Text('No matching transactions found', style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.5))),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.only(left: 20, right: 20, top: 8, bottom: 96),
                      itemCount: filtered.length,
                      itemBuilder: (context, index) {
                        final tx = filtered[index];
                        final isInc = tx.type == TransactionType.INCOME;

                        return Dismissible(
                          key: Key('tx_${tx.id}_${tx.timestamp}'),
                          direction: DismissDirection.endToStart,
                          background: Container(
                            alignment: Alignment.centerRight,
                            padding: const EdgeInsets.only(right: 20),
                            decoration: BoxDecoration(
                              color: AppColors.expenseRed,
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: const Icon(Icons.delete_outline_rounded, color: Colors.white),
                          ),
                          onDismissed: (_) {
                            ref.read(financeProvider.notifier).deleteTransaction(tx.id);
                          },
                          child: InkWell(
                            onTap: () {
                              showDialog(
                                context: context,
                                builder: (_) => AddEditTransactionDialog(existingTransaction: tx),
                              );
                            },
                            borderRadius: BorderRadius.circular(16),
                            child: Container(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: theme.colorScheme.surface,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
                              ),
                              child: Row(
                                children: [
                                  Container(
                                    width: 42,
                                    height: 42,
                                    decoration: BoxDecoration(
                                      color: isInc ? AppColors.incomeGreen.withOpacity(0.12) : AppColors.expenseRed.withOpacity(0.12),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: Icon(
                                      isInc ? Icons.arrow_downward_rounded : Icons.shopping_bag_outlined,
                                      color: isInc ? AppColors.incomeGreen : AppColors.expenseRed,
                                      size: 20,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(tx.title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                                        const SizedBox(height: 2),
                                        Text(
                                          '${tx.category.name} • ${tx.paymentMethod.name} • ${DateFormatter.toDateString(DateTime.fromMillisecondsSinceEpoch(tx.timestamp))}',
                                          style: TextStyle(fontSize: 11, color: theme.colorScheme.onSurface.withOpacity(0.6)),
                                        ),
                                      ],
                                    ),
                                  ),
                                  Text(
                                    '${isInc ? '+' : '-'} ${CurrencyFormatter.format(tx.amount, symbol: symbol)}',
                                    style: TextStyle(
                                      fontWeight: FontWeight.w900,
                                      fontSize: 15,
                                      color: isInc ? AppColors.incomeGreen : AppColors.expenseRed,
                                    ),
                                  ),
                                ],
                              ),
                            ),
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
