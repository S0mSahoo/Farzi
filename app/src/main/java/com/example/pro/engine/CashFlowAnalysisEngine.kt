package com.example.pro.engine

import com.example.data.model.TransactionItem
import com.example.data.model.BudgetModel
import com.example.data.model.RecurringRule
import com.example.pro.engine.models.ForecastInput
import java.time.YearMonth
import java.time.LocalDate

class CashFlowAnalysisEngine {

    fun generateForecastInput(
        allTransactions: List<TransactionItem>,
        currentYearMonth: YearMonth,
        currentBalance: Double,
        recurringCommitmentAnalyzer: RecurringCommitmentAnalyzer,
        recurringRules: List<RecurringRule>,
        budgetModel: BudgetModel?
    ): ForecastInput {
        val currentTransactions = allTransactions.filter {
            FinancialCalculationUtils.isTimestampInYearMonth(it.timestamp, currentYearMonth)
        }

        val engine = FinancialIntelligenceEngine()
        val currentIncome = engine.calculateTotalIncome(currentTransactions)
        val currentSpend = engine.calculateTotalExpenses(currentTransactions)

        val today = LocalDate.now()
        val daysInPeriod = currentYearMonth.lengthOfMonth()
        val elapsedDays = if (YearMonth.from(today) == currentYearMonth) {
            today.dayOfMonth.coerceIn(1, daysInPeriod)
        } else {
            daysInPeriod
        }
        val remainingDays = (daysInPeriod - elapsedDays).coerceAtLeast(0)
        val dailyRate = if (elapsedDays > 0) currentSpend / elapsedDays.toDouble() else 0.0

        val recurringSummary = recurringCommitmentAnalyzer.analyze(recurringRules, currentIncome)

        val budgetLimit = budgetModel?.totalBudget ?: 0.0
        val budgetRemaining = (budgetLimit - currentSpend).coerceAtLeast(0.0)

        // Historical average monthly spend (computed across available transactions)
        val monthlySpends = allTransactions
            .filter { it.type == com.example.data.model.TransactionType.EXPENSE }
            .groupBy { 
                val date = FinancialCalculationUtils.millisToLocalDate(it.timestamp)
                YearMonth.from(date)
            }
            .map { it.value.sumOf { tx -> tx.amount } }

        val historicalAvgSpend = if (monthlySpends.isNotEmpty()) monthlySpends.average() else currentSpend

        return ForecastInput(
            currentBalance = currentBalance,
            historicalAverageMonthlySpend = historicalAvgSpend,
            currentPeriodSpend = currentSpend,
            currentPeriodIncome = currentIncome,
            recurringMonthlyExpense = recurringSummary.totalMonthlyRecurringExpense,
            recurringMonthlyIncome = recurringSummary.totalMonthlyRecurringIncome,
            remainingDaysInPeriod = remainingDays,
            dailySpendingRate = dailyRate,
            budgetLimit = budgetLimit,
            budgetRemaining = budgetRemaining
        )
    }
}
