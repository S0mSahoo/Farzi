import 'package:flutter/material.dart';

class AppColors {
  // Brand accents
  static const Color primary = Color(0xFF6366F1); // Indigo Primary
  static const Color primaryLight = Color(0xFF818CF8);
  static const Color primaryDark = Color(0xFF4F46E5);

  // Functional colors
  static const Color incomeGreen = Color(0xFF10B981);
  static const Color incomeGreenDark = Color(0xFF059669);
  static const Color expenseRed = Color(0xFFEF4444);
  static const Color expenseRedDark = Color(0xFFDC2626);
  static const Color minimalBlue = Color(0xFF3B82F6);
  static const Color warningOrange = Color(0xFFF59E0B);

  // AMOLED Dark Surface Hierarchy
  static const Color darkCanvas = Color(0xFF000000);
  static const Color darkSurface1 = Color(0xFF0A0A0A);
  static const Color darkSurface2 = Color(0xFF141414);
  static const Color darkSurface3 = Color(0xFF1F1F1F);
  static const Color darkBorder = Color(0xFF2E2E2E);

  // Light Mode Surfaces
  static const Color lightCanvas = Color(0xFFF8FAFC);
  static const Color lightSurface1 = Color(0xFFFFFFFF);
  static const Color lightSurface2 = Color(0xFFF1F5F9);
  static const Color lightBorder = Color(0xFFE2E8F0);
}

class AppTheme {
  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      scaffoldBackgroundColor: AppColors.lightCanvas,
      colorScheme: const ColorScheme.light(
        primary: AppColors.primary,
        onPrimary: Colors.white,
        primaryContainer: Color(0xFFEEF2FF),
        onPrimaryContainer: AppColors.primaryDark,
        secondary: AppColors.minimalBlue,
        surface: AppColors.lightSurface1,
        onSurface: Color(0xFF0F172A),
        surfaceContainerHighest: AppColors.lightSurface2,
        outline: AppColors.lightBorder,
        error: AppColors.expenseRed,
      ),
      cardTheme: CardTheme(
        color: AppColors.lightSurface1,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(color: AppColors.lightBorder, width: 1),
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.lightCanvas,
        scrolledUnderElevation: 0,
        elevation: 0,
        titleTextStyle: TextStyle(
          color: Color(0xFF0F172A),
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  static ThemeData get amoledDarkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.darkCanvas,
      colorScheme: const ColorScheme.dark(
        primary: AppColors.primaryLight,
        onPrimary: Colors.black,
        primaryContainer: Color(0xFF312E81),
        onPrimaryContainer: Color(0xFFE0E7FF),
        secondary: AppColors.minimalBlue,
        surface: AppColors.darkSurface1,
        onSurface: Color(0xFFF8FAFC),
        surfaceContainerHighest: AppColors.darkSurface2,
        outline: AppColors.darkBorder,
        error: AppColors.expenseRed,
      ),
      cardTheme: CardTheme(
        color: AppColors.darkSurface1,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(color: AppColors.darkBorder, width: 1),
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.darkCanvas,
        scrolledUnderElevation: 0,
        elevation: 0,
        titleTextStyle: TextStyle(
          color: Colors.white,
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
