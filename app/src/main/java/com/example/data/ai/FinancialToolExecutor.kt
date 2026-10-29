package com.example.data.ai

import com.example.data.insight.FinanceInsightEngine
import com.example.data.model.BudgetModel
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Interface defining execution of read-only financial query tools.
 * Implementations strictly perform read operations against local financial data
 * and return structured JSON responses.
 */
interface FinancialToolExecutor {
  suspend fun executeTool(name: String, args: Map<String, Any?>): JSONObject
}

/**
 * Default implementation of [FinancialToolExecutor] operating on local records
 * supplied by providers or repositories.
 */
class DefaultFinancialToolExecutor(
  private val getTransactions: suspend () -> List<TransactionItem>,
  private val getBudgets: suspend () -> List<BudgetModel>,
  private val getRecurringRules: suspend () -> List<RecurringRule>,
  private val getCurrencySymbol: () -> String = { "₹" }
) : FinancialToolExecutor {

  companion object {
    const val MAX_TRANSACTION_SEARCH_LIMIT = 25
    const val DEFAULT_TRANSACTION_SEARCH_LIMIT = 15
  }

  override suspend fun executeTool(name: String, args: Map<String, Any?>): JSONObject {
    return try {
      when (name) {
        "getFinancialSummary" -> executeGetFinancialSummary(args)
        "comparePeriods" -> executeComparePeriods(args)
        "getCategorySpending" -> executeGetCategorySpending(args)
        "getBudgetStatus" -> executeGetBudgetStatus(args)
        "getRecurringCommitments" -> executeGetRecurringCommitments(args)
        "searchTransactions" -> executeSearchTransactions(args)
        else -> JSONObject().apply {
          put("status", "ERROR")
          put("errorCode", "UNKNOWN_FUNCTION")
          put("message", "Unknown query tool function '$name'. Supported tools: getFinancialSummary, comparePeriods, getCategorySpending, getBudgetStatus, getRecurringCommitments, searchTransactions.")
        }
      }
    } catch (e: IllegalArgumentException) {
      JSONObject().apply {
        put("status", "ERROR")
        put("errorCode", "INVALID_ARGUMENTS")
        put("message", e.message ?: "Invalid tool arguments.")
      }
    } catch (e: Exception) {
      JSONObject().apply {
        put("status", "ERROR")
        put("errorCode", "EXECUTION_ERROR")
        put("message", "Failed to execute query tool '$name': ${e.message}")
      }
    }
  }

  // -------------------------------------------------------------
  // 1. getFinancialSummary
  // -------------------------------------------------------------
  private suspend fun executeGetFinancialSummary(args: Map<String, Any?>): JSONObject {
    val periodType = (args["periodType"] as? String)?.trim()?.uppercase(Locale.US)
      ?: throw IllegalArgumentException("Missing required parameter 'periodType' (expected 'MONTHLY' or 'YEARLY').")
    val period = (args["period"] as? String)?.trim()
      ?: throw IllegalArgumentException("Missing required parameter 'period'.")

    val transactions = getTransactions()
    val budgets = getBudgets()
    val rules = getRecurringRules()
    val currency = getCurrencySymbol()

    return when (periodType) {
      "MONTHLY" -> {
        if (!period.matches(Regex("""^\d{4}-\d{2}$"""))) {
          throw IllegalArgumentException("Invalid monthly period '$period'. Expected format 'YYYY-MM' (e.g. '2026-08').")
        }
        val monthTxs = transactions.filter { DateUtils.getMonthKey(it.timestamp) == period }
        val totalIncome = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0.0) ((netSavings / totalIncome) * 100.0).coerceAtLeast(0.0) else 0.0

        val budget = budgets.find { it.monthKey == period }
        val budgetLimit = budget?.totalBudget
        val budgetUsedPercent = if (budgetLimit != null && budgetLimit > 0.0) {
          ((totalExpense / budgetLimit) * 100.0)
        } else null

        val insights = FinanceInsightEngine.generateMonthlyInsights(
          monthKey = period,
          transactions = transactions,
          budgets = budgets,
          recurringRules = rules,
          currencySymbol = currency
        )

        JSONObject().apply {
          put("status", "SUCCESS")
          put("periodType", "MONTHLY")
          put("period", period)
          put("currencySymbol", currency)
          put("hasData", monthTxs.isNotEmpty())
          put("transactionCount", monthTxs.size)
          put("totalIncome", totalIncome)
          put("totalExpense", totalExpense)
          put("netSavings", netSavings)
          put("savingsRatePercent", String.format(Locale.US, "%.1f", savingsRate).toDouble())
          if (budgetLimit != null) {
            put("budgetLimit", budgetLimit)
            put("budgetUsedPercent", String.format(Locale.US, "%.1f", budgetUsedPercent ?: 0.0).toDouble())
            put("isOverBudget", totalExpense > budgetLimit)
          } else {
            put("budgetLimit", JSONObject.NULL)
            put("budgetStatus", "NO_BUDGET_CONFIGURED")
          }
          val insightsArray = JSONArray()
          insights.take(4).forEach { ins ->
            insightsArray.put(JSONObject().apply {
              put("id", ins.id)
              put("type", ins.type.name)
              put("title", ins.title)
              put("description", ins.description)
              put("severity", ins.severity.name)
            })
          }
          put("insights", insightsArray)
        }
      }
      "YEARLY" -> {
        if (!period.matches(Regex("""^\d{4}$"""))) {
          throw IllegalArgumentException("Invalid yearly period '$period'. Expected format 'YYYY' (e.g. '2026').")
        }
        val targetYear = period.toIntOrNull()
          ?: throw IllegalArgumentException("Invalid year integer '$period'.")
        val yearTxs = transactions.filter {
          val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
          cal.get(Calendar.YEAR) == targetYear
        }
        val totalIncome = yearTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = yearTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0.0) ((netSavings / totalIncome) * 100.0).coerceAtLeast(0.0) else 0.0

        val insights = FinanceInsightEngine.generateYearlyInsights(
          year = targetYear,
          transactions = transactions,
          budgets = budgets,
          recurringRules = rules,
          currencySymbol = currency
        )

        JSONObject().apply {
          put("status", "SUCCESS")
          put("periodType", "YEARLY")
          put("period", period)
          put("currencySymbol", currency)
          put("hasData", yearTxs.isNotEmpty())
          put("transactionCount", yearTxs.size)
          put("totalIncome", totalIncome)
          put("totalExpense", totalExpense)
          put("netSavings", netSavings)
          put("savingsRatePercent", String.format(Locale.US, "%.1f", savingsRate).toDouble())
          val insightsArray = JSONArray()
          insights.take(4).forEach { ins ->
            insightsArray.put(JSONObject().apply {
              put("id", ins.id)
              put("type", ins.type.name)
              put("title", ins.title)
              put("description", ins.description)
              put("severity", ins.severity.name)
            })
          }
          put("insights", insightsArray)
        }
      }
      else -> throw IllegalArgumentException("Invalid periodType '$periodType'. Must be 'MONTHLY' or 'YEARLY'.")
    }
  }

  // -------------------------------------------------------------
  // 2. comparePeriods
  // -------------------------------------------------------------
  private suspend fun executeComparePeriods(args: Map<String, Any?>): JSONObject {
    val periodType = (args["periodType"] as? String)?.trim()?.uppercase(Locale.US)
      ?: throw IllegalArgumentException("Missing required parameter 'periodType' ('MONTHLY' or 'YEARLY').")
    val period1 = (args["period1"] as? String)?.trim()
      ?: throw IllegalArgumentException("Missing required parameter 'period1'.")
    val period2 = (args["period2"] as? String)?.trim()
      ?: throw IllegalArgumentException("Missing required parameter 'period2'.")

    val transactions = getTransactions()
    val currency = getCurrencySymbol()

    fun getTotalsForPeriod(p: String): Triple<Double, Double, Int> {
      val matching = if (periodType == "MONTHLY") {
        if (!p.matches(Regex("""^\d{4}-\d{2}$"""))) {
          throw IllegalArgumentException("Invalid month format '$p'. Expected 'YYYY-MM'.")
        }
        transactions.filter { DateUtils.getMonthKey(it.timestamp) == p }
      } else {
        if (!p.matches(Regex("""^\d{4}$"""))) {
          throw IllegalArgumentException("Invalid year format '$p'. Expected 'YYYY'.")
        }
        val y = p.toInt()
        transactions.filter {
          val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
          cal.get(Calendar.YEAR) == y
        }
      }
      val inc = matching.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
      val exp = matching.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
      return Triple(inc, exp, matching.size)
    }

    val (inc1, exp1, count1) = getTotalsForPeriod(period1)
    val (inc2, exp2, count2) = getTotalsForPeriod(period2)

    val expenseDiff = exp2 - exp1
    val expenseDiffPercent = if (exp1 > 0.0) ((expenseDiff / exp1) * 100.0) else if (exp2 > 0.0) 100.0 else 0.0
    val incomeDiff = inc2 - inc1
    val net1 = inc1 - exp1
    val net2 = inc2 - exp2
    val netDiff = net2 - net1

    return JSONObject().apply {
      put("status", "SUCCESS")
      put("periodType", periodType)
      put("currencySymbol", currency)
      put("period1", JSONObject().apply {
        put("period", period1)
        put("income", inc1)
        put("expense", exp1)
        put("netSavings", net1)
        put("transactionCount", count1)
      })
      put("period2", JSONObject().apply {
        put("period", period2)
        put("income", inc2)
        put("expense", exp2)
        put("netSavings", net2)
        put("transactionCount", count2)
      })
      put("expenseDifference", expenseDiff)
      put("expenseDifferencePercent", String.format(Locale.US, "%.1f", expenseDiffPercent).toDouble())
      put("incomeDifference", incomeDiff)
      put("netSavingsDifference", netDiff)
      put("comparisonSummary", when {
        expenseDiff > 0 -> "Spending in $period2 was higher by $currency${String.format(Locale.US, "%.2f", expenseDiff)} (${String.format(Locale.US, "%.1f", expenseDiffPercent)}%) compared to $period1."
        expenseDiff < 0 -> "Spending in $period2 was lower by $currency${String.format(Locale.US, "%.2f", -expenseDiff)} (${String.format(Locale.US, "%.1f", -expenseDiffPercent)}%) compared to $period1."
        else -> "Spending remained identical between $period1 and $period2."
      })
    }
  }

  // -------------------------------------------------------------
  // 3. getCategorySpending
  // -------------------------------------------------------------
  private suspend fun executeGetCategorySpending(args: Map<String, Any?>): JSONObject {
    val startDateStr = (args["startDate"] as? String)?.trim()
      ?: throw IllegalArgumentException("Missing required parameter 'startDate'.")
    val endDateStr = (args["endDate"] as? String)?.trim()
      ?: throw IllegalArgumentException("Missing required parameter 'endDate'.")
    val categoryFilter = (args["category"] as? String)?.trim()?.uppercase(Locale.US)
    val typeStr = (args["type"] as? String)?.trim()?.uppercase(Locale.US) ?: "EXPENSE"

    val targetType = try {
      TransactionType.valueOf(typeStr)
    } catch (_: Exception) {
      throw IllegalArgumentException("Invalid type '$typeStr'. Must be 'EXPENSE' or 'INCOME'.")
    }

    val startTimestamp = parseDateBoundary(startDateStr, isStart = true)
    val endTimestamp = parseDateBoundary(endDateStr, isStart = false)

    if (startTimestamp > endTimestamp) {
      throw IllegalArgumentException("Start date '$startDateStr' cannot be after end date '$endDateStr'.")
    }

    val transactions = getTransactions().filter {
      it.timestamp in startTimestamp..endTimestamp && it.type == targetType
    }

    val filteredTxs = if (!categoryFilter.isNullOrBlank()) {
      val cat = parseCategory(categoryFilter)
      transactions.filter { it.category == cat }
    } else {
      transactions
    }

    val totalAmount = filteredTxs.sumOf { it.amount }
    val currency = getCurrencySymbol()

    val categoryBreakdown = filteredTxs
      .groupBy { it.category }
      .map { (cat, txs) ->
        val sum = txs.sumOf { it.amount }
        val pct = if (totalAmount > 0.0) ((sum / totalAmount) * 100.0) else 0.0
        JSONObject().apply {
          put("category", cat.name)
          put("displayName", cat.displayName)
          put("amount", sum)
          put("percentage", String.format(Locale.US, "%.1f", pct).toDouble())
          put("transactionCount", txs.size)
        }
      }
      .sortedByDescending { it.getDouble("amount") }

    return JSONObject().apply {
      put("status", "SUCCESS")
      put("startDate", startDateStr)
      put("endDate", endDateStr)
      put("type", targetType.name)
      put("currencySymbol", currency)
      put("totalAmount", totalAmount)
      put("totalTransactions", filteredTxs.size)
      put("categories", JSONArray(categoryBreakdown))
    }
  }

  // -------------------------------------------------------------
  // 4. getBudgetStatus
  // -------------------------------------------------------------
  private suspend fun executeGetBudgetStatus(args: Map<String, Any?>): JSONObject {
    val monthKey = (args["monthKey"] as? String)?.trim()
      ?: throw IllegalArgumentException("Missing required parameter 'monthKey'.")

    if (!monthKey.matches(Regex("""^\d{4}-\d{2}$"""))) {
      throw IllegalArgumentException("Invalid monthKey '$monthKey'. Expected format 'YYYY-MM' (e.g. '2026-08').")
    }

    val budgets = getBudgets()
    val transactions = getTransactions()
    val currency = getCurrencySymbol()

    val budget = budgets.find { it.monthKey == monthKey }
    val monthExpenses = transactions
      .filter { DateUtils.getMonthKey(it.timestamp) == monthKey && it.type == TransactionType.EXPENSE }
      .sumOf { it.amount }

    return JSONObject().apply {
      put("status", "SUCCESS")
      put("monthKey", monthKey)
      put("currencySymbol", currency)
      put("totalExpense", monthExpenses)
      if (budget != null) {
        val limit = budget.totalBudget
        val remaining = limit - monthExpenses
        val percentUsed = if (limit > 0.0) (monthExpenses / limit) * 100.0 else 0.0
        put("budgetLimit", limit)
        put("remaining", remaining)
        put("percentUsed", String.format(Locale.US, "%.1f", percentUsed).toDouble())
        put("isOverBudget", monthExpenses > limit)
        put("budgetStatus", when {
          monthExpenses > limit -> "OVER_BUDGET"
          percentUsed >= 85.0 -> "NEAR_LIMIT"
          else -> "UNDER_BUDGET"
        })
      } else {
        put("budgetLimit", JSONObject.NULL)
        put("remaining", JSONObject.NULL)
        put("percentUsed", JSONObject.NULL)
        put("budgetStatus", "NOT_SET")
        put("message", "No budget has been configured for $monthKey.")
      }
    }
  }

  // -------------------------------------------------------------
  // 5. getRecurringCommitments
  // -------------------------------------------------------------
  private suspend fun executeGetRecurringCommitments(args: Map<String, Any?>): JSONObject {
    val typeFilter = (args["type"] as? String)?.trim()?.uppercase(Locale.US) ?: "ALL"
    val allRules = getRecurringRules().filter { it.isActive }
    val currency = getCurrencySymbol()

    val filteredRules = when (typeFilter) {
      "EXPENSE" -> allRules.filter { it.type == TransactionType.EXPENSE }
      "INCOME" -> allRules.filter { it.type == TransactionType.INCOME }
      "ALL" -> allRules
      else -> throw IllegalArgumentException("Invalid type '$typeFilter'. Must be 'EXPENSE', 'INCOME', or 'ALL'.")
    }

    fun monthlyEquivalent(rule: RecurringRule): Double {
      return when (rule.interval) {
        RecurrenceInterval.DAILY -> rule.amount * 30.0
        RecurrenceInterval.WEEKLY -> rule.amount * 4.3333
        RecurrenceInterval.MONTHLY -> rule.amount
        RecurrenceInterval.YEARLY -> rule.amount / 12.0
      }
    }

    val totalMonthlyExpense = filteredRules
      .filter { it.type == TransactionType.EXPENSE }
      .sumOf { monthlyEquivalent(it) }

    val totalMonthlyIncome = filteredRules
      .filter { it.type == TransactionType.INCOME }
      .sumOf { monthlyEquivalent(it) }

    val rulesArray = JSONArray()
    filteredRules.sortedByDescending { it.amount }.forEach { rule ->
      rulesArray.put(JSONObject().apply {
        put("id", rule.id)
        put("title", rule.title)
        put("amount", rule.amount)
        put("type", rule.type.name)
        put("category", rule.category.name)
        put("interval", rule.interval.name)
        put("estimatedMonthlyAmount", String.format(Locale.US, "%.2f", monthlyEquivalent(rule)).toDouble())
      })
    }

    return JSONObject().apply {
      put("status", "SUCCESS")
      put("typeFilter", typeFilter)
      put("currencySymbol", currency)
      put("activeRuleCount", filteredRules.size)
      put("estimatedMonthlyCommittedExpense", totalMonthlyExpense)
      put("estimatedMonthlyCommittedIncome", totalMonthlyIncome)
      put("commitments", rulesArray)
    }
  }

  // -------------------------------------------------------------
  // 6. searchTransactions
  // -------------------------------------------------------------
  private suspend fun executeSearchTransactions(args: Map<String, Any?>): JSONObject {
    val query = (args["query"] as? String)?.trim()?.lowercase(Locale.US)
    val startDateStr = (args["startDate"] as? String)?.trim()
    val endDateStr = (args["endDate"] as? String)?.trim()
    val categoryStr = (args["category"] as? String)?.trim()?.uppercase(Locale.US)
    val typeStr = (args["type"] as? String)?.trim()?.uppercase(Locale.US) ?: "ALL"
    val rawLimit = when (val l = args["limit"]) {
      is Number -> l.toInt()
      is String -> l.toIntOrNull() ?: DEFAULT_TRANSACTION_SEARCH_LIMIT
      else -> DEFAULT_TRANSACTION_SEARCH_LIMIT
    }
    // Hard cap maximum results to protect payload and device memory
    val boundedLimit = rawLimit.coerceIn(1, MAX_TRANSACTION_SEARCH_LIMIT)

    val startTimestamp = if (!startDateStr.isNullOrBlank()) {
      parseDateBoundary(startDateStr, isStart = true)
    } else 0L

    val endTimestamp = if (!endDateStr.isNullOrBlank()) {
      parseDateBoundary(endDateStr, isStart = false)
    } else Long.MAX_VALUE

    if (startTimestamp > endTimestamp) {
      throw IllegalArgumentException("Start date '$startDateStr' cannot be after end date '$endDateStr'.")
    }

    val targetCategory = if (!categoryStr.isNullOrBlank()) parseCategory(categoryStr) else null

    val transactions = getTransactions()
    val matches = transactions.filter { tx ->
      val matchesTime = tx.timestamp in startTimestamp..endTimestamp
      val matchesType = when (typeStr) {
        "EXPENSE" -> tx.type == TransactionType.EXPENSE
        "INCOME" -> tx.type == TransactionType.INCOME
        "ALL" -> true
        else -> throw IllegalArgumentException("Invalid type '$typeStr'. Expected 'EXPENSE', 'INCOME', or 'ALL'.")
      }
      val matchesCategory = targetCategory == null || tx.category == targetCategory
      val matchesQuery = query.isNullOrBlank() || tx.title.lowercase(Locale.US).contains(query)

      matchesTime && matchesType && matchesCategory && matchesQuery
    }.sortedByDescending { it.timestamp }

    val currency = getCurrencySymbol()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val resultsArray = JSONArray()

    matches.take(boundedLimit).forEach { tx ->
      resultsArray.put(JSONObject().apply {
        put("id", tx.id)
        put("title", tx.title)
        put("amount", tx.amount)
        put("type", tx.type.name)
        put("category", tx.category.name)
        put("paymentMethod", tx.paymentMethod.name)
        put("date", sdf.format(Date(tx.timestamp)))
        // Note is deliberately omitted to prevent leaking unnecessary private user notes
      })
    }

    return JSONObject().apply {
      put("status", "SUCCESS")
      put("totalMatches", matches.size)
      put("returnedCount", resultsArray.length())
      put("limitApplied", boundedLimit)
      put("currencySymbol", currency)
      put("transactions", resultsArray)
    }
  }

  // -------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------
  private fun parseDateBoundary(dateStr: String, isStart: Boolean): Long {
    val trimmed = dateStr.trim()
    return when {
      trimmed.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) -> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val date = try {
          sdf.parse(trimmed) ?: throw IllegalArgumentException("Failed to parse date '$dateStr'.")
        } catch (e: ParseException) {
          throw IllegalArgumentException("Malformed date '$dateStr'. Expected valid 'YYYY-MM-DD'.")
        }
        val cal = Calendar.getInstance().apply { time = date }
        if (isStart) {
          DateUtils.getStartOfDay(cal.timeInMillis)
        } else {
          DateUtils.getEndOfDay(cal.timeInMillis)
        }
      }
      trimmed.matches(Regex("""^\d{4}-\d{2}$""")) -> {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US).apply { isLenient = false }
        val date = try {
          sdf.parse(trimmed) ?: throw IllegalArgumentException("Failed to parse month '$dateStr'.")
        } catch (e: ParseException) {
          throw IllegalArgumentException("Malformed month '$dateStr'. Expected valid 'YYYY-MM'.")
        }
        val cal = Calendar.getInstance().apply { time = date }
        if (isStart) {
          DateUtils.getStartOfMonth(cal)
        } else {
          DateUtils.getEndOfMonth(cal)
        }
      }
      trimmed.matches(Regex("""^\d{4}$""")) -> {
        val year = trimmed.toInt()
        val cal = Calendar.getInstance().apply {
          clear()
          set(Calendar.YEAR, year)
        }
        if (isStart) {
          cal.set(Calendar.MONTH, Calendar.JANUARY)
          cal.set(Calendar.DAY_OF_MONTH, 1)
          DateUtils.getStartOfDay(cal.timeInMillis)
        } else {
          cal.set(Calendar.MONTH, Calendar.DECEMBER)
          cal.set(Calendar.DAY_OF_MONTH, 31)
          DateUtils.getEndOfDay(cal.timeInMillis)
        }
      }
      else -> throw IllegalArgumentException("Unsupported date format '$dateStr'. Expected 'YYYY-MM-DD', 'YYYY-MM', or 'YYYY'.")
    }
  }

  private fun parseCategory(name: String): TransactionCategory {
    val canonical = name.trim().uppercase(Locale.US)
    return try {
      TransactionCategory.valueOf(canonical)
    } catch (_: Exception) {
      TransactionCategory.entries.find {
        it.displayName.equals(name.trim(), ignoreCase = true)
      } ?: throw IllegalArgumentException("Unknown category '$name'. Available categories: ${TransactionCategory.entries.joinToString { it.name }}")
    }
  }
}
