package com.example.pro.engine.whatif

import com.example.pro.engine.forecast.CashFlowForecastResult

data class WhatIfSimulationResult(
    val scenario: WhatIfScenario,
    val currentForecast: CashFlowForecastResult,
    val simulatedForecast: CashFlowForecastResult,
    val warnings: List<String>
) {
    val projectedDifference: Double = simulatedForecast.projectedEndPeriodPosition - currentForecast.projectedEndPeriodPosition
}
