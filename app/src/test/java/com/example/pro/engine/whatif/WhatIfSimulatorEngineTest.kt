package com.example.pro.engine.whatif

import com.example.data.model.*
import com.example.pro.engine.forecast.CashFlowForecastEngine
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WhatIfSimulatorEngineTest {

    private val forecastEngine = CashFlowForecastEngine()
    private val simulator = WhatIfSimulatorEngine(forecastEngine)

    @Test
    fun `test one-time expense scenario`() {
        val ym = YearMonth.of(2026, 9)
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

        val scenario = WhatIfScenario(
            ScenarioType.ONE_TIME_EXPENSE,
            ScenarioChange.OneTimeExpense(20000.0, LocalDate.of(2026, 9, 15))
        )

        val result = simulator.simulate(
            allTransactions = transactions,
            recurringRules = emptyList(),
            paidOccurrences = emptyList(),
            activeBudget = null,
            yearMonth = ym,
            scenario = scenario
        )

        assertTrue(result.projectedDifference < 0)
        assertEquals(-20000.0, result.projectedDifference, 0.001)
    }
}
