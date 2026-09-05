package com.example.pro.ai

import com.example.pro.engine.models.FinancialContext
import com.example.pro.engine.forecast.CashFlowForecastResult
import com.example.pro.engine.whatif.WhatIfSimulationResult
import com.example.ui.components.IndianCurrencyFormatter
import java.util.Locale

/**
 * Service orchestrating AI-assisted and deterministic financial inquiries.
 * The AI is never the financial calculator — deterministic engines provide authoritative facts,
 * and this service presents them clearly with context.
 */
class FinancialAiService(
    private val aiProvider: AiProvider = MockAiProvider()
) {
    suspend fun getAiExplanation(
        question: String,
        context: FinancialContext,
        currencySymbol: String = "₹",
        forecast: CashFlowForecastResult? = null,
        simulation: WhatIfSimulationResult? = null
    ): FinancialAiResponse {
        val intent = IntentClassifier.classify(question)
        val keyFacts = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()

        keyFacts["Period"] = context.analysisPeriodKey
        keyFacts["Total Income"] = IndianCurrencyFormatter.formatWithSymbol(context.monthlyIncome, currencySymbol)
        keyFacts["Total Expenses"] = IndianCurrencyFormatter.formatWithSymbol(context.monthlyExpenses, currencySymbol)
        keyFacts["Net Savings"] = IndianCurrencyFormatter.formatWithSymbol(context.savings, currencySymbol, includeSign = true)

        val answer = when (intent) {
            AiIntent.SPENDING_ANALYSIS -> {
                val highestCategory = context.categoryBreakdown.maxByOrNull { it.value }
                if (highestCategory != null && highestCategory.value > 0.0) {
                    keyFacts["Top Spending Category"] = "${highestCategory.key.displayName}: ${IndianCurrencyFormatter.formatWithSymbol(highestCategory.value, currencySymbol)}"
                }
                if (context.monthlyExpenses == 0.0) {
                    "You haven't recorded any expenses for ${context.analysisPeriodKey} yet."
                } else {
                    val highestText = if (highestCategory != null && highestCategory.value > 0.0) {
                        " Your largest expense category is ${highestCategory.key.displayName} (${IndianCurrencyFormatter.formatWithSymbol(highestCategory.value, currencySymbol)})."
                    } else ""
                    "You have spent a total of ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyExpenses, currencySymbol)} in ${context.analysisPeriodKey}.$highestText"
                }
            }
            AiIntent.SAVINGS_ANALYSIS -> {
                keyFacts["Savings Rate"] = String.format(Locale.getDefault(), "%.1f%%", context.savingsRate)
                if (context.savings >= 0) {
                    "You have saved ${IndianCurrencyFormatter.formatWithSymbol(context.savings, currencySymbol)} in ${context.analysisPeriodKey}, achieving a ${String.format(Locale.getDefault(), "%.1f%%", context.savingsRate)} savings rate."
                } else {
                    warnings.add("Expenses exceeded income for this period.")
                    "Your expenses exceeded income by ${IndianCurrencyFormatter.formatWithSymbol(-context.savings, currencySymbol)} in ${context.analysisPeriodKey}."
                }
            }
            AiIntent.CATEGORY_ANALYSIS -> {
                val activeCategories = context.categoryBreakdown.filter { it.value > 0.0 }
                if (activeCategories.isEmpty()) {
                    "No categorized spending records found for ${context.analysisPeriodKey}."
                } else {
                    val top3 = activeCategories.entries.sortedByDescending { it.value }.take(3)
                    top3.forEach { (cat, amt) ->
                        keyFacts[cat.displayName] = IndianCurrencyFormatter.formatWithSymbol(amt, currencySymbol)
                    }
                    val breakdownStr = top3.joinToString(", ") { "${it.key.displayName}: ${IndianCurrencyFormatter.formatWithSymbol(it.value, currencySymbol)}" }
                    "Your top spending categories for ${context.analysisPeriodKey} are: $breakdownStr."
                }
            }
            AiIntent.RECURRING_ANALYSIS -> {
                keyFacts["Monthly Recurring Total"] = IndianCurrencyFormatter.formatWithSymbol(context.recurringCommitmentsTotal, currencySymbol)
                if (context.recurringCommitmentsTotal > 0.0) {
                    "Your scheduled recurring monthly commitments total ${IndianCurrencyFormatter.formatWithSymbol(context.recurringCommitmentsTotal, currencySymbol)}."
                } else {
                    "You have no active recurring commitments scheduled."
                }
            }
            AiIntent.FORECAST_QUERY -> {
                if (forecast != null) {
                    keyFacts["Projected End Balance"] = IndianCurrencyFormatter.formatWithSymbol(forecast.projectedEndPeriodPosition, currencySymbol)
                    keyFacts["Known Upcoming Expenses"] = IndianCurrencyFormatter.formatWithSymbol(forecast.knownFutureExpenses, currencySymbol)
                    keyFacts["Estimated Remaining Spending"] = IndianCurrencyFormatter.formatWithSymbol(forecast.estimatedRemainingExpenses, currencySymbol)
                    keyFacts["Forecast Confidence"] = forecast.confidence.name
                    warnings.addAll(forecast.warnings)

                    "Based on your recorded income/expenses, known recurring commitments (${IndianCurrencyFormatter.formatWithSymbol(forecast.knownFutureExpenses, currencySymbol)}), and daily run rate, your projected month-end balance is ${IndianCurrencyFormatter.formatWithSymbol(forecast.projectedEndPeriodPosition, currencySymbol)}. Your estimated remaining discretionary allowance is ${IndianCurrencyFormatter.formatWithSymbol(forecast.estimatedRemainingExpenses, currencySymbol)}."
                } else {
                    "Forecast data is currently being evaluated based on your historical records."
                }
            }
            AiIntent.WHAT_IF_QUERY -> {
                if (simulation != null) {
                    val diff = simulation.projectedDifference
                    keyFacts["Current Projection"] = IndianCurrencyFormatter.formatWithSymbol(simulation.currentForecast.projectedEndPeriodPosition, currencySymbol)
                    keyFacts["Simulated Projection"] = IndianCurrencyFormatter.formatWithSymbol(simulation.simulatedForecast.projectedEndPeriodPosition, currencySymbol)
                    keyFacts["Projected Impact"] = IndianCurrencyFormatter.formatWithSymbol(diff, currencySymbol, includeSign = true)
                    warnings.addAll(simulation.warnings)

                    "If this scenario occurs, your projected month-end position would move from ${IndianCurrencyFormatter.formatWithSymbol(simulation.currentForecast.projectedEndPeriodPosition, currencySymbol)} to ${IndianCurrencyFormatter.formatWithSymbol(simulation.simulatedForecast.projectedEndPeriodPosition, currencySymbol)}, creating a net impact of ${IndianCurrencyFormatter.formatWithSymbol(diff, currencySymbol, includeSign = true)}."
                } else {
                    "Simulating this scenario against your current financial run-rate."
                }
            }
            AiIntent.BUDGET_ANALYSIS -> {
                context.budgetStatusSummary
            }
            AiIntent.COMPARISON_QUERY -> {
                "In ${context.analysisPeriodKey}, total recorded income is ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyIncome, currencySymbol)} and expenses are ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyExpenses, currencySymbol)}, yielding net savings of ${IndianCurrencyFormatter.formatWithSymbol(context.savings, currencySymbol, includeSign = true)}."
            }
            AiIntent.TRANSACTION_LOOKUP -> {
                "Financial snapshot for ${context.analysisPeriodKey}: ${context.spendingTrendsSummary}"
            }
            AiIntent.GENERAL_SUMMARY, AiIntent.UNSUPPORTED -> {
                "In ${context.analysisPeriodKey}, you have recorded ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyIncome, currencySymbol)} in income and ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyExpenses, currencySymbol)} in expenses, with net savings of ${IndianCurrencyFormatter.formatWithSymbol(context.savings, currencySymbol, includeSign = true)}."
            }
        }

        return FinancialAiResponse(
            answer = answer,
            keyFacts = keyFacts,
            warnings = warnings,
            confidence = forecast?.confidence?.name ?: "High"
        )
    }
}
