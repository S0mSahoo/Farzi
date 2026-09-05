package com.example.pro.ai

import com.example.pro.engine.forecast.CashFlowForecastResult
import com.example.pro.engine.whatif.WhatIfSimulationResult

data class FinancialAiResponse(
    val answer: String,
    val keyFacts: Map<String, String>,
    val warnings: List<String>,
    val confidence: String
)
