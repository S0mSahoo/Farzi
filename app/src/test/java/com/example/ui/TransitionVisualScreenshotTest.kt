package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailySpendingPoint
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.TransactionType
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumSegmentedToggle
import com.example.ui.components.PremiumTrendChart
import com.example.ui.components.PremiumYearlyComparisonChart
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual screenshot and transition validation test suite using Roborazzi with native graphics mode.
 * Provides pixel-level verification of Compose animated transitions, selection semantics,
 * and layout fidelity without requiring an external physical device or ADB connection.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class TransitionVisualScreenshotTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun testSegmentedToggleVisualAndSelectionStates() {
    var selectedIndex by mutableIntStateOf(0)

    composeTestRule.setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          PremiumSegmentedToggle(
            options = listOf("Monthly", "Yearly"),
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
            role = Role.RadioButton,
            modifier = Modifier.testTag("segmented_toggle")
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    // Assert initial selection state
    composeTestRule.onNodeWithText("Monthly").assertIsSelected()
    composeTestRule.onNodeWithText("Monthly").assert(
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    )

    // Capture visual screenshot for Monthly state
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/segmented_toggle_monthly.png")

    // Click "Yearly" and verify transition and selection change
    composeTestRule.onNodeWithText("Yearly").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Selected index should update to 1", 1, selectedIndex)
    composeTestRule.onNodeWithText("Yearly").assertIsSelected()

    // Capture visual screenshot for Yearly state
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/segmented_toggle_yearly.png")
  }

  @Test
  fun testDashboardHeroCardAnimatedTransition() {
    var isAnnualView by mutableStateOf(false)

    val samplePoints = listOf(
      DailySpendingPoint(dayOfMonth = 1, dateKey = "2026-10-01", dayLabel = "1", expense = 1200.0, income = 0.0, net = -1200.0),
      DailySpendingPoint(dayOfMonth = 2, dateKey = "2026-10-02", dayLabel = "2", expense = 850.0, income = 0.0, net = -850.0),
      DailySpendingPoint(dayOfMonth = 3, dateKey = "2026-10-03", dayLabel = "3", expense = 2400.0, income = 45000.0, net = 42600.0),
      DailySpendingPoint(dayOfMonth = 4, dateKey = "2026-10-04", dayLabel = "4", expense = 650.0, income = 0.0, net = -650.0)
    )

    val sampleYearly = listOf(
      MonthlyFinancialSummary(monthKey = "2026-01", monthLabel = "Jan", totalIncome = 50000.0, totalExpense = 32000.0, savings = 18000.0),
      MonthlyFinancialSummary(monthKey = "2026-02", monthLabel = "Feb", totalIncome = 52000.0, totalExpense = 31000.0, savings = 21000.0),
      MonthlyFinancialSummary(monthKey = "2026-03", monthLabel = "Mar", totalIncome = 55000.0, totalExpense = 34000.0, savings = 21000.0)
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("dashboard_hero_card")
          ) {
            Column(
              modifier = Modifier.padding(22.dp),
              verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
              // 1. Hero Summary Transition
              AnimatedContent(
                targetState = isAnnualView,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "dashboard_hero_summary_transition"
              ) { annual ->
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                  Text(
                    text = if (annual) "ANNUAL NET SAVINGS" else "NET CASH FLOW",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                  )
                  Text(
                    text = if (annual) "₹4,85,000" else "₹42,500",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                  Text(
                    text = if (annual) "38.5% of recorded annual income retained" else "32.0% of recorded income retained",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                  )
                }
              }

              // 2. Chart Transition
              AnimatedContent(
                targetState = isAnnualView,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "dashboard_hero_chart_transition"
              ) { annual ->
                if (annual) {
                  PremiumYearlyComparisonChart(
                    monthlyBreakdown = sampleYearly,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                  )
                } else {
                  PremiumTrendChart(
                    points = samplePoints,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                  )
                }
              }

              // 3. Totals Transition
              AnimatedContent(
                targetState = isAnnualView,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "dashboard_hero_totals_transition"
              ) { annual ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = if (annual) "TOTAL INCOME (YEAR)" else "TOTAL INCOME",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = if (annual) "₹12,40,000" else "₹95,000",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = IncomeGreen
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = if (annual) "TOTAL SPENT (YEAR)" else "TOTAL SPENT",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = if (annual) "₹7,55,000" else "₹52,500",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = ExpenseRed
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    composeTestRule.waitForIdle()

    // Assert Monthly UI texts are visible
    composeTestRule.onNodeWithText("NET CASH FLOW").assertExists()
    composeTestRule.onNodeWithText("₹42,500").assertExists()

    // Visual screenshot for Monthly state
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard_hero_monthly.png")

    // Toggle to Annual state
    isAnnualView = true
    composeTestRule.waitForIdle()

    // Assert Annual UI texts transitioned in
    composeTestRule.onNodeWithText("ANNUAL NET SAVINGS").assertExists()
    composeTestRule.onNodeWithText("₹4,85,000").assertExists()
    composeTestRule.onNodeWithText("TOTAL INCOME (YEAR)").assertExists()

    // Visual screenshot for Annual state
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard_hero_yearly.png")
  }

  @Test
  fun testRecurringScreenAnimatedTabTransition() {
    var selectedTab by mutableIntStateOf(0)

    composeTestRule.setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumSegmentedToggle(
              options = listOf("Upcoming Bills", "Rules"),
              selectedIndex = selectedTab,
              onSelect = { selectedTab = it },
              role = Role.Tab
            )

            AnimatedContent(
              targetState = selectedTab,
              transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
              label = "recurring_tab_transition"
            ) { tab ->
              if (tab == 0) {
                PremiumCard(modifier = Modifier.fillMaxWidth().testTag("upcoming_bills_card")) {
                  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Internet Broadband Bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Due in 3 days • ₹1,299", style = MaterialTheme.typography.bodyMedium, color = ExpenseRed)
                  }
                }
              } else {
                PremiumCard(modifier = Modifier.fillMaxWidth().testTag("recurring_rules_card")) {
                  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active Recurring Rule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Monthly on the 1st • ₹1,299", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }
        }
      }
    }
    composeTestRule.waitForIdle()

    // Verify upcoming bills card visible initially
    composeTestRule.onNodeWithTag("upcoming_bills_card").assertExists()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recurring_tab_upcoming.png")

    // Switch to Rules tab
    composeTestRule.onNodeWithText("Rules").performClick()
    composeTestRule.waitForIdle()

    // Verify recurring rules card visible
    assertEquals(1, selectedTab)
    composeTestRule.onNodeWithTag("recurring_rules_card").assertExists()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recurring_tab_rules.png")
  }

  @Test
  fun testCategoriesTypeAnimatedTransition() {
    var selectedType by mutableStateOf(TransactionType.EXPENSE)

    composeTestRule.setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumSegmentedToggle(
              options = listOf("Spending", "Income"),
              selectedIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1,
              onSelect = { selectedType = if (it == 0) TransactionType.EXPENSE else TransactionType.INCOME },
              role = Role.RadioButton
            )

            AnimatedContent(
              targetState = selectedType,
              transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
              label = "categories_type_transition"
            ) { type ->
              PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (type == TransactionType.EXPENSE) {
                    Text("Food & Dining: ₹14,200 (42%)", style = MaterialTheme.typography.titleMedium, color = ExpenseRed)
                    Text("Shopping: ₹8,500 (25%)", style = MaterialTheme.typography.bodyMedium)
                  } else {
                    Text("Salary: ₹85,000 (88%)", style = MaterialTheme.typography.titleMedium, color = IncomeGreen)
                    Text("Investment: ₹11,500 (12%)", style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
            }
          }
        }
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Food & Dining: ₹14,200 (42%)").assertExists()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/categories_type_expense.png")

    composeTestRule.onNodeWithText("Income").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Salary: ₹85,000 (88%)").assertExists()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/categories_type_income.png")
  }

  @Test
  fun testTransactionSheetSelectionSemantics() {
    var selectedType by mutableStateOf(TransactionType.EXPENSE)

    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
          Row(
            modifier = Modifier
              .padding(4.dp)
              .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            for (type in TransactionType.entries) {
              val isSelected = selectedType == type
              val itemColor = if (isSelected) {
                if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
              } else {
                Color.Transparent
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(itemColor)
                  .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = { selectedType = type }
                  )
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = if (type == TransactionType.EXPENSE) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }
    composeTestRule.waitForIdle()

    // Assert Expense is selected with RadioButton role
    composeTestRule.onNodeWithText("Expense").assertIsSelected()
    composeTestRule.onNodeWithText("Expense").assert(
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    )

    // Capture visual screenshot
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/transaction_sheet_expense_selected.png")

    // Click Income
    composeTestRule.onNodeWithText("Income").performClick()
    composeTestRule.waitForIdle()

    // Assert Income is now selected
    assertEquals(TransactionType.INCOME, selectedType)
    composeTestRule.onNodeWithText("Income").assertIsSelected()

    // Capture visual screenshot
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/transaction_sheet_income_selected.png")
  }
}
