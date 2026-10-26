package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paisa design tokens.
 *
 * Neutral surfaces keep financial figures readable; coral is reserved for primary
 * actions and expense emphasis, while category accents stay distinct in charts.
 */

// Dark surfaces
val DarkBackground = Color(0xFF0B0F16)
val DarkSurface = Color(0xFF121822)
val DarkSurfaceContainerLow = Color(0xFF0F141D)
val DarkSurfaceContainer = Color(0xFF171F2B)
val DarkSurfaceContainerHigh = Color(0xFF202A39)
val DarkSurfaceContainerHighest = Color(0xFF293548)
val DarkSurfaceOutline = Color(0xFF354255)
val DarkPrimary = Color(0xFFFF4963)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF421D2A)
val DarkOnPrimaryContainer = Color(0xFFFFDCE4)
val DarkSecondary = Color(0xFFBAC5D5)
val DarkSecondaryContainer = Color(0xFF253142)
val DarkTertiary = Color(0xFF73B9F5)
val DarkOnTertiary = Color(0xFF0B2438)
val DarkPrimaryText = Color(0xFFF4F6FA)
val DarkSecondaryText = Color(0xFFB0BBCB)
val DarkTertiaryText = Color(0xFF8390A4)
val DarkDivider = Color(0xFF273243)
val DarkNavSelectedBg = Color(0xFF321F2B)
val DarkNavSelectedFg = Color(0xFFFF7184)
val DarkNavUnselectedFg = Color(0xFF9AA6B8)

// Light surfaces
val LightBackground = Color(0xFFF5F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFBFCFE)
val LightSurfaceContainer = Color(0xFFEEF2F8)
val LightSurfaceContainerHigh = Color(0xFFE6EBF3)
val LightSurfaceContainerHighest = Color(0xFFDDE4EE)
val LightSurfaceOutline = Color(0xFFCDD5E2)
val LightPrimary = Color(0xFFE93651)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFE3E9)
val LightOnPrimaryContainer = Color(0xFF5F1424)
val LightSecondary = Color(0xFF596678)
val LightSecondaryContainer = Color(0xFFE8EDF5)
val LightTertiary = Color(0xFF1769AA)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightPrimaryText = Color(0xFF151B26)
val LightSecondaryText = Color(0xFF5D687A)
val LightTertiaryText = Color(0xFF778397)
val LightDivider = Color(0xFFE0E5ED)
val LightNavSelectedBg = Color(0xFFFFE6EB)
val LightNavSelectedFg = Color(0xFFB51F3B)
val LightNavUnselectedFg = Color(0xFF687588)

// Shared semantic colors
val IncomeGreen = Color(0xFF22B985)
val ExpenseRed = Color(0xFFF0445D)
val WarningAmber = Color(0xFFF0AA35)
val ErrorContainer = Color(0xFF4A2028)
val OnErrorContainer = Color(0xFFFFDAD7)
val LightErrorContainer = Color(0xFFFFDADF)
val LightOnErrorContainer = Color(0xFF690018)
val DisabledContent = Color(0xFF7D8981)
val LightDisabledContent = Color(0xFF6F8075)

// Chart/category accents: deliberately distinct for readable legends and category icons
val AccentAmber = Color(0xFFF4B33F)
val AccentIndigo = Color(0xFF7C83F6)
val AccentSky = Color(0xFF4AA7F2)
val AccentPurple = Color(0xFFAB79F5)
val AccentPink = Color(0xFFED75B2)
val AccentTeal = Color(0xFF31BFC0)
val AccentOrange = Color(0xFFF07A4B)
val EmeraldPrimary = Color(0xFF26B98C)
val Slate500 = Color(0xFF7B8798)

// Backward-compatibility aliases retained for existing screens and models.
val DarkTextPrimary = DarkPrimaryText
val DarkTextSecondary = DarkSecondaryText
val DarkTextTertiary = DarkTertiaryText
val LightTextPrimary = LightPrimaryText
val LightTextSecondary = LightSecondaryText
val LightTextTertiary = LightTertiaryText
val MinimalIndigo = AccentIndigo
val MinimalBlue = AccentSky
val MinimalPurple = AccentPurple
val MinimalPink = AccentPink
val MinimalEmerald = EmeraldPrimary
