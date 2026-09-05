package com.example.pro.engine.models

import com.example.data.model.TransactionCategory

data class FinancialContext(
    val analysisPeriodKey: String,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val savings: Double,
    val savingsRate: Double,
    val categoryBreakdown: Map<TransactionCategory, Double>,
    val spendingTrendsSummary: String,
    val recurringCommitmentsTotal: Double,
    val budgetStatusSummary: String,
    val forecastSummary: String
)
