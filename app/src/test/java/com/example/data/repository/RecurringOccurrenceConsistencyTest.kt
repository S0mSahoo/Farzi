package com.example.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecurringOccurrenceConsistencyTest {

  private lateinit var repository: FinanceRepository

  @Before
  fun setUp() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    repository = FinanceRepository(app)
    repository.clearLocalCache()
  }

  private suspend fun createRule(title: String = "Recurring test"): Pair<RecurringRule, Long> {
    val now = System.currentTimeMillis()
    val rule = RecurringRule(
      title = title,
      amount = 125.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.OTHER_EXPENSE,
      interval = RecurrenceInterval.MONTHLY,
      startDate = now,
      endDate = null,
      lastGeneratedDate = 0L,
      paymentMethod = PaymentMethod.UPI,
      note = "Consistency test",
      isActive = true
    )
    val id = repository.insertRecurringRule(rule)
    return rule.copy(id = id) to now
  }

  private fun dateKey(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

  private suspend fun markPaid(rule: RecurringRule, timestamp: Long): Long =
    repository.markRecurringPaymentPaid(rule, timestamp)

  @Test
  fun deletingPaidTransactionAlsoRemovesItsPaidOccurrenceMarker() = runBlocking {
    val (rule, timestamp) = createRule()
    val transactionId = markPaid(rule, timestamp)

    assertEquals(1, repository.allPaidOccurrences.first().size)
    assertEquals(1, repository.deleteTransactionById(transactionId))
    assertTrue(repository.allPaidOccurrences.first().isEmpty())
    assertTrue(repository.allTransactions.first().isEmpty())
  }

  @Test
  fun bulkAndDateRangeDeletionAlsoRemoveLinkedOccurrenceMarkers() = runBlocking {
    val (bulkRule, bulkTimestamp) = createRule("Bulk delete rule")
    val bulkTransactionId = markPaid(bulkRule, bulkTimestamp)
    assertEquals(1, repository.deleteTransactionsByIds(listOf(bulkTransactionId)))
    assertTrue(repository.allPaidOccurrences.first().isEmpty())

    val (rangeRule, rangeTimestamp) = createRule("Range delete rule")
    val rangeTransactionId = markPaid(rangeRule, rangeTimestamp)
    assertEquals(1, repository.deleteTransactionsBetween(rangeTimestamp - 1_000L, rangeTimestamp + 1_000L))
    assertTrue(repository.allTransactions.first().none { it.id == rangeTransactionId })
    assertTrue(repository.allPaidOccurrences.first().isEmpty())
  }

  @Test
  fun skippingPaidOccurrenceFailsAndRestoreCannotErasePaidMarker() = runBlocking {
    val (rule, timestamp) = createRule()
    val transactionId = markPaid(rule, timestamp)
    val key = dateKey(timestamp)

    var rejected = false
    try {
      repository.cancelRecurringOccurrence(rule.id, key)
    } catch (_: IllegalStateException) {
      rejected = true
    }
    assertTrue("A paid occurrence must not be overwritten as skipped", rejected)

    repository.restoreRecurringOccurrence(rule.id, key)
    val marker = repository.allPaidOccurrences.first().single()
    assertEquals(transactionId, marker.paidTransactionId)
    assertFalse(marker.isCancelled)
    assertEquals(1, repository.allTransactions.first().size)
  }

  @Test
  fun restoringSkippedOccurrenceOnlyRemovesItsCancelledMarker() = runBlocking {
    val (rule, timestamp) = createRule()
    val key = dateKey(timestamp)

    repository.cancelRecurringOccurrence(rule.id, key)
    val skipped = repository.allPaidOccurrences.first().single()
    assertTrue(skipped.isCancelled)
    assertEquals(-1L, skipped.paidTransactionId)

    repository.restoreRecurringOccurrence(rule.id, key)
    assertTrue(repository.allPaidOccurrences.first().isEmpty())
  }

  @Test
  fun deletingRuleRemovesScheduleMarkersButPreservesRecordedTransactions() = runBlocking {
    val (rule, timestamp) = createRule("Keep historical payment")
    val transactionId = markPaid(rule, timestamp)

    assertEquals(1, repository.deleteRecurringRule(rule.id))
    assertTrue(repository.allRecurringRules.first().none { it.id == rule.id })
    assertTrue(repository.allPaidOccurrences.first().isEmpty())
    assertEquals(transactionId, repository.allTransactions.first().single().id)
  }
}
