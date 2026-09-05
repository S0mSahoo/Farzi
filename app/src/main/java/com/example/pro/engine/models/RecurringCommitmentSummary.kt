package com.example.pro.engine.models

data class RecurringCommitmentSummary(
    val totalMonthlyRecurringExpense: Double,
    val totalMonthlyRecurringIncome: Double,
    val expenseToIncomeRatio: Double, // Safe division
    val activeSubscriptionCount: Int,
    val upcomingObligationsCount: Int
)
