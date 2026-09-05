package com.example.pro.engine

import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.pro.engine.models.FinancialContext
import com.example.pro.engine.models.FinancialSnapshot
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class FinancialContextBuilder(private val engine: FinancialIntelligenceEngine) {

    fun buildContext(snapshot: FinancialSnapshot, allTransactions: List<TransactionItem> = emptyList()): FinancialContext {
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

        val fullDataSummary = if (allTransactions.isNotEmpty()) {
            val byMonth = allTransactions.groupBy { tx ->
                val date = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                YearMonth.from(date)
            }.toSortedMap()

            byMonth.entries.joinToString(separator = "\n") { (ym, txs) ->
                val inc = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val exp = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val net = inc - exp
                val isSelected = ym.toString() == snapshot.analysisPeriodKey
                String.format(
                    Locale.getDefault(),
                    "Month %s%s: Income=₹%.2f, Expenses=₹%.2f, Net=₹%.2f (Transactions count: %d)",
                    ym,
                    if (isSelected) " [CURRENTLY SELECTED IN UI]" else "",
                    inc,
                    exp,
                    net,
                    txs.size
                )
            }
        } else {
            null
        }

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
            forecastSummary = forecastSummary,
            fullDataSummary = fullDataSummary
        )
    }
}
