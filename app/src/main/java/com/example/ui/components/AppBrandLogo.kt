package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalIndigo

/**
 * Minimalist, geometric abstract brand mark for Paisa.
 * Composed of clean geometric curves and balanced financial trajectory nodes.
 */
@Composable
fun AppBrandLogo(
  modifier: Modifier = Modifier,
  size: Dp = 32.dp,
  primaryColor: Color = MaterialTheme.colorScheme.primary,
  accentColor: Color = IncomeGreen
) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height

      // Main structural geometric rounded card / arch
      val mainWidth = w * 0.58f
      val mainHeight = h * 0.72f
      val leftOffset = w * 0.16f
      val topOffset = h * 0.14f
      val cornerRadius = CornerRadius(w * 0.18f, h * 0.18f)

      drawRoundRect(
        color = primaryColor,
        topLeft = Offset(leftOffset, topOffset),
        size = Size(mainWidth, mainHeight),
        cornerRadius = cornerRadius
      )

      // Inner minimalist geometric cutout for high contrast
      val innerWidth = w * 0.30f
      val innerHeight = h * 0.36f
      val innerCorner = CornerRadius(w * 0.10f, h * 0.10f)

      drawRoundRect(
        color = Color.White.copy(alpha = 0.95f),
        topLeft = Offset(leftOffset + (mainWidth - innerWidth) / 2f, topOffset + h * 0.08f),
        size = Size(innerWidth, innerHeight),
        cornerRadius = innerCorner
      )

      // Dynamic upward geometric accent node
      val nodeRadius = w * 0.13f
      val nodeCenter = Offset(w * 0.76f, h * 0.72f)
      drawCircle(
        color = accentColor,
        radius = nodeRadius,
        center = nodeCenter
      )
    }
  }
}
