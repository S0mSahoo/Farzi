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
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

// ==========================================
// Google-inspired Material 3 Dark Color Scheme
// ==========================================
private val M3DarkColorScheme = darkColorScheme(
  primary = GoogleBlueDark,
  onPrimary = Color(0xFF041E49),
  primaryContainer = GoogleBlueContainerDark,
  onPrimaryContainer = GoogleOnBlueContainerDark,
  secondary = GoogleTealDark,
  onSecondary = Color(0xFF003731),
  secondaryContainer = GoogleTealContainerDark,
  onSecondaryContainer = GoogleOnTealContainerDark,
  tertiary = IncomeGreenDark,
  onTertiary = Color(0xFF003919),
  tertiaryContainer = IncomeGreenContainerDark,
  onTertiaryContainer = Color(0xFF9DF2B5),
  background = GoogleBackgroundDark,
  onBackground = GoogleTextPrimaryDark,
  surface = GoogleSurfaceDark,
  onSurface = GoogleTextPrimaryDark,
  surfaceVariant = GoogleSurfaceVariantDark,
  onSurfaceVariant = GoogleTextSecondaryDark,
  outline = GoogleOutlineDark,
  outlineVariant = GoogleOutlineVariantDark,
  error = ExpenseRedDark,
  onError = Color(0xFF601410),
  errorContainer = ExpenseRedContainerDark,
  onErrorContainer = Color(0xFFF9DEDC)
)

// ==========================================
// Google-inspired Material 3 Light Color Scheme
// ==========================================
private val M3LightColorScheme = lightColorScheme(
  primary = GoogleBlue,
  onPrimary = Color.White,
  primaryContainer = GoogleBlueContainer,
  onPrimaryContainer = GoogleOnBlueContainer,
  secondary = GoogleTeal,
  onSecondary = Color.White,
  secondaryContainer = GoogleTealContainer,
  onSecondaryContainer = GoogleOnTealContainer,
  tertiary = IncomeGreen,
  onTertiary = Color.White,
  tertiaryContainer = IncomeGreenContainer,
  onTertiaryContainer = Color(0xFF07210E),
  background = GoogleBackgroundLight,
  onBackground = GoogleTextPrimaryLight,
  surface = GoogleSurfaceLight,
  onSurface = GoogleTextPrimaryLight,
  surfaceVariant = GoogleSurfaceVariantLight,
  onSurfaceVariant = GoogleTextSecondaryLight,
  outline = GoogleOutlineLight,
  outlineVariant = GoogleOutlineVariantLight,
  error = ExpenseRed,
  onError = Color.White,
  errorContainer = ExpenseRedContainer,
  onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
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
