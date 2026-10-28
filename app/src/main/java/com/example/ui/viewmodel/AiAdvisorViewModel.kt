package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var activeChat: com.google.firebase.ai.Chat? = null
    private var financeViewModel: FinanceViewModel? = null

    fun bindFinanceViewModel(viewModel: FinanceViewModel) {
        this.financeViewModel = viewModel
    }

    private fun obtainChat(): com.google.firebase.ai.Chat {
        activeChat?.let { return it }
        val currentMonthKey = DateUtils.getMonthKey(System.currentTimeMillis())
        val currentMonthLabel = DateUtils.getMonthLabel(System.currentTimeMillis())

        val newChat = Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite"
        ).startChat(
            history = listOf(
                content("user") {
                    text("""
SYSTEM DIRECTIVE: You are Paisa AI, the complete AGENTIC personal financial assistant in Paisa Pro.
You can answer questions AND perform direct financial actions (logging, modifying, and deleting transactions, budgets, recurring rules, or updating user profile) on behalf of the user across any past, present, or future date/month.

Current Date / Month: $currentMonthLabel (Month Key: $currentMonthKey, Year: 2026).

When asked to take single OR multiple actions (e.g. "log expenses for Jan-Mar", "delete my last Starbucks transaction", "update my Netflix recurring rule amount to $18", "delete budget for January"):
1. Give a brief, crisp text answer confirming the action.
2. Append an `ACTION_JSON:` block containing a single action object OR an array of action objects.

Supported Action Schemas:

1. Log Transaction (supports custom `date` in "YYYY-MM-DD" or "YYYY-MM"):
ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Groceries", "amount": 45.0, "type": "EXPENSE", "category": "GROCERIES", "paymentMethod": "UPI", "date": "2026-01-15", "note": "Weekly veggies"}

Multiple Transactions Array Example (e.g. for multi-month requests):
ACTION_JSON: [
  {"action": "LOG_TRANSACTION", "title": "Salary", "amount": 3000.0, "type": "INCOME", "category": "SALARY", "paymentMethod": "NET_BANKING", "date": "2026-01-01"},
  {"action": "LOG_TRANSACTION", "title": "Salary", "amount": 3000.0, "type": "INCOME", "category": "SALARY", "paymentMethod": "NET_BANKING", "date": "2026-02-01"}
]

2. Modify / Update Existing Transaction:
ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "Groceries", "targetDate": "2026-01-15", "newTitle": "Fresh Groceries", "newAmount": 50.0, "newCategory": "GROCERIES", "newPaymentMethod": "CREDIT_CARD"}

3. Delete Transaction(s):
ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "Groceries", "targetDate": "2026-01-15"}
(Use "targetQuery": "ALL" and "targetDate": "YYYY-MM" to delete all transactions for a specific month)

4. Set Monthly Budget:
ACTION_JSON: {"action": "SET_BUDGET", "monthKey": "2026-01", "totalBudget": 1500.0}

5. Delete Monthly Budget:
ACTION_JSON: {"action": "DELETE_BUDGET", "monthKey": "2026-01"}

6. Create Recurring Rule:
ACTION_JSON: {"action": "LOG_RECURRING", "title": "Netflix", "amount": 15.0, "type": "EXPENSE", "category": "SUBSCRIPTIONS", "interval": "MONTHLY", "paymentMethod": "CREDIT_CARD"}

7. Modify / Toggle Recurring Rule:
ACTION_JSON: {"action": "UPDATE_RECURRING", "targetQuery": "Netflix", "newAmount": 18.0, "isActive": true}

8. Delete Recurring Rule:
ACTION_JSON: {"action": "DELETE_RECURRING", "targetQuery": "Netflix"}

9. Update User Profile:
ACTION_JSON: {"action": "UPDATE_PROFILE", "name": "John", "currencySymbol": "$"}

Categories:
EXPENSE: FOOD_DINING, GROCERIES, TRANSPORTATION, HOUSING, UTILITIES, SHOPPING, ENTERTAINMENT, HEALTHCARE, EDUCATION, TRAVEL, FITNESS, PERSONAL_CARE, SUBSCRIPTIONS, OTHER_EXPENSE.
INCOME: SALARY, FREELANCE, BUSINESS, INVESTMENTS, BONUS, GIFT, RENTAL_INCOME, OTHER_INCOME.

Payment Methods: UPI, CASH, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, OTHER.
Intervals: DAILY, WEEKLY, MONTHLY, YEARLY.

Keep all responses SHORT, CRISP, CLEAR, and DIRECTLY ACTIONABLE.
""".trimIndent())
                },
                content("model") {
                    text("Understood! I am ready to perform any financial action for you, including logging, modifying, or deleting transactions, budgets, recurring rules, or profile settings.")
                }
            )
        )
        activeChat = newChat
        return newChat
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("model", "Hello! I'm Paisa AI. I can log, edit, or delete transactions, budgets, and recurring rules for you across any month or year!")
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
                val currentChat = obtainChat()
                withTimeout(60_000L) {
                    currentChat.sendMessageStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulated.append(chunkText)
                        _uiState.value = AiUiState.Streaming(accumulated.toString())
                    }
                }
                val rawAnswer = accumulated.toString()
                if (rawAnswer.isNotBlank()) {
                    val (cleanedText, actionConfirmation) = processAgenticAction(rawAnswer)
                    val finalText = if (actionConfirmation.isNotBlank()) {
                        "$cleanedText\n\n$actionConfirmation"
                    } else {
                        cleanedText
                    }
                    _messages.value = _messages.value + ChatMessage("model", finalText)
                    _uiState.value = AiUiState.Success(finalText)
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
                activeChat = null
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Failed to get AI response. Please try again.")
            }
        }
    }

    private fun processAgenticAction(rawText: String): Pair<String, String> {
        val actionIndex = rawText.indexOf("ACTION_JSON:")
        if (actionIndex == -1) {
            return Pair(rawText.trim(), "")
        }

        val cleanedText = rawText.substring(0, actionIndex).trim()
        val jsonString = rawText.substring(actionIndex + "ACTION_JSON:".length).trim()

        return try {
            val confirmations = mutableListOf<String>()

            if (jsonString.startsWith("[")) {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val itemObj = array.getJSONObject(i)
                    val conf = executeSingleAction(itemObj)
                    if (conf.isNotBlank()) confirmations.add(conf)
                }
            } else {
                val jsonObj = JSONObject(jsonString)
                if (jsonObj.has("actions")) {
                    val array = jsonObj.getJSONArray("actions")
                    for (i in 0 until array.length()) {
                        val itemObj = array.getJSONObject(i)
                        val conf = executeSingleAction(itemObj)
                        if (conf.isNotBlank()) confirmations.add(conf)
                    }
                } else {
                    val conf = executeSingleAction(jsonObj)
                    if (conf.isNotBlank()) confirmations.add(conf)
                }
            }

            val finalConfirmation = if (confirmations.size == 1) {
                confirmations.first()
            } else if (confirmations.size > 1) {
                "✅ **Executed ${confirmations.size} Actions**:\n" + confirmations.joinToString("\n")
            } else {
                ""
            }

            Pair(cleanedText, finalConfirmation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse or execute agentic action JSON", e)
            Pair(cleanedText, "")
        }
    }

    private fun executeSingleAction(json: JSONObject): String {
        val action = json.optString("action", "")
        return when (action) {
            "LOG_TRANSACTION", "ADD_TRANSACTION" -> {
                val title = json.optString("title", "Expense")
                val amount = json.optDouble("amount", 0.0)
                val typeStr = json.optString("type", "EXPENSE")
                val categoryStr = json.optString("category", "OTHER_EXPENSE")
                val paymentMethodStr = json.optString("paymentMethod", "UPI")
                val dateStr = json.optString("date", "")
                val note = json.optString("note", "Logged via Paisa AI")

                val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val category = parseCategory(categoryStr, type)
                val paymentMethod = parsePaymentMethod(paymentMethodStr)
                val timestamp = parseDateToTimestamp(dateStr)
                val monthLabel = DateUtils.getMonthLabel(timestamp)

                if (amount > 0.0) {
                    financeViewModel?.addTransaction(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        timestamp = timestamp,
                        note = note,
                        paymentMethod = paymentMethod
                    )
                    "• Logged **${type.displayName}** of **₹${String.format(Locale.US, "%.2f", amount)}** (*$title* - ${category.displayName} [$monthLabel])."
                } else ""
            }
            "UPDATE_TRANSACTION", "MODIFY_TRANSACTION" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                val targetDate = json.optString("targetDate", "").trim()
                val newTitle = if (json.has("newTitle")) json.optString("newTitle") else null
                val newAmount = if (json.has("newAmount")) json.optDouble("newAmount") else null
                val newTypeStr = if (json.has("newType")) json.optString("newType") else null
                val newCategoryStr = if (json.has("newCategory")) json.optString("newCategory") else null
                val newPaymentMethodStr = if (json.has("newPaymentMethod")) json.optString("newPaymentMethod") else null
                val newDateStr = if (json.has("newDate")) json.optString("newDate") else null
                val newNote = if (json.has("newNote")) json.optString("newNote") else null

                val allTxs = financeViewModel?.allTransactions?.value ?: emptyList()
                val match = allTxs.find { tx ->
                    val queryMatch = targetQuery.isBlank() || tx.title.contains(targetQuery, ignoreCase = true) || tx.note.contains(targetQuery, ignoreCase = true)
                    val dateMatch = targetDate.isBlank() || DateUtils.getMonthKey(tx.timestamp) == targetDate || SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) == targetDate
                    queryMatch && dateMatch
                }

                if (match != null) {
                    val newType = if (!newTypeStr.isNullOrBlank()) {
                        if (newTypeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                    } else match.type

                    val updated = match.copy(
                        title = if (!newTitle.isNullOrBlank()) newTitle.trim() else match.title,
                        amount = if (newAmount != null && newAmount > 0) newAmount else match.amount,
                        type = newType,
                        category = if (!newCategoryStr.isNullOrBlank()) parseCategory(newCategoryStr, newType) else match.category,
                        paymentMethod = if (!newPaymentMethodStr.isNullOrBlank()) parsePaymentMethod(newPaymentMethodStr) else match.paymentMethod,
                        timestamp = if (!newDateStr.isNullOrBlank()) parseDateToTimestamp(newDateStr) else match.timestamp,
                        note = if (newNote != null) newNote.trim() else match.note
                    )
                    financeViewModel?.updateTransaction(updated)
                    "• Updated transaction **${match.title}** → **${updated.title}** (₹${String.format(Locale.US, "%.2f", updated.amount)})."
                } else {
                    "• Could not find matching transaction for '$targetQuery' to update."
                }
            }
            "DELETE_TRANSACTION" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                val targetDate = json.optString("targetDate", "").trim()
                val amount = if (json.has("amount")) json.optDouble("amount") else null

                val allTxs = financeViewModel?.allTransactions?.value ?: emptyList()
                val matches = allTxs.filter { tx ->
                    val isAll = targetQuery.equals("ALL", ignoreCase = true) || targetQuery.equals("EVERYTHING", ignoreCase = true)
                    val queryMatch = isAll || targetQuery.isBlank() || tx.title.contains(targetQuery, ignoreCase = true) || tx.category.displayName.contains(targetQuery, ignoreCase = true) || tx.note.contains(targetQuery, ignoreCase = true)
                    val dateMatch = targetDate.isBlank() || DateUtils.getMonthKey(tx.timestamp) == targetDate || SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) == targetDate
                    val amountMatch = amount == null || amount <= 0.0 || Math.abs(tx.amount - amount) < 0.01
                    queryMatch && dateMatch && amountMatch
                }

                if (matches.isNotEmpty()) {
                    val ids = matches.map { it.id }
                    financeViewModel?.deleteTransactionsByIds(ids)
                    "• Deleted ${matches.size} transaction(s) matching '$targetQuery'."
                } else {
                    "• No transactions found matching '$targetQuery' to delete."
                }
            }
            "SET_BUDGET", "SAVE_BUDGET" -> {
                val monthKey = json.optString("monthKey", DateUtils.getMonthKey(System.currentTimeMillis()))
                val totalBudget = json.optDouble("totalBudget", 0.0)

                if (totalBudget > 0.0) {
                    financeViewModel?.saveMonthlyBudget(
                        monthKey = monthKey,
                        totalBudget = totalBudget,
                        categoryBudgets = emptyMap()
                    )
                    "• Set **$monthKey** total budget to **₹${String.format(Locale.US, "%.2f", totalBudget)}**."
                } else ""
            }
            "DELETE_BUDGET" -> {
                val monthKey = json.optString("monthKey", DateUtils.getMonthKey(System.currentTimeMillis())).trim()
                if (monthKey.isNotBlank()) {
                    financeViewModel?.deleteBudget(monthKey)
                    "• Deleted budget for month **$monthKey**."
                } else ""
            }
            "LOG_RECURRING", "ADD_RECURRING" -> {
                val title = json.optString("title", "Recurring Item")
                val amount = json.optDouble("amount", 0.0)
                val typeStr = json.optString("type", "EXPENSE")
                val categoryStr = json.optString("category", "SUBSCRIPTIONS")
                val intervalStr = json.optString("interval", "MONTHLY")
                val paymentMethodStr = json.optString("paymentMethod", "UPI")

                val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val category = parseCategory(categoryStr, type)
                val interval = parseInterval(intervalStr)
                val paymentMethod = parsePaymentMethod(paymentMethodStr)

                if (amount > 0.0) {
                    financeViewModel?.addRecurringRule(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        interval = interval,
                        startDate = System.currentTimeMillis(),
                        endDate = null,
                        paymentMethod = paymentMethod,
                        note = "Created via Paisa AI"
                    )
                    "• Added recurring **${interval.displayName}** rule for **$title** (**₹${String.format(Locale.US, "%.2f", amount)}**)."
                } else ""
            }
            "UPDATE_RECURRING", "MODIFY_RECURRING" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                val newTitle = if (json.has("newTitle")) json.optString("newTitle") else null
                val newAmount = if (json.has("newAmount")) json.optDouble("newAmount") else null
                val newTypeStr = if (json.has("newType")) json.optString("newType") else null
                val newCategoryStr = if (json.has("newCategory")) json.optString("newCategory") else null
                val newIntervalStr = if (json.has("newInterval")) json.optString("newInterval") else null
                val newPaymentMethodStr = if (json.has("newPaymentMethod")) json.optString("newPaymentMethod") else null
                val isActive = if (json.has("isActive")) json.optBoolean("isActive") else null

                val allRules = financeViewModel?.allRecurringRules?.value ?: emptyList()
                val match = allRules.find { r ->
                    r.title.contains(targetQuery, ignoreCase = true) || r.note.contains(targetQuery, ignoreCase = true)
                }

                if (match != null) {
                    val newType = if (!newTypeStr.isNullOrBlank()) {
                        if (newTypeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                    } else match.type

                    val updated = match.copy(
                        title = if (!newTitle.isNullOrBlank()) newTitle.trim() else match.title,
                        amount = if (newAmount != null && newAmount > 0) newAmount else match.amount,
                        type = newType,
                        category = if (!newCategoryStr.isNullOrBlank()) parseCategory(newCategoryStr, newType) else match.category,
                        interval = if (!newIntervalStr.isNullOrBlank()) parseInterval(newIntervalStr) else match.interval,
                        paymentMethod = if (!newPaymentMethodStr.isNullOrBlank()) parsePaymentMethod(newPaymentMethodStr) else match.paymentMethod,
                        isActive = isActive ?: match.isActive
                    )
                    financeViewModel?.updateRecurringRule(updated)
                    "• Updated recurring rule **${match.title}** → **${updated.title}** (₹${String.format(Locale.US, "%.2f", updated.amount)})."
                } else {
                    "• Could not find recurring rule '$targetQuery' to modify."
                }
            }
            "DELETE_RECURRING" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                val allRules = financeViewModel?.allRecurringRules?.value ?: emptyList()
                val match = allRules.find { r ->
                    r.title.contains(targetQuery, ignoreCase = true) || r.note.contains(targetQuery, ignoreCase = true)
                }

                if (match != null) {
                    financeViewModel?.deleteRecurringRule(match.id)
                    "• Deleted recurring rule **${match.title}**."
                } else {
                    "• No recurring rule found matching '$targetQuery' to delete."
                }
            }
            "UPDATE_PROFILE" -> {
                val name = if (json.has("name")) json.optString("name") else null
                financeViewModel?.updateUserProfile(name)
                "• Updated user profile settings."
            }
            else -> ""
        }
    }

    private fun parseDateToTimestamp(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        val clean = dateStr.trim()
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("yyyy-MM", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("yyyy/MM", Locale.US)
        )
        for (fmt in formats) {
            try {
                val parsed = fmt.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun parseCategory(str: String, type: TransactionType): TransactionCategory {
        val clean = str.trim().uppercase()
        return try {
            TransactionCategory.valueOf(clean)
        } catch (_: Exception) {
            TransactionCategory.values().find {
                it.name.equals(clean, ignoreCase = true) ||
                it.displayName.equals(clean, ignoreCase = true)
            } ?: if (type == TransactionType.INCOME) TransactionCategory.OTHER_INCOME else TransactionCategory.OTHER_EXPENSE
        }
    }

    private fun parsePaymentMethod(str: String): PaymentMethod {
        val clean = str.trim().uppercase()
        return try {
            PaymentMethod.valueOf(clean)
        } catch (_: Exception) {
            PaymentMethod.values().find {
                it.name.equals(clean, ignoreCase = true) ||
                it.displayName.contains(clean, ignoreCase = true)
            } ?: PaymentMethod.UPI
        }
    }

    private fun parseInterval(str: String): RecurrenceInterval {
        val clean = str.trim().uppercase()
        return try {
            RecurrenceInterval.valueOf(clean)
        } catch (_: Exception) {
            RecurrenceInterval.MONTHLY
        }
    }

    fun retryLastMessage() {
        val lastUserMsg = _messages.value.lastOrNull { it.role == "user" }
        if (lastUserMsg != null) {
            sendMessage(lastUserMsg.text)
        }
    }

    fun requestFinancialInsight(financialSummary: String) {
        sendMessage("Here is my current financial summary: $financialSummary. Please give me 3 actionable tips to improve my savings and budgeting.")
    }
}
