package com.example.pro.ai

import com.example.pro.engine.models.FinancialContext
import kotlinx.coroutines.flow.Flow

/**
 * Interface for AI service providers.
 * Allows switching between Gemini, other LLMs, or mock implementations.
 */
interface AiProvider {
    /**
     * Streams the natural-language response based on the provided financial context and user question.
     */
    fun ask(question: String, context: FinancialContext): Flow<String>
}
