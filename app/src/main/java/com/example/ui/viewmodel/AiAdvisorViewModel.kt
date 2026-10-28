package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
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

data class ProposedActionItem(
    val actionType: String,
    val title: String,
    val detailSummary: String,
    val isDestructive: Boolean,
    val data: ProposedActionData
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

    fun bindFinanceViewModel(viewModel: FinanceViewModel) {
        this.financeViewModel = viewModel
    }

    private fun getCurrencySymbol(): String {
        return financeViewModel?.userProfile?.value?.currencySymbol ?: "₹"
    }

    private fun obtainChat(): com.google.firebase.ai.Chat {
        activeChat?.let { return it }
        val currentMonthKey = DateUtils.getMonthKey(System.currentTimeMillis())
        val currentMonthLabel = DateUtils.getMonthLabel(System.currentTimeMillis())
        val currentYear = SimpleDateFormat("yyyy", Locale.US).format(Date())

        val newChat = Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite"
        ).startChat(
            history = listOf(
                content("user") {
                    text("""
SYSTEM DIRECTIVE: You are Paisa AI, the complete AGENTIC personal financial assistant in Paisa Pro.
You have full access to the user's LIVE FINANCIAL DATABASE SNAPSHOT provided with each user query.
You MUST use the exact numbers, transactions, category totals, budgets, and recurring rules from the snapshot to answer queries.

Current Date / Month: $currentMonthLabel (Month Key: $currentMonthKey, Year: $currentYear).

When asked for spending analysis, financial summaries, or queries about specific months/years (e.g. "August 2026", "current month", "year 2026", "this year"):
1. Identify the requested month(s) or year in the snapshot.
2. If multiple months or a year are requested, perform the analysis for EACH period found in the snapshot, then provide an overall summary.
3. For each period, calculate and present Total Income, Total Expense, Net Savings, Savings Rate, Top Spending Categories with amounts/percentages, and Budget utilization.
4. If no transactions exist for the requested period in the snapshot, You MUST state clearly: "I see no financial records for [Period] in your database." Do NOT hallucinate or provide generic summaries if data is missing.
5. Provide high-value, personalized financial analysis and actionable tips based directly on the actual calculated numbers for the requested time range.

When asked to take single OR multiple actions (e.g. "log my salary of 3000 for August 2026", "log expenses for Jan-Mar", "delete my last Starbucks transaction", "update my Netflix recurring rule amount to $18", "delete budget for January"):
1. Give a brief, crisp text answer confirming the action proposal.
2. Append an `ACTION_JSON:` block containing a single action object OR an array of action objects.

Supported Action Schemas:

1. Log Transaction (supports custom `date` in "YYYY-MM-DD" or "YYYY-MM"):
ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Groceries", "amount": 45.0, "type": "EXPENSE", "category": "GROCERIES", "paymentMethod": "UPI", "date": "2026-08-15", "note": "Weekly veggies"}

Multiple Transactions Array Example:
ACTION_JSON: [
  {"action": "LOG_TRANSACTION", "title": "Salary", "amount": 3000.0, "type": "INCOME", "category": "SALARY", "paymentMethod": "NET_BANKING", "date": "2026-08-01"},
  {"action": "LOG_TRANSACTION", "title": "Salary", "amount": 3000.0, "type": "INCOME", "category": "SALARY", "paymentMethod": "NET_BANKING", "date": "2026-09-01"}
]

2. Modify / Update Existing Transaction:
ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "Groceries", "targetDate": "2026-08-15", "newTitle": "Fresh Groceries", "newAmount": 50.0, "newCategory": "GROCERIES", "newPaymentMethod": "CREDIT_CARD"}

3. Delete Transaction(s):
ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "Groceries", "targetDate": "2026-08-15"}
(Use "targetQuery": "ALL" and "targetDate": "YYYY-MM" to delete all transactions for a specific month)

4. Set Monthly Budget:
ACTION_JSON: {"action": "SET_BUDGET", "monthKey": "2026-08", "totalBudget": 1500.0}

5. Delete Monthly Budget:
ACTION_JSON: {"action": "DELETE_BUDGET", "monthKey": "2026-08"}

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
                    text("Understood! I am Paisa AI. I will provide strict, data-driven financial analysis using ONLY your provided snapshot data, and I will propose safe financial actions for your confirmation. I will explicitly state when data for a requested period is missing.")
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
            ChatMessage("model", "Hello! I'm Paisa AI. I can analyze your spending for any month or year, and propose transactions, budgets, or recurring rules for you to confirm!")
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || _isProcessingAction.value || _pendingActionGroup.value != null) return
        val userMsg = ChatMessage("user", prompt)
        _messages.value = _messages.value + userMsg
        _uiState.value = AiUiState.Loading

        val snapshot = buildFinancialDatabaseSnapshot()
        val fullPromptWithSnapshot = """
            |[LIVE FINANCIAL DATABASE SNAPSHOT]:
            |$snapshot
            |
            |[USER QUESTION / REQUEST]:
            |$prompt
        """.trimMargin()

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val currentChat = obtainChat()
                withTimeout(60_000L) {
                    currentChat.sendMessageStream(fullPromptWithSnapshot).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulated.append(chunkText)
                        _uiState.value = AiUiState.Streaming(accumulated.toString())
                    }
                }
                val rawAnswer = accumulated.toString()
                if (rawAnswer.isNotBlank()) {
                    val (cleanedText, actionNotice) = processAgenticAction(rawAnswer)
                    val finalText = if (actionNotice.isNotBlank()) {
                        "$cleanedText\n\n$actionNotice"
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

    private fun parseAndValidateSingleAction(json: JSONObject): ActionParseResult {
        val currency = getCurrencySymbol()
        val action = json.optString("action", "").trim().uppercase()
        return when (action) {
            "LOG_TRANSACTION", "ADD_TRANSACTION" -> {
                val title = json.optString("title", "Transaction").trim()
                val amount = json.optDouble("amount", 0.0)
                if (amount <= 0.0 || !amount.isFinite()) {
                    return ActionParseResult(error = "Invalid transaction amount: must be positive and finite.")
                }
                val typeStr = json.optString("type", "EXPENSE")
                val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val categoryStr = json.optString("category", if (type == TransactionType.INCOME) "OTHER_INCOME" else "OTHER_EXPENSE")
                val category = parseCategory(categoryStr, type)
                val paymentMethodStr = json.optString("paymentMethod", "UPI")
                val paymentMethod = parsePaymentMethod(paymentMethodStr)
                val dateStr = json.optString("date", "")
                val timestamp = parseDateToTimestamp(dateStr)
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

                val allTxs = financeViewModel?.allTransactions?.value ?: emptyList()
                val matches = allTxs.filter { tx ->
                    val queryMatch = targetQuery.isBlank() || tx.title.contains(targetQuery, ignoreCase = true) || tx.note.contains(targetQuery, ignoreCase = true)
                    val dateMatch = targetDate.isBlank() || DateUtils.getMonthKey(tx.timestamp) == targetDate || SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) == targetDate
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
                val newAmount = if (json.has("newAmount")) json.optDouble("newAmount", targetTx.amount) else targetTx.amount
                if (newAmount <= 0.0 || !newAmount.isFinite()) {
                    return ActionParseResult(error = "New transaction amount must be positive and finite.")
                }
                val newTypeStr = if (json.has("newType")) json.optString("newType") else targetTx.type.name
                val newType = if (newTypeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val newCategoryStr = if (json.has("newCategory")) json.optString("newCategory") else targetTx.category.name
                val newCategory = parseCategory(newCategoryStr, newType)
                val newPaymentMethodStr = if (json.has("newPaymentMethod")) json.optString("newPaymentMethod") else targetTx.paymentMethod.name
                val newPaymentMethod = parsePaymentMethod(newPaymentMethodStr)
                val newDateStr = if (json.has("newDate")) json.optString("newDate") else ""
                val newTimestamp = if (newDateStr.isNotBlank()) parseDateToTimestamp(newDateStr) else targetTx.timestamp
                val newNote = if (json.has("newNote")) json.optString("newNote").trim() else targetTx.note

                val data = ProposedActionData.UpdateTransactionData(
                    targetTransaction = targetTx,
                    newTitle = newTitle.ifBlank { targetTx.title },
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

                val allTxs = financeViewModel?.allTransactions?.value ?: emptyList()
                val isAll = targetQuery.equals("ALL", ignoreCase = true) || targetQuery.equals("EVERYTHING", ignoreCase = true)

                val matches = allTxs.filter { tx ->
                    val queryMatch = isAll || targetQuery.isBlank() || tx.title.contains(targetQuery, ignoreCase = true) || tx.category.displayName.contains(targetQuery, ignoreCase = true) || tx.note.contains(targetQuery, ignoreCase = true)
                    val dateMatch = targetDate.isBlank() || DateUtils.getMonthKey(tx.timestamp) == targetDate || SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) == targetDate
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
                val monthKey = json.optString("monthKey", DateUtils.getMonthKey(System.currentTimeMillis())).trim()
                if (monthKey.isBlank()) {
                    return ActionParseResult(error = "Month key for budget deletion cannot be blank.")
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
                val title = json.optString("title", "Recurring Bill").trim()
                val amount = json.optDouble("amount", 0.0)
                if (amount <= 0.0 || !amount.isFinite()) {
                    return ActionParseResult(error = "Recurring rule amount must be positive and finite.")
                }
                val typeStr = json.optString("type", "EXPENSE")
                val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val categoryStr = json.optString("category", "SUBSCRIPTIONS")
                val category = parseCategory(categoryStr, type)
                val intervalStr = json.optString("interval", "MONTHLY")
                val interval = parseInterval(intervalStr)
                val paymentMethodStr = json.optString("paymentMethod", "UPI")
                val paymentMethod = parsePaymentMethod(paymentMethodStr)

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
                val newAmount = if (json.has("newAmount")) json.optDouble("newAmount", targetRule.amount) else targetRule.amount
                if (newAmount <= 0.0 || !newAmount.isFinite()) {
                    return ActionParseResult(error = "New recurring rule amount must be positive and finite.")
                }
                val newTypeStr = if (json.has("newType")) json.optString("newType") else targetRule.type.name
                val newType = if (newTypeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val newCategoryStr = if (json.has("newCategory")) json.optString("newCategory") else targetRule.category.name
                val newCategory = parseCategory(newCategoryStr, newType)
                val newIntervalStr = if (json.has("newInterval")) json.optString("newInterval") else targetRule.interval.name
                val newInterval = parseInterval(newIntervalStr)
                val newPaymentMethodStr = if (json.has("newPaymentMethod")) json.optString("newPaymentMethod") else targetRule.paymentMethod.name
                val newPaymentMethod = parsePaymentMethod(newPaymentMethodStr)
                val isActive = if (json.has("isActive")) json.optBoolean("isActive", targetRule.isActive) else targetRule.isActive

                val data = ProposedActionData.UpdateRecurringData(
                    targetRule = targetRule,
                    newTitle = newTitle.ifBlank { targetRule.title },
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
                val name = if (json.has("name")) json.optString("name").trim() else null
                val symbol = if (json.has("currencySymbol")) json.optString("currencySymbol").trim() else null
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
        val confirmations = mutableListOf<String>()

        try {
            for (item in group.actionItems) {
                val conf = executeActionData(item.data)
                if (conf.isNotBlank()) confirmations.add(conf)
            }

            val finalMsg = if (confirmations.isNotEmpty()) {
                "✅ **Action Executed Successfully**:\n" + confirmations.joinToString("\n")
            } else {
                "✅ Action executed."
            }

            _messages.value = _messages.value + ChatMessage("model", finalMsg)
            _pendingActionGroup.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute confirmed action", e)
            _messages.value = _messages.value + ChatMessage("model", "❌ Action failed: ${e.localizedMessage}")
        } finally {
            _isProcessingAction.value = false
        }
    }

    fun cancelPendingAction() {
        _pendingActionGroup.value = null
    }

    private fun executeActionData(data: ProposedActionData): String {
        val currency = getCurrencySymbol()
        return when (data) {
            is ProposedActionData.LogTransactionData -> {
                financeViewModel?.addTransaction(
                    title = data.title,
                    amount = data.amount,
                    type = data.type,
                    category = data.category,
                    timestamp = data.timestamp,
                    note = data.note,
                    paymentMethod = data.paymentMethod
                )
                val monthLabel = DateUtils.getMonthLabel(data.timestamp)
                "• Logged **${data.type.displayName}** of **$currency${String.format(Locale.US, "%.2f", data.amount)}** (*${data.title}* - ${data.category.displayName} [$monthLabel])."
            }
            is ProposedActionData.UpdateTransactionData -> {
                val updated = data.targetTransaction.copy(
                    title = data.newTitle,
                    amount = data.newAmount,
                    type = data.newType,
                    category = data.newCategory,
                    paymentMethod = data.newPaymentMethod,
                    timestamp = data.newTimestamp,
                    note = data.newNote
                )
                financeViewModel?.updateTransaction(updated)
                "• Updated transaction **${data.targetTransaction.title}** → **${updated.title}** ($currency${String.format(Locale.US, "%.2f", updated.amount)})."
            }
            is ProposedActionData.DeleteTransactionsData -> {
                val ids = data.transactionsToDelete.map { it.id }
                financeViewModel?.deleteTransactionsByIds(ids)
                "• Deleted ${data.transactionsToDelete.size} transaction(s): ${data.scopeDescription}."
            }
            is ProposedActionData.SetBudgetData -> {
                financeViewModel?.saveMonthlyBudget(
                    monthKey = data.monthKey,
                    totalBudget = data.totalBudget,
                    categoryBudgets = emptyMap()
                )
                "• Set budget for month **${data.monthKey}** to **$currency${String.format(Locale.US, "%.2f", data.totalBudget)}**."
            }
            is ProposedActionData.DeleteBudgetData -> {
                financeViewModel?.deleteBudget(data.monthKey)
                "• Deleted monthly budget for **${data.monthKey}**."
            }
            is ProposedActionData.LogRecurringData -> {
                financeViewModel?.addRecurringRule(
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
                "• Added recurring **${data.interval.displayName}** rule for **${data.title}** ($currency${String.format(Locale.US, "%.2f", data.amount)})."
            }
            is ProposedActionData.UpdateRecurringData -> {
                val updated = data.targetRule.copy(
                    title = data.newTitle,
                    amount = data.newAmount,
                    type = data.newType,
                    category = data.newCategory,
                    interval = data.newInterval,
                    paymentMethod = data.newPaymentMethod,
                    isActive = data.isActive
                )
                financeViewModel?.updateRecurringRule(updated)
                "• Updated recurring rule **${data.targetRule.title}** → **${updated.title}** ($currency${String.format(Locale.US, "%.2f", updated.amount)})."
            }
            is ProposedActionData.DeleteRecurringData -> {
                financeViewModel?.deleteRecurringRule(data.targetRule.id)
                "• Deleted recurring rule **${data.targetRule.title}**."
            }
            is ProposedActionData.UpdateProfileData -> {
                financeViewModel?.updateUserProfile(data.name)
                "• Updated user profile settings."
            }
        }
    }

    private fun parseDateToTimestamp(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        val clean = dateStr.trim()
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("yyyy-MM", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("yyyy/MM", Locale.US),
            SimpleDateFormat("MMMM yyyy", Locale.US),
            SimpleDateFormat("MMM yyyy", Locale.US),
            SimpleDateFormat("MMMM, yyyy", Locale.US)
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

    fun requestFinancialInsight(financialSummary: String = "") {
        val prompt = "Analyze my spending and financial records for the current month and my overall history from the snapshot. Provide a clear breakdown of income vs expenses, top spending categories, savings rate, and budget usage. Give me 3 actionable tips based ONLY on my actual numbers."
        sendMessage(prompt)
    }
}
