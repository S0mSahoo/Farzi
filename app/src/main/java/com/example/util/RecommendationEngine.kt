package com.example.util

import com.example.data.model.BudgetModel
import com.example.data.model.FinancialRecommendation
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.RecommendationSeverity
import com.example.data.model.ScheduledRecurringOccurrence
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import java.util.Calendar

object RecommendationEngine {

  /**
   * Generates intelligent, deterministic recommendations strictly derived from actual data.
   * Returns empty list if data is insufficient rather than generating fake or generic advice.
   */
  fun evaluate(
    currentMonthSummary: MonthlyFinancialSummary,
    historicalTransactions: List<TransactionItem>,
    currentBudget: BudgetModel?,
    recurringOccurrences: List<ScheduledRecurringOccurrence>,
    selectedCalendar: Calendar
  ): List<FinancialRecommendation> {
    val recommendations = mutableListOf<FinancialRecommendation>()
    val cal = selectedCalendar.clone() as Calendar
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val now = Calendar.getInstance()
    val isCurrentMonth = DateUtils.getMonthKey(cal) == DateUtils.getMonthKey(now)
    val currentDay = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else daysInMonth
    val daysLeft = (daysInMonth - currentDay).coerceAtLeast(0)

    val startOfMonth = DateUtils.getStartOfMonth(cal)
    val endOfMonth = DateUtils.getEndOfMonth(cal)
    val monthTransactions = historicalTransactions.filter { it.timestamp in startOfMonth..endOfMonth }
    val monthExpenses = monthTransactions.filter { it.type == TransactionType.EXPENSE }

    // 1. Category Budget Exceeded & Threshold Warnings
    if (currentBudget != null && currentBudget.categoryBudgets.isNotEmpty()) {
      val expenseByCategory = monthExpenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }

      for ((category, limit) in currentBudget.categoryBudgets) {
        if (limit <= 0) continue
        val spent = expenseByCategory[category] ?: 0.0

        if (spent > limit) {
          val overspend = spent - limit
          recommendations.add(
            FinancialRecommendation(
              id = "budget_exceeded_${category.name}",
              title = "${category.displayName} Budget Exceeded",
              message = "${category.displayName} is ${IndianCurrencyFormatter.format(overspend)} over this month's budget limit of ${IndianCurrencyFormatter.format(limit)}.",
              severity = RecommendationSeverity.ALERT,
              category = category,
              actionLabel = "Adjust Budget"
            )
          )
        } else if (isCurrentMonth && daysLeft > 3) {
          val usagePct = (spent / limit) * 100.0
          if (usagePct >= 80.0) {
            recommendations.add(
              FinancialRecommendation(
                id = "budget_warning_${category.name}",
                title = "${category.displayName} Nearing Limit",
                message = "You've used ${usagePct.toInt()}% of your ${category.displayName} budget with $daysLeft days left in the month.",
                severity = RecommendationSeverity.WARNING,
                category = category,
                actionLabel = "View Transactions"
              )
            )
          }
        }
      }
    }

    // 2. Total Monthly Budget Overall Check
    if (currentBudget != null && currentBudget.totalBudget > 0) {
      val totalSpent = currentMonthSummary.totalExpense
      val totalLimit = currentBudget.totalBudget
      if (totalSpent > totalLimit) {
        val diff = totalSpent - totalLimit
        recommendations.add(
          FinancialRecommendation(
            id = "total_budget_exceeded",
            title = "Monthly Budget Exceeded",
            message = "Total monthly expenses exceed your overall budget by ${IndianCurrencyFormatter.format(diff)}.",
            severity = RecommendationSeverity.ALERT,
            actionLabel = "Review Budget"
          )
        )
      } else if (isCurrentMonth && daysLeft > 0) {
        val overallPct = (totalSpent / totalLimit) * 100.0
        if (overallPct >= 85.0) {
          recommendations.add(
            FinancialRecommendation(
              id = "total_budget_warning",
              title = "Overall Budget Alert",
              message = "You have consumed ${overallPct.toInt()}% of your total budget (${IndianCurrencyFormatter.format(totalSpent)} / ${IndianCurrencyFormatter.format(totalLimit)}) with $daysLeft days left.",
              severity = RecommendationSeverity.WARNING,
              actionLabel = "Review Expenses"
            )
          )
        }
      }
    }

    // 3. Upcoming Recurring Commitments
    val upcomingOccurrences = recurringOccurrences.filter { !it.isPaid && it.daysDiff in 0..14 }
    val totalUpcoming = upcomingOccurrences.sumOf { it.amount }
    if (totalUpcoming > 0 && currentMonthSummary.totalIncome > 0) {
      val recurringShare = (totalUpcoming / currentMonthSummary.totalIncome) * 100.0
      if (recurringShare >= 25.0) {
        recommendations.add(
          FinancialRecommendation(
            id = "recurring_share_high",
            title = "Scheduled Payments Commitment",
            message = "${IndianCurrencyFormatter.format(totalUpcoming)} in scheduled payments are coming up soon, representing ${recurringShare.toInt()}% of this month's recorded income.",
            severity = RecommendationSeverity.INFO,
            actionLabel = "View Recurring"
          )
        )
      }
    }

    // 4. Overdue Payments Warning
    val overdueCount = recurringOccurrences.count { it.status == com.example.data.model.OccurrenceStatus.OVERDUE }
    if (overdueCount > 0) {
      val overdueTotal = recurringOccurrences.filter { it.status == com.example.data.model.OccurrenceStatus.OVERDUE }.sumOf { it.amount }
      recommendations.add(
        FinancialRecommendation(
          id = "overdue_recurring_warning",
          title = "$overdueCount Overdue Payment${if (overdueCount > 1) "s" else ""}",
          message = "You have $overdueCount scheduled payment${if (overdueCount > 1) "s" else ""} totaling ${IndianCurrencyFormatter.format(overdueTotal)} that passed their scheduled date.",
          severity = RecommendationSeverity.ALERT,
          actionLabel = "Mark as Paid"
        )
      )
    }

    // 5. Positive Savings Observation (if savings rate is healthy > 25% and at least 3 transactions recorded)
    if (currentMonthSummary.transactionCount >= 3 && currentMonthSummary.savingsRate >= 30.0 && currentMonthSummary.savings > 5000.0) {
      recommendations.add(
        FinancialRecommendation(
          id = "healthy_savings_rate",
          title = "Healthy Savings Rate",
          message = "Great discipline! You have saved ${currentMonthSummary.savingsRate.toInt()}% (${IndianCurrencyFormatter.format(currentMonthSummary.savings)}) of your income this month.",
          severity = RecommendationSeverity.SUCCESS
        )
      )
    }

    return recommendations.take(3)
  }
}
