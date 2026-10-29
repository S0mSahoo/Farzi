package com.example.data.insight

import com.example.data.model.BudgetModel
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
import java.util.Calendar
import java.util.Locale

object FinanceInsightEngine {

  fun generateMonthlyInsights(
    monthKey: String,
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>
  ): List<FinanceInsight> {
    val insights = mutableListOf<FinanceInsight>()

    val monthTransactions = transactions.filter {
      DateUtils.getMonthKey(it.timestamp) == monthKey
    }

    if (monthTransactions.isEmpty() && budgets.none { it.monthKey == monthKey } && recurringRules.none { it.isActive }) {
      insights.add(
        FinanceInsight(
          id = "empty_month_$monthKey",
          type = InsightType.CASH_FLOW,
          title = "No Data Recorded",
          description = "No financial transactions or budgets recorded for $monthKey.",
          severity = InsightSeverity.NEUTRAL,
          monthKey = monthKey
        )
      )
      return insights
    }

    val totalIncome = monthTransactions
      .filter { it.type == TransactionType.INCOME }
      .sumOf { it.amount }

    val totalExpense = monthTransactions
      .filter { it.type == TransactionType.EXPENSE }
      .sumOf { it.amount }

    // 1. Cash Flow & Savings Rate
    generateCashFlowInsight(monthKey, totalIncome, totalExpense)?.let { insights.add(it) }

    // 2. Period Comparison (vs Previous Month)
    val prevMonthKey = getPreviousMonthKey(monthKey)
    if (prevMonthKey != null) {
      val prevMonthTransactions = transactions.filter {
        DateUtils.getMonthKey(it.timestamp) == prevMonthKey
      }
      val prevTotalExpense = prevMonthTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

      generatePeriodComparisonInsight(monthKey, totalExpense, prevTotalExpense, prevMonthKey)?.let {
        insights.add(it)
      }
    }

    // 3. Top Spending Categories & Concentration (>30%)
    generateTopCategoryInsight(monthKey, monthTransactions, totalExpense)?.let { insights.add(it) }

    // 4. Budget Utilization
    val monthBudget = budgets.find { it.monthKey == monthKey }
    if (monthBudget != null && monthBudget.totalBudget > 0.0) {
      generateBudgetInsight(monthKey, monthBudget, totalExpense)?.let { insights.add(it) }
    }

    // 5. Active Recurring Commitments
    generateRecurringInsight(monthKey, recurringRules)?.let { insights.add(it) }

    return insights
  }

  fun generateYearlyInsights(
    year: Int,
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>
  ): List<FinanceInsight> {
    val insights = mutableListOf<FinanceInsight>()

    val yearTransactions = transactions.filter {
      val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
      cal.get(Calendar.YEAR) == year
    }

    if (yearTransactions.isEmpty()) {
      insights.add(
        FinanceInsight(
          id = "empty_year_$year",
          type = InsightType.CASH_FLOW,
          title = "No Data Recorded",
          description = "No financial records found for year $year.",
          severity = InsightSeverity.NEUTRAL
        )
      )
      return insights
    }

    val totalIncome = yearTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = yearTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    generateCashFlowInsight(year.toString(), totalIncome, totalExpense)?.let { insights.add(it) }

    // Year-over-Year comparison
    val prevYear = year - 1
    val prevYearTransactions = transactions.filter {
      val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
      cal.get(Calendar.YEAR) == prevYear
    }
    if (prevYearTransactions.isNotEmpty()) {
      val prevYearExpense = prevYearTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
      generatePeriodComparisonInsight(year.toString(), totalExpense, prevYearExpense, prevYear.toString())?.let {
        insights.add(it)
      }
    }

    generateTopCategoryInsight(year.toString(), yearTransactions, totalExpense)?.let { insights.add(it) }

    return insights
  }

  private fun generateCashFlowInsight(
    periodLabel: String,
    totalIncome: Double,
    totalExpense: Double
  ): FinanceInsight? {
    val netCashFlow = totalIncome - totalExpense

    return if (totalIncome > 0.0) {
      val savingsRate = (netCashFlow / totalIncome) * 100.0
      val severity = when {
        savingsRate >= 20.0 -> InsightSeverity.SUCCESS
        savingsRate >= 0.0 -> InsightSeverity.INFO
        else -> InsightSeverity.WARNING
      }

      val title = if (netCashFlow >= 0.0) "Positive Cash Flow" else "Net Deficit"
      val formattedRate = String.format(Locale.US, "%.1f", savingsRate)
      val description = if (netCashFlow >= 0.0) {
        "Savings rate is $formattedRate% with net savings of ${formatAmount(netCashFlow)}."
      } else {
        "Spending exceeds income by ${formatAmount(-netCashFlow)} (savings rate: $formattedRate%)."
      }

      FinanceInsight(
        id = "cash_flow_$periodLabel",
        type = InsightType.CASH_FLOW,
        title = title,
        description = description,
        severity = severity,
        amount = netCashFlow,
        percentage = savingsRate
      )
    } else if (totalExpense > 0.0) {
      FinanceInsight(
        id = "cash_flow_$periodLabel",
        type = InsightType.CASH_FLOW,
        title = "No Income Recorded",
        description = "Total spending for $periodLabel is ${formatAmount(totalExpense)} with no recorded income.",
        severity = InsightSeverity.INFO,
        amount = -totalExpense,
        percentage = null
      )
    } else {
      null
    }
  }

