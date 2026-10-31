package com.example.ui.util

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
  @Test
  fun compactPhoneUsesBottomNavigationAndSinglePane() {
    val info = calculateAdaptiveInfo(360.dp, 800.dp)
    assertEquals(WindowWidthSizeClass.COMPACT, info.widthSizeClass)
    assertEquals(AppNavigationType.BOTTOM_BAR, info.navigationType)
    assertFalse(info.useTwoPaneLayout)
  }

  @Test
  fun mediumTabletPortraitUsesRailAndTwoPanesWhenHeightAllows() {
    val info = calculateAdaptiveInfo(700.dp, 900.dp)
    assertEquals(WindowWidthSizeClass.MEDIUM, info.widthSizeClass)
    assertEquals(AppNavigationType.NAVIGATION_RAIL, info.navigationType)
    assertTrue(info.useTwoPaneLayout)
  }

  @Test
  fun expandedTabletUsesPermanentDrawerAndTwoPanes() {
    val info = calculateAdaptiveInfo(1024.dp, 768.dp)
    assertEquals(WindowWidthSizeClass.EXPANDED, info.widthSizeClass)
    assertEquals(AppNavigationType.PERMANENT_DRAWER, info.navigationType)
    assertTrue(info.useTwoPaneLayout)
  }

  @Test
  fun landscapePhoneKeepsSinglePaneDespiteWideWidth() {
    val info = calculateAdaptiveInfo(800.dp, 400.dp)
    assertTrue(info.isLandscapePhone)
    assertFalse(info.useTwoPaneLayout)
    assertEquals(AppNavigationType.NAVIGATION_RAIL, info.navigationType)
  }
}
