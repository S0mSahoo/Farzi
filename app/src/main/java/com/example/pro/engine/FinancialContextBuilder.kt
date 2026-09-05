package com.example.pro.engine

import com.example.pro.engine.models.FinancialContext
import com.example.pro.engine.models.FinancialSnapshot
import java.util.Locale

class FinancialContextBuilder(private val engine: FinancialIntelligenceEngine) {

    fun buildContext(snapshot: FinancialSnapshot): FinancialContext {
        val categoryBreakdown = snapshot.spendingAnalysis.categoryTrends.associate { it.category to it.currentPeriodSpend }

        val trendsSummary = String.format(
            Locale.getDefault(),
            "Total expense: %.2f across %d active categories. Highest spend category: %s (%.2f). Unusual spending detected: %b.",
            snapshot.spendingAnalysis.totalExpense,
            snapshot.spendingAnalysis.categoryTrends.size,
            snapshot.spendingAnalysis.highestCategory?.displayName ?: "None",
            snapshot.spendingAnalysis.highestCategorySpend,
            snapshot.spendingAnalysis.unusualSpendingDetected
        )

        val budgetSummary = if (snapshot.activeBudget != null) {
            String.format(
                Locale.getDefault(),
                "Budget limit: %.2f, spent: %.2f, usage: %.1f%%.",
                snapshot.activeBudget.totalBudget,
                snapshot.totalExpenses,
                snapshot.budgetUsagePercent
            )
        } else {
            "No active budget configured for this period."
        }

        val forecastSummary = String.format(
            Locale.getDefault(),
            "Derived balance: %.2f, recurring monthly commitment: %.2f, daily spending rate: %.2f.",
            snapshot.derivedBalance,
            snapshot.recurringSummary.totalMonthlyRecurringExpense,
            snapshot.spendingAnalysis.dailySpendingRate
        )

        return FinancialContext(
            analysisPeriodKey = snapshot.analysisPeriodKey,
            monthlyIncome = snapshot.totalIncome,
            monthlyExpenses = snapshot.totalExpenses,
            savings = snapshot.savings,
            savingsRate = snapshot.savingsRate,
            categoryBreakdown = categoryBreakdown,
            spendingTrendsSummary = trendsSummary,
            recurringCommitmentsTotal = snapshot.recurringSummary.totalMonthlyRecurringExpense,
            budgetStatusSummary = budgetSummary,
            forecastSummary = forecastSummary
        )
    }
}
