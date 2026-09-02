package com.example.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
  tableName = "paid_recurring_occurrences",
  primaryKeys = ["ruleId", "occurrenceDate"],
  indices = [
    Index(value = ["ruleId"]),
    Index(value = ["occurrenceDate"])
  ]
)
data class PaidRecurringOccurrenceEntity(
  val ruleId: Long,
  val occurrenceDate: String, // "YYYY-MM-DD"
  val transactionId: Long,
  val paidAt: Long = System.currentTimeMillis()
)
