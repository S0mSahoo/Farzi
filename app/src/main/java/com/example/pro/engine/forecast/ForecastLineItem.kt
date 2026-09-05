package com.example.pro.engine.forecast

enum class ForecastCategoryType {
    ACTUAL,
    KNOWN_FUTURE,
    ESTIMATED_REMAINING
}

data class ForecastLineItem(
    val title: String,
    val amount: Double,
    val type: ForecastCategoryType,
    val description: String
)
