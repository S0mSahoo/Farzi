package com.example.pro.engine

import com.example.data.model.TransactionItem
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.pro.engine.models.CategorySpendingTrend
import com.example.pro.engine.models.SpendingAnalysis
import java.time.YearMonth

class SpendingAnalysisEngine {

    fun analyze(
        currentTransactions: List<TransactionItem>,
        previousTransactions: List<TransactionItem>,
        daysInPeriod: Int,
        elapsedDays: Int
    ): SpendingAnalysis {
        val currentExpenses = currentTransactions.filter { it.type == TransactionType.EXPENSE }
        val previousExpenses = previousTransactions.filter { it.type == TransactionType.EXPENSE }

        val totalExpense = currentExpenses.sumOf { it.amount }
        val previousTotalExpense = previousExpenses.sumOf { it.amount }

        val dailyRate = if (elapsedDays > 0) totalExpense / elapsedDays.toDouble() else 0.0

        val currentByCategory = currentExpenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        val previousByCategory = previousExpenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }

        val allCategories = (currentByCategory.keys + previousByCategory.keys).distinct()

        val trends = allCategories.map { category ->
            val currentSpend = currentByCategory[category] ?: 0.0
            val previousSpend = previousByCategory[category] ?: 0.0
            val absChange = currentSpend - previousSpend
            val pctChange = FinancialCalculationUtils.safePercentageChange(currentSpend, previousSpend)

            CategorySpendingTrend(
                category = category,
                currentPeriodSpend = currentSpend,
                previousPeriodSpend = previousSpend,
                absoluteChange = absChange,
                percentageChange = pctChange,
                historicalAverage = previousSpend // simple historical baseline
            )
        }.sortedByDescending { it.currentPeriodSpend }

        val highest = trends.maxByOrNull { it.currentPeriodSpend }

        // Unusual spending detection: category spend > 50% higher than previous period and > 10% of total
        val unusualCategories = trends.filter { trend ->
            trend.percentageChange > 50.0 && trend.currentPeriodSpend > (totalExpense * 0.10) && trend.previousPeriodSpend > 0.0
        }.map { it.category }

        return SpendingAnalysis(
            totalExpense = totalExpense,
            dailySpendingRate = dailyRate,
            categoryTrends = trends,
            highestCategory = highest?.category,
            highestCategorySpend = highest?.currentPeriodSpend ?: 0.0,
            unusualSpendingDetected = unusualCategories.isNotEmpty(),
            unusualCategories = unusualCategories
        )
    }
}
