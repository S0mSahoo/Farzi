package com.example.pro.engine

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class FinancialIntelligenceEngineTest {

    private val engine = FinancialIntelligenceEngine()
    private val analyzer = RecurringCommitmentAnalyzer()

    private val sampleTransactions = listOf(
        TransactionItem(
            id = 1,
            title = "Salary",
            amount = 50000.0,
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            timestamp = System.currentTimeMillis()
        ),
        TransactionItem(
            id = 2,
            title = "Rent",
            amount = 15000.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.HOUSING,
            timestamp = System.currentTimeMillis()
        ),
        TransactionItem(
            id = 3,
            title = "Groceries",
            amount = 5000.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            timestamp = System.currentTimeMillis()
        )
    )

    @Test
    fun `test basic income and expense calculations`() {
        val income = engine.calculateTotalIncome(sampleTransactions)
        val expense = engine.calculateTotalExpenses(sampleTransactions)
        val savings = engine.calculateSavings(income, expense)
        val savingsRate = engine.calculateSavingsRate(income, expense)

        assertEquals(50000.0, income, 0.001)
        assertEquals(20000.0, expense, 0.001)
        assertEquals(30000.0, savings, 0.001)
        assertEquals(60.0, savingsRate, 0.001)
    }

    @Test
    fun `test zero denominator safety in calculations`() {
        val rate = engine.calculateSavingsRate(0.0, 0.0)
        assertEquals(0.0, rate, 0.001)

        val pct = FinancialCalculationUtils.safePercentageChange(100.0, 0.0)
        assertEquals(100.0, pct, 0.001)
    }

    @Test
    fun `test recurring commitments analyzer`() {
        val rules = listOf(
            RecurringRule(
                id = 1,
                title = "Netflix",
                amount = 500.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.SUBSCRIPTIONS,
                interval = RecurrenceInterval.MONTHLY,
                startDate = System.currentTimeMillis()
            ),
            RecurringRule(
                id = 2,
                title = "Gym",
                amount = 1200.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FITNESS,
                interval = RecurrenceInterval.MONTHLY,
                startDate = System.currentTimeMillis(),
                isActive = false // Inactive should be ignored
            )
        )

        val summary = analyzer.analyze(rules, 50000.0)
        assertEquals(500.0, summary.totalMonthlyRecurringExpense, 0.001)
        assertEquals(1, summary.activeSubscriptionCount)
        assertEquals(1, summary.upcomingObligationsCount)
    }

    @Test
    fun `test snapshot generation and context building`() {
        val currentYM = YearMonth.now()
        val snapshot = engine.generateSnapshot(
            allTransactions = sampleTransactions,
            currentYearMonth = currentYM,
            recurringRules = emptyList(),
            activeBudget = BudgetModel(monthKey = currentYM.toString(), totalBudget = 30000.0)
        )

        assertEquals(50000.0, snapshot.totalIncome, 0.001)
        assertEquals(20000.0, snapshot.totalExpenses, 0.001)
        assertEquals(30000.0, snapshot.savings, 0.001)
        assertEquals(30000.0, snapshot.activeBudget?.totalBudget ?: 0.0, 0.001)

        val contextBuilder = FinancialContextBuilder(engine)
        val context = contextBuilder.buildContext(snapshot)

        assertEquals(50000.0, context.monthlyIncome, 0.001)
        assertEquals(20000.0, context.monthlyExpenses, 0.001)
        assertTrue(context.spendingTrendsSummary.isNotBlank())
        assertTrue(context.budgetStatusSummary.contains("30000.00"))
    }
}
