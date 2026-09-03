package com.example.data.local

import androidx.room.Entity

@Entity(
  tableName = "paid_recurring_occurrences",
  primaryKeys = ["ruleId", "occurrenceDateKey"]
)
data class PaidRecurringOccurrenceEntity(
  val ruleId: Long,
  val occurrenceDateKey: String, // e.g. "2026-09-05"
  val paidTransactionId: Long = -1L,
  val paidTimestamp: Long = System.currentTimeMillis(),
  val isCancelled: Boolean = false
)
