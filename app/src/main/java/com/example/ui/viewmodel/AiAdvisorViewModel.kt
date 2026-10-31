package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.ai.DefaultFinancialToolExecutor
import com.example.data.ai.FinancialQueryTools
import com.example.data.ai.FinancialToolExecutor
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.Candidate
import com.google.firebase.ai.type.GroundingMetadata
import com.google.firebase.ai.type.WebGroundingChunk
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<Source> = emptyList()
)

data class Source(val title: String, val url: String)

sealed interface ProposedActionData {
    data class LogTransactionData(
        val title: String,
        val amount: Double,
        val type: TransactionType,
        val category: TransactionCategory,
        val paymentMethod: PaymentMethod,
        val timestamp: Long,
        val note: String
    ) : ProposedActionData

    data class UpdateTransactionData(
        val targetTransaction: TransactionItem,
        val newTitle: String,
        val newAmount: Double,
        val newType: TransactionType,
        val newCategory: TransactionCategory,
        val newPaymentMethod: PaymentMethod,
        val newTimestamp: Long,
        val newNote: String
    ) : ProposedActionData

    data class DeleteTransactionsData(
        val transactionsToDelete: List<TransactionItem>,
        val scopeDescription: String,
        val isDeleteAll: Boolean
    ) : ProposedActionData

    data class SetBudgetData(
        val monthKey: String,
        val totalBudget: Double
    ) : ProposedActionData

    data class DeleteBudgetData(
        val monthKey: String,
        val currentBudgetLimit: Double
    ) : ProposedActionData

    data class LogRecurringData(
        val title: String,
        val amount: Double,
        val type: TransactionType,
        val category: TransactionCategory,
        val interval: RecurrenceInterval,
        val paymentMethod: PaymentMethod,
        val note: String
    ) : ProposedActionData

    data class UpdateRecurringData(
        val targetRule: RecurringRule,
        val newTitle: String,
        val newAmount: Double,
        val newType: TransactionType,
        val newCategory: TransactionCategory,
        val newInterval: RecurrenceInterval,
        val newPaymentMethod: PaymentMethod,
        val isActive: Boolean
    ) : ProposedActionData

    data class DeleteRecurringData(
        val targetRule: RecurringRule
    ) : ProposedActionData

    data class UpdateProfileData(
        val name: String?,
        val currencySymbol: String?
    ) : ProposedActionData
}

enum class ActionExecutionStatus {
    PENDING,
    SUCCESS,
    FAILED
}

data class ProposedActionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: String,
    val title: String,
    val detailSummary: String,
    val isDestructive: Boolean,
    val data: ProposedActionData,
    val status: ActionExecutionStatus = ActionExecutionStatus.PENDING,
    val errorMessage: String? = null
)

data class PendingActionGroup(
    val rawTextExplanation: String,
    val actionItems: List<ProposedActionItem>,
    val isDestructive: Boolean,
    val requiresExplicitDeleteAllConfirmation: Boolean,
    val validationError: String? = null
)

private data class ActionParseResult(
    val item: ProposedActionItem? = null,
    val error: String? = null
)

class AiAdvisorViewModel : ViewModel() {
    private var activeChat: com.google.firebase.ai.Chat? = null
    private var financeViewModel: FinanceViewModel? = null

    private val _pendingActionGroup = MutableStateFlow<PendingActionGroup?>(null)
    val pendingActionGroup: StateFlow<PendingActionGroup?> = _pendingActionGroup.asStateFlow()

    private val _isProcessingAction = MutableStateFlow(false)
    val isProcessingAction: StateFlow<Boolean> = _isProcessingAction.asStateFlow()

    var toolExecutor: FinancialToolExecutor = DefaultFinancialToolExecutor(
        getTransactions = { financeViewModel?.allTransactions?.value ?: emptyList() },
        getBudgets = { financeViewModel?.allBudgets?.value ?: emptyList() },
        getRecurringRules = { financeViewModel?.allRecurringRules?.value ?: emptyList() },
        getCurrencySymbol = { getCurrencySymbol() }
    )

    fun bindFinanceViewModel(viewModel: FinanceViewModel) {
        this.financeViewModel = viewModel
        this.toolExecutor = DefaultFinancialToolExecutor(
            getTransactions = { viewModel.allTransactions.value },
            getBudgets = { viewModel.allBudgets.value },
            getRecurringRules = { viewModel.allRecurringRules.value },
            getCurrencySymbol = { getCurrencySymbol() }
        )
    }

    var lastFailedPrompt: String? = null
        private set

    fun resetChat() {
        activeChat = null
        lastFailedPrompt = null
    }

    private fun getCurrencySymbol(): String {
        return financeViewModel?.userProfile?.value?.currencySymbol ?: "₹"
    }

