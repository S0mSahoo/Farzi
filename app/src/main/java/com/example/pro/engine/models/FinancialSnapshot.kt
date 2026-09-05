package com.example.pro.engine.models

import com.example.data.model.BudgetModel

data class FinancialSnapshot(
    val analysisPeriodKey: String, // e.g. "2026-09"
    val totalIncome: Double,
    val totalExpenses: Double,
    val savings: Double,
    val savingsRate: Double,
    val derivedBalance: Double, // Cumulative income - cumulative expenses across all records
    val spendingAnalysis: SpendingAnalysis,
    val recurringSummary: RecurringCommitmentSummary,
    val activeBudget: BudgetModel?,
    val budgetUsagePercent: Double,
    val forecastInput: ForecastInput
)
