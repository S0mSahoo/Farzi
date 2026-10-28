package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAdvisorViewModel"

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AiAdvisorViewModel : ViewModel() {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val chat by lazy {
        model.startChat(
            history = listOf(
                content("user") {
                    text("Hello, you are Paisa AI, an expert personal financial advisor embedded in the Paisa Pro Android app. Help users analyze their spending habits, budgeting, savings, and financial health with friendly, encouraging, and professional advice.")
                },
                content("model") {
                    text("Hello! I am Paisa AI, your personal financial advisor. I'm here to help you manage your expenses, analyze your spending patterns, plan your monthly budgets, and reach your savings goals. How can I assist you today?")
                }
            )
        )
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("model", "Hello! I'm Paisa AI, your personal finance advisor. Ask me anything about your spending, savings, or budgeting!")
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage("user", prompt)
        _messages.value = _messages.value + userMsg
        _uiState.value = AiUiState.Loading

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    chat.sendMessageStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulated.append(chunkText)
                        _uiState.value = AiUiState.Streaming(accumulated.toString())
                    }
                }
                val finalAnswer = accumulated.toString()
                if (finalAnswer.isNotBlank()) {
                    _messages.value = _messages.value + ChatMessage("model", finalAnswer)
                    _uiState.value = AiUiState.Success(finalAnswer)
                } else {
                    _uiState.value = AiUiState.Error("AI returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                _uiState.value = AiUiState.Error("Generation timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Failed to get AI response. Check App Check / internet.")
            }
        }
    }

    fun requestFinancialInsight(financialSummary: String) {
        sendMessage("Here is my current financial summary: $financialSummary. Please give me 3 actionable tips to improve my savings and budgeting.")
    }
}
