package com.example.data.insight

import com.example.data.model.BudgetModel
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinanceInsightEngineTest {

  private fun createTimestamp(year: Int, month: Int, day: Int = 15): Long {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, day, 12, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  @Test
  fun testMonthOverMonthSpendingIncreaseAndDecrease() {
    val JulyTimestamp = createTimestamp(2026, 7, 10)
    val AugustTimestamp = createTimestamp(2026, 8, 10)

    val txJuly = TransactionItem(
      id = 1,
      title = "July Expense",
      amount = 1000.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.SHOPPING,
      timestamp = JulyTimestamp
    )

    val txAugust = TransactionItem(
      id = 2,
      title = "August Expense",
      amount = 1500.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.SHOPPING,
      timestamp = AugustTimestamp
    )

    val insights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txJuly, txAugust),
      budgets = emptyList(),
      recurringRules = emptyList()
    )

    val compInsight = insights.find { it.type == InsightType.PERIOD_COMPARISON }
    assertNotNull("Period comparison insight should exist", compInsight)
    assertEquals(InsightSeverity.WARNING, compInsight!!.severity)
    assertEquals(50.0, compInsight.percentage!!, 0.01)
    assertTrue(compInsight.description.contains("up 50.0%"))
  }

  @Test
  fun testMonthOverMonthZeroBaselineHandling() {
    val AugustTimestamp = createTimestamp(2026, 8, 10)

    val txAugust = TransactionItem(
      id = 1,
      title = "August Expense",
      amount = 500.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.FOOD_DINING,
      timestamp = AugustTimestamp
    )

    val insights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txAugust),
      budgets = emptyList(),
      recurringRules = emptyList()
    )

    val compInsight = insights.find { it.type == InsightType.PERIOD_COMPARISON }
    assertNotNull(compInsight)
    assertEquals(InsightSeverity.INFO, compInsight!!.severity)
    assertNull("Percentage should be null for 0 baseline", compInsight.percentage)
    assertTrue(compInsight.description.contains("no recorded spending"))
  }

  @Test
  fun testTopCategoryConcentrationAlert() {
    val AugustTimestamp = createTimestamp(2026, 8, 10)

    val txFood = TransactionItem(
      id = 1,
      title = "Dining Out",
      amount = 400.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.FOOD_DINING,
      timestamp = AugustTimestamp
    )

    val txGroceries = TransactionItem(
      id = 2,
      title = "Supermarket",
      amount = 600.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.GROCERIES,
      timestamp = AugustTimestamp
    )

    val insights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txFood, txGroceries),
      budgets = emptyList(),
      recurringRules = emptyList()
    )

    val topCatInsight = insights.find { it.type == InsightType.TOP_CATEGORY }
    assertNotNull(topCatInsight)
    assertEquals(TransactionCategory.GROCERIES, topCatInsight!!.category)
    assertEquals(60.0, topCatInsight.percentage!!, 0.01)
    assertEquals(InsightSeverity.WARNING, topCatInsight.severity)
    assertEquals("High Category Concentration", topCatInsight.title)
  }

  @Test
  fun testSavingsRateCalculationWithPositiveIncomeAndZeroIncome() {
    val AugustTimestamp = createTimestamp(2026, 8, 10)

    val txSalary = TransactionItem(
      id = 1,
      title = "Salary",
      amount = 10000.0,
      type = TransactionType.INCOME,
      category = TransactionCategory.SALARY,
      timestamp = AugustTimestamp
    )

    val txExpense = TransactionItem(
      id = 2,
      title = "Rent",
      amount = 4000.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.HOUSING,
      timestamp = AugustTimestamp
    )

    val insights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txSalary, txExpense),
      budgets = emptyList(),
      recurringRules = emptyList()
    )

    val cashFlowInsight = insights.find { it.type == InsightType.CASH_FLOW }
    assertNotNull(cashFlowInsight)
    assertEquals(InsightSeverity.SUCCESS, cashFlowInsight!!.severity)
    assertEquals(60.0, cashFlowInsight.percentage!!, 0.01)
    assertEquals(6000.0, cashFlowInsight.amount!!, 0.01)

    // Test zero income scenario
    val zeroIncomeInsights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txExpense),
      budgets = emptyList(),
      recurringRules = emptyList()
    )

    val zeroIncomeCashFlow = zeroIncomeInsights.find { it.type == InsightType.CASH_FLOW }
    assertNotNull(zeroIncomeCashFlow)
    assertNull("Percentage should be null when income is 0", zeroIncomeCashFlow!!.percentage)
    assertEquals("No Income Recorded", zeroIncomeCashFlow.title)
  }

  @Test
  fun testBudgetUtilizationThresholds() {
    val AugustTimestamp = createTimestamp(2026, 8, 10)

    val budget = BudgetModel(
      id = 1,
      monthKey = "2026-08",
      totalBudget = 1000.0
    )

    val txExpenseBreach = TransactionItem(
      id = 1,
      title = "Shopping",
      amount = 1100.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.SHOPPING,
      timestamp = AugustTimestamp
    )

    val insightsBreach = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txExpenseBreach),
      budgets = listOf(budget),
      recurringRules = emptyList()
    )

    val budgetBreachInsight = insightsBreach.find { it.type == InsightType.BUDGET_STATUS }
    assertNotNull(budgetBreachInsight)
    assertEquals(InsightSeverity.CRITICAL, budgetBreachInsight!!.severity)
    assertEquals("Budget Exceeded", budgetBreachInsight.title)

    // Warning threshold (85%)
    val txExpenseWarning = txExpenseBreach.copy(amount = 850.0)
    val insightsWarning = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txExpenseWarning),
      budgets = listOf(budget),
      recurringRules = emptyList()
    )

    val budgetWarningInsight = insightsWarning.find { it.type == InsightType.BUDGET_STATUS }
    assertNotNull(budgetWarningInsight)
    assertEquals(InsightSeverity.WARNING, budgetWarningInsight!!.severity)
    assertEquals("Approaching Budget Limit", budgetWarningInsight.title)
  }

  @Test
  fun testMissingBudgetNotTreatedAsZeroBudget() {
    val AugustTimestamp = createTimestamp(2026, 8, 10)

    val txExpense = TransactionItem(
      id = 1,
      title = "Groceries",
      amount = 200.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.GROCERIES,
      timestamp = AugustTimestamp
    )

    val insights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = listOf(txExpense),
      budgets = emptyList(), // Missing budget!
      recurringRules = emptyList()
    )

    val budgetInsight = insights.find { it.type == InsightType.BUDGET_STATUS }
    assertNull("Budget status insight should NOT be generated when no budget exists", budgetInsight)
  }

  @Test
  fun testActiveRecurringCommitmentsDistinctFromTransactions() {
    val activeRule1 = RecurringRule(
      id = 1,
      title = "Netflix",
      amount = 199.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.SUBSCRIPTIONS,
      interval = RecurrenceInterval.MONTHLY,
      startDate = createTimestamp(2026, 1, 1),
      isActive = true
    )

    val inactiveRule = RecurringRule(
      id = 2,
      title = "Gym",
      amount = 1000.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.FITNESS,
      interval = RecurrenceInterval.MONTHLY,
      startDate = createTimestamp(2026, 1, 1),
      isActive = false
    )

    val insights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = emptyList(),
      budgets = emptyList(),
      recurringRules = listOf(activeRule1, inactiveRule)
    )

    val recurringInsight = insights.find { it.type == InsightType.RECURRING_COMMITMENTS }
    assertNotNull(recurringInsight)
    assertEquals(199.0, recurringInsight!!.amount!!, 0.01)
    assertTrue(recurringInsight.description.contains("1 active recurring rules"))
  }

  @Test
  fun testEmptyDataHandling() {
    val insights = FinanceInsightEngine.generateMonthlyInsights(
      monthKey = "2026-08",
      transactions = emptyList(),
      budgets = emptyList(),
      recurringRules = emptyList()
    )

    assertEquals(1, insights.size)
    assertEquals("No Data Recorded", insights[0].title)
    assertEquals(InsightSeverity.NEUTRAL, insights[0].severity)
  }

  @Test
  fun testPreviousMonthKeyParsing() {
    assertEquals("2025-12", FinanceInsightEngine.getPreviousMonthKey("2026-01"))
    assertEquals("2026-07", FinanceInsightEngine.getPreviousMonthKey("2026-08"))
    assertNull(FinanceInsightEngine.getPreviousMonthKey("invalid-month"))
  }
}
