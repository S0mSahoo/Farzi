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

private val DarkColorScheme = darkColorScheme(
  primary = Color.White,
  onPrimary = MinimalBlack,
  primaryContainer = Gray800,
  onPrimaryContainer = Color.White,
  secondary = Gray300,
  onSecondary = MinimalBlack,
  secondaryContainer = Gray700,
  onSecondaryContainer = Color.White,
  tertiary = MinimalEmeraldLight,
  onTertiary = MinimalBlack,
  background = DarkBackground,
  onBackground = Color(0xFFF1F5F9),
  surface = DarkSurface,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = DarkBorder,
  error = MinimalRose,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = MinimalBlack,
  onPrimary = Color.White,
  primaryContainer = Gray100,
  onPrimaryContainer = MinimalBlack,
  secondary = MinimalDark,
  onSecondary = Color.White,
  secondaryContainer = Gray200,
  onSecondaryContainer = MinimalBlack,
  tertiary = MinimalEmerald,
  onTertiary = Color.White,
  background = Gray50,
  onBackground = MinimalBlack,
  surface = Color.White,
  onSurface = MinimalBlack,
  surfaceVariant = Gray100,
  onSurfaceVariant = Gray500,
  outline = Gray200,
  error = MinimalRose,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep curated financial palette by default
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
