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
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

// ==========================================
// Farzi Material 3 Color Schemes
// ==========================================
private val M3DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = DarkOnPrimary,
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = DarkOnPrimaryContainer,
  secondary = DarkSecondary,
  onSecondary = DarkSurface,
  secondaryContainer = DarkSecondaryContainer,
  onSecondaryContainer = DarkSecondaryText,
  tertiary = DarkTertiary,
  onTertiary = DarkOnTertiary,
  tertiaryContainer = DarkSurfaceContainerHigh,
  onTertiaryContainer = DarkTertiaryText,
  background = DarkBackground,
  onBackground = DarkPrimaryText,
  surface = DarkSurface,
  onSurface = DarkPrimaryText,
  surfaceVariant = DarkSurfaceContainer,
  onSurfaceVariant = DarkSecondaryText,
  outline = DarkSurfaceOutline,
  outlineVariant = DarkDivider,
  error = ExpenseRed,
  onError = DarkBackground,
  errorContainer = ErrorContainer,
  onErrorContainer = OnErrorContainer
)

private val M3LightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = LightOnPrimary,
  primaryContainer = LightPrimaryContainer,
  onPrimaryContainer = LightOnPrimaryContainer,
  secondary = LightSecondary,
  onSecondary = LightSurface,
  secondaryContainer = LightSecondaryContainer,
  onSecondaryContainer = LightSecondaryText,
  tertiary = LightTertiary,
  onTertiary = LightOnTertiary,
  tertiaryContainer = LightSurfaceContainerHigh,
  onTertiaryContainer = LightTertiaryText,
  background = LightBackground,
  onBackground = LightPrimaryText,
  surface = LightSurface,
  onSurface = LightPrimaryText,
  surfaceVariant = LightSurfaceContainer,
  onSurfaceVariant = LightSecondaryText,
  outline = LightSurfaceOutline,
  outlineVariant = LightDivider,
  error = ExpenseRed,
  onError = LightSurface,
  errorContainer = LightErrorContainer,
  onErrorContainer = LightOnErrorContainer
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
