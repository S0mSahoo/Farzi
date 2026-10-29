package com.example.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.insight.FinanceInsight
import com.example.data.insight.InsightSeverity
import com.example.data.insight.InsightType
import com.example.data.model.TransactionCategory
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SpendingInsightsSectionTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun emptyInsightsRendersThoughtfulEmptyState() {
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
      .onNodeWithTag("spending_insights_section")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("insights_empty_state")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithText("No spending insights yet")
      .assertIsDisplayed()
  }

  @Test
  fun populatedInsightsRendersCardsWithHierarchyAndMetrics() {
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

    val budgetInsight = FinanceInsight(
      id = "budget_status_2026-08",
      type = InsightType.BUDGET_STATUS,
      title = "Budget Exceeded",
      description = "Budget exceeded! Spent ₹11,000.00 of ₹10,000.00 budget (110.0%).",
      severity = InsightSeverity.CRITICAL,
      amount = 11000.0,
      percentage = 110.0
    )

    var budgetAdjustClicked = false
    var clickedCategory: TransactionCategory? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        FinancialInsightsSection(
          insights = listOf(budgetInsight, topCatInsight),
          periodLabel = "August 2026",
          currencySymbol = "₹",
          onOpenSetBudget = { budgetAdjustClicked = true },
          onCategoryClick = { clickedCategory = it }
        )
      }
    }
    composeTestRule.waitForIdle()

    // Assert section heading & alerts count
    composeTestRule
      .onNodeWithText("Spending Insights & Analysis")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithText("2 Alerts")
      .assertIsDisplayed()

    // Assert individual cards are displayed
    composeTestRule
      .onNodeWithTag("insight_card_budget_status_2026-08")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("insight_card_top_cat_2026-08")
      .assertIsDisplayed()

    // Assert action button exists and triggers callback
    composeTestRule
      .onNodeWithTag("action_adjust_budget")
      .assertIsDisplayed()
      .performClick()

    assertTrue("Clicking Adjust Budget should trigger callback", budgetAdjustClicked)

    // Assert category card click triggers callback
    composeTestRule
      .onNodeWithTag("insight_card_top_cat_2026-08")
      .performClick()

    assertEquals(TransactionCategory.SHOPPING, clickedCategory)
  }

  @Test
  fun yearlyPeriodLabelUpdatesSubtitleCorrectly() {
    val insight = FinanceInsight(
      id = "cash_flow_2026",
      type = InsightType.CASH_FLOW,
      title = "Positive Cash Flow",
      description = "Savings rate is 25.0% with net savings of ₹25,000.00.",
      severity = InsightSeverity.SUCCESS,
      amount = 25000.0,
      percentage = 25.0
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        FinancialInsightsSection(
          insights = listOf(insight),
          periodLabel = "2026",
          currencySymbol = "₹"
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("Key observations for 2026")
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("insight_card_cash_flow_2026")
      .assertIsDisplayed()
  }
}
