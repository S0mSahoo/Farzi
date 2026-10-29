package com.example.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.insight.FinanceInsight
import com.example.data.insight.InsightSeverity
import com.example.data.insight.InsightType
import com.example.data.model.TransactionCategory
import com.example.ui.components.FinancialInsightsSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
class AnalyticsAndRecurringScreensTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var viewModel: FinanceViewModel

  @Before
  fun setUp() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    viewModel = FinanceViewModel(app)
  }

  @Test
  fun analyticsScreenRendersAndTogglesMonthlyYearlyView() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AnalyticsScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()

    // Assert main screen and controls are displayed
    composeTestRule
      .onNodeWithTag("analytics_screen")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("analytics_chip_monthly")
      .assertIsDisplayed()
      .assertIsSelected()

    composeTestRule
      .onNodeWithTag("analytics_chip_yearly")
      .assertIsDisplayed()

    // Monthly hero should be visible
    composeTestRule
      .onNodeWithText("NET CASH FLOW")
      .assertIsDisplayed()

    // Toggle to yearly view
    composeTestRule
      .onNodeWithTag("analytics_chip_yearly")
      .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("analytics_chip_yearly")
      .assertIsSelected()

    composeTestRule
      .onNodeWithText("YEARLY OVERVIEW")
      .assertIsDisplayed()

    // Toggle back to monthly view
    composeTestRule
      .onNodeWithTag("analytics_chip_monthly")
      .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("analytics_chip_monthly")
      .assertIsSelected()

    composeTestRule
      .onNodeWithText("NET CASH FLOW")
      .assertIsDisplayed()
  }

  @Test
  fun recurringScreenTabSwitchingRendersUpcomingAndRules() {
    var addRuleClicked = false
    composeTestRule.setContent {
      MyApplicationTheme {
        RecurringScreen(
          viewModel = viewModel,
          onOpenAddRecurringRule = { addRuleClicked = true },
          onEditRecurringRule = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // Assert recurring screen and tabs are displayed
    composeTestRule
      .onNodeWithTag("recurring_screen")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("recurring_tab_upcoming")
      .assertIsDisplayed()
      .assertIsSelected()

    composeTestRule
      .onNodeWithTag("recurring_tab_rules")
      .assertIsDisplayed()

    // Switch to Rules tab
    composeTestRule
      .onNodeWithTag("recurring_tab_rules")
      .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("recurring_tab_rules")
      .assertIsSelected()

    // Switch back to Upcoming tab
    composeTestRule
      .onNodeWithTag("recurring_tab_upcoming")
      .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("recurring_tab_upcoming")
      .assertIsSelected()
  }

  @Test
  fun financialInsightsSectionIntegrationWithCategoryNavigation() {
    val topCatInsight = FinanceInsight(
      id = "top_cat_2026-08",
      type = InsightType.TOP_CATEGORY,
      title = "High Category Concentration",
      description = "Shopping accounts for 65.0% of spending (₹6,500.00).",
      severity = InsightSeverity.WARNING,
      amount = 6500.0,
      percentage = 65.0,
      category = TransactionCategory.SHOPPING
    )

    var navigatedCategory: TransactionCategory? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        FinancialInsightsSection(
          insights = listOf(topCatInsight),
          periodLabel = "August 2026",
          currencySymbol = "₹",
          onCategoryClick = { category ->
            navigatedCategory = category
            viewModel.selectCategoryForDetail(category)
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    // Verify card is rendered
    composeTestRule
      .onNodeWithTag("insight_card_top_cat_2026-08")
      .assertIsDisplayed()
      .performClick()

    // Verify category navigation callback and ViewModel selection
    assertEquals(TransactionCategory.SHOPPING, navigatedCategory)
    assertEquals(TransactionCategory.SHOPPING, viewModel.selectedCategory.value)
  }

  @Test
  fun financialInsightsSectionEmptyStateHandledGracefully() {
    composeTestRule.setContent {
      MyApplicationTheme {
        FinancialInsightsSection(
          insights = emptyList(),
          periodLabel = "August 2026",
          currencySymbol = "₹"
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("insights_empty_state")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithText("No spending insights yet")
      .assertIsDisplayed()
  }
}
