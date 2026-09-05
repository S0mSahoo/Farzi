package com.example.pro.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

object FinancialCalculationUtils {

    fun safePercentageChange(current: Double, previous: Double): Double {
        if (previous == 0.0) {
            return if (current > 0.0) 100.0 else 0.0
        }
        val change = ((current - previous) / kotlin.math.abs(previous)) * 100.0
        if (change.isNaN() || change.isInfinite()) return 0.0
        return change
    }

    fun safeRatio(numerator: Double, denominator: Double): Double {
        if (denominator == 0.0) return 0.0
        val ratio = (numerator / denominator) * 100.0
        if (ratio.isNaN() || ratio.isInfinite()) return 0.0
        return ratio
    }

    fun millisToLocalDate(epochMillis: Long): LocalDate {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun isTimestampInYearMonth(epochMillis: Long, yearMonth: YearMonth): Boolean {
        val date = millisToLocalDate(epochMillis)
        return YearMonth.from(date) == yearMonth
    }
}
