package com.example.pro.engine.forecast

import com.example.data.model.*
import com.example.data.local.PaidRecurringOccurrenceEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CashFlowForecastEngineTest {

    private val forecastEngine = CashFlowForecastEngine()

    @Test
    fun `test basic forecast period calculation`() {
        val ym = YearMonth.of(2026, 9)
        val refDate = LocalDate.of(2026, 9, 15)
        val period = forecastEngine.calculatePeriod(ym, refDate)

        assertEquals(ym, period.yearMonth)
        assertEquals(LocalDate.of(2026, 9, 1), period.startDate)
        assertEquals(LocalDate.of(2026, 9, 30), period.endDate)
        assertEquals(refDate, period.today)
        assertEquals(30, period.totalDaysInPeriod)
        assertEquals(15, period.elapsedDays)
        assertEquals(15, period.remainingDays)
    }

    @Test
    fun `test forecast generation with actuals and recurring rules`() {
        val ym = YearMonth.of(2026, 9)
        val refDate = LocalDate.of(2026, 9, 15)

        val transactions = listOf(
            TransactionItem(
                id = 1,
                title = "Salary",
                amount = 60000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                timestamp = LocalDate.of(2026, 9, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            ),
            TransactionItem(
                id = 2,
                title = "Groceries",
                amount = 10000.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES,
                timestamp = LocalDate.of(2026, 9, 10).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        )

        val recurringRules = listOf(
            RecurringRule(
                id = 10,
                title = "Rent",
                amount = 20000.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.HOUSING,
                interval = RecurrenceInterval.MONTHLY,
                startDate = LocalDate.of(2026, 9, 20).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        )

        val paidOccurrences = emptyList<PaidRecurringOccurrenceEntity>()
        val activeBudget = BudgetModel(monthKey = "2026-09", totalBudget = 40000.0)

        val result = forecastEngine.generateForecast(
            allTransactions = transactions,
            recurringRules = recurringRules,
            paidOccurrences = paidOccurrences,
            activeBudget = activeBudget,
            yearMonth = ym,
            referenceDate = refDate
        )

        assertEquals(60000.0, result.actualIncome, 0.001)
        assertEquals(10000.0, result.actualExpenses, 0.001)
        assertEquals(20000.0, result.knownFutureExpenses, 0.001) // Rent on Sept 20
        assertTrue(result.estimatedRemainingExpenses > 0.0)
        assertNotNull(result.confidence)
        assertTrue(result.lineItems.isNotEmpty())
    }

    @Test
    fun `test recurring occurrence paid deduplication`() {
        val ym = YearMonth.of(2026, 9)
        val refDate = LocalDate.of(2026, 9, 15)

        val transactions = listOf(
            TransactionItem(
                id = 1,
                title = "Salary",
                amount = 50000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                timestamp = LocalDate.of(2026, 9, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        )

        val recurringRules = listOf(
            RecurringRule(
                id = 10,
                title = "Internet",
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.UTILITIES,
                interval = RecurrenceInterval.MONTHLY,
                startDate = LocalDate.of(2026, 9, 18).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        )

        // Mark internet on Sept 18 as already paid
        val paidOccurrences = listOf(
            PaidRecurringOccurrenceEntity(
                ruleId = 10,
                occurrenceDateKey = "2026-09-18",
                isCancelled = false
            )
        )

        val result = forecastEngine.generateForecast(
            allTransactions = transactions,
            recurringRules = recurringRules,
            paidOccurrences = paidOccurrences,
            activeBudget = null,
            yearMonth = ym,
            referenceDate = refDate
        )

        // Since it's marked paid, knownFutureExpenses for this rule should be 0.0
        assertEquals(0.0, result.knownFutureExpenses, 0.001)
    }

    @Test
    fun `test numeric safety and zero division prevention`() {
        val ym = YearMonth.of(2026, 9)
        val result = forecastEngine.generateForecast(
            allTransactions = emptyList(),
            recurringRules = emptyList(),
            paidOccurrences = emptyList(),
            activeBudget = null,
            yearMonth = ym,
            referenceDate = LocalDate.of(2026, 9, 10)
        )

        assertFalse(result.projectedSavingsRate.isNaN())
        assertFalse(result.projectedSavingsRate.isInfinite())
        assertEquals(ForecastConfidence.INSUFFICIENT_DATA, result.confidence)
    }
}
