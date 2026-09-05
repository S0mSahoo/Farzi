package com.example.pro.engine.models

import com.example.data.model.TransactionCategory

data class CategorySpendingTrend(
    val category: TransactionCategory,
    val currentPeriodSpend: Double,
    val previousPeriodSpend: Double,
    val absoluteChange: Double,
    val percentageChange: Double, // Safe division (0.0 if previous is 0)
    val historicalAverage: Double
)
