package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Refined Dark Color Scheme (Subtle dark layered tones with soft accents)
private val RefinedDarkColorScheme = darkColorScheme(
  primary = MinimalIndigo,
  onPrimary = Color.White,
  primaryContainer = DarkSurfaceElevated,
  onPrimaryContainer = Color.White,
  secondary = MinimalBlue,
  onSecondary = Color.White,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = DarkTextPrimary,
  tertiary = MinimalEmerald,
  onTertiary = Color.White,
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorder,
  outlineVariant = DarkBorderLight,
  error = MinimalRose,
  onError = Color.White
)

// Refined Clean Light Color Scheme (Soft slate & warm white)
private val RefinedLightColorScheme = lightColorScheme(
  primary = MinimalIndigo,
  onPrimary = Color.White,
  primaryContainer = MinimalIndigoBg,
  onPrimaryContainer = MinimalIndigo,
  secondary = MinimalBlue,
  onSecondary = Color.White,
  secondaryContainer = MinimalBlueBg,
  onSecondaryContainer = MinimalBlue,
  tertiary = MinimalEmerald,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = LightBorder,
  outlineVariant = LightBorderDarker,
  error = MinimalRose,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> RefinedDarkColorScheme
    else -> RefinedLightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