  private fun generatePeriodComparisonInsight(
    currentPeriod: String,
    currentExpense: Double,
    prevExpense: Double,
    prevPeriod: String
  ): FinanceInsight? {
    if (prevExpense > 0.0) {
      val diff = currentExpense - prevExpense
      val percentage = (diff / prevExpense) * 100.0
      val formattedPct = String.format(Locale.US, "%.1f", Math.abs(percentage))

      val (severity, title, description) = when {
        percentage > 15.0 -> Triple(
          InsightSeverity.WARNING,
          "Spending Increased",
          "Spending is up $formattedPct% compared to $prevPeriod (+${formatAmount(diff)})."
        )
        percentage > 0.0 -> Triple(
          InsightSeverity.INFO,
          "Slight Spending Increase",
          "Spending increased by $formattedPct% compared to $prevPeriod."
        )
        percentage < 0.0 -> Triple(
          InsightSeverity.SUCCESS,
          "Spending Decreased",
          "Spending dropped by $formattedPct% compared to $prevPeriod (-${formatAmount(-diff)})."
        )
        else -> Triple(
          InsightSeverity.INFO,
          "Stable Spending",
          "Spending is identical to $prevPeriod."
        )
      }

      return FinanceInsight(
        id = "period_comp_$currentPeriod",
        type = InsightType.PERIOD_COMPARISON,
        title = title,
        description = description,
        severity = severity,
        amount = diff,
        percentage = percentage
      )
    } else if (currentExpense > 0.0) {
      return FinanceInsight(
        id = "period_comp_$currentPeriod",
        type = InsightType.PERIOD_COMPARISON,
        title = "New Spending Period",
        description = "Total spending for $currentPeriod is ${formatAmount(currentExpense)} (no recorded spending in $prevPeriod).",
        severity = InsightSeverity.INFO,
        amount = currentExpense,
        percentage = null
      )
    }
    return null
  }

  private fun generateTopCategoryInsight(
    periodLabel: String,
    transactions: List<TransactionItem>,
    totalExpense: Double
  ): FinanceInsight? {
    if (totalExpense <= 0.0) return null

    val categorySums = transactions
      .filter { it.type == TransactionType.EXPENSE }
      .groupBy { it.category }
      .mapValues { entry -> entry.value.sumOf { it.amount } }

    val topEntry = categorySums.maxByOrNull { it.value } ?: return null
    val topCategory = topEntry.key
    val topAmount = topEntry.value
    val sharePct = (topAmount / totalExpense) * 100.0
    val formattedShare = String.format(Locale.US, "%.1f", sharePct)

    val (severity, title) = if (sharePct >= 30.0) {
      Pair(InsightSeverity.WARNING, "High Category Concentration")
    } else {
      Pair(InsightSeverity.INFO, "Top Expense Category")
    }

    return FinanceInsight(
      id = "top_cat_$periodLabel",
      type = InsightType.TOP_CATEGORY,
      title = title,
      description = "${topCategory.displayName} accounts for $formattedShare% of spending (${formatAmount(topAmount)}).",
      severity = severity,
      amount = topAmount,
      percentage = sharePct,
      category = topCategory
    )
  }

  private fun generateBudgetInsight(
    monthKey: String,
    budget: BudgetModel,
    totalExpense: Double
  ): FinanceInsight? {
    if (budget.totalBudget <= 0.0) return null

    val usagePct = (totalExpense / budget.totalBudget) * 100.0
    val formattedUsage = String.format(Locale.US, "%.1f", usagePct)

    val (severity, title) = when {
      usagePct >= 100.0 -> Pair(InsightSeverity.CRITICAL, "Budget Exceeded")
      usagePct >= 80.0 -> Pair(InsightSeverity.WARNING, "Approaching Budget Limit")
      else -> Pair(InsightSeverity.SUCCESS, "Within Budget")
    }

    val description = when {
      usagePct >= 100.0 -> "Budget exceeded! Spent ${formatAmount(totalExpense)} of ${formatAmount(budget.totalBudget)} budget ($formattedUsage%)."
      else -> "Used $formattedUsage% of ${formatAmount(budget.totalBudget)} monthly budget (${formatAmount(totalExpense)} spent)."
    }

    return FinanceInsight(
      id = "budget_status_$monthKey",
      type = InsightType.BUDGET_STATUS,
      title = title,
      description = description,
      severity = severity,
      amount = totalExpense,
      percentage = usagePct,
      monthKey = monthKey
    )
  }

  private fun generateRecurringInsight(
    monthKey: String,
    recurringRules: List<RecurringRule>
  ): FinanceInsight? {
    val activeRules = recurringRules.filter { it.isActive }
    if (activeRules.isEmpty()) return null

    val totalMonthlyCommitted = activeRules.sumOf { rule ->
      when (rule.interval) {
        RecurrenceInterval.DAILY -> rule.amount * 30.0
        RecurrenceInterval.WEEKLY -> rule.amount * 4.33
        RecurrenceInterval.MONTHLY -> rule.amount
        RecurrenceInterval.YEARLY -> rule.amount / 12.0
      }
    }

    return FinanceInsight(
      id = "recurring_$monthKey",
      type = InsightType.RECURRING_COMMITMENTS,
      title = "Active Recurring Commitments",
      description = "${activeRules.size} active recurring rules (~${formatAmount(totalMonthlyCommitted)}/month committed).",
      severity = InsightSeverity.INFO,
      amount = totalMonthlyCommitted,
      percentage = null
    )
  }

  fun parseMonthKey(monthKey: String): Pair<Int, Int>? {
    val parts = monthKey.split("-")
    if (parts.size != 2) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (m !in 1..12) return null
    return Pair(y, m)
  }

  fun getPreviousMonthKey(monthKey: String): String? {
    val (y, m) = parseMonthKey(monthKey) ?: return null
    return if (m == 1) {
      String.format(Locale.US, "%04d-12", y - 1)
    } else {
      String.format(Locale.US, "%04d-%02d", y, m - 1)
    }
  }

  private fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%.2f", amount)
  }
}
