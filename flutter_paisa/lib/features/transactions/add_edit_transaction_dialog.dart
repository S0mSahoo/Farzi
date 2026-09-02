import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/theme/app_theme.dart';
import '../../data/models/transaction_item.dart';
import '../../state/finance_notifier.dart';

class AddEditTransactionDialog extends ConsumerStatefulWidget {
  final TransactionItem? existingTransaction;

  const AddEditTransactionDialog({super.key, this.existingTransaction});

  @override
  ConsumerState<AddEditTransactionDialog> createState() => _AddEditTransactionDialogState();
}

class _AddEditTransactionDialogState extends ConsumerState<AddEditTransactionDialog> {
  late TransactionType _type;
  late TransactionCategory _category;
  late PaymentMethod _paymentMethod;
  late TextEditingController _titleController;
  late TextEditingController _amountController;
  late TextEditingController _noteController;
  late DateTime _selectedDate;

  @override
  void initState() {
    super.initState();
    final tx = widget.existingTransaction;
    _type = tx?.type ?? TransactionType.EXPENSE;
    _category = tx?.category ?? TransactionCategory.FOOD;
    _paymentMethod = tx?.paymentMethod ?? PaymentMethod.UPI;
    _titleController = TextEditingController(text: tx?.title ?? '');
    _amountController = TextEditingController(text: tx != null ? tx.amount.toStringAsFixed(0) : '');
    _noteController = TextEditingController(text: tx?.note ?? '');
    _selectedDate = tx != null ? DateTime.fromMillisecondsSinceEpoch(tx.timestamp) : DateTime.now();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _amountController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  void _save() {
    final title = _titleController.text.trim();
    final amount = double.tryParse(_amountController.text.trim()) ?? 0.0;

    if (title.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a title')),
      );
      return;
    }

    if (amount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a valid amount')),
      );
      return;
    }

    final notifier = ref.read(financeProvider.notifier);

    if (widget.existingTransaction != null) {
      final updated = widget.existingTransaction!.copyWith(
        title: title,
        amount: amount,
        type: _type,
        category: _category,
        paymentMethod: _paymentMethod,
        timestamp: _selectedDate.millisecondsSinceEpoch,
        note: _noteController.text.trim(),
        updatedAt: DateTime.now().millisecondsSinceEpoch,
      );
      notifier.updateTransaction(updated);
    } else {
      final newItem = TransactionItem(
        id: DateTime.now().microsecondsSinceEpoch % 1000000,
        title: title,
        amount: amount,
        type: _type,
        category: _category,
        paymentMethod: _paymentMethod,
        timestamp: _selectedDate.millisecondsSinceEpoch,
        note: _noteController.text.trim(),
      );
      notifier.addTransaction(newItem);
    }

    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isExpense = _type == TransactionType.EXPENSE;

    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      backgroundColor: theme.colorScheme.surface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      child: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    widget.existingTransaction != null ? 'Edit Record' : 'Add Record',
                    style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close_rounded),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const SizedBox(height: 12),

              // Segmented Switcher (Income vs Expense)
              Container(
                height: 44,
                decoration: BoxDecoration(
                  color: theme.colorScheme.surfaceContainerHighest.withOpacity(0.5),
                  borderRadius: BorderRadius.circular(14),
                ),
                padding: const EdgeInsets.all(3),
                child: Row(
                  children: [
                    Expanded(
                      child: GestureDetector(
                        onTap: () => setState(() => _type = TransactionType.EXPENSE),
                        child: Container(
                          decoration: BoxDecoration(
                            color: isExpense ? AppColors.expenseRed : Colors.transparent,
                            borderRadius: BorderRadius.circular(11),
                          ),
                          alignment: Alignment.Center,
                          child: Text(
                            'Expense',
                            style: TextStyle(
                              color: isExpense ? Colors.white : theme.colorScheme.onSurface,
                              fontWeight: isExpense ? FontWeight.bold : FontWeight.normal,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: GestureDetector(
                        onTap: () => setState(() => _type = TransactionType.INCOME),
                        child: Container(
                          decoration: BoxDecoration(
                            color: !isExpense ? AppColors.incomeGreen : Colors.transparent,
                            borderRadius: BorderRadius.circular(11),
                          ),
                          alignment: Alignment.Center,
                          child: Text(
                            'Income',
                            style: TextStyle(
                              color: !isExpense ? Colors.white : theme.colorScheme.onSurface,
                              fontWeight: !isExpense ? FontWeight.bold : FontWeight.normal,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Amount Field
              TextField(
                controller: _amountController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: isExpense ? AppColors.expenseRed : AppColors.incomeGreen,
                ),
                decoration: InputDecoration(
                  prefixText: '₹ ',
                  labelText: 'Amount',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                  filled: true,
                  fillColor: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
                ),
              ),
              const SizedBox(height: 14),

              // Title Field
              TextField(
                controller: _titleController,
                decoration: InputDecoration(
                  labelText: 'Title / Description',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                  filled: true,
                  fillColor: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
                ),
              ),
              const SizedBox(height: 14),

              // Category Selector
              DropdownButtonFormField<TransactionCategory>(
                value: _category,
                decoration: InputDecoration(
                  labelText: 'Category',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                  filled: true,
                  fillColor: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
                ),
                items: TransactionCategory.values.map((cat) {
                  return DropdownMenuItem(
                    value: cat,
                    child: Text(cat.name),
                  );
                }).toList(),
                onChanged: (val) {
                  if (val != null) setState(() => _category = val);
                },
              ),
              const SizedBox(height: 14),

              // Payment Method & Date Row
              Row(
                children: [
                  Expanded(
                    child: DropdownButtonFormField<PaymentMethod>(
                      value: _paymentMethod,
                      decoration: InputDecoration(
                        labelText: 'Mode',
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                        filled: true,
                        fillColor: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
                      ),
                      items: PaymentMethod.values.map((m) {
                        return DropdownMenuItem(value: m, child: Text(m.name));
                      }).toList>,
                      onChanged: (val) {
                        if (val != null) setState(() => _paymentMethod = val);
                      },
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        final picked = await showDatePicker(
                          context: context,
                          initialDate: _selectedDate,
                          firstDate: DateTime(2020),
                          lastDate: DateTime(2030),
                        );
                        if (picked != null) {
                          setState(() => _selectedDate = picked);
                        }
                      },
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                      icon: const Icon(Icons.calendar_today_rounded, size: 16),
                      label: Text(
                        DateFormat('dd MMM').format(_selectedDate),
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),

              // Save Button
              SizedBox(
                height: 50,
                child: ElevatedButton(
                  onPressed: _save,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: theme.colorScheme.primary,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  ),
                  child: const Text('Save Record', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
