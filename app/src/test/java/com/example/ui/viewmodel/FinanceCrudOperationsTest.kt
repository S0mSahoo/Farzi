package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceCrudOperationsTest {

  private lateinit var viewModel: FinanceViewModel

  private fun flushCoroutines(condition: (() -> Boolean)? = null) {
    val deadline = System.currentTimeMillis() + 5000
    ShadowLooper.idleMainLooper()
    while (condition != null && !condition() && System.currentTimeMillis() < deadline) {
      ShadowLooper.idleMainLooper()
      Thread.sleep(20)
      ShadowLooper.idleMainLooper()
    }
    ShadowLooper.idleMainLooper()
  }

  @Before
  fun setUp() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    viewModel = FinanceViewModel(app)
    flushCoroutines()
  }

  @Test
  fun testAddTransactionSuspendSucceedsLocally() = runBlocking {
    val id = viewModel.addTransactionSuspend(
      title = "Test Coffee",
      amount = 5.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.FOOD_DINING,
      timestamp = System.currentTimeMillis(),
      note = "Local test",
      paymentMethod = PaymentMethod.CASH
    )
    assertTrue("Inserted transaction ID should be positive", id > 0)

    flushCoroutines { viewModel.allTransactions.value.any { it.title == "Test Coffee" } }
    val added = viewModel.allTransactions.value.find { it.title == "Test Coffee" }
    assertNotNull("Transaction must be present in local flow state", added)
    assertEquals(5.0, added?.amount ?: 0.0, 0.01)
  }

  @Test
  fun testInvalidParametersThrowException() {
    runBlocking {
      try {
        viewModel.addTransactionSuspend(
          title = "",
          amount = -10.0,
          type = TransactionType.EXPENSE,
          category = TransactionCategory.FOOD_DINING,
          timestamp = System.currentTimeMillis(),
          note = "",
          paymentMethod = PaymentMethod.CASH
        )
        fail("Should have thrown IllegalArgumentException for invalid amount and title")
      } catch (e: IllegalArgumentException) {
        assertTrue(e.message?.contains("Invalid transaction parameters") == true)
      }
    }
  }

  @Test
  fun testUpdateNonExistentTransactionThrowsException() {
    runBlocking {
      val dummy = TransactionItem(
        id = 9999999L,
        title = "Ghost Tx",
        amount = 50.0,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.OTHER_EXPENSE,
        timestamp = System.currentTimeMillis(),
        note = "",
        paymentMethod = PaymentMethod.CASH
      )
      try {
        viewModel.updateTransactionSuspend(dummy)
        fail("Should have thrown IllegalStateException when updating non-existent record")
      } catch (e: IllegalStateException) {
        assertTrue(e.message?.contains("no longer exists") == true)
      }
    }
  }

  @Test
  fun testDeleteTransactionSuspendSucceedsAndFailsOnGhost() = runBlocking {
    val id = viewModel.addTransactionSuspend(
      title = "Tx To Delete",
      amount = 20.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.GROCERIES,
      timestamp = System.currentTimeMillis(),
      note = "",
      paymentMethod = PaymentMethod.CASH
    )
    flushCoroutines { viewModel.allTransactions.value.any { it.title == "Tx To Delete" } }
    val tx = viewModel.allTransactions.value.first { it.title == "Tx To Delete" }

    viewModel.deleteTransactionSuspend(tx)
    flushCoroutines { viewModel.allTransactions.value.none { it.title == "Tx To Delete" } }
    assertTrue(viewModel.allTransactions.value.none { it.title == "Tx To Delete" })

    try {
      viewModel.deleteTransactionSuspend(tx)
      fail("Deleting ghost transaction should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("no longer exists") == true)
    }
  }

  @Test
  fun testDeleteBudgetSuspendSucceedsAndFailsOnGhost() = runBlocking {
    val monthKey = "2026-11"
    viewModel.saveMonthlyBudgetSuspend(monthKey, 1500.0)
    flushCoroutines { viewModel.allBudgets.value.any { it.monthKey == monthKey } }

    viewModel.deleteBudgetSuspend(monthKey)
    flushCoroutines { viewModel.allBudgets.value.none { it.monthKey == monthKey } }
    assertTrue(viewModel.allBudgets.value.none { it.monthKey == monthKey })

    try {
      viewModel.deleteBudgetSuspend(monthKey)
      fail("Deleting ghost budget should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("no longer exists") == true)
    }
  }

  @Test
  fun testDeleteRecurringRuleSuspendSucceedsAndFailsOnGhost() = runBlocking {
    val ruleId = viewModel.addRecurringRuleSuspend(
      title = "Gym Sub",
      amount = 40.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.FITNESS,
      interval = com.example.data.model.RecurrenceInterval.MONTHLY,
      startDate = System.currentTimeMillis(),
      endDate = null,
      paymentMethod = PaymentMethod.CREDIT_CARD,
      note = ""
    )
    flushCoroutines { viewModel.allRecurringRules.value.any { it.id == ruleId } }

    viewModel.deleteRecurringRuleSuspend(ruleId)
    flushCoroutines { viewModel.allRecurringRules.value.none { it.id == ruleId } }
    assertTrue(viewModel.allRecurringRules.value.none { it.id == ruleId })

    try {
      viewModel.deleteRecurringRuleSuspend(ruleId)
      fail("Deleting ghost rule should throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("no longer exists") == true)
    }
  }

  @Test
  fun testVerifyAndSetRecoveryPassphraseGuestMode() = runBlocking {
    val result = viewModel.verifyAndSetRecoveryPassphrase("new passphrase for guest mode 2026")
    assertTrue("Guest mode passphrase setup should succeed. Got error: ${result.exceptionOrNull()?.message}", result.isSuccess)

    val shortResult = viewModel.verifyAndSetRecoveryPassphrase("short")
    assertTrue("Short passphrase should fail validation. Got success: ${shortResult.isSuccess}", shortResult.isFailure)
  }
}
