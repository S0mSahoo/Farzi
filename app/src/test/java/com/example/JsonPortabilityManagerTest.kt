package com.example

import com.example.data.model.BudgetModel
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.util.JsonPortabilityManager
import com.example.util.PaisaJsonBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonPortabilityManagerTest {

  private fun transaction(
    id: Long,
    title: String,
    createdAt: Long = 1_000L,
    updatedAt: Long = 1_000L
  ) = TransactionItem(
    id = id,
    title = title,
    amount = 125.0,
    type = TransactionType.EXPENSE,
    category = TransactionCategory.HOUSING,
    timestamp = 1_700_000_000_000L,
    note = "Test transaction",
    createdAt = createdAt,
    updatedAt = updatedAt
  )

  @Test
  fun mergeDoesNotTreatEqualLocalIdsAsTheSameTransactionAcrossDevices() {
    val local = transaction(id = 7L, title = "Rent")
    val imported = transaction(id = 7L, title = "Electricity")

    val merged = JsonPortabilityManager.mergeData(
      currentTransactions = listOf(local),
      currentBudgets = emptyList(),
      currentRecurringRules = emptyList(),
      importBackup = PaisaJsonBackup(transactions = listOf(imported))
    )

    assertEquals(2, merged.first.size)
    assertTrue(merged.first.any { it.title == "Rent" && it.id == 7L })
    assertTrue(merged.first.any { it.title == "Electricity" && it.id != 7L })
  }

  @Test
  fun mergeIsIdempotentForTheSameTransactionAndKeepsLocalId() {
    val local = transaction(id = 9L, title = "Rent", createdAt = 3_000L, updatedAt = 10L)
    val imported = transaction(id = 9L, title = "Rent", createdAt = 3_000L, updatedAt = 20L)

    val merged = JsonPortabilityManager.mergeData(
      currentTransactions = listOf(local),
      currentBudgets = emptyList(),
      currentRecurringRules = emptyList(),
      importBackup = PaisaJsonBackup(transactions = listOf(imported))
    )

    assertEquals(1, merged.first.size)
    assertEquals(9L, merged.first.single().id)
    assertEquals(20L, merged.first.single().updatedAt)
  }

  @Test
  fun newerBudgetCanExplicitlyClearLimits() {
    val local = BudgetModel(
      id = 4L,
      monthKey = "2026-10",
      totalBudget = 500.0,
      categoryBudgets = mapOf(TransactionCategory.HOUSING to 100.0),
      updatedAt = 10L
    )
    val imported = local.copy(
      id = 4L,
      totalBudget = 0.0,
      categoryBudgets = emptyMap(),
      updatedAt = 20L
    )

    val merged = JsonPortabilityManager.mergeData(
      currentTransactions = emptyList(),
      currentBudgets = listOf(local),
      currentRecurringRules = emptyList(),
      importBackup = PaisaJsonBackup(budgets = listOf(imported))
    )

    assertEquals(1, merged.second.size)
    assertEquals(4L, merged.second.single().id)
    assertEquals(0.0, merged.second.single().totalBudget, 0.0)
    assertTrue(merged.second.single().categoryBudgets.isEmpty())
  }

  @Test
  fun similarlyNamedRecurringRulesWithDifferentStartDatesRemainDistinct() {
    val local = RecurringRule(
      id = 3L,
      title = "Rent",
      amount = 500.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.HOUSING,
      interval = RecurrenceInterval.MONTHLY,
      startDate = 1_700_000_000_000L
    )
    val imported = local.copy(id = 3L, startDate = 1_702_678_400_000L)

    val merged = JsonPortabilityManager.mergeData(
      currentTransactions = emptyList(),
      currentBudgets = emptyList(),
      currentRecurringRules = listOf(local),
      importBackup = PaisaJsonBackup(recurringTransactions = listOf(imported))
    )

    assertEquals(2, merged.third.size)
    assertTrue(merged.third.any { it.startDate == local.startDate && it.id == local.id })
    assertTrue(merged.third.any { it.startDate == imported.startDate && it.id != imported.id })
  }
}
