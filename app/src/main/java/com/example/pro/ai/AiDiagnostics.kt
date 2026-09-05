package com.example.pro.ai

import android.util.Log

/**
 * Diagnostic tracker for AI Copilot / Gemini model calls.
 * Provides verifiable evidence of real network calls without logging API keys,
 * PII, tokens, or financial transaction specifics.
 */
object AiDiagnostics {
    private const val TAG = "PaisaGeminiDiagnostics"

    @Volatile
    var totalRequests: Int = 0
        private set

    @Volatile
    var successfulRequests: Int = 0
        private set

    @Volatile
    var failedRequests: Int = 0
        private set

    @Volatile
    var lastModelUsed: String = ""
        private set

    @Volatile
    var lastLatencyMs: Long = 0L
        private set

    @Volatile
    var lastStatus: String = "Idle"
        private set

    fun recordAttempt(model: String) {
        totalRequests++
        lastModelUsed = model
        lastStatus = "Request in progress..."
        Log.i(TAG, "Gemini request initiated for model: $model (Total requests: $totalRequests)")
    }

    fun recordSuccess(model: String, latencyMs: Long) {
        successfulRequests++
        lastModelUsed = model
        lastLatencyMs = latencyMs
        lastStatus = "Success"
        Log.i(TAG, "Gemini request completed successfully in ${latencyMs}ms (Success count: $successfulRequests)")
    }

    fun recordFailure(model: String, errorType: String) {
        failedRequests++
        lastModelUsed = model
        lastStatus = "Failed: $errorType"
        Log.e(TAG, "Gemini request failed: $errorType (Fail count: $failedRequests)")
    }

    fun reset() {
        totalRequests = 0
        successfulRequests = 0
        failedRequests = 0
        lastModelUsed = ""
        lastLatencyMs = 0L
        lastStatus = "Idle"
    }
}
