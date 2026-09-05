package com.example.pro.engine.forecast

data class CashFlowForecastResult(
    val periodKey: String,
    val period: ForecastPeriod,
    val startingPosition: Double,
    val actualIncome: Double,
    val actualExpenses: Double,
    val actualNet: Double,
    val knownFutureIncome: Double,
    val knownFutureExpenses: Double,
    val estimatedRemainingExpenses: Double,
    val estimatedRemainingIncome: Double,
    val projectedTotalIncome: Double,
    val projectedTotalExpenses: Double,
    val projectedEndPeriodPosition: Double,
    val projectedSavings: Double,
    val projectedSavingsRate: Double,
    val confidence: ForecastConfidence,
    val confidenceReason: String,
    val lineItems: List<ForecastLineItem>,
    val warnings: List<String>
)
