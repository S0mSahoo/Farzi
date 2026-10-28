package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// ==========================================
// Google Standard Typography - Plus Jakarta Sans & Outfit
// ==========================================

val PlusJakartaSansFont = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val OutfitFont = FontFamily(
  Font(R.font.outfit, FontWeight.Normal),
  Font(R.font.outfit, FontWeight.Medium),
  Font(R.font.outfit, FontWeight.SemiBold),
  Font(R.font.outfit, FontWeight.Bold)
)

val Typography = Typography(
  // Standout figures and display numbers
  displayLarge = TextStyle(
    fontFamily = OutfitFont,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 52.sp,
    letterSpacing = (-0.5).sp
  ),
  displayMedium = TextStyle(
    fontFamily = OutfitFont,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.25).sp
  ),
  displaySmall = TextStyle(
    fontFamily = OutfitFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    lineHeight = 38.sp,
    letterSpacing = 0.sp
  ),

  // Key screen titles and financial summaries
  headlineLarge = TextStyle(
    fontFamily = OutfitFont,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = 0.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = OutfitFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.sp
  ),
  headlineSmall = TextStyle(
    fontFamily = OutfitFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
  ),

  // Section titles, card headers
  titleLarge = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp
  ),
  titleMedium = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.1.sp
  ),
  titleSmall = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp
  ),

  // Body content, notes, descriptions
  bodyLarge = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.15.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.15.sp
  ),
  bodySmall = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp
  ),

  // Buttons, chips, status badges
  labelLarge = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp
  ),
  labelMedium = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.3.sp
  ),
  labelSmall = TextStyle(
    fontFamily = PlusJakartaSansFont,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.2.sp
  )
)
