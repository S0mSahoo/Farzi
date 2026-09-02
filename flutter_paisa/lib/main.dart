import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/onboarding_screen.dart';
import 'features/main_navigation/main_scaffold.dart';
import 'state/finance_notifier.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ProviderScope(child: PaisaApp()));
}

class PaisaApp extends ConsumerWidget {
  const PaisaApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(financeProvider);
    final isAuthenticated = state.userProfile.email.isNotEmpty;

    return MaterialApp(
      title: 'Paisa',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.amoledDarkTheme,
      themeMode: ThemeMode.system, // Automatic system light vs AMOLED dark
      home: isAuthenticated ? const MainScaffold() : const OnboardingScreen(),
    );
  }
}
