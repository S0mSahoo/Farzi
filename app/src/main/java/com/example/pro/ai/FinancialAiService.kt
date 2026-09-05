package com.example.pro.ai

import com.example.pro.engine.forecast.CashFlowForecastResult
import com.example.pro.engine.models.FinancialContext
import com.example.pro.engine.whatif.ScenarioChange
import com.example.pro.engine.whatif.WhatIfScenario
import com.example.pro.engine.whatif.WhatIfSimulationResult
import com.example.ui.components.IndianCurrencyFormatter
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale

/**
 * Service orchestrating AI-assisted financial inquiries.
 * The deterministic engines are the authoritative source of truth for all calculations.
 * The AI provider (Gemini) is invoked to interpret, explain, and reason about the verified facts.
 */
class FinancialAiService(
    private val aiProvider: AiProvider = GeminiAiProvider()
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

        // 1. Always extract verified deterministic key facts
        keyFacts["Period"] = context.analysisPeriodKey
        keyFacts["Total Income"] = IndianCurrencyFormatter.formatWithSymbol(context.monthlyIncome, currencySymbol)
        keyFacts["Total Expenses"] = IndianCurrencyFormatter.formatWithSymbol(context.monthlyExpenses, currencySymbol)
        keyFacts["Net Savings"] = IndianCurrencyFormatter.formatWithSymbol(context.savings, currencySymbol, includeSign = true)
        keyFacts["Savings Rate"] = String.format(Locale.getDefault(), "%.1f%%", context.savingsRate)

        val topCategory = context.categoryBreakdown.maxByOrNull { it.value }
        if (topCategory != null && topCategory.value > 0.0) {
            keyFacts["Top Category"] = "${topCategory.key.displayName} (${IndianCurrencyFormatter.formatWithSymbol(topCategory.value, currencySymbol)})"
        }

        if (forecast != null) {
            keyFacts["Projected End Balance"] = IndianCurrencyFormatter.formatWithSymbol(forecast.projectedEndPeriodPosition, currencySymbol)
            keyFacts["Upcoming Recurring Expenses"] = IndianCurrencyFormatter.formatWithSymbol(forecast.knownFutureExpenses, currencySymbol)
            keyFacts["Estimated Discretionary Run-Rate"] = IndianCurrencyFormatter.formatWithSymbol(forecast.estimatedRemainingExpenses, currencySymbol)
            keyFacts["Forecast Confidence"] = forecast.confidence.name
            warnings.addAll(forecast.warnings)
        }

        if (simulation != null) {
            val diff = simulation.projectedDifference
            keyFacts["Current Projected Balance"] = IndianCurrencyFormatter.formatWithSymbol(simulation.currentForecast.projectedEndPeriodPosition, currencySymbol)
            keyFacts["Simulated Projected Balance"] = IndianCurrencyFormatter.formatWithSymbol(simulation.simulatedForecast.projectedEndPeriodPosition, currencySymbol)
            keyFacts["Projected Impact"] = IndianCurrencyFormatter.formatWithSymbol(diff, currencySymbol, includeSign = true)
            warnings.addAll(simulation.warnings)
        }

        // 2. Pure deterministic direct arithmetic check (e.g. "how much did I spend this month")
        if (isPureDeterministicLookup(question)) {
            val lookupAnswer = answerPureDeterministic(question, context, currencySymbol)
            if (lookupAnswer != null) {
                return FinancialAiResponse(
                    answer = lookupAnswer,
                    keyFacts = keyFacts,
                    warnings = warnings,
                    confidence = "Authoritative"
                )
            }
        }

        // 3. Build enriched context with formatted forecast & simulation strings
        val forecastDetails = if (forecast != null) {
            "- Projected Month-End Balance: ${IndianCurrencyFormatter.formatWithSymbol(forecast.projectedEndPeriodPosition, currencySymbol)}\n" +
            "- Known Upcoming Expenses: ${IndianCurrencyFormatter.formatWithSymbol(forecast.knownFutureExpenses, currencySymbol)}\n" +
            "- Estimated Remaining Discretionary Spending: ${IndianCurrencyFormatter.formatWithSymbol(forecast.estimatedRemainingExpenses, currencySymbol)}\n" +
            "- Forecast Confidence: ${forecast.confidence.name}\n" +
            "- Warnings: ${if (forecast.warnings.isNotEmpty()) forecast.warnings.joinToString("; ") else "None"}"
        } else null

        val simulationDetails = if (simulation != null) {
            val diff = simulation.projectedDifference
            "- Current Projected Position: ${IndianCurrencyFormatter.formatWithSymbol(simulation.currentForecast.projectedEndPeriodPosition, currencySymbol)}\n" +
            "- Simulated Projected Position: ${IndianCurrencyFormatter.formatWithSymbol(simulation.simulatedForecast.projectedEndPeriodPosition, currencySymbol)}\n" +
            "- Net Projected Impact: ${IndianCurrencyFormatter.formatWithSymbol(diff, currencySymbol, includeSign = true)}\n" +
            "- Scenario Description: ${describeScenario(simulation.scenario, currencySymbol)}\n" +
            "- Scenario Warnings: ${if (simulation.warnings.isNotEmpty()) simulation.warnings.joinToString("; ") else "None"}"
        } else null

        val enrichedContext = context.copy(
            forecastDetails = forecastDetails,
            simulationDetails = simulationDetails
        )

        // 4. Delegate reasoning and natural language explanation to Gemini AI provider
        val aiResponseText = try {
            aiProvider.ask(question, enrichedContext).firstOrNull()
        } catch (e: Exception) {
            null
        }

        val answer = if (!aiResponseText.isNullOrBlank()) {
            aiResponseText
        } else {
            // Graceful offline fallback
            "Paisa AI Copilot is currently offline or unable to reach Gemini. " +
            "Based on your recorded facts for ${context.analysisPeriodKey}, your total income is ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyIncome, currencySymbol)}, " +
            "total expenses are ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyExpenses, currencySymbol)}, and net savings are ${IndianCurrencyFormatter.formatWithSymbol(context.savings, currencySymbol, includeSign = true)}."
        }

        return FinancialAiResponse(
            answer = answer,
            keyFacts = keyFacts,
            warnings = warnings,
            confidence = forecast?.confidence?.name ?: "Verified"
        )
    }

    private fun isPureDeterministicLookup(question: String): Boolean {
        val q = question.lowercase().trim()
        val isExactTotal = (q.startsWith("how much") || q.startsWith("what is my")) &&
            (q.contains("spend") || q.contains("spent") || q.contains("income") || q.contains("savings rate") || q.contains("net savings")) &&
            !q.contains("why") && !q.contains("what if") && !q.contains("forecast") && !q.contains("compare") && !q.contains("should") && !q.contains("can i")
        return isExactTotal
    }

    private fun answerPureDeterministic(question: String, context: FinancialContext, currencySymbol: String): String? {
        val q = question.lowercase().trim()
        return when {
            q.contains("spend") || q.contains("spent") || q.contains("expense") -> {
                "In ${context.analysisPeriodKey}, your total recorded expenses are ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyExpenses, currencySymbol)}."
            }
            q.contains("income") || q.contains("earn") -> {
                "In ${context.analysisPeriodKey}, your total recorded income is ${IndianCurrencyFormatter.formatWithSymbol(context.monthlyIncome, currencySymbol)}."
            }
            q.contains("saving") || q.contains("saved") -> {
                "In ${context.analysisPeriodKey}, your net savings are ${IndianCurrencyFormatter.formatWithSymbol(context.savings, currencySymbol, includeSign = true)} with a savings rate of ${String.format(Locale.getDefault(), "%.1f%%", context.savingsRate)}."
            }
            else -> null
        }
    }

    private fun describeScenario(scenario: WhatIfScenario, currencySymbol: String): String {
        return when (val change = scenario.change) {
            is ScenarioChange.OneTimeExpense -> "One-time expense of ${IndianCurrencyFormatter.formatWithSymbol(change.amount, currencySymbol)} on ${change.date} (${change.category.displayName})"
            is ScenarioChange.RecurringExpenseChange -> "Recurring rule #${change.ruleId} amount changed to ${IndianCurrencyFormatter.formatWithSymbol(change.newAmount, currencySymbol)}"
            is ScenarioChange.RecurringExpenseRemoval -> "Recurring rule #${change.ruleId} cancelled"
            is ScenarioChange.IncomeChange -> if (change.isPercentage) "Income changed by ${change.amountOrPercentage}%" else "Income changed by ${IndianCurrencyFormatter.formatWithSymbol(change.amountOrPercentage, currencySymbol)}"
            is ScenarioChange.SavingsAdjustment -> "Monthly savings target set to ${IndianCurrencyFormatter.formatWithSymbol(change.monthlyTarget, currencySymbol)}"
        }
    }
}
