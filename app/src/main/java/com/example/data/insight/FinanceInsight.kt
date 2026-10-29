package com.example.data.insight

import androidx.compose.runtime.Immutable
import com.example.data.model.TransactionCategory

enum class InsightSeverity {
  INFO,
  SUCCESS,
  WARNING,
  CRITICAL,
  NEUTRAL
}

enum class InsightType {
  PERIOD_COMPARISON,
  TOP_CATEGORY,
  SAVINGS_RATE,
  BUDGET_STATUS,
  RECURRING_COMMITMENTS,
  CASH_FLOW
}

@Immutable
data class FinanceInsight(
  val id: String,
  val type: InsightType,
  val title: String,
  val description: String,
  val severity: InsightSeverity,
  val amount: Double? = null,
  val percentage: Double? = null,
  val monthKey: String? = null,
  val category: TransactionCategory? = null
)
