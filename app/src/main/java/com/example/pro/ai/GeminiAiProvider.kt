package com.example.pro.ai

import android.util.Log
import com.example.BuildConfig
import com.example.pro.ai.config.GeminiModelConfiguration
import com.example.pro.ai.gemini.GeminiApiService
import com.example.pro.ai.gemini.GeminiContent
import com.example.pro.ai.gemini.GeminiGenerationConfig
import com.example.pro.ai.gemini.GeminiPart
import com.example.pro.ai.gemini.GeminiRequest
import com.example.pro.ai.gemini.GeminiRetrofitClient
import com.example.pro.engine.models.FinancialContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.util.Locale

/**
 * Production Gemini implementation of AiProvider.
 * Connects directly to Google Generative Language REST API using the configured model.
 */
class GeminiAiProvider(
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
    private val modelProvider: () -> String = { GeminiModelConfiguration.activeModel },
    private val apiService: GeminiApiService = GeminiRetrofitClient.apiService
) : AiProvider {

    override fun ask(question: String, context: FinancialContext): Flow<String> = flow {
        val apiKey = apiKeyProvider().trim()
        val model = modelProvider().trim()

        if (apiKey.isBlank()) {
            Log.w(TAG, "Gemini API key is not configured in BuildConfig / .env")
            emit("Paisa AI Copilot requires a Gemini API key. Please configure your API key in AI Studio Secrets.")
            return@flow
        }

        val labeledPrompt = buildLabeledPrompt(question, context)
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = labeledPrompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = SYSTEM_INSTRUCTION))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.3f,
                topP = 0.95f,
                topK = 40,
                maxOutputTokens = 800
            )
        )

        val startTime = System.currentTimeMillis()
        AiDiagnostics.recordAttempt(model)

        try {
            val response = apiService.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            val duration = System.currentTimeMillis() - startTime

            if (response.error != null) {
                val errorMsg = response.error.message ?: "Unknown API error"
                Log.e(TAG, "Gemini API error (Code ${response.error.code}): $errorMsg")
                AiDiagnostics.recordFailure(model, "API_${response.error.code}")
                emit("AI explanation is temporarily unavailable: $errorMsg")
                return@flow
            }

            val text = response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("") ?: ""
            if (text.isNotBlank()) {
                AiDiagnostics.recordSuccess(model, duration)
                emit(text.trim())
            } else {
                Log.w(TAG, "Gemini returned empty candidate content")
                AiDiagnostics.recordFailure(model, "EMPTY_RESPONSE")
                emit("I was unable to generate an explanation for this query. Please check your financial records or try asking differently.")
            }
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Network failure contacting Gemini: ${e.message}")
            AiDiagnostics.recordFailure(model, "NETWORK_ERROR")
            emit("Network connection issue. Please check your internet connection and try again.")
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Unexpected error calling Gemini: ${e.message}")
            AiDiagnostics.recordFailure(model, "UNEXPECTED_EXCEPTION")
            emit("Encountered an unexpected issue processing your financial explanation. Please try again.")
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "PaisaGemini"

        const val SYSTEM_INSTRUCTION = """You are the AI Financial Copilot for Paisa, a personal expense tracking application.
The financial facts and calculations provided in the prompt are pre-calculated by Paisa's deterministic engine and are 100% authoritative.
Rules:
1. NEVER invent, fabricate, or hallucinate financial figures, transactions, or account balances.
2. NEVER claim access to private records or banking data not provided in the prompt.
3. Clearly distinguish ACTUAL (recorded history) from KNOWN (scheduled recurring rules), ESTIMATED (discretionary run rate), FORECAST (projected future), and SIMULATED (hypothetical scenario) figures.
4. Do NOT represent hypothetical or simulated scenarios as real transactions.
5. If the provided context is insufficient to answer the question, clearly state that more transaction records are needed.
6. Keep your answers concise, clear, and helpful (under 3-4 short paragraphs). Use bold formatting for key metrics.
7. Do not provide regulated financial, legal, tax, or investment advice."""

        fun buildLabeledPrompt(question: String, context: FinancialContext): String {
            val sb = StringBuilder()
            sb.appendLine("AUTHORITATIVE FINANCIAL CONTEXT (PRE-CALCULATED BY PAISA ENGINE):")
            sb.appendLine("Analysis Period: ${context.analysisPeriodKey}")
            sb.appendLine()
            sb.appendLine("ACTUAL:")
            sb.appendLine("- Total Income: ₹${String.format(Locale.US, "%.2f", context.monthlyIncome)}")
            sb.appendLine("- Total Expenses: ₹${String.format(Locale.US, "%.2f", context.monthlyExpenses)}")
            sb.appendLine("- Net Savings: ₹${String.format(Locale.US, "%.2f", context.savings)} (Savings Rate: ${String.format(Locale.US, "%.1f", context.savingsRate)}%)")

            if (context.categoryBreakdown.isNotEmpty()) {
                sb.appendLine("- Spending by Category:")
                context.categoryBreakdown.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
                    if (amt > 0.0) {
                        sb.appendLine("  * ${cat.displayName}: ₹${String.format(Locale.US, "%.2f", amt)}")
                    }
                }
            }

            sb.appendLine()
            sb.appendLine("KNOWN:")
            sb.appendLine("- Scheduled Monthly Recurring Commitments Total: ₹${String.format(Locale.US, "%.2f", context.recurringCommitmentsTotal)}")
            sb.appendLine("- Budget Status: ${context.budgetStatusSummary}")

            sb.appendLine()
            sb.appendLine("ESTIMATED & TRENDS:")
            sb.appendLine("- Spending Trends: ${context.spendingTrendsSummary}")
            sb.appendLine("- Daily Run Rate & Derived Position: ${context.forecastSummary}")

            if (!context.forecastDetails.isNullOrBlank()) {
                sb.appendLine()
                sb.appendLine("FORECAST:")
                sb.appendLine(context.forecastDetails)
            }

            if (!context.simulationDetails.isNullOrBlank()) {
                sb.appendLine()
                sb.appendLine("SIMULATED:")
                sb.appendLine(context.simulationDetails)
            }

            if (!context.fullDataSummary.isNullOrBlank()) {
                sb.appendLine()
                sb.appendLine("FULL DATA ACROSS ALL MONTHS (Previous, Current, Future):")
                sb.appendLine(context.fullDataSummary)
            }

            sb.appendLine()
            sb.appendLine("USER QUESTION:")
            sb.appendLine(question)
            return sb.toString()
        }
    }
}
