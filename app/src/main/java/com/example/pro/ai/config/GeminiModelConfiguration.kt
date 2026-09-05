package com.example.pro.ai.config

/**
 * Isolated configuration point for the selected Gemini model.
 * The rest of Paisa remains decoupled from the specific model name.
 */
object GeminiModelConfiguration {
    /**
     * Default least-cost Gemini model for text reasoning and financial explanations.
     */
    const val DEFAULT_MODEL = "gemini-3.1-flash-lite-preview"

    @Volatile
    var activeModel: String = DEFAULT_MODEL

    fun resetToDefault() {
        activeModel = DEFAULT_MODEL
    }
}
