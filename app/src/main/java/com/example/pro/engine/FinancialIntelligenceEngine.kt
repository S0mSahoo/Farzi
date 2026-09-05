package com.example.pro.engine

import com.example.data.model.TransactionItem
import com.example.data.model.BudgetModel
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.pro.engine.models.FinancialSnapshot
import java.time.YearMonth

class FinancialIntelligenceEngine {

    private val spendingEngine = SpendingAnalysisEngine()
    private val recurringAnalyzer = RecurringCommitmentAnalyzer()
    private val cashFlowEngine = CashFlowAnalysisEngine()

    fun calculateTotalIncome(transactions: List<TransactionItem>): Double {
        return transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    fun calculateTotalExpenses(transactions: List<TransactionItem>): Double {
        return transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    fun calculateSavings(totalIncome: Double, totalExpenses: Double): Double {
        return totalIncome - totalExpenses
    }

    fun calculateSavingsRate(totalIncome: Double, totalExpenses: Double): Double {
        return FinancialCalculationUtils.safeRatio(calculateSavings(totalIncome, totalExpenses), totalIncome)
    }

    fun deriveCurrentBalance(allTransactions: List<TransactionItem>): Double {
        val totalInc = calculateTotalIncome(allTransactions)
        val totalExp = calculateTotalExpenses(allTransactions)
        return totalInc - totalExp
    }

    fun generateSnapshot(
        allTransactions: List<TransactionItem>,
        currentYearMonth: YearMonth,
        recurringRules: List<RecurringRule>,
        activeBudget: BudgetModel?
    ): FinancialSnapshot {
        val currentTransactions = allTransactions.filter {
            FinancialCalculationUtils.isTimestampInYearMonth(it.timestamp, currentYearMonth)
        }
        val previousYearMonth = currentYearMonth.minusMonths(1)
        val previousTransactions = allTransactions.filter {
            FinancialCalculationUtils.isTimestampInYearMonth(it.timestamp, previousYearMonth)
        }

        val totalInc = calculateTotalIncome(currentTransactions)
        val totalExp = calculateTotalExpenses(currentTransactions)
        val savings = calculateSavings(totalInc, totalExp)
        val savingsRate = calculateSavingsRate(totalInc, totalExp)
        val derivedBalance = deriveCurrentBalance(allTransactions)

        val daysInPeriod = currentYearMonth.lengthOfMonth()
        val today = java.time.LocalDate.now()
        val elapsedDays = if (YearMonth.from(today) == currentYearMonth) {
            today.dayOfMonth.coerceIn(1, daysInPeriod)
        } else {
            daysInPeriod
        }

        val spendingAnalysis = spendingEngine.analyze(currentTransactions, previousTransactions, daysInPeriod, elapsedDays)
        val recurringSummary = recurringAnalyzer.analyze(recurringRules, totalInc)

        val budgetLimit = activeBudget?.totalBudget ?: 0.0
        val budgetUsagePct = if (budgetLimit > 0.0) FinancialCalculationUtils.safeRatio(totalExp, budgetLimit) else 0.0

        val forecastInput = cashFlowEngine.generateForecastInput(
            allTransactions = allTransactions,
            currentYearMonth = currentYearMonth,
            currentBalance = derivedBalance,
            recurringCommitmentAnalyzer = recurringAnalyzer,
            recurringRules = recurringRules,
            budgetModel = activeBudget
        )

        val periodKey = currentYearMonth.toString()

        return FinancialSnapshot(
            analysisPeriodKey = periodKey,
            totalIncome = totalInc,
            totalExpenses = totalExp,
            savings = savings,
            savingsRate = savingsRate,
            derivedBalance = derivedBalance,
            spendingAnalysis = spendingAnalysis,
            recurringSummary = recurringSummary,
            activeBudget = activeBudget,
            budgetUsagePercent = budgetUsagePct,
            forecastInput = forecastInput
        )
    }
}
