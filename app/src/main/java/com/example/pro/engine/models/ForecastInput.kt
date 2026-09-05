package com.example.pro.engine.models

data class ForecastInput(
    val currentBalance: Double,
    val historicalAverageMonthlySpend: Double,
    val currentPeriodSpend: Double,
    val currentPeriodIncome: Double,
    val recurringMonthlyExpense: Double,
    val recurringMonthlyIncome: Double,
    val remainingDaysInPeriod: Int,
    val dailySpendingRate: Double,
    val budgetLimit: Double,
    val budgetRemaining: Double
)
