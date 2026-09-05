package com.example.pro.engine.forecast

import java.time.LocalDate
import java.time.YearMonth

data class ForecastPeriod(
    val yearMonth: YearMonth,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val today: LocalDate,
    val totalDaysInPeriod: Int,
    val elapsedDays: Int,
    val remainingDays: Int
)
