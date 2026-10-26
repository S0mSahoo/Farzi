package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Google Material 3 Color System
// Clean, trustworthy, and modern financial palette
// ==========================================

// Light Mode Google Palette
val GoogleBlue = Color(0xFF0B57D0)
val GoogleBlueContainer = Color(0xFFD3E3FD)
val GoogleOnBlueContainer = Color(0xFF041E49)

val GoogleTeal = Color(0xFF006A60)
val GoogleTealContainer = Color(0xFFCCE8E3)
val GoogleOnTealContainer = Color(0xFF00201D)

val GoogleBackgroundLight = Color(0xFFF8F9FA)
val GoogleSurfaceLight = Color(0xFFFFFFFF)
val GoogleSurfaceVariantLight = Color(0xFFEFF2F8)
val GoogleOutlineLight = Color(0xFFD2D6DC)
val GoogleOutlineVariantLight = Color(0xFFE6E9EE)
val GoogleTextPrimaryLight = Color(0xFF1F1F1F)
val GoogleTextSecondaryLight = Color(0xFF5E6368)
val GoogleTextMutedLight = Color(0xFF80868B)

// Dark Mode Google Palette (Sophisticated Deep Charcoal / Navy)
val GoogleBlueDark = Color(0xFF8AB4F8)
val GoogleBlueContainerDark = Color(0xFF0B306B)
val GoogleOnBlueContainerDark = Color(0xFFD3E3FD)

val GoogleTealDark = Color(0xFF509D93)
val GoogleTealContainerDark = Color(0xFF003731)
val GoogleOnTealContainerDark = Color(0xFF9DF2DE)

val GoogleBackgroundDark = Color(0xFF0F1117)
val GoogleSurfaceDark = Color(0xFF161A22)
val GoogleSurfaceVariantDark = Color(0xFF212630)
val GoogleSurfaceElevatedDark = Color(0xFF2C323E)
val GoogleOutlineDark = Color(0xFF373E4D)
val GoogleOutlineVariantDark = Color(0xFF232833)
val GoogleTextPrimaryDark = Color(0xFFECEFF4)
val GoogleTextSecondaryDark = Color(0xFF9099A8)
val GoogleTextMutedDark = Color(0xFF636E82)

// ==========================================
// Financial Semantic Colors (Accessible & Consistent)
// ==========================================

// Income / Positive
val IncomeGreen = Color(0xFF188038)
val IncomeGreenLight = Color(0xFF34A853)
val IncomeGreenDark = Color(0xFF5BB974)
val IncomeGreenContainer = Color(0xFFE6F4EA)
val IncomeGreenContainerDark = Color(0xFF0F381F)
val OnIncomeGreen = Color(0xFFFFFFFF)

// Expense / Negative
val ExpenseRed = Color(0xFFD93025)
val ExpenseRedLight = Color(0xFFEA4335)
val ExpenseRedDark = Color(0xFFF28B82)
val ExpenseRedContainer = Color(0xFFFCE8E6)
val ExpenseRedContainerDark = Color(0xFF5A1A18)
val OnExpenseRed = Color(0xFFFFFFFF)

// Warning / Caution / Approaching Limit
val WarningAmber = Color(0xFFE37400)
val WarningAmberDark = Color(0xFFFDD663)
val WarningAmberContainer = Color(0xFFFEF7E0)
val WarningAmberContainerDark = Color(0xFF422D00)

// Backward Compatibility Aliases for Existing Code
val DarkBackground = GoogleBackgroundDark
val DarkSurface = GoogleSurfaceDark
val DarkSurfaceVariant = GoogleSurfaceVariantDark
val DarkSurfaceElevated = GoogleSurfaceElevatedDark
val DarkBorder = GoogleOutlineDark
val DarkBorderLight = GoogleOutlineVariantDark
val DarkTextPrimary = GoogleTextPrimaryDark
val DarkTextSecondary = GoogleTextSecondaryDark
val DarkTextMuted = GoogleTextMutedDark
val AmoledBlack = Color(0xFF000000)
val AmoledBackground = DarkBackground

val LightBackground = GoogleBackgroundLight
val LightSurface = GoogleSurfaceLight
val LightSurfaceVariant = GoogleSurfaceVariantLight
val LightBorder = GoogleOutlineLight
val LightBorderDarker = GoogleOutlineVariantLight
val LightTextPrimary = GoogleTextPrimaryLight
val LightTextSecondary = GoogleTextSecondaryLight
val LightTextMuted = GoogleTextMutedLight

val MinimalEmerald = IncomeGreen
val MinimalEmeraldLight = IncomeGreenLight
val MinimalEmeraldBg = IncomeGreenContainer
val EmeraldPrimary = MinimalEmerald
val EmeraldLight = MinimalEmeraldLight
val EmeraldDark = Color(0xFF0D533B)
val EmeraldContainer = IncomeGreenContainer
val OnEmeraldContainer = Color(0xFF0D533B)

val MinimalRose = ExpenseRed
val MinimalRoseLight = ExpenseRedLight
val MinimalRoseBg = ExpenseRedContainer

val MinimalBlue = GoogleBlue
val MinimalBlueBg = GoogleBlueContainer

val MinimalAmber = WarningAmber
val MinimalAmberBg = WarningAmberContainer
val AccentAmber = WarningAmber
val AccentAmberContainer = WarningAmberContainer

val MinimalIndigo = Color(0xFF3F51B5)
val MinimalIndigoBg = Color(0xFFEDE7F6)
val MinimalPurple = Color(0xFF9334E6)
val MinimalPurpleBg = Color(0xFFF3E8FD)

val AccentIndigo = MinimalIndigo
val AccentIndigoContainer = MinimalIndigoBg
val AccentSky = MinimalBlue
val AccentSkyContainer = MinimalBlueBg
val AccentPurple = MinimalPurple
val AccentPurpleContainer = MinimalPurpleBg
val AccentTeal = Color(0xFF00796B)
val AccentOrange = Color(0xFFE8710A)
val AccentPink = Color(0xFFD01884)

val Gray50 = Color(0xFFF8F9FA)
val Gray100 = Color(0xFFF1F3F4)
val Gray200 = Color(0xFFE8EAED)
val Gray300 = Color(0xFFDADCE0)
val Gray400 = Color(0xFFBDC1C6)
val Gray500 = Color(0xFF9AA0A6)
val Gray600 = Color(0xFF80868B)
val Gray700 = Color(0xFF5F6368)
val Gray800 = Color(0xFF3C4043)
val Gray900 = Color(0xFF202124)

val Slate50 = Gray50
val Slate100 = Gray100
val Slate200 = Gray200
val Slate300 = Gray300
val Slate400 = Gray400
val Slate500 = Gray500
val Slate600 = Gray600
val Slate700 = Gray700
val Slate800 = Gray800
val Slate900 = Gray900
