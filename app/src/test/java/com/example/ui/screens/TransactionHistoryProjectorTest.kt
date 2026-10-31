package com.example.ui.screens

import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionHistoryProjectorTest {
  private fun at(day: Int, month: Int = Calendar.OCTOBER): Long =
    Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, month)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, 12)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

  private fun item(
    id: Long,
    day: Int,
    amount: Double,
    type: TransactionType,
    month: Int = Calendar.OCTOBER
  ) = TransactionItem(
    id = id,
    title = "Transaction $id",
    amount = amount,
    type = type,
    category = if (type == TransactionType.INCOME) TransactionCategory.SALARY else TransactionCategory.GROCERIES,
    timestamp = at(day, month),
    paymentMethod = PaymentMethod.UPI
  )

  @Test
  fun projectionFiltersMonthAggregatesTotalsAndBuildsStableFeed() {
    val calendar = Calendar.getInstance().apply { timeInMillis = at(6) }
    val transactions = listOf(
      item(1, 4, 100.0, TransactionType.EXPENSE),
      item(2, 5, 250.0, TransactionType.INCOME),
      item(3, 6, 50.0, TransactionType.EXPENSE),
      item(4, 30, 900.0, TransactionType.INCOME, Calendar.SEPTEMBER)
    )
    val result = TransactionHistoryProjector.project(
      transactions = transactions,
      startOfMonthMillis = DateUtils.getStartOfMonth(calendar),
      endOfMonthMillis = DateUtils.getEndOfMonth(calendar),
      selectedDayOfMonth = null,
      referenceTimestamp = at(6)
    )

    assertEquals(3, result.recordCount)
    assertEquals(250.0, result.totalIncome, 0.001)
    assertEquals(150.0, result.totalExpense, 0.001)
    assertEquals(listOf(3L, 2L, 1L), result.visibleTransactions.map { it.id })
    assertEquals(setOf(4, 5, 6), result.daysWithTransactions)
    assertTrue(result.feedEntries.any { it.dateLabel == "Today" })
    assertTrue(result.feedEntries.any { it.dateLabel == "Yesterday" })
    assertEquals(result.recordCount + 2, result.feedEntries.size)
    assertTrue(result.feedEntries.filter { it.transaction != null }.all { it.key.startsWith("tx_") })
  }

  @Test
  fun selectedDayFiltersVisibleRowsButRetainsMonthlyDayMarkers() {
    val calendar = Calendar.getInstance().apply { timeInMillis = at(6) }
    val transactions = listOf(
      item(1, 5, 120.0, TransactionType.EXPENSE),
      item(2, 6, 500.0, TransactionType.INCOME),
      item(3, 6, 80.0, TransactionType.EXPENSE)
    )
    val result = TransactionHistoryProjector.project(
      transactions = transactions,
      startOfMonthMillis = DateUtils.getStartOfMonth(calendar),
      endOfMonthMillis = DateUtils.getEndOfMonth(calendar),
      selectedDayOfMonth = 6,
      referenceTimestamp = at(6)
    )

    assertEquals(2, result.recordCount)
    assertEquals(500.0, result.totalIncome, 0.001)
    assertEquals(80.0, result.totalExpense, 0.001)
    assertEquals(listOf(3L, 2L), result.visibleTransactions.map { it.id })
    assertEquals(setOf(5, 6), result.daysWithTransactions)
  }

  @Test
  fun emptySelectionReturnsZeroTotalsAndNoFeedEntries() {
    val calendar = Calendar.getInstance().apply { timeInMillis = at(6) }
    val result = TransactionHistoryProjector.project(
      transactions = emptyList(),
      startOfMonthMillis = DateUtils.getStartOfMonth(calendar),
      endOfMonthMillis = DateUtils.getEndOfMonth(calendar),
      selectedDayOfMonth = null,
      referenceTimestamp = at(6)
    )

    assertEquals(0, result.recordCount)
    assertEquals(0.0, result.totalIncome, 0.001)
    assertEquals(0.0, result.totalExpense, 0.001)
    assertTrue(result.feedEntries.isEmpty())
    assertTrue(result.daysWithTransactions.isEmpty())
  }
}
