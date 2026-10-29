package com.example.data.ai

import com.example.data.model.BudgetModel
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinancialQueryToolsTest {

  private val sampleTransactions = listOf(
    TransactionItem(
      id = 1L,
      title = "Salary",
      amount = 5000.0,
      type = TransactionType.INCOME,
      category = TransactionCategory.SALARY,
      paymentMethod = PaymentMethod.NET_BANKING,
      timestamp = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 1, 10, 0, 0)
      }.timeInMillis,
      note = "Monthly salary payment"
    ),
    TransactionItem(
      id = 2L,
      title = "Groceries",
      amount = 150.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.GROCERIES,
      paymentMethod = PaymentMethod.UPI,
      timestamp = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 10, 12, 0, 0)
      }.timeInMillis,
      note = "Weekly supermarket run"
    ),
    TransactionItem(
      id = 3L,
      title = "Electricity Bill",
      amount = 80.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.UTILITIES,
      paymentMethod = PaymentMethod.NET_BANKING,
      timestamp = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 15, 14, 0, 0)
      }.timeInMillis,
      note = "Monthly power bill"
    )
  )

  private val sampleBudgets = listOf(
    BudgetModel(monthKey = "2026-08", totalBudget = 1000.0)
  )

  private val sampleRules = listOf(
    RecurringRule(
      id = 1L,
      title = "Netflix",
      amount = 15.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.SUBSCRIPTIONS,
      interval = RecurrenceInterval.MONTHLY,
      startDate = System.currentTimeMillis(),
      paymentMethod = PaymentMethod.CREDIT_CARD,
      isActive = true
    )
  )

  private val executor = DefaultFinancialToolExecutor(
    getTransactions = { sampleTransactions },
    getBudgets = { sampleBudgets },
    getRecurringRules = { sampleRules },
    getCurrencySymbol = { "$" }
  )

  @Test
  fun testGetFinancialSummaryMonthly() = runBlocking {
    val result = executor.executeTool(
      "getFinancialSummary",
      mapOf("periodType" to "MONTHLY", "period" to "2026-08")
    )
    assertEquals("SUCCESS", result.getString("status"))
    assertEquals("MONTHLY", result.getString("periodType"))
    assertEquals("2026-08", result.getString("period"))
    assertEquals(5000.0, result.getDouble("totalIncome"), 0.01)
    assertEquals(230.0, result.getDouble("totalExpense"), 0.01)
    assertEquals(4770.0, result.getDouble("netSavings"), 0.01)
    assertEquals(1000.0, result.getDouble("budgetLimit"), 0.01)
    assertEquals(23.0, result.getDouble("budgetUsedPercent"), 0.1)
  }

  @Test
  fun testComparePeriods() = runBlocking {
    val result = executor.executeTool(
      "comparePeriods",
      mapOf("periodType" to "MONTHLY", "period1" to "2026-07", "period2" to "2026-08")
    )
    assertEquals("SUCCESS", result.getString("status"))
    assertNotNull(result.getString("comparisonSummary"))
    assertEquals(230.0, result.getJSONObject("period2").getDouble("expense"), 0.01)
  }

  @Test
  fun testGetCategorySpending() = runBlocking {
    val result = executor.executeTool(
      "getCategorySpending",
      mapOf(
        "startDate" to "2026-08-01",
        "endDate" to "2026-08-31",
        "type" to "EXPENSE"
      )
    )
    assertEquals("SUCCESS", result.getString("status"))
    assertEquals(230.0, result.getDouble("totalAmount"), 0.01)
    val categories = result.getJSONArray("categories")
    assertTrue(categories.length() >= 2)
  }

  @Test
  fun testGetBudgetStatus() = runBlocking {
    val result = executor.executeTool(
      "getBudgetStatus",
      mapOf("monthKey" to "2026-08")
    )
    assertEquals("SUCCESS", result.getString("status"))
    assertEquals(1000.0, result.getDouble("budgetLimit"), 0.01)
    assertEquals(230.0, result.getDouble("totalExpense"), 0.01)
    assertEquals(770.0, result.getDouble("remaining"), 0.01)
    assertEquals("UNDER_BUDGET", result.getString("budgetStatus"))
  }

  @Test
  fun testGetRecurringCommitments() = runBlocking {
    val result = executor.executeTool(
      "getRecurringCommitments",
      mapOf("type" to "EXPENSE")
    )
    assertEquals("SUCCESS", result.getString("status"))
    assertEquals(1, result.getInt("activeRuleCount"))
    assertEquals(15.0, result.getDouble("estimatedMonthlyCommittedExpense"), 0.01)
  }

  @Test
  fun testSearchTransactionsBoundedLimitAndNoNotes() = runBlocking {
    val result = executor.executeTool(
      "searchTransactions",
      mapOf(
        "query" to "Groceries",
        "limit" to 50 // should be capped at 25
      )
    )
    assertEquals("SUCCESS", result.getString("status"))
    assertEquals(25, result.getInt("limitApplied"))
    val txs = result.getJSONArray("transactions")
    assertEquals(1, txs.length())
    val tx = txs.getJSONObject(0)
    assertEquals("Groceries", tx.getString("title"))
    // Verify private note is not exposed in search results
    assertTrue(!tx.has("note"))
  }

  @Test
  fun testUnknownFunctionHandling() = runBlocking {
    val result = executor.executeTool("deleteEverything", emptyMap())
    assertEquals("ERROR", result.getString("status"))
    assertEquals("UNKNOWN_FUNCTION", result.getString("errorCode"))
  }

  @Test
  fun testInvalidArgumentsHandling() = runBlocking {
    val result = executor.executeTool("getBudgetStatus", mapOf("monthKey" to "invalid-month"))
    assertEquals("ERROR", result.getString("status"))
    assertEquals("INVALID_ARGUMENTS", result.getString("errorCode"))
  }

  @Test
  fun testRepositoryFailureGracefulHandling() = runBlocking {
    val failingExecutor = DefaultFinancialToolExecutor(
      getTransactions = { throw RuntimeException("Database error") },
      getBudgets = { emptyList() },
      getRecurringRules = { emptyList() }
    )
    val result = failingExecutor.executeTool(
      "getFinancialSummary",
      mapOf("periodType" to "MONTHLY", "period" to "2026-08")
    )
    assertEquals("ERROR", result.getString("status"))
    assertEquals("EXECUTION_ERROR", result.getString("errorCode"))
  }
}
