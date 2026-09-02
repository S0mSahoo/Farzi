import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/date_formatter.dart';
import '../../state/finance_notifier.dart';
import '../../state/finance_state.dart';
import '../export/export_data_modal.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(financeProvider);
    final theme = Theme.of(context);
    final profile = state.userProfile;

    return Scaffold(
      appBar: AppBar(title: const Text('Settings & Cloud')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.only(left: 20, right: 20, top: 16, bottom: 96),
          children: [
            // Profile Card
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: theme.colorScheme.surface,
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
              ),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 28,
                    backgroundColor: theme.colorScheme.primaryContainer,
                    backgroundImage: profile.photoUrl.isNotEmpty ? NetworkImage(profile.photoUrl) : null,
                    child: profile.photoUrl.isEmpty
                        ? Text(
                            profile.name.isNotEmpty ? profile.name[0].toUpperCase() : 'U',
                            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: theme.colorScheme.primary),
                          )
                        : null,
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          profile.name.isNotEmpty ? profile.name : 'Paisa User',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          profile.email.isNotEmpty ? profile.email : 'Local Account',
                          style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.6)),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Google Drive Cloud Sync Card
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: theme.colorScheme.surface,
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Icon(
                            state.syncStatus == CloudSyncStatus.syncing
                                ? Icons.sync_rounded
                                : Icons.cloud_done_rounded,
                            color: state.syncStatus == CloudSyncStatus.error ? AppColors.expenseRed : AppColors.incomeGreen,
                            size: 22,
                          ),
                          const SizedBox(width: 10),
                          const Text('Google Drive Sync', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                        ],
                      ),
                      TextButton(
                        onPressed: state.syncStatus == CloudSyncStatus.syncing
                            ? null
                            : () => ref.read(financeProvider.notifier).syncWithCloud(force: true),
                        child: state.syncStatus == CloudSyncStatus.syncing
                            ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                            : const Text('Sync Now'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    state.lastSyncTimestamp != null
                        ? 'Last synchronized: ${DateFormatter.formatTimestamp(state.lastSyncTimestamp!)}'
                        : 'Connected to private Google Drive AppData folder.',
                    style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.6)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Data Portability Card
            Container(
              decoration: BoxDecoration(
                color: theme.colorScheme.surface,
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
              ),
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.picture_as_pdf_rounded, color: AppColors.primary),
                    title: const Text('Export Statement & Portability', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    subtitle: const Text('Generate PDF or complete JSON backup', style: TextStyle(fontSize: 12)),
                    trailing: const Icon(Icons.chevron_right_rounded),
                    onTap: () {
                      showModalBottomSheet(
                        context: context,
                        isScrollControlled: true,
                        backgroundColor: Colors.transparent,
                        builder: (_) => const ExportDataModal(),
                      );
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Account & Sign Out Actions
            Container(
              decoration: BoxDecoration(
                color: theme.colorScheme.surface,
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: theme.colorScheme.outline.withOpacity(0.4)),
              ),
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.delete_forever_rounded, color: AppColors.expenseRed),
                    title: const Text('Clear All Local Records', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14, color: AppColors.expenseRed)),
                    onTap: () {
                      showDialog(
                        context: context,
                        builder: (ctx) => AlertDialog(
                          title: const Text('Clear All Records?'),
                          content: const Text('This will reset your local transactions and budget records.'),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
                            ElevatedButton(
                              onPressed: () {
                                ref.read(financeProvider.notifier).clearAllRecords();
                                Navigator.pop(ctx);
                              },
                              style: ElevatedButton.styleFrom(backgroundColor: AppColors.expenseRed, foregroundColor: Colors.white),
                              child: const Text('Clear'),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.logout_rounded, color: theme.colorScheme.onSurface),
                    title: const Text('Sign Out of Google', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    onTap: () {
                      ref.read(financeProvider.notifier).signOut();
                    },
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
