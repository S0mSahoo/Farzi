package com.example.data.insight

import com.example.data.model.BudgetModel
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinanceAggregationsTest {
  private fun timestamp(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
      set(year, month, day, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

  private fun tx(
    id: Long,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    year: Int,
    month: Int,
    day: Int
  ) = TransactionItem(
    id = id,
    title = "Test $id",
    amount = amount,
    type = type,
    category = category,
    timestamp = timestamp(year, month, day),
    paymentMethod = PaymentMethod.UPI
  )

  @Test
  fun monthlySummaryUsesSinglePassTotalsAndPreservesNegativeSavings() {
    val transactions = listOf(
      tx(1, 30_000.0, TransactionType.INCOME, TransactionCategory.SALARY, 2026, Calendar.OCTOBER, 1),
      tx(2, 35_000.0, TransactionType.EXPENSE, TransactionCategory.HOUSING, 2026, Calendar.OCTOBER, 2),
      tx(3, 5_000.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, 2026, Calendar.SEPTEMBER, 2)
    )
    val calendar = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 15, 12, 0, 0) }
    val summary = FinanceAggregations.monthlySummary(
      transactions,
      listOf(BudgetModel(monthKey = "2026-10", totalBudget = 40_000.0)),
      calendar
    )

    assertEquals(30_000.0, summary.totalIncome, 0.001)
    assertEquals(35_000.0, summary.totalExpense, 0.001)
    assertEquals(-5_000.0, summary.savings, 0.001)
    assertEquals(-16.6667, summary.savingsRate, 0.001)
    assertEquals(2, summary.transactionCount)
    assertEquals(40_000.0, summary.budgetLimit, 0.001)
  }

  @Test
  fun yearlySummaryAggregatesEachTransactionOnceIntoMonthBuckets() {
    val source = listOf(
      tx(1, 10_000.0, TransactionType.INCOME, TransactionCategory.SALARY, 2026, Calendar.JANUARY, 2),
      tx(2, 2_000.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, 2026, Calendar.JANUARY, 3),
      tx(3, 5_000.0, TransactionType.INCOME, TransactionCategory.FREELANCE, 2026, Calendar.MARCH, 1),
      tx(4, 8_000.0, TransactionType.EXPENSE, TransactionCategory.HOUSING, 2025, Calendar.DECEMBER, 31)
    )
    var reads = 0
    val transactions = object : AbstractList<TransactionItem>() {
      override val size: Int get() = source.size
      override fun get(index: Int): TransactionItem {
        reads++
        return source[index]
      }
    }
    val summary = FinanceAggregations.yearlySummary(transactions, 2026)
    assertEquals(source.size, reads)

    assertEquals(15_000.0, summary.totalIncome, 0.001)
    assertEquals(2_000.0, summary.totalExpense, 0.001)
    assertEquals(13_000.0, summary.savings, 0.001)
    assertEquals(12, summary.monthlyBreakdown.size)
    assertEquals(10_000.0, summary.monthlyBreakdown[Calendar.JANUARY].totalIncome, 0.001)
    assertEquals(5_000.0, summary.monthlyBreakdown[Calendar.MARCH].totalIncome, 0.001)
    assertEquals(0, summary.monthlyBreakdown[Calendar.FEBRUARY].transactionCount)
  }

  @Test
  fun categoryAndDailyAggregationsKeepCountsAndIncomeExpenseSeparate() {
    val transactions = listOf(
      tx(1, 100.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, 2026, Calendar.OCTOBER, 4),
      tx(2, 200.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, 2026, Calendar.OCTOBER, 4),
      tx(3, 500.0, TransactionType.INCOME, TransactionCategory.SALARY, 2026, Calendar.OCTOBER, 4)
    )
    val calendar = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 1, 12, 0, 0) }
    val categories = FinanceAggregations.categorySpending(transactions, calendar)
    val trend = FinanceAggregations.dailyTrend(transactions, calendar)

    assertEquals(1, categories.size)
    assertEquals(300.0, categories.single().amount, 0.001)
    assertEquals(2, categories.single().count)
    assertEquals(31, trend.size)
    assertEquals(300.0, trend[3].expense, 0.001)
    assertEquals(500.0, trend[3].income, 0.001)
    assertEquals(200.0, trend[3].net, 0.001)
    assertTrue(trend.take(3).all { it.expense == 0.0 && it.income == 0.0 })
  }
  @Test
  fun monthlyMetricsVisitsEachTransactionOnceAndFeedsAllMonthlyProjections() {
    val source = listOf(
      tx(1, 12_000.0, TransactionType.INCOME, TransactionCategory.SALARY, 2026, Calendar.OCTOBER, 2),
      tx(2, 1_000.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, 2026, Calendar.OCTOBER, 3),
      tx(3, 500.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, 2026, Calendar.OCTOBER, 3),
      tx(4, 9_000.0, TransactionType.EXPENSE, TransactionCategory.HOUSING, 2026, Calendar.SEPTEMBER, 30)
    )
    var reads = 0
    val countingTransactions = object : AbstractList<TransactionItem>() {
      override val size: Int get() = source.size
      override fun get(index: Int): TransactionItem {
        reads++
        return source[index]
      }
    }
    val calendar = Calendar.getInstance().apply {
      set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val metrics = FinanceAggregations.monthlyMetrics(countingTransactions, calendar)

    assertEquals(source.size, reads)
    assertEquals(12_000.0, metrics.totalIncome, 0.001)
    assertEquals(1_500.0, metrics.totalExpense, 0.001)
    assertEquals(3, metrics.transactionCount)
    assertEquals(1, metrics.categorySpending.size)
    assertEquals(1_500.0, metrics.categorySpending.single().amount, 0.001)
    assertEquals(1_500.0, metrics.dailyTrend[2].expense, 0.001)
    assertEquals(12_000.0, metrics.dailyTrend[1].income, 0.001)

    val summary = FinanceAggregations.monthlySummary(
      metrics,
      listOf(BudgetModel(monthKey = "2026-10", totalBudget = 2_000.0))
    )
    assertEquals(2_000.0, summary.budgetLimit, 0.001)
    assertEquals(500.0, summary.budgetRemaining, 0.001)
  }

}
