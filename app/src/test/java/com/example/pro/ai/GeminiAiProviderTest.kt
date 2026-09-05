package com.example.pro.ai

import com.example.data.model.TransactionCategory
import com.example.pro.ai.config.GeminiModelConfiguration
import com.example.pro.ai.gemini.GeminiApiService
import com.example.pro.ai.gemini.GeminiCandidate
import com.example.pro.ai.gemini.GeminiContent
import com.example.pro.ai.gemini.GeminiErrorDetails
import com.example.pro.ai.gemini.GeminiPart
import com.example.pro.ai.gemini.GeminiRequest
import com.example.pro.ai.gemini.GeminiResponse
import com.example.pro.engine.models.FinancialContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeminiAiProviderTest {

    private lateinit var sampleContext: FinancialContext

    @Before
    fun setUp() {
        AiDiagnostics.reset()
        GeminiModelConfiguration.resetToDefault()

        sampleContext = FinancialContext(
            analysisPeriodKey = "2026-09",
            monthlyIncome = 90000.0,
            monthlyExpenses = 45000.0,
            savings = 45000.0,
            savingsRate = 50.0,
            categoryBreakdown = mapOf(
                TransactionCategory.FOOD_DINING to 18000.0,
                TransactionCategory.TRANSPORTATION to 7000.0
            ),
            spendingTrendsSummary = "Total expense 45000.00 across 2 categories.",
            recurringCommitmentsTotal = 12000.0,
            budgetStatusSummary = "Budget limit: 50000.00, spent: 45000.00",
            forecastSummary = "Derived balance: 45000.00",
            forecastDetails = "Projected month-end position: ₹45,000",
            simulationDetails = "Scenario: One-time expense ₹10,000"
        )
    }

    @Test
    fun `test prompt formatting contains labeled ACTUAL, KNOWN, ESTIMATED, FORECAST, SIMULATED sections`() {
        val prompt = GeminiAiProvider.buildLabeledPrompt(
            question = "Why is my food expense so high?",
            context = sampleContext
        )

        assertTrue(prompt.contains("ACTUAL:"))
        assertTrue(prompt.contains("Total Income: ₹90000.00"))
        assertTrue(prompt.contains("Total Expenses: ₹45000.00"))
        assertTrue(prompt.contains("Food & Dining: ₹18000.00"))
        assertTrue(prompt.contains("KNOWN:"))
        assertTrue(prompt.contains("Scheduled Monthly Recurring Commitments Total: ₹12000.00"))
        assertTrue(prompt.contains("ESTIMATED & TRENDS:"))
        assertTrue(prompt.contains("FORECAST:"))
        assertTrue(prompt.contains("Projected month-end position: ₹45,000"))
        assertTrue(prompt.contains("SIMULATED:"))
        assertTrue(prompt.contains("Scenario: One-time expense ₹10,000"))
        assertTrue(prompt.contains("USER QUESTION:\nWhy is my food expense so high?"))
    }

    @Test
    fun `test missing API key gracefully prompts without crashing`() = runBlocking {
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                throw IllegalStateException("API should not be called when key is blank")
            }
        }

        val provider = GeminiAiProvider(
            apiKeyProvider = { "" },
            modelProvider = { "gemini-3.5-flash" },
            apiService = mockService
        )

        val result = provider.ask("Where did my money go?", sampleContext).first()
        assertTrue(result.contains("requires a Gemini API key"))
    }

    @Test
    fun `test successful Gemini call returns candidate text and records diagnostics`() = runBlocking {
        var calledModel = ""
        var calledKey = ""
        var capturedRequest: GeminiRequest? = null

        val mockService = object : GeminiApiService {
            override suspend fun generateContent(model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                calledModel = model
                calledKey = apiKey
                capturedRequest = request
                return GeminiResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            content = GeminiContent(
                                parts = listOf(GeminiPart(text = "Your food spending represents 40% of total expenses this month."))
                            ),
                            finishReason = "STOP"
                        )
                    )
                )
            }
        }

        val provider = GeminiAiProvider(
            apiKeyProvider = { "test_gemini_api_key_123" },
            modelProvider = { "gemini-3.5-flash" },
            apiService = mockService
        )

        val response = provider.ask("Where did my money go?", sampleContext).first()

        assertEquals("gemini-3.5-flash", calledModel)
        assertEquals("test_gemini_api_key_123", calledKey)
        assertNotNull(capturedRequest)
        assertEquals(GeminiAiProvider.SYSTEM_INSTRUCTION, capturedRequest?.systemInstruction?.parts?.firstOrNull()?.text)
        assertEquals("Your food spending represents 40% of total expenses this month.", response)
        assertEquals(1, AiDiagnostics.successfulRequests)
        assertEquals("Success", AiDiagnostics.lastStatus)
    }

    @Test
    fun `test Gemini API error response handled gracefully`() = runBlocking {
        val mockService = object : GeminiApiService {
            override suspend fun generateContent(model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                return GeminiResponse(
                    error = GeminiErrorDetails(
                        code = 400,
                        message = "API key not valid",
                        status = "INVALID_ARGUMENT"
                    )
                )
            }
        }

        val provider = GeminiAiProvider(
            apiKeyProvider = { "invalid_key" },
            modelProvider = { "gemini-3.5-flash" },
            apiService = mockService
        )

        val response = provider.ask("Explain my spending", sampleContext).first()

        assertTrue(response.contains("AI explanation is temporarily unavailable"))
        assertTrue(response.contains("API key not valid"))
        assertEquals(1, AiDiagnostics.failedRequests)
    }
}
