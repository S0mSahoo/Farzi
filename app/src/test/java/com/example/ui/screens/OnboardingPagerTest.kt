package com.example.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class OnboardingPagerTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var viewModel: FinanceViewModel

  @Before
  fun setUp() {
    viewModel = FinanceViewModel(ApplicationProvider.getApplicationContext<Application>())
  }

  @Test
  fun onboardingShowsEightPagesAndKeepsAccountActionsVisible() {
    composeTestRule.setContent {
      MyApplicationTheme {
        OnboardingScreen(viewModel)
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("onboarding_pager").assertIsDisplayed()
    composeTestRule.onNodeWithText("Page 1 of 8").assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("guest_login_button").assertIsDisplayed()
  }

  @Test
  fun nextButtonMovesThroughTheFeaturePagerAndBackReturnsToPreviousPage() {
    composeTestRule.setContent {
      MyApplicationTheme {
        OnboardingScreen(viewModel)
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Page 2 of 8").assertIsDisplayed()
    composeTestRule.onNodeWithText("TRANSACTIONS").assertIsDisplayed()

    composeTestRule.onNodeWithTag("onboarding_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Page 1 of 8").assertIsDisplayed()
  }
}
