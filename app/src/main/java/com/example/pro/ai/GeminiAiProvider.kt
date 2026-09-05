package com.example.pro.ai

import com.example.pro.engine.models.FinancialContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Placeholder for Gemini-based implementation.
 * API keys and model configuration should be managed securely as per guidelines.
 */
class GeminiAiProvider : AiProvider {
    override fun ask(question: String, context: FinancialContext): Flow<String> = flow {
        // Implementation would use Gemini SDK here.
        emit("Gemini response for: $question")
    }
}
