package com.example.pro.ai

enum class CopilotSender {
    USER,
    COPILOT
}

data class CopilotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: CopilotSender,
    val text: String,
    val keyFacts: Map<String, String> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val confidence: String = "High",
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)
