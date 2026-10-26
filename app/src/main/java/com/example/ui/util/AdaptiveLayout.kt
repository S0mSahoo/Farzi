package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 Window Width Size Classes
 */
enum class WindowWidthSizeClass {
  COMPACT,   // < 600 dp (typical phones in portrait)
  MEDIUM,    // 600 - 839 dp (foldables unfolded, tablets portrait, landscape phones)
  EXPANDED   // >= 840 dp (tablets landscape, desktop, large foldables)
}

/**
 * Standard Material 3 Window Height Size Classes
 */
enum class WindowHeightSizeClass {
  COMPACT,   // < 480 dp (landscape phones)
  MEDIUM,    // 480 - 899 dp (typical portrait phones, landscape tablets)
  EXPANDED   // >= 900 dp (tall tablets in portrait)
}

/**
 * Primary navigation affordance based on available screen space
 */
enum class AppNavigationType {
  BOTTOM_BAR,        // Compact width with standard height
  NAVIGATION_RAIL,   // Medium width or compact height (landscape phone)
  PERMANENT_DRAWER   // Expanded width with adequate height
}

/**
 * Encapsulates responsive window metrics and layout recommendations
 */
data class WindowAdaptiveInfo(
  val width: Dp,
  val height: Dp,
  val widthSizeClass: WindowWidthSizeClass,
  val heightSizeClass: WindowHeightSizeClass,
  val navigationType: AppNavigationType,
  val isLandscapePhone: Boolean,
  val useTwoPaneLayout: Boolean
)

/**
 * CompositionLocal providing current window adaptive configuration to descendant composables
 */
val LocalAdaptiveInfo = compositionLocalOf<WindowAdaptiveInfo> {
  WindowAdaptiveInfo(
    width = 360.dp,
    height = 640.dp,
    widthSizeClass = WindowWidthSizeClass.COMPACT,
    heightSizeClass = WindowHeightSizeClass.MEDIUM,
    navigationType = AppNavigationType.BOTTOM_BAR,
    isLandscapePhone = false,
    useTwoPaneLayout = false
  )
}

/**
 * Computes adaptive configuration from current window dimensions
 */
fun calculateAdaptiveInfo(width: Dp, height: Dp): WindowAdaptiveInfo {
  val widthSizeClass = when {
    width < 600.dp -> WindowWidthSizeClass.COMPACT
    width < 840.dp -> WindowWidthSizeClass.MEDIUM
    else -> WindowWidthSizeClass.EXPANDED
  }

  val heightSizeClass = when {
    height < 480.dp -> WindowHeightSizeClass.COMPACT
    height < 900.dp -> WindowHeightSizeClass.MEDIUM
    else -> WindowHeightSizeClass.EXPANDED
  }

  // A landscape phone typically has medium/expanded width but very constrained height (< 480dp)
  val isLandscapePhone = (width >= 480.dp && height < 480.dp)

  val navigationType = when {
    isLandscapePhone -> AppNavigationType.NAVIGATION_RAIL
    widthSizeClass == WindowWidthSizeClass.COMPACT -> AppNavigationType.BOTTOM_BAR
    widthSizeClass == WindowWidthSizeClass.MEDIUM -> AppNavigationType.NAVIGATION_RAIL
    else -> AppNavigationType.PERMANENT_DRAWER
  }

  // Use two-pane master-detail/split when width is sufficient:
  // - On tablets with width >= 720dp (or medium width with standard height >= 600dp)
  // - On landscape phones with width >= 640dp, side-by-side or compact split is also viable
  val useTwoPaneLayout = (width >= 720.dp) || (width >= 600.dp && height >= 540.dp)

  return WindowAdaptiveInfo(
    width = width,
    height = height,
    widthSizeClass = widthSizeClass,
    heightSizeClass = heightSizeClass,
    navigationType = navigationType,
    isLandscapePhone = isLandscapePhone,
    useTwoPaneLayout = useTwoPaneLayout
  )
}
