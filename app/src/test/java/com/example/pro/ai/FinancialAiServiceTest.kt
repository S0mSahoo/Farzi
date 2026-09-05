package com.example.pro.ai

import com.example.data.model.TransactionCategory
import com.example.pro.engine.forecast.CashFlowForecastResult
import com.example.pro.engine.forecast.ForecastConfidence
import com.example.pro.engine.forecast.ForecastPeriod
import com.example.pro.engine.models.FinancialContext
import com.example.pro.engine.whatif.ScenarioChange
import com.example.pro.engine.whatif.ScenarioType
import com.example.pro.engine.whatif.WhatIfScenario
import com.example.pro.engine.whatif.WhatIfSimulationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class FinancialAiServiceTest {

    private lateinit var sampleContext: FinancialContext

    @Before
    fun setUp() {
        sampleContext = FinancialContext(
            analysisPeriodKey = "2026-09",
            monthlyIncome = 85000.0,
            monthlyExpenses = 42000.0,
            savings = 43000.0,
            savingsRate = 50.58,
            categoryBreakdown = mapOf(
                TransactionCategory.FOOD_DINING to 15000.0,
                TransactionCategory.SHOPPING to 12000.0,
                TransactionCategory.UTILITIES to 5000.0
            ),
            spendingTrendsSummary = "Total expense 42000.00 across 3 categories.",
            recurringCommitmentsTotal = 15000.0,
            budgetStatusSummary = "Budget limit: 50000.00, spent: 42000.00",
            forecastSummary = "Derived balance: 43000.00"
        )
    }

    @Test
    fun `test pure deterministic arithmetic query answers locally without AI call`() = runBlocking {
        var aiCalled = false
        val mockProvider = object : AiProvider {
            override fun ask(question: String, context: FinancialContext): Flow<String> = flow {
                aiCalled = true
                emit("AI should not be called for exact metric lookup")
            }
        }

        val service = FinancialAiService(aiProvider = mockProvider)
        val response = service.getAiExplanation("How much did I spend this month?", sampleContext)

        assertFalse("Pure deterministic query should not call AI", aiCalled)
        assertTrue("Answer should contain expenses", response.answer.contains("42,000"))
        assertEquals("Authoritative", response.confidence)
        assertEquals("2026-09", response.keyFacts["Period"])
    }

    @Test
    fun `test explanatory query delegates to AI provider with enriched context`() = runBlocking {
        var capturedQuestion = ""
        var capturedContext: FinancialContext? = null

        val mockProvider = object : AiProvider {
            override fun ask(question: String, context: FinancialContext): Flow<String> = flow {
                capturedQuestion = question
                capturedContext = context
                emit("Gemini explanation: You spent more on Food (₹15,000) this month.")
            }
        }

        val service = FinancialAiService(aiProvider = mockProvider)
        val response = service.getAiExplanation("Why did I spend more this month?", sampleContext)

        assertEquals("Why did I spend more this month?", capturedQuestion)
        assertNotNull(capturedContext)
        assertEquals("Gemini explanation: You spent more on Food (₹15,000) this month.", response.answer)
        assertTrue(response.keyFacts["Total Income"]?.contains("85,000") == true)
        assertTrue(response.keyFacts["Total Expenses"]?.contains("42,000") == true)
    }

    @Test
    fun `test what if query enriches context with simulation results and passes to AI`() = runBlocking {
        var capturedPromptContext: FinancialContext? = null

        val mockProvider = object : AiProvider {
            override fun ask(question: String, context: FinancialContext): Flow<String> = flow {
                capturedPromptContext = context
                emit("Gemini simulation advice: Spending ₹20,000 on shopping will reduce your month-end position to ₹23,000.")
            }
        }

        val now = LocalDate.now()
        val ym = YearMonth.of(now.year, now.monthValue)
        val forecastPeriod = ForecastPeriod(
            yearMonth = ym,
            startDate = ym.atDay(1),
            endDate = ym.atEndOfMonth(),
            today = now,
            totalDaysInPeriod = ym.lengthOfMonth(),
            elapsedDays = now.dayOfMonth,
            remainingDays = ym.lengthOfMonth() - now.dayOfMonth
        )

        val currentForecast = CashFlowForecastResult(
            periodKey = "2026-09",
            period = forecastPeriod,
            startingPosition = 0.0,
            actualIncome = 85000.0,
            actualExpenses = 42000.0,
            actualNet = 43000.0,
            knownFutureIncome = 0.0,
            knownFutureExpenses = 0.0,
            estimatedRemainingExpenses = 0.0,
            estimatedRemainingIncome = 0.0,
            projectedTotalIncome = 85000.0,
            projectedTotalExpenses = 42000.0,
            projectedEndPeriodPosition = 43000.0,
            projectedSavings = 43000.0,
            projectedSavingsRate = 50.58,
            confidence = ForecastConfidence.HIGH,
            confidenceReason = "Consistent transaction data",
            lineItems = emptyList(),
            warnings = emptyList()
        )

        val simulatedForecast = currentForecast.copy(
            projectedEndPeriodPosition = 23000.0
        )

        val simulation = WhatIfSimulationResult(
            scenario = WhatIfScenario(
                type = ScenarioType.ONE_TIME_EXPENSE,
                change = ScenarioChange.OneTimeExpense(20000.0, now, TransactionCategory.SHOPPING)
            ),
            currentForecast = currentForecast,
            simulatedForecast = simulatedForecast,
            warnings = listOf("Spending ₹20,000 reduces projected savings.")
        )

        val service = FinancialAiService(aiProvider = mockProvider)
        val response = service.getAiExplanation(
            question = "What happens if I spend ₹20,000 today?",
            context = sampleContext,
            simulation = simulation
        )

        assertNotNull(capturedPromptContext?.simulationDetails)
        assertTrue(capturedPromptContext!!.simulationDetails!!.contains("20,000"))
        assertTrue(response.answer.contains("Gemini simulation advice"))
        assertTrue(response.keyFacts["Current Projected Balance"]?.contains("43,000") == true)
        assertTrue(response.keyFacts["Simulated Projected Balance"]?.contains("23,000") == true)
        assertTrue(response.keyFacts["Projected Impact"]?.contains("20,000") == true)
    }

    @Test
    fun `test fallback when AI provider throws exception`() = runBlocking {
        val failingProvider = object : AiProvider {
            override fun ask(question: String, context: FinancialContext): Flow<String> = flow {
                throw RuntimeException("Network unreachable")
            }
        }

        val service = FinancialAiService(aiProvider = failingProvider)
        val response = service.getAiExplanation("Why did my savings drop?", sampleContext)

        assertNotNull(response.answer)
        assertTrue("Fallback answer should contain period facts", response.answer.contains("2026-09"))
        assertTrue(response.keyFacts["Total Income"]?.contains("85,000") == true)
    }
}
