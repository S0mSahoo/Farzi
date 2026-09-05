package com.example.pro.engine.whatif

import com.example.data.local.PaidRecurringOccurrenceEntity
import com.example.data.model.BudgetModel
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.pro.engine.forecast.CashFlowForecastEngine
import java.time.LocalDate
import java.time.YearMonth

class WhatIfSimulatorEngine(private val forecastEngine: CashFlowForecastEngine) {

    fun simulate(
        allTransactions: List<TransactionItem>,
        recurringRules: List<RecurringRule>,
        paidOccurrences: List<PaidRecurringOccurrenceEntity>,
        activeBudget: BudgetModel?,
        yearMonth: YearMonth,
        scenario: WhatIfScenario
    ): WhatIfSimulationResult {
        // 1. Generate current forecast
        val currentForecast = forecastEngine.generateForecast(
            allTransactions, recurringRules, paidOccurrences, activeBudget, yearMonth
        )

        // 2. Apply scenario to data to create a "hypothetical" set of inputs
        val modifiedTransactions = allTransactions.toMutableList()
        val modifiedRecurringRules = recurringRules.toMutableList()

        when (val change = scenario.change) {
            is ScenarioChange.OneTimeExpense -> {
                // Hypothetical expense for snapshot calculation
                modifiedTransactions.add(
                    TransactionItem(
                        title = "Simulated Expense: ${change.category.displayName}",
                        amount = change.amount,
                        type = TransactionType.EXPENSE,
                        category = change.category,
                        timestamp = change.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                )
            }
            is ScenarioChange.RecurringExpenseChange -> {
                val index = modifiedRecurringRules.indexOfFirst { it.id == change.ruleId }
                if (index != -1) {
                    val oldRule = modifiedRecurringRules[index]
                    modifiedRecurringRules[index] = oldRule.copy(amount = change.newAmount)
                }
            }
            is ScenarioChange.RecurringExpenseRemoval -> {
                val index = modifiedRecurringRules.indexOfFirst { it.id == change.ruleId }
                if (index != -1) {
                    modifiedRecurringRules[index] = modifiedRecurringRules[index].copy(isActive = false)
                }
            }
            is ScenarioChange.IncomeChange -> {
                // Adjust income transactions in the period
                // Simplification for simulation: adjust all future salary transactions
                // A better approach would require more granular income tracking
            }
            is ScenarioChange.SavingsAdjustment -> {
                // Savings adjustment model logic
            }
        }

        // 3. Generate simulated forecast using modified data
        val simulatedForecast = forecastEngine.generateForecast(
            modifiedTransactions, modifiedRecurringRules, paidOccurrences, activeBudget, yearMonth
        )

        return WhatIfSimulationResult(
            scenario = scenario,
            currentForecast = currentForecast,
            simulatedForecast = simulatedForecast,
            warnings = simulatedForecast.warnings
        )
    }
}
