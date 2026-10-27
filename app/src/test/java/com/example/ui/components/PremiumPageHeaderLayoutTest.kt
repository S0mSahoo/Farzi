package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PremiumPageHeaderLayoutTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun compactWidthPlacesTrailingActionBelowHeading() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Box(Modifier.width(320.dp)) {
          PremiumPageHeader(
            title = "Transactions",
            subtitle = "Every rupee, accounted for.",
            trailing = {
              Text("See all", modifier = Modifier.testTag("premium_header_action"))
            }
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    val heading = composeTestRule
      .onNodeWithTag("premium_page_header_title")
      .fetchSemanticsNode()
      .boundsInRoot
    val action = composeTestRule
      .onNodeWithTag("premium_header_action")
      .fetchSemanticsNode()
      .boundsInRoot

    assertTrue(
      "On compact screens, the action should sit below the full title block",
      action.top >= heading.bottom
    )
  }

  @Test
  fun wideWidthKeepsTrailingActionBesideHeading() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Box(Modifier.width(720.dp)) {
          PremiumPageHeader(
            title = "Transactions",
            subtitle = "Every rupee, accounted for.",
            trailing = {
              Text("See all", modifier = Modifier.testTag("premium_header_action"))
            }
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    val heading = composeTestRule
      .onNodeWithTag("premium_page_header_title")
      .fetchSemanticsNode()
      .boundsInRoot
    val action = composeTestRule
      .onNodeWithTag("premium_header_action")
      .fetchSemanticsNode()
      .boundsInRoot

    assertTrue(
      "On wide screens, the trailing action should remain to the right of the heading",
      action.left >= heading.right
    )
  }
}
