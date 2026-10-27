package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paisa Design System - Consistent Violet to Purplish UI Theme
 * with Glassmorphism, Pure AMOLED Dark Mode, and Vibrant Multi-Color Data Palette.
 */

// Violet / Purplish Brand Palette
val VioletPrimary = Color(0xFF8B5CF6)              // Vivid Violet
val VioletPrimaryDark = Color(0xFF7C3AED)          // Deep Violet
val VioletContainerDark = Color(0xFF4C1D95)        // Rich Deep Purple Container
val VioletContainerLight = Color(0xFFF3E8FF)       // Soft Lavender Container
val VioletOnContainerDark = Color(0xFFEDE9FE)
val VioletOnContainerLight = Color(0xFF3B0764)
val VioletAccent = Color(0xFFA855F7)               // Vivid Purple Accent
val LilacGlow = Color(0xFFC084FC)                   // Soft Lilac Glow

// Dark Theme - True AMOLED Pure Black
val DarkBackground = Color(0xFF000000)             // Pure #000000 AMOLED Black
val DarkSurface = Color(0xFF13111C)                // Deep Violet-tinted dark surface
val DarkSurfaceContainerLow = Color(0xFF1A162B)    // Subtle low container card
val DarkSurfaceContainer = Color(0xFF241E3A)       // Interactive glass container
val DarkSurfaceContainerHigh = Color(0xFF322A4E)   // Raised glass container
val DarkSurfaceContainerHighest = Color(0xFF3E3460)
val DarkSurfaceOutline = Color(0xFF3A3056)         // Glass border stroke
val DarkPrimary = Color(0xFFA78BFA)                // Crisp Violet Primary
val DarkOnPrimary = Color(0xFF2E1065)
val DarkPrimaryContainer = Color(0xFF4C1D95)       // Deep Violet container
val DarkOnPrimaryContainer = Color(0xFFEDE9FE)
val DarkSecondary = Color(0xFFC084FC)              // Lilac Accent
val DarkSecondaryContainer = Color(0xFF581C87)
val DarkTertiary = Color(0xFFE9D5FF)               // Light Lilac
val DarkOnTertiary = Color(0xFF3B0764)
val DarkPrimaryText = Color(0xFFF8FAFC)            // Crisp white text
val DarkSecondaryText = Color(0xFFA5B4FC)          // Muted lavender text
val DarkTertiaryText = Color(0xFF818CF8)           // Caption text
val DarkDivider = Color(0xFF231C38)                // Divider line

// Light Theme - Glass & Layered Canvas
val LightBackground = Color(0xFFF8FAFC)            // Soft slate canvas
val LightSurface = Color(0xFFFFFFFF)               // Pure white card surface
val LightSurfaceContainerLow = Color(0xFFF3E8FF)   // Soft low lavender container
val LightSurfaceContainer = Color(0xFFE9D5FF)       // Interactive container
val LightSurfaceContainerHigh = Color(0xFFDDD6FE)    // Raised container
val LightSurfaceContainerHighest = Color(0xFFC4B5FD)
val LightSurfaceOutline = Color(0xFFE9D5FF)        // Glass border stroke
val LightPrimary = Color(0xFF7C3AED)               // Rich Violet Primary
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFF3E8FF)      // Light lavender container
val LightOnPrimaryContainer = Color(0xFF3B0764)
val LightSecondary = Color(0xFF9333EA)             // Deep Purple Accent
val LightSecondaryContainer = Color(0xFFFAF5FF)
val LightTertiary = Color(0xFFA855F7)              // Lavender Accent
val LightOnTertiary = Color(0xFFFFFFFF)
val LightPrimaryText = Color(0xFF0F172A)           // Deep slate body text
val LightSecondaryText = Color(0xFF4C1D95)         // Purple subtitle text
val LightTertiaryText = Color(0xFF6B21A8)          // Caption text
val LightDivider = Color(0xFFE9D5FF)

// Semantic Financial Indicators (Kept strictly for Income & Expense data)
val IncomeGreen = Color(0xFF10B981)                // Vibrant Emerald Income
val ExpenseRed = Color(0xFFF43F5E)                 // Radiant Rose/Red Expense
val WarningAmber = Color(0xFFF59E0B)               // Warm Amber Warning
val ErrorContainer = Color(0xFF881337)
val OnErrorContainer = Color(0xFFFFE4E6)
val LightErrorContainer = Color(0xFFFFE4E6)
val LightOnErrorContainer = Color(0xFF9F1239)
val DisabledContent = Color(0xFF64748B)
val LightDisabledContent = Color(0xFF94A3B8)

// Vibrant Data Category Spectrum
val AccentAmber = Color(0xFFF59E0B)               // Food & Dining / Bonus
val AccentIndigo = Color(0xFF6366F1)              // Housing & Rent / Business
val AccentSky = Color(0xFF0EA5E9)                 // Transport / Travel
val AccentPurple = Color(0xFF8B5CF6)              // Subscriptions / Utilities
val AccentPink = Color(0xFFEC4899)                // Shopping / Gifts
val AccentTeal = Color(0xFF14B8A6)                // Freelance / Fitness
val AccentOrange = Color(0xFFF97316)              // Investments / Entertainment
val AccentLime = Color(0xFF84CC16)                // Groceries / Health
val AccentRose = Color(0xFFF43F5E)                // Healthcare / Expenses
val AccentCyan = Color(0xFF06B6D4)                // Education / Net Banking
val EmeraldPrimary = Color(0xFF10B981)            // Salary / Green Accent
val Slate500 = Color(0xFF64748B)                  // Other

// Compatibility Aliases
val RoyalIndigo = VioletPrimaryDark
val DarkRoyalIndigo = VioletPrimaryDark
val DeepIndigoContainer = VioletContainerDark
val LightIndigoContainer = VioletContainerLight
val OnIndigoContainerText = VioletOnContainerDark
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
