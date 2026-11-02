package com.example.data.insight

import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFilterProjectorTest {
  private fun tx(
    id: Long,
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    note: String = ""
  ) = TransactionItem(
    id = id,
    title = title,
    amount = amount,
    type = type,
    category = category,
    timestamp = 1_798_761_600_000L + id,
    note = note,
    paymentMethod = PaymentMethod.UPI
  )

  private val transactions = listOf(
    tx(1, "Coffee Shop", 150.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, "Morning coffee"),
    tx(2, "Monthly Salary", 50_000.0, TransactionType.INCOME, TransactionCategory.SALARY),
    tx(3, "Grocery Store", 900.0, TransactionType.EXPENSE, TransactionCategory.GROCERIES)
  )

  @Test
  fun blankQueryWithoutFiltersReturnsAllTransactions() {
    assertEquals(transactions, TransactionFilterProjector.filter(transactions, "", null, null))
    assertEquals(transactions, TransactionFilterProjector.filter(transactions, "   ", null, null))
  }

  @Test
  fun textSearchIsCaseInsensitiveAndIncludesNotes() {
    assertEquals(listOf(1L), TransactionFilterProjector.filter(transactions, "cOfFeE", null, null).map { it.id })
    assertEquals(listOf(1L), TransactionFilterProjector.filter(transactions, "morning", null, null).map { it.id })
  }

  @Test
  fun typeAndCategoryFiltersAreCombinedWithTextSearch() {
    val result = TransactionFilterProjector.filter(
      transactions,
      query = "coffee",
      type = TransactionType.EXPENSE,
      category = TransactionCategory.FOOD_DINING
    )
    assertEquals(listOf(1L), result.map { it.id })
    assertEquals(emptyList<Long>(), TransactionFilterProjector.filter(
      transactions,
      query = "coffee",
      type = TransactionType.INCOME,
      category = TransactionCategory.FOOD_DINING
    ).map { it.id })
  }

  @Test
  fun numericSearchMatchesAmountText() {
    assertEquals(listOf(1L), TransactionFilterProjector.filter(transactions, "150.0", null, null).map { it.id })
  }
}
