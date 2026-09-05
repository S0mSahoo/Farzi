package com.example.pro.ai

import com.example.pro.engine.models.FinancialContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock implementation of AiProvider for testing and offline fallback.
 */
class MockAiProvider : AiProvider {
    override fun ask(question: String, context: FinancialContext): Flow<String> = flow {
        emit("This is a mock AI response for: $question. In a production build, this would be replaced by the Gemini implementation.")
    }
}
