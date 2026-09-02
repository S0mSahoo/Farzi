import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/date_formatter.dart';
import '../../data/models/backup_payload.dart';
import '../../data/services/json_portability_service.dart';
import '../../data/services/pdf_export_service.dart';
import '../../state/finance_notifier.dart';

class ExportDataModal extends ConsumerStatefulWidget {
  const ExportDataModal({super.key});

  @override
  ConsumerState<ExportDataModal> createState() => _ExportDataModalState();
}

class _ExportDataModalState extends ConsumerState<ExportDataModal> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  ExportPeriod _selectedPeriod = ExportPeriod.CURRENT_MONTH;
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _exportPdf() async {
    final state = ref.read(financeProvider);
    setState(() => _isProcessing = true);

    try {
      final periodTitle = _selectedPeriod == ExportPeriod.CURRENT_MONTH
          ? DateFormatter.toMonthYear(state.selectedMonth)
          : (_selectedPeriod == ExportPeriod.SELECTED_YEAR
              ? 'Year ${state.selectedMonth.year}'
              : 'All Time Records');

      final txs = _selectedPeriod == ExportPeriod.CURRENT_MONTH
          ? state.currentMonthTransactions
          : (_selectedPeriod == ExportPeriod.SELECTED_YEAR
              ? state.transactions.where((t) => DateTime.fromMillisecondsSinceEpoch(t.timestamp).year == state.selectedMonth.year).toList()
              : state.transactions);

      await PdfExportService.generateAndSharePdf(
        profile: state.userProfile,
        transactions: txs,
        periodTitle: periodTitle,
      );
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed: ${e.toString()}')));
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _exportJson() async {
    final state = ref.read(financeProvider);
    setState(() => _isProcessing = true);

    try {
      final file = await JsonPortabilityService.exportToJsonFile(
        profile: state.userProfile,
        transactions: state.transactions,
        budgets: state.budgets,
        recurringRules: state.recurringRules,
      );
      await JsonPortabilityService.shareJsonFile(file);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed: ${e.toString()}')));
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _pickAndImportJson() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['json'],
    );

    if (result != null && result.files.single.path != null) {
      final file = File(result.files.single.path!);
      final content = await file.readAsString();

      final validation = await JsonPortabilityService.validateImportString(content);
      if (!validation.isSuccess || validation.backup == null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(validation.errorMessage ?? 'Invalid backup file')),
          );
        }
        return;
      }

      if (mounted) {
        _showImportConfirmDialog(validation.backup!, validation.summary!);
      }
    }
  }

  void _showImportConfirmDialog(BackupPayload backup, ValidationSummary summary) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Import Paisa Backup?'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Valid backup identified. Existing data will be preserved and merged safely without duplicates.',
              style: TextStyle(fontSize: 13),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Theme.of(ctx).colorScheme.surfaceContainerHighest.withOpacity(0.5),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('• Transactions: ${summary.transactionCount} records', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                  Text('• Budget Plans: ${summary.budgetCount} months', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                  Text('• Recurring Rules: ${summary.recurringCount} rules', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                ],
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx);
              ref.read(financeProvider.notifier).applyImport(backup);
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Backup merged & synced successfully!')),
              );
            },
            child: const Text('Merge & Restore'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Data Export & Portability', style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                  Text('Share statements or backup data', style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurface.withOpacity(0.6))),
                ],
              ),
              IconButton(icon: const Icon(Icons.close), onPressed: () => Navigator.pop(context)),
            ],
          ),
          const SizedBox(height: 16),

          TabBar(
            controller: _tabController,
            tabs: const [
              Tab(icon: Icon(Icons.picture_as_pdf_rounded), text: 'PDF Statement'),
              Tab(icon: Icon(Icons.code_rounded), text: 'JSON Backup'),
            ],
          ),
          const SizedBox(height: 20),

          SizedBox(
            height: 220,
            child: TabBarView(
              controller: _tabController,
              children: [
                // PDF Tab
                Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    DropdownButtonFormField<ExportPeriod>(
                      value: _selectedPeriod,
                      decoration: InputDecoration(
                        labelText: 'Statement Period',
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
                      ),
                      items: const [
                        DropdownMenuItem(value: ExportPeriod.CURRENT_MONTH, child: Text('Selected Month')),
                        DropdownMenuItem(value: ExportPeriod.SELECTED_YEAR, child: Text('Selected Year')),
                        DropdownMenuItem(value: ExportPeriod.ALL_TIME, child: Text('All Time Records')),
                      ],
                      onChanged: (val) {
                        if (val != null) setState(() => _selectedPeriod = val);
                      },
                    ),
                    const Spacer(),
                    ElevatedButton.icon(
                      onPressed: _isProcessing ? null : _exportPdf,
                      icon: const Icon(Icons.share_rounded),
                      label: const Text('Generate & Share PDF'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                    ),
                  ],
                ),
                // JSON Tab
                Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                      'Export your entire financial records (transactions, budgets, and recurring rules) into portable JSON.',
                      style: TextStyle(fontSize: 13),
                    ),
                    const Spacer(),
                    ElevatedButton.icon(
                      onPressed: _isProcessing ? null : _exportJson,
                      icon: const Icon(Icons.file_download_rounded),
                      label: const Text('Export JSON Backup'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                    ),
                    const SizedBox(height: 10),
                    OutlinedButton.icon(
                      onPressed: _isProcessing ? null : _pickAndImportJson,
                      icon: const Icon(Icons.file_upload_rounded),
                      label: const Text('Import & Merge JSON Backup'),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