    private fun obtainChat(): com.google.firebase.ai.Chat {
        activeChat?.let { return it }

        val newChat = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = FinancialQueryTools.allTools
        ).startChat(
            history = listOf(
                content("user") {
                    text("""
SYSTEM DIRECTIVE: You are Paisa AI, the intelligent personal financial advisor in Paisa Pro.

READ-ONLY QUERY TOOLS:
You have access to structured, read-only tools to query the user's live financial data:
- `getFinancialSummary(periodType, period)`: Retrieve income, expense, savings rate, and insights for a month ('YYYY-MM') or year ('YYYY').
- `comparePeriods(periodType, period1, period2)`: Compare totals, percentage differences, and cash flows between two months or years.
- `getCategorySpending(startDate, endDate, category, type)`: Category breakdown and spending totals for any date range.
- `getBudgetStatus(monthKey)`: Budget limit, expense, remaining funds, and utilization percentage for a month ('YYYY-MM').
- `getRecurringCommitments(type)`: Active recurring subscriptions, bills, and estimated monthly commitments.
- `searchTransactions(query, startDate, endDate, category, type, limit)`: Bounded search across transactions without sensitive private notes.

GOOGLE SEARCH GROUNDING:
You have access to Google Search for answering questions that genuinely require current external information (e.g., market rates, tax rules, financial product details). 
1. DO NOT use Google Search to retrieve the user's private financial data. Use the READ-ONLY QUERY TOOLS for that.
2. Use Google Search only for general knowledge or real-time external facts.
3. If the user's question can be answered from their local data, prioritize the QUERY TOOLS.
4. If you use external information, specify that it is from grounded search.

RULES FOR DATA ACCESS & QUERIES:
1. ALWAYS use the query tools whenever the user asks about their financial figures, trends, category totals, budgets, or recurring commitments. DO NOT guess, fabricate, or hallucinate numbers.
2. Query ONLY the specific periods, categories, or records needed to answer the user's explicit question. Minimise private data exposure.
3. Keep responses SHORT, CRISP, CLEAR, and DIRECTLY ACTIONABLE.

ACTION PROPOSALS & MUTATION SAFETY:
The query tools are strictly read-only and CANNOT mutate records. If the user asks you to log, modify, or delete a transaction, budget, or recurring rule:
1. Provide a brief text response describing the proposed change.
2. Append an `ACTION_JSON:` block containing the proposal for explicit user confirmation.
3. The app's preview flow will require user review before any mutation occurs.

Supported Action Schemas:
ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Groceries", "amount": 45.0, "type": "EXPENSE", "category": "GROCERIES", "paymentMethod": "UPI", "date": "YYYY-MM-DD"}
ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "Groceries", "targetDate": "YYYY-MM-DD", "newAmount": 50.0}
ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "Groceries", "targetDate": "YYYY-MM-DD"}
ACTION_JSON: {"action": "SET_BUDGET", "monthKey": "YYYY-MM", "totalBudget": 1500.0}
ACTION_JSON: {"action": "DELETE_BUDGET", "monthKey": "YYYY-MM"}
ACTION_JSON: {"action": "LOG_RECURRING", "title": "Netflix", "amount": 15.0, "type": "EXPENSE", "category": "SUBSCRIPTIONS", "interval": "MONTHLY", "paymentMethod": "CREDIT_CARD"}
ACTION_JSON: {"action": "UPDATE_RECURRING", "targetQuery": "Netflix", "newAmount": 18.0}
ACTION_JSON: {"action": "DELETE_RECURRING", "targetQuery": "Netflix"}
ACTION_JSON: {"action": "UPDATE_PROFILE", "name": "John", "currencySymbol": "$"}

Categories:
EXPENSE: FOOD_DINING, GROCERIES, TRANSPORTATION, HOUSING, UTILITIES, SHOPPING, ENTERTAINMENT, HEALTHCARE, EDUCATION, TRAVEL, FITNESS, PERSONAL_CARE, SUBSCRIPTIONS, OTHER_EXPENSE.
INCOME: SALARY, FREELANCE, BUSINESS, INVESTMENTS, BONUS, GIFT, RENTAL_INCOME, OTHER_INCOME.
Payment Methods: UPI, CASH, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, OTHER.
Intervals: DAILY, WEEKLY, MONTHLY, YEARLY.
""".trimIndent())
                },
                content("model") {
                    text("Understood! I am Paisa AI. I will query your financial records strictly via read-only tools to retrieve exact numbers, use Google Search for external general information, and propose financial actions via ACTION_JSON: for your explicit confirmation.")
                }
            )
        )
        activeChat = newChat
        return newChat
    }

    fun buildFinancialDatabaseSnapshot(): String {
        val vm = financeViewModel ?: return "No financial records available."
        val profile = vm.userProfile.value
        val currency = profile.currencySymbol
        val transactions = vm.allTransactions.value
        val budgets = vm.allBudgets.value
        val recurringRules = vm.allRecurringRules.value

        val sb = StringBuilder()
        sb.append("USER PROFILE: Name = \"${profile.name}\", Currency = \"$currency\"\n\n")

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense

        sb.append("OVERALL FINANCIAL SUMMARY:\n")
        sb.append("- Total Recorded Transactions: ${transactions.size}\n")
        sb.append("- Total Income: $currency${String.format(Locale.US, "%.2f", totalIncome)}\n")
        sb.append("- Total Expenses: $currency${String.format(Locale.US, "%.2f", totalExpense)}\n")
        sb.append("- Net Balance: $currency${String.format(Locale.US, "%.2f", netSavings)}\n\n")

        if (transactions.isEmpty()) {
            sb.append("TRANSACTIONS: No transactions recorded yet.\n\n")
        } else {
            val sdfFullDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val groupedByMonth = transactions.groupBy { DateUtils.getMonthKey(it.timestamp) }
                .toSortedMap(compareByDescending { it })

            sb.append("MONTHLY BREAKDOWNS & TRANSACTIONS:\n")
            groupedByMonth.forEach { (monthKey, txs) ->
                val monthLabel = DateUtils.getMonthLabel(txs.first().timestamp)
                val monthIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val monthExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val monthNet = monthIncome - monthExpense
                val budget = budgets.find { it.monthKey == monthKey }
                val budgetStr = if (budget != null) "$currency${String.format(Locale.US, "%.2f", budget.totalBudget)}" else "Not set"

                sb.append("=== Month: $monthLabel (Month Key: $monthKey) ===\n")
                sb.append("  * Income: $currency${String.format(Locale.US, "%.2f", monthIncome)}\n")
                sb.append("  * Expense: $currency${String.format(Locale.US, "%.2f", monthExpense)}\n")
                sb.append("  * Net Cash Flow: $currency${String.format(Locale.US, "%.2f", monthNet)}\n")
                sb.append("  * Monthly Budget Limit: $budgetStr\n")

                val categoryTotals = txs.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }

                if (categoryTotals.isNotEmpty()) {
                    val catStr = categoryTotals.joinToString(", ") { "${it.first.displayName}: $currency${String.format(Locale.US, "%.2f", it.second)}" }
                    sb.append("  * Expense Categories: $catStr\n")
                }

                sb.append("  * Recorded Entries (${txs.size}):\n")
                txs.sortedByDescending { it.timestamp }.forEach { tx ->
                    val dateFormatted = sdfFullDate.format(Date(tx.timestamp))
                    sb.append("    - [$dateFormatted] ${tx.title}: $currency${String.format(Locale.US, "%.2f", tx.amount)} (${tx.type.name}, ${tx.category.displayName}, ${tx.paymentMethod.displayName})\n")
                }
                sb.append("\n")
            }
        }

        if (budgets.isNotEmpty()) {
            sb.append("SET MONTHLY BUDGETS:\n")
            budgets.forEach { b ->
                sb.append("- Month ${b.monthKey}: Limit = $currency${String.format(Locale.US, "%.2f", b.totalBudget)}\n")
            }
            sb.append("\n")
        }

        if (recurringRules.isNotEmpty()) {
            sb.append("RECURRING RULES & BILLS:\n")
            recurringRules.forEach { r ->
                val status = if (r.isActive) "Active" else "Paused"
                sb.append("- ${r.title}: $currency${String.format(Locale.US, "%.2f", r.amount)} (${r.type.name}, ${r.interval.displayName}, $status)\n")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("model", "Hello! I'm Paisa AI, your agentic financial assistant. 🌟\n\nI can analyze your spending, compare periods, or help you log transactions and set budgets. How can I help you manage your finances today?")
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(prompt: String) {
        executeSend(prompt, isRetry = false)
    }

    private fun executeSend(prompt: String, isRetry: Boolean) {
        if (prompt.isBlank() || _isProcessingAction.value || _pendingActionGroup.value != null) return
        if (!isRetry) {
            val userMsg = ChatMessage("user", prompt)
            _messages.value = _messages.value + userMsg
            lastFailedPrompt = null
        }
        _uiState.value = AiUiState.Loading

        val currentMonthKey = DateUtils.getMonthKey(System.currentTimeMillis())
        val currentMonthLabel = DateUtils.getMonthLabel(System.currentTimeMillis())
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentYear = SimpleDateFormat("yyyy", Locale.US).format(Date())

        // Privacy-preserving context: current temporal anchors without dumping entire database
        val contextualPrompt = "[Current Context: Today is $todayDate ($currentMonthLabel), Month Key: $currentMonthKey, Year: $currentYear]\n$prompt"

        viewModelScope.launch {
            try {
                val currentChat = obtainChat()
                var currentResponse = withTimeout(60_000L) {
                    currentChat.sendMessage(contextualPrompt)
                }

                var rounds = 0
                val maxToolRounds = 5
                while (currentResponse.functionCalls.isNotEmpty() && rounds < maxToolRounds) {
                    rounds++
                    val functionCalls = currentResponse.functionCalls
                    val responseParts = mutableListOf<FunctionResponsePart>()
                    for (call in functionCalls) {
                        val mapArgs = call.args.mapValues { jsonElementToAny(it.value) }
                        val toolResultJson = toolExecutor.executeTool(call.name, mapArgs)
                        val kotlinxObj = kotlinx.serialization.json.Json.parseToJsonElement(toolResultJson.toString()) as kotlinx.serialization.json.JsonObject
                        responseParts.add(
                            FunctionResponsePart(call.name, kotlinxObj, call.id)
                        )
                    }

                    currentResponse = withTimeout(60_000L) {
                        currentChat.sendMessage(
                            com.google.firebase.ai.type.Content("function", responseParts)
                        )
                    }
                }

                val rawAnswer = currentResponse.text.orEmpty()
                if (rawAnswer.isNotBlank()) {
                    val (cleanedText, actionNotice) = processAgenticAction(rawAnswer)
                    val finalText = if (actionNotice.isNotBlank()) {
                        "$cleanedText\n\n$actionNotice"
                    } else {
                        cleanedText
                    }

                    // Extract sources from grounding metadata
                    val sources = mutableListOf<Source>()
                    try {
                        val groundingMetadata = currentResponse.candidates.firstOrNull()?.groundingMetadata
                        groundingMetadata?.groundingChunks?.forEach { chunk ->
                            val web = chunk.web
                            if (web != null) {
                                val uri = web.uri
                                val title = web.title
                                if (!uri.isNullOrBlank() && !title.isNullOrBlank()) {
                                    sources.add(Source(title, uri))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting grounding metadata", e)
                    }

                    _messages.value = _messages.value + ChatMessage("model", finalText, sources = sources)
                    _uiState.value = AiUiState.Success(finalText)
                    lastFailedPrompt = null
                } else if (currentResponse.functionCalls.isNotEmpty()) {
                    val fallbackMsg = "I reached the maximum tool query depth for this question. Please try asking a more specific question."
                    _messages.value = _messages.value + ChatMessage("model", fallbackMsg)
                    _uiState.value = AiUiState.Success(fallbackMsg)
                    lastFailedPrompt = null
                } else {
                    activeChat = null
                    lastFailedPrompt = prompt
                    _uiState.value = AiUiState.Error("AI returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                activeChat = null
                lastFailedPrompt = prompt
                _uiState.value = AiUiState.Error("Generation timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed", e)
                activeChat = null
                lastFailedPrompt = prompt
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Failed to get AI response. Please try again.")
            }
        }
    }

    private fun jsonElementToAny(element: kotlinx.serialization.json.JsonElement?): Any? {
        return when (element) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                if (element.isString) {
                    element.content
                } else {
                    val content = element.content
                    content.toIntOrNull() ?: content.toLongOrNull() ?: content.toDoubleOrNull() ?: content.toBooleanStrictOrNull() ?: content
                }
            }
            is kotlinx.serialization.json.JsonObject -> element.mapValues { jsonElementToAny(it.value) }
            is kotlinx.serialization.json.JsonArray -> element.map { jsonElementToAny(it) }
            else -> null
        }
    }

    private fun jsonObjectToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.opt(key)
            map[key] = if (value == JSONObject.NULL) null else value
        }
        return map
    }

    private fun processAgenticAction(rawText: String): Pair<String, String> {
        val actionIndex = rawText.indexOf("ACTION_JSON:")
        if (actionIndex == -1) {
            return Pair(rawText.trim(), "")
        }

        val cleanedText = rawText.substring(0, actionIndex).trim()
        val jsonString = rawText.substring(actionIndex + "ACTION_JSON:".length).trim()

        return try {
            val jsonObjects = mutableListOf<JSONObject>()
            if (jsonString.startsWith("[")) {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    jsonObjects.add(array.getJSONObject(i))
                }
            } else {
                val jsonObj = JSONObject(jsonString)
                if (jsonObj.has("actions")) {
                    val array = jsonObj.getJSONArray("actions")
                    for (i in 0 until array.length()) {
                        jsonObjects.add(array.getJSONObject(i))
                    }
                } else {
                    jsonObjects.add(jsonObj)
                }
            }

            val actionItems = mutableListOf<ProposedActionItem>()
            var firstValidationError: String? = null
            var groupIsDestructive = false
            var requiresDeleteAllConfirmation = false

            for (obj in jsonObjects) {
                val parsedResult = parseAndValidateSingleAction(obj)
                if (parsedResult.error != null && firstValidationError == null) {
                    firstValidationError = parsedResult.error
                }
                parsedResult.item?.let { item ->
                    actionItems.add(item)
                    if (item.isDestructive) groupIsDestructive = true
                    if (item.data is ProposedActionData.DeleteTransactionsData && item.data.isDeleteAll) {
                        requiresDeleteAllConfirmation = true
                    }
                }
            }

            if (actionItems.isNotEmpty() || firstValidationError != null) {
                _pendingActionGroup.value = PendingActionGroup(
                    rawTextExplanation = cleanedText,
                    actionItems = actionItems,
                    isDestructive = groupIsDestructive,
                    requiresExplicitDeleteAllConfirmation = requiresDeleteAllConfirmation,
                    validationError = firstValidationError
                )
            }

            val pendingNotice = if (firstValidationError != null) {
                "⚠️ **Proposed Action Error**: $firstValidationError"
            } else {
                "📋 **Action Proposed**: Please review and confirm the proposed financial action(s) below."
            }

            Pair(cleanedText, pendingNotice)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse agentic action JSON", e)
            Pair(cleanedText, "")
        }
    }

    private sealed interface DateTarget {
        data class MonthKey(val monthKey: String) : DateTarget
        data class DayKey(val dayKey: String) : DateTarget
    }

    private fun parseAndNormalizeDateTarget(targetDate: String): Result<DateTarget> {
        if (targetDate.isBlank()) return Result.failure(IllegalArgumentException("Target date cannot be blank."))
        val clean = targetDate.trim()
        val formats = listOf(
            "yyyy-MM-dd" to false,
            "yyyy-MM" to true,
            "yyyy/MM/dd" to false,
            "yyyy/MM" to true,
            "MMMM yyyy" to true,
            "MMM yyyy" to true,
            "MMMM, yyyy" to true
        )

        val sdfDayOut = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfMonthOut = SimpleDateFormat("yyyy-MM", Locale.US)

        for ((pattern, isMonthOnly) in formats) {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val pos = java.text.ParsePosition(0)
            val parsed = sdf.parse(clean, pos)
            if (parsed != null && pos.index == clean.length) {
                return if (isMonthOnly) {
                    Result.success(DateTarget.MonthKey(sdfMonthOut.format(parsed)))
                } else {
                    Result.success(DateTarget.DayKey(sdfDayOut.format(parsed)))
                }
            }
        }
        return Result.failure(IllegalArgumentException("Invalid target date format '$targetDate'."))
    }

    private fun parseAndValidateSingleAction(json: JSONObject): ActionParseResult {
        val currency = getCurrencySymbol()
        val action = json.optString("action", "").trim().uppercase()
        return when (action) {
            "LOG_TRANSACTION", "ADD_TRANSACTION" -> {
                val title = json.optString("title", "").trim()
                if (title.isBlank()) {
                    return ActionParseResult(error = "Transaction title cannot be blank.")
                }
                val amount = json.optDouble("amount", 0.0)
                if (amount <= 0.0 || !amount.isFinite()) {
                    return ActionParseResult(error = "Invalid transaction amount: must be positive and finite.")
                }
                val typeResult = parseTransactionType(if (json.has("type")) json.optString("type") else null)
                val type = typeResult.getOrElse { return ActionParseResult(error = it.message) }

                if (!json.has("category") || json.optString("category").isBlank()) {
                    return ActionParseResult(error = "Category is required for transaction.")
                }
                val catResult = parseCategory(json.optString("category"), type)
                val category = catResult.getOrElse { return ActionParseResult(error = it.message) }

                if (!json.has("paymentMethod") || json.optString("paymentMethod").isBlank()) {
                    return ActionParseResult(error = "Payment method is required for transaction.")
                }
                val pmResult = parsePaymentMethod(json.optString("paymentMethod"))
                val paymentMethod = pmResult.getOrElse { return ActionParseResult(error = it.message) }

                val dateResult = parseDateToTimestamp(if (json.has("date")) json.optString("date") else null)
                val timestamp = dateResult.getOrElse { return ActionParseResult(error = it.message) }

                val note = json.optString("note", "Logged via Paisa AI").trim()
                val monthLabel = DateUtils.getMonthLabel(timestamp)

                val data = ProposedActionData.LogTransactionData(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    paymentMethod = paymentMethod,
                    timestamp = timestamp,
                    note = note
                )
                val item = ProposedActionItem(
                    actionType = "Log Transaction",
                    title = "Add ${type.displayName}: $title",
                    detailSummary = "$currency${String.format(Locale.US, "%.2f", amount)} • ${category.displayName} • ${paymentMethod.displayName} ($monthLabel)",
                    isDestructive = false,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "UPDATE_TRANSACTION", "MODIFY_TRANSACTION" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                val targetDate = json.optString("targetDate", "").trim()
                if (targetQuery.isBlank() && targetDate.isBlank()) {
                    return ActionParseResult(error = "Update target cannot be blank. Please specify a transaction title or date.")
                }

                var normalizedDateTarget: DateTarget? = null
                if (targetDate.isNotBlank()) {
                    val normResult = parseAndNormalizeDateTarget(targetDate)
                    if (normResult.isFailure) {
                        return ActionParseResult(error = normResult.exceptionOrNull()?.message ?: "Invalid target date format '$targetDate'.")
                    }
                    normalizedDateTarget = normResult.getOrNull()
                }

                val allTxs = financeViewModel?.allTransactions?.value ?: emptyList()
                val matches = allTxs.filter { tx ->
                    val queryMatch = targetQuery.isBlank() || tx.title.contains(targetQuery, ignoreCase = true) || tx.note.contains(targetQuery, ignoreCase = true)
                    val dateMatch = when (normalizedDateTarget) {
                        null -> true
                        is DateTarget.MonthKey -> DateUtils.getMonthKey(tx.timestamp) == normalizedDateTarget.monthKey
                        is DateTarget.DayKey -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) == normalizedDateTarget.dayKey
                    }
                    queryMatch && dateMatch
                }

                if (matches.isEmpty()) {
                    return ActionParseResult(error = "No transaction found matching target query '$targetQuery' ${if (targetDate.isNotBlank()) "on $targetDate" else ""}.")
                }
                if (matches.size > 1) {
                    return ActionParseResult(error = "Ambiguous target query '$targetQuery': matches ${matches.size} transactions (${matches.take(3).joinToString { it.title }}). Please specify a unique title or date.")
                }

                val targetTx = matches.first()
                val newTitle = if (json.has("newTitle")) json.optString("newTitle").trim() else targetTx.title
                if (newTitle.isBlank()) {
                    return ActionParseResult(error = "New transaction title cannot be blank.")
                }
                val newAmount = if (json.has("newAmount")) json.optDouble("newAmount", targetTx.amount) else targetTx.amount
                if (newAmount <= 0.0 || !newAmount.isFinite()) {
                    return ActionParseResult(error = "New transaction amount must be positive and finite.")
                }
                val newTypeResult = if (json.has("newType")) parseTransactionType(json.optString("newType")) else Result.success(targetTx.type)
                val newType = newTypeResult.getOrElse { return ActionParseResult(error = it.message) }

                val newCategory = if (json.has("newCategory")) {
                    val catResult = parseCategory(json.optString("newCategory"), newType)
                    catResult.getOrElse { return ActionParseResult(error = it.message) }
                } else {
                    if (targetTx.category.defaultType != newType) {
                        return ActionParseResult(error = "Existing category '${targetTx.category.displayName}' is not valid for ${newType.displayName} transactions. Please specify a new category.")
                    }
                    targetTx.category
                }

                val pmResult = if (json.has("newPaymentMethod")) parsePaymentMethod(json.optString("newPaymentMethod")) else Result.success(targetTx.paymentMethod)
                val newPaymentMethod = pmResult.getOrElse { return ActionParseResult(error = it.message) }

                val dateResult = if (json.has("newDate")) parseDateToTimestamp(json.optString("newDate"), isUpdate = true, originalTimestamp = targetTx.timestamp) else Result.success(targetTx.timestamp)
                val newTimestamp = dateResult.getOrElse { return ActionParseResult(error = it.message) }

                val newNote = if (json.has("newNote")) json.optString("newNote").trim() else targetTx.note

                val data = ProposedActionData.UpdateTransactionData(
                    targetTransaction = targetTx,
                    newTitle = newTitle,
                    newAmount = newAmount,
                    newType = newType,
                    newCategory = newCategory,
                    newPaymentMethod = newPaymentMethod,
                    newTimestamp = newTimestamp,
                    newNote = newNote
                )
                val item = ProposedActionItem(
                    actionType = "Update Transaction",
                    title = "Modify Transaction: ${targetTx.title}",
                    detailSummary = "Original: $currency${String.format(Locale.US, "%.2f", targetTx.amount)} ➔ New: $currency${String.format(Locale.US, "%.2f", newAmount)} ($newTitle)",
                    isDestructive = false,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "DELETE_TRANSACTION" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                val targetDate = json.optString("targetDate", "").trim()
                val amount = if (json.has("amount")) json.optDouble("amount") else null

                if (targetQuery.isBlank() && targetDate.isBlank()) {
                    return ActionParseResult(error = "Delete target cannot be blank. Please specify a transaction title or date.")
                }

                var normalizedDateTarget: DateTarget? = null
                if (targetDate.isNotBlank()) {
                    val normResult = parseAndNormalizeDateTarget(targetDate)
                    if (normResult.isFailure) {
                        return ActionParseResult(error = normResult.exceptionOrNull()?.message ?: "Invalid target date format '$targetDate'.")
                    }
                    normalizedDateTarget = normResult.getOrNull()
                }

                val allTxs = financeViewModel?.allTransactions?.value ?: emptyList()
                val isAll = targetQuery.equals("ALL", ignoreCase = true) || targetQuery.equals("EVERYTHING", ignoreCase = true)

                val matches = allTxs.filter { tx ->
                    val queryMatch = isAll || targetQuery.isBlank() || tx.title.contains(targetQuery, ignoreCase = true) || tx.category.displayName.contains(targetQuery, ignoreCase = true) || tx.note.contains(targetQuery, ignoreCase = true)
                    val dateMatch = when (normalizedDateTarget) {
                        null -> true
                        is DateTarget.MonthKey -> DateUtils.getMonthKey(tx.timestamp) == normalizedDateTarget.monthKey
                        is DateTarget.DayKey -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) == normalizedDateTarget.dayKey
                    }
                    val amountMatch = amount == null || amount <= 0.0 || Math.abs(tx.amount - amount) < 0.01
                    queryMatch && dateMatch && amountMatch
                }

                if (matches.isEmpty()) {
                    return ActionParseResult(error = "No transactions found matching target query '$targetQuery' to delete.")
                }

                if (!isAll && matches.size > 1) {
                    return ActionParseResult(error = "Ambiguous delete target '$targetQuery': matches ${matches.size} transactions (${matches.take(3).joinToString { it.title }}). Please specify a unique transaction.")
                }

                val scopeDescription = if (isAll) {
                    if (targetDate.isNotBlank()) "ALL ${matches.size} transaction(s) for $targetDate" else "ALL ${matches.size} transaction(s) in database history"
                } else {
                    "${matches.size} transaction (${matches.first().title})"
                }

                val data = ProposedActionData.DeleteTransactionsData(
                    transactionsToDelete = matches,
                    scopeDescription = scopeDescription,
                    isDeleteAll = isAll
                )
                val item = ProposedActionItem(
                    actionType = "Delete Transaction(s)",
                    title = "Delete: $scopeDescription",
                    detailSummary = "Will remove ${matches.size} record(s) totaling $currency${String.format(Locale.US, "%.2f", matches.sumOf { it.amount })}",
                    isDestructive = true,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "SET_BUDGET", "SAVE_BUDGET" -> {
                val monthKey = json.optString("monthKey", DateUtils.getMonthKey(System.currentTimeMillis())).trim()
                try {
                    java.time.YearMonth.parse(monthKey)
                } catch (_: Exception) {
                    return ActionParseResult(error = "Invalid month key format '$monthKey'. Expected YYYY-MM.")
                }
                val totalBudget = json.optDouble("totalBudget", 0.0)
                if (totalBudget <= 0.0 || !totalBudget.isFinite()) {
                    return ActionParseResult(error = "Monthly budget amount must be greater than zero.")
                }
                val data = ProposedActionData.SetBudgetData(monthKey = monthKey, totalBudget = totalBudget)
                val item = ProposedActionItem(
                    actionType = "Set Monthly Budget",
                    title = "Set Budget for Month $monthKey",
                    detailSummary = "Limit: $currency${String.format(Locale.US, "%.2f", totalBudget)}",
                    isDestructive = false,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "DELETE_BUDGET" -> {
                val monthKey = json.optString("monthKey", "").trim()
                if (monthKey.isBlank()) {
                    return ActionParseResult(error = "Month key for budget deletion cannot be blank.")
                }
                try {
                    java.time.YearMonth.parse(monthKey)
                } catch (_: Exception) {
                    return ActionParseResult(error = "Invalid month key format '$monthKey'. Expected YYYY-MM.")
                }
                val allBudgets = financeViewModel?.allBudgets?.value ?: emptyList()
                val existing = allBudgets.find { it.monthKey == monthKey }
                    ?: return ActionParseResult(error = "No active budget set for month '$monthKey' to delete.")

                val data = ProposedActionData.DeleteBudgetData(monthKey = monthKey, currentBudgetLimit = existing.totalBudget)
                val item = ProposedActionItem(
                    actionType = "Delete Monthly Budget",
                    title = "Delete Budget for Month $monthKey",
                    detailSummary = "Current Limit: $currency${String.format(Locale.US, "%.2f", existing.totalBudget)}",
                    isDestructive = true,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "LOG_RECURRING", "ADD_RECURRING" -> {
                val title = json.optString("title", "").trim()
                if (title.isBlank()) {
                    return ActionParseResult(error = "Recurring rule title cannot be blank.")
                }
                val amount = json.optDouble("amount", 0.0)
                if (amount <= 0.0 || !amount.isFinite()) {
                    return ActionParseResult(error = "Recurring rule amount must be positive and finite.")
                }
                val typeResult = parseTransactionType(if (json.has("type")) json.optString("type") else null)
                val type = typeResult.getOrElse { return ActionParseResult(error = it.message) }

                if (!json.has("category") || json.optString("category").isBlank()) {
                    return ActionParseResult(error = "Category is required for recurring rule.")
                }
                val catResult = parseCategory(json.optString("category"), type)
                val category = catResult.getOrElse { return ActionParseResult(error = it.message) }

                if (!json.has("interval") || json.optString("interval").isBlank()) {
                    return ActionParseResult(error = "Recurrence interval is required.")
                }
                val intervalResult = parseInterval(json.optString("interval"))
                val interval = intervalResult.getOrElse { return ActionParseResult(error = it.message) }

                if (!json.has("paymentMethod") || json.optString("paymentMethod").isBlank()) {
                    return ActionParseResult(error = "Payment method is required.")
                }
                val pmResult = parsePaymentMethod(json.optString("paymentMethod"))
                val paymentMethod = pmResult.getOrElse { return ActionParseResult(error = it.message) }

                val data = ProposedActionData.LogRecurringData(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    interval = interval,
                    paymentMethod = paymentMethod,
                    note = "Created via Paisa AI"
                )
                val item = ProposedActionItem(
                    actionType = "Create Recurring Rule",
                    title = "Add Recurring: $title",
                    detailSummary = "$currency${String.format(Locale.US, "%.2f", amount)} • ${interval.displayName} • ${category.displayName}",
                    isDestructive = false,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "UPDATE_RECURRING", "MODIFY_RECURRING" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                if (targetQuery.isBlank()) {
                    return ActionParseResult(error = "Target recurring rule title cannot be blank.")
                }
                val allRules = financeViewModel?.allRecurringRules?.value ?: emptyList()
                val matches = allRules.filter { r ->
                    r.title.contains(targetQuery, ignoreCase = true) || r.note.contains(targetQuery, ignoreCase = true)
                }
                if (matches.isEmpty()) {
                    return ActionParseResult(error = "No recurring rule found matching '$targetQuery'.")
                }
                if (matches.size > 1) {
                    return ActionParseResult(error = "Ambiguous recurring rule query '$targetQuery': matches ${matches.size} rules. Please specify exact title.")
                }

                val targetRule = matches.first()
                val newTitle = if (json.has("newTitle")) json.optString("newTitle").trim() else targetRule.title
                if (newTitle.isBlank()) {
                    return ActionParseResult(error = "New recurring rule title cannot be blank.")
                }
                val newAmount = if (json.has("newAmount")) json.optDouble("newAmount", targetRule.amount) else targetRule.amount
                if (newAmount <= 0.0 || !newAmount.isFinite()) {
                    return ActionParseResult(error = "New recurring rule amount must be positive and finite.")
                }
                val newTypeResult = if (json.has("newType")) parseTransactionType(json.optString("newType")) else Result.success(targetRule.type)
                val newType = newTypeResult.getOrElse { return ActionParseResult(error = it.message) }

                val newCategory = if (json.has("newCategory")) {
                    val catResult = parseCategory(json.optString("newCategory"), newType)
                    catResult.getOrElse { return ActionParseResult(error = it.message) }
                } else {
                    if (targetRule.category.defaultType != newType) {
                        return ActionParseResult(error = "Existing category '${targetRule.category.displayName}' is not valid for ${newType.displayName} transactions. Please specify a new category.")
                    }
                    targetRule.category
                }

                val intervalResult = if (json.has("newInterval")) parseInterval(json.optString("newInterval")) else Result.success(targetRule.interval)
                val newInterval = intervalResult.getOrElse { return ActionParseResult(error = it.message) }

                val pmResult = if (json.has("newPaymentMethod")) parsePaymentMethod(json.optString("newPaymentMethod")) else Result.success(targetRule.paymentMethod)
                val newPaymentMethod = pmResult.getOrElse { return ActionParseResult(error = it.message) }

                val isActive = if (json.has("isActive")) json.optBoolean("isActive", targetRule.isActive) else targetRule.isActive

                val data = ProposedActionData.UpdateRecurringData(
                    targetRule = targetRule,
                    newTitle = newTitle,
                    newAmount = newAmount,
                    newType = newType,
                    newCategory = newCategory,
                    newInterval = newInterval,
                    newPaymentMethod = newPaymentMethod,
                    isActive = isActive
                )
                val item = ProposedActionItem(
                    actionType = "Update Recurring Rule",
                    title = "Modify Recurring: ${targetRule.title}",
                    detailSummary = "Amount: $currency${String.format(Locale.US, "%.2f", newAmount)} • Interval: ${newInterval.displayName}",
                    isDestructive = false,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "DELETE_RECURRING" -> {
                val targetQuery = json.optString("targetQuery", "").trim()
                if (targetQuery.isBlank()) {
                    return ActionParseResult(error = "Target recurring rule title cannot be blank for deletion.")
                }
                val allRules = financeViewModel?.allRecurringRules?.value ?: emptyList()
                val matches = allRules.filter { r ->
                    r.title.contains(targetQuery, ignoreCase = true) || r.note.contains(targetQuery, ignoreCase = true)
                }
                if (matches.isEmpty()) {
                    return ActionParseResult(error = "No recurring rule found matching '$targetQuery' to delete.")
                }
                if (matches.size > 1) {
                    return ActionParseResult(error = "Ambiguous recurring rule query '$targetQuery': matches ${matches.size} rules. Please specify exact title.")
                }

                val targetRule = matches.first()
                val data = ProposedActionData.DeleteRecurringData(targetRule = targetRule)
                val item = ProposedActionItem(
                    actionType = "Delete Recurring Rule",
                    title = "Delete Recurring: ${targetRule.title}",
                    detailSummary = "Will stop auto-logging $currency${String.format(Locale.US, "%.2f", targetRule.amount)} (${targetRule.interval.displayName})",
                    isDestructive = true,
                    data = data
                )
                ActionParseResult(item = item)
            }
            "UPDATE_PROFILE" -> {
                val name = if (json.has("name")) json.optString("name").trim().ifBlank { null } else null
                val symbol = if (json.has("currencySymbol")) json.optString("currencySymbol").trim().ifBlank { null } else null

                if (name == null && symbol == null) {
                    return ActionParseResult(error = "Profile update must specify name or currency symbol.")
                }

                if (symbol != null && (symbol.length > 5 || symbol.isBlank())) {
                    return ActionParseResult(error = "Invalid currency symbol '$symbol'. Must be 1 to 5 characters.")
                }

                val data = ProposedActionData.UpdateProfileData(name = name, currencySymbol = symbol)
                val item = ProposedActionItem(
                    actionType = "Update Profile",
                    title = "Update User Profile",
                    detailSummary = "Name: ${name ?: "Unchanged"}, Currency: ${symbol ?: "Unchanged"}",
                    isDestructive = false,
                    data = data
                )
                ActionParseResult(item = item)
            }
            else -> ActionParseResult(error = "Unknown action type '$action'.")
        }
    }

    fun confirmAndExecutePendingAction() {
        val group = _pendingActionGroup.value ?: return
        if (group.validationError != null) return
        if (_isProcessingAction.value) return

        _isProcessingAction.value = true

        viewModelScope.launch {
            val updatedItems = group.actionItems.toMutableList()
            val confirmations = mutableListOf<String>()
            val errors = mutableListOf<String>()

            try {
                for (i in updatedItems.indices) {
                    val item = updatedItems[i]
                    if (item.status == ActionExecutionStatus.SUCCESS) {
                        confirmations.add("• (Already completed) ${item.title}")
                        continue
                    }
                    try {
                        val conf = executeActionData(item.data)
                        val successItem = item.copy(status = ActionExecutionStatus.SUCCESS, errorMessage = null)
                        updatedItems[i] = successItem
                        _pendingActionGroup.value = group.copy(actionItems = updatedItems.toList())
                        if (conf.isNotBlank()) confirmations.add(conf)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to execute action ${item.title}", e)
                        val errorMsg = e.localizedMessage ?: "Execution failed"
                        val failedItem = item.copy(status = ActionExecutionStatus.FAILED, errorMessage = errorMsg)
                        updatedItems[i] = failedItem
                        _pendingActionGroup.value = group.copy(actionItems = updatedItems.toList())
                        errors.add("• Failed: ${item.title} ($errorMsg)")
                        break // Stop executing subsequent actions in this batch
                    }
                }

                val allSuccess = updatedItems.all { it.status == ActionExecutionStatus.SUCCESS }

                if (allSuccess) {
                    val finalMsg = if (confirmations.isNotEmpty()) {
                        "✅ **Action(s) Executed Successfully**:\n" + confirmations.joinToString("\n")
                    } else {
                        "✅ Action executed."
                    }
                    _messages.value = _messages.value + ChatMessage("model", finalMsg)
                    _pendingActionGroup.value = null
                } else {
                    val partialMsg = buildString {
                        append("⚠️ **Action Batch Incomplete**:\n")
                        if (confirmations.isNotEmpty()) {
                            append("\nSucceeded:\n").append(confirmations.joinToString("\n")).append("\n")
                        }
                        if (errors.isNotEmpty()) {
                            append("\nFailed:\n").append(errors.joinToString("\n")).append("\n")
                        }
                        append("\nPlease retry the failed action(s) or cancel.")
                    }
                    _messages.value = _messages.value + ChatMessage("model", partialMsg)
                    _pendingActionGroup.value = group.copy(actionItems = updatedItems.toList())
                }
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    fun cancelPendingAction() {
        if (_isProcessingAction.value) return
        _pendingActionGroup.value = null
    }

    private suspend fun executeActionData(data: ProposedActionData): String {
        val vm = financeViewModel ?: throw IllegalStateException("FinanceViewModel is not attached.")
        val currency = getCurrencySymbol()
        return when (data) {
            is ProposedActionData.LogTransactionData -> {
                val existing = vm.allTransactions.value.find {
                    it.title == data.title &&
                    Math.abs(it.amount - data.amount) < 0.001 &&
                    it.timestamp == data.timestamp &&
                    it.category == data.category
                }
                if (existing == null) {
                    vm.addTransactionSuspend(
                        title = data.title,
                        amount = data.amount,
                        type = data.type,
                        category = data.category,
                        timestamp = data.timestamp,
                        note = data.note,
                        paymentMethod = data.paymentMethod
                    )
                }
                val monthLabel = DateUtils.getMonthLabel(data.timestamp)
                "• Logged **${data.type.displayName}** of **$currency${String.format(Locale.US, "%.2f", data.amount)}** (*${data.title}* - ${data.category.displayName} [$monthLabel])."
            }
            is ProposedActionData.UpdateTransactionData -> {
                val currentTx = vm.allTransactions.value.find { it.id == data.targetTransaction.id }
                    ?: throw IllegalStateException("Transaction '${data.targetTransaction.title}' no longer exists in database.")

                val updated = currentTx.copy(
                    title = data.newTitle,
                    amount = data.newAmount,
                    type = data.newType,
                    category = data.newCategory,
                    paymentMethod = data.newPaymentMethod,
                    timestamp = data.newTimestamp,
                    note = data.newNote
                )
                vm.updateTransactionSuspend(updated)
                "• Updated transaction **${data.targetTransaction.title}** → **${updated.title}** ($currency${String.format(Locale.US, "%.2f", updated.amount)})."
            }
            is ProposedActionData.DeleteTransactionsData -> {
                val remainingIds = data.transactionsToDelete.map { it.id }
                val currentMatching = vm.allTransactions.value.filter { it.id in remainingIds }
                if (currentMatching.isEmpty()) {
                    throw IllegalStateException("Target transaction(s) no longer exist in database.")
                }
                val count = vm.deleteTransactionsByIdsSuspend(currentMatching.map { it.id })
                "• Deleted $count transaction(s): ${data.scopeDescription}."
            }
            is ProposedActionData.SetBudgetData -> {
                vm.saveMonthlyBudgetSuspend(
                    monthKey = data.monthKey,
                    totalBudget = data.totalBudget,
                    categoryBudgets = emptyMap()
                )
                "• Set budget for month **${data.monthKey}** to **$currency${String.format(Locale.US, "%.2f", data.totalBudget)}**."
            }
            is ProposedActionData.DeleteBudgetData -> {
                val currentBudget = vm.allBudgets.value.find { it.monthKey == data.monthKey }
                    ?: throw IllegalStateException("No active budget found for month '${data.monthKey}'.")
                vm.deleteBudgetSuspend(currentBudget.monthKey)
                "• Deleted monthly budget for **${data.monthKey}**."
            }
            is ProposedActionData.LogRecurringData -> {
                val existing = vm.allRecurringRules.value.find {
                    it.title == data.title &&
                    Math.abs(it.amount - data.amount) < 0.001 &&
                    it.interval == data.interval
                }
                if (existing == null) {
                    vm.addRecurringRuleSuspend(
                        title = data.title,
                        amount = data.amount,
                        type = data.type,
                        category = data.category,
                        interval = data.interval,
                        startDate = System.currentTimeMillis(),
                        endDate = null,
                        paymentMethod = data.paymentMethod,
                        note = data.note
                    )
                }
                "• Added recurring **${data.interval.displayName}** rule for **${data.title}** ($currency${String.format(Locale.US, "%.2f", data.amount)})."
            }
            is ProposedActionData.UpdateRecurringData -> {
                val currentRule = vm.allRecurringRules.value.find { it.id == data.targetRule.id }
                    ?: throw IllegalStateException("Recurring rule '${data.targetRule.title}' no longer exists in database.")

                val updated = currentRule.copy(
                    title = data.newTitle,
                    amount = data.newAmount,
                    type = data.newType,
                    category = data.newCategory,
                    interval = data.newInterval,
                    paymentMethod = data.newPaymentMethod,
                    isActive = data.isActive
                )
                vm.updateRecurringRuleSuspend(updated)
                "• Updated recurring rule **${data.targetRule.title}** → **${updated.title}** ($currency${String.format(Locale.US, "%.2f", updated.amount)})."
            }
            is ProposedActionData.DeleteRecurringData -> {
                val currentRule = vm.allRecurringRules.value.find { it.id == data.targetRule.id }
                    ?: throw IllegalStateException("Recurring rule '${data.targetRule.title}' no longer exists in database.")
                vm.deleteRecurringRuleSuspend(currentRule.id)
                "• Deleted recurring rule **${data.targetRule.title}**."
            }
            is ProposedActionData.UpdateProfileData -> {
                vm.updateUserProfileSuspend(name = data.name, currencySymbol = data.currencySymbol)
                "• Updated user profile settings."
            }
        }
    }

    private fun parseDateToTimestamp(
        dateStr: String?,
        isUpdate: Boolean = false,
        originalTimestamp: Long? = null
    ): Result<Long> {
        if (dateStr.isNullOrBlank()) {
            return if (isUpdate) {
                Result.success(originalTimestamp ?: System.currentTimeMillis())
            } else {
                Result.success(System.currentTimeMillis())
            }
        }
        val clean = dateStr.trim()
        val formats = listOf(
            "yyyy-MM-dd" to false,
            "yyyy-MM" to true,
            "yyyy/MM/dd" to false,
            "yyyy/MM" to true,
            "MMMM yyyy" to true,
            "MMM yyyy" to true,
            "MMMM, yyyy" to true
        )

        for ((pattern, isMonthOnly) in formats) {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val pos = java.text.ParsePosition(0)
            val parsed = sdf.parse(clean, pos)
            if (parsed != null && pos.index == clean.length) {
                return if (isMonthOnly) {
                    val cal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    Result.success(cal.timeInMillis)
                } else {
                    Result.success(parsed.time)
                }
            }
        }
        return Result.failure(IllegalArgumentException("Invalid date format '$dateStr'. Please use YYYY-MM-DD or YYYY-MM."))
    }

    private fun parseTransactionType(str: String?, default: TransactionType? = null): Result<TransactionType> {
        if (str.isNullOrBlank()) {
            return if (default != null) Result.success(default)
            else Result.failure(IllegalArgumentException("Transaction type cannot be blank."))
        }
        val clean = str.trim()
        val found = TransactionType.values().find {
            it.name.equals(clean, ignoreCase = true) ||
            it.displayName.equals(clean, ignoreCase = true)
        } ?: return Result.failure(IllegalArgumentException("Unknown transaction type '$str'."))

        return Result.success(found)
    }

    private fun parseCategory(str: String?, type: TransactionType): Result<TransactionCategory> {
        if (str.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Category cannot be blank."))
        }
        val clean = str.trim()
        val found = TransactionCategory.values().find {
            it.name.equals(clean, ignoreCase = true) ||
            it.displayName.equals(clean, ignoreCase = true)
        } ?: return Result.failure(IllegalArgumentException("Unknown category '$str'."))

        if (found.defaultType != type) {
            return Result.failure(IllegalArgumentException("Category '${found.displayName}' is not valid for ${type.displayName} transactions."))
        }
        return Result.success(found)
    }

    private fun parsePaymentMethod(str: String?): Result<PaymentMethod> {
        if (str.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Payment method cannot be blank."))
        }
        val clean = str.trim()
        val uppercase = clean.uppercase()

        val explicitAliasMap = mapOf(
            "CC" to PaymentMethod.CREDIT_CARD,
            "DC" to PaymentMethod.DEBIT_CARD,
            "NETBANKING" to PaymentMethod.NET_BANKING,
            "NET BANKING" to PaymentMethod.NET_BANKING,
            "CREDIT CARD" to PaymentMethod.CREDIT_CARD,
            "DEBIT CARD" to PaymentMethod.DEBIT_CARD,
            "BANK_TRANSFER" to PaymentMethod.NET_BANKING,
            "BANK TRANSFER" to PaymentMethod.NET_BANKING
        )

        explicitAliasMap[uppercase]?.let { return Result.success(it) }

        val found = PaymentMethod.values().find {
            it.name.equals(clean, ignoreCase = true) ||
            it.displayName.equals(clean, ignoreCase = true)
        } ?: return Result.failure(IllegalArgumentException("Unknown payment method '$str'."))

        return Result.success(found)
    }

    private fun parseInterval(str: String?): Result<RecurrenceInterval> {
        if (str.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Recurrence interval cannot be blank."))
        }
        val clean = str.trim()
        val found = RecurrenceInterval.values().find {
            it.name.equals(clean, ignoreCase = true) ||
            it.displayName.equals(clean, ignoreCase = true)
        } ?: return Result.failure(IllegalArgumentException("Unknown recurrence interval '$str'."))

        return Result.success(found)
    }

    fun retryLastMessage() {
        val promptToRetry = lastFailedPrompt ?: _messages.value.lastOrNull { it.role == "user" }?.text
        if (promptToRetry != null && !_isProcessingAction.value && _pendingActionGroup.value == null) {
            executeSend(promptToRetry, isRetry = true)
        }
    }

    fun requestFinancialInsight(financialSummary: String = "") {
        val prompt = "Analyze my spending and financial records for the current month and my overall history from the snapshot. Provide a clear breakdown of income vs expenses, top spending categories, savings rate, and budget usage. Give me 3 actionable tips based ONLY on my actual numbers."
        sendMessage(prompt)
    }
}


