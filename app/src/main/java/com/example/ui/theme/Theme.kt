package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// ==========================================
// Material 3 Shapes Scale
// ==========================================
val Shapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(24.dp)
)

// ==========================================
// Farzi Material 3 Color Schemes
// ==========================================
private val M3DarkColorScheme = darkColorScheme(
  primary = DarkPrimaryAccent,
  onPrimary = DarkAccentOnColor,
  primaryContainer = DarkPrimaryAccent.copy(alpha = 0.2f),
  onPrimaryContainer = DarkPrimaryAccent,
  secondary = DarkSecondaryText,
  onSecondary = DarkSurface,
  secondaryContainer = DarkSurfaceOutline,
  onSecondaryContainer = DarkSecondaryText,
  tertiary = IncomeGreen,
  onTertiary = DarkBackground,
  tertiaryContainer = IncomeGreen.copy(alpha = 0.2f),
  onTertiaryContainer = IncomeGreen,
  background = DarkBackground,
  onBackground = DarkPrimaryText,
  surface = DarkSurface,
  onSurface = DarkPrimaryText,
  surfaceVariant = DarkElevatedSurface,
  onSurfaceVariant = DarkSecondaryText,
  outline = DarkSurfaceOutline,
  outlineVariant = DarkDivider,
  error = ExpenseRed,
  onError = DarkBackground,
  errorContainer = ExpenseRed.copy(alpha = 0.2f),
  onErrorContainer = ExpenseRed
)

private val M3LightColorScheme = lightColorScheme(
  primary = LightPrimaryAccent,
  onPrimary = LightAccentOnColor,
  primaryContainer = LightPrimaryAccent.copy(alpha = 0.2f),
  onPrimaryContainer = LightPrimaryAccent,
  secondary = LightSecondaryText,
  onSecondary = LightSurface,
  secondaryContainer = LightSurfaceOutline,
  onSecondaryContainer = LightSecondaryText,
  tertiary = IncomeGreen,
  onTertiary = LightSurface,
  tertiaryContainer = IncomeGreen.copy(alpha = 0.2f),
  onTertiaryContainer = IncomeGreen,
  background = LightBackground,
  onBackground = LightPrimaryText,
  surface = LightSurface,
  onSurface = LightPrimaryText,
  surfaceVariant = LightElevatedSurface,
  onSurfaceVariant = LightSecondaryText,
  outline = LightSurfaceOutline,
  outlineVariant = LightDivider,
  error = ExpenseRed,
  onError = LightSurface,
  errorContainer = ExpenseRed.copy(alpha = 0.2f),
  onErrorContainer = ExpenseRed
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disabled by default to fix theme issues
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> M3DarkColorScheme
    else -> M3LightColorScheme
  }


  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = Shapes,
    content = content
  )
}
