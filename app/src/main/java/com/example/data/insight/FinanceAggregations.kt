package com.example.data.insight

import com.example.data.model.BudgetModel
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpendingPoint
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.YearlyFinancialSummary
import com.example.ui.components.DateUtils
import java.util.Calendar

/**
 * Single-pass period aggregations shared by dashboard, budget and analytics flows.
 * These functions are pure apart from locale/time-zone aware calendar formatting.
 */
data class MonthlyFinanceMetrics(
  val monthKey: String,
  val monthLabel: String,
  val totalIncome: Double,
  val totalExpense: Double,
  val transactionCount: Int,
  val categorySpending: List<CategorySpending>,
  val dailyTrend: List<DailySpendingPoint>
)

object FinanceAggregations {
  /**
   * Calculates every selected-month dashboard projection in one transaction pass.
   * The returned metrics are shared by dashboard and budget flows.
   */
  fun monthlyMetrics(
    transactions: List<TransactionItem>,
    selectedCalendar: Calendar
  ): MonthlyFinanceMetrics {
    val calendar = selectedCalendar.clone() as Calendar
    val monthKey = DateUtils.getMonthKey(calendar)
    val start = DateUtils.getStartOfMonth(calendar)
    val end = DateUtils.getEndOfMonth(calendar)
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val categoryAmounts = DoubleArray(TransactionCategory.values().size)
    val categoryCounts = IntArray(TransactionCategory.values().size)
    val dailyIncome = DoubleArray(daysInMonth)
    val dailyExpense = DoubleArray(daysInMonth)
    val transactionCalendar = calendar.clone() as Calendar
    var income = 0.0
    var expense = 0.0
    var transactionCount = 0

    for (transaction in transactions) {
      if (transaction.timestamp !in start..end) continue
      transactionCount++
      transactionCalendar.timeInMillis = transaction.timestamp
      val dayIndex = transactionCalendar.get(Calendar.DAY_OF_MONTH) - 1
      when (transaction.type) {
        TransactionType.INCOME -> {
          income += transaction.amount
          dailyIncome[dayIndex] += transaction.amount
        }
        TransactionType.EXPENSE -> {
          expense += transaction.amount
          dailyExpense[dayIndex] += transaction.amount
          val categoryIndex = transaction.category.ordinal
          categoryAmounts[categoryIndex] += transaction.amount
          categoryCounts[categoryIndex]++
        }
      }
    }

    val categories = if (expense <= 0.0) {
      emptyList()
    } else {
      TransactionCategory.values().mapIndexedNotNull { index, category ->
        val amount = categoryAmounts[index]
        if (amount <= 0.0) null else CategorySpending(
          category = category,
          amount = amount,
          percentage = (amount / expense).toFloat(),
          count = categoryCounts[index],
          color = category.color
        )
      }.sortedByDescending { it.amount }
    }

    val dailyTrend = (1..daysInMonth).map { day ->
      val dayCalendar = calendar.clone() as Calendar
      dayCalendar.set(Calendar.YEAR, year)
      dayCalendar.set(Calendar.MONTH, month)
      dayCalendar.set(Calendar.DAY_OF_MONTH, day)
      dayCalendar.set(Calendar.HOUR_OF_DAY, 12)
      dayCalendar.set(Calendar.MINUTE, 0)
      dayCalendar.set(Calendar.SECOND, 0)
      dayCalendar.set(Calendar.MILLISECOND, 0)
      val dayIncome = dailyIncome[day - 1]
      val dayExpense = dailyExpense[day - 1]
      DailySpendingPoint(
        dayOfMonth = day,
        dateKey = DateUtils.getDayKey(dayCalendar),
        dayLabel = day.toString(),
        expense = dayExpense,
        income = dayIncome,
        net = dayIncome - dayExpense
      )
    }

    return MonthlyFinanceMetrics(
      monthKey = monthKey,
      monthLabel = DateUtils.getMonthLabel(calendar),
      totalIncome = income,
      totalExpense = expense,
      transactionCount = transactionCount,
      categorySpending = categories,
      dailyTrend = dailyTrend
    )
  }

  fun monthlySummary(
    metrics: MonthlyFinanceMetrics,
    budgets: List<BudgetModel>
  ): MonthlyFinancialSummary {
    val savings = metrics.totalIncome - metrics.totalExpense
    val budgetLimit = budgets.firstOrNull { it.monthKey == metrics.monthKey }?.totalBudget ?: 0.0
    return MonthlyFinancialSummary(
      monthKey = metrics.monthKey,
      monthLabel = metrics.monthLabel,
      totalIncome = metrics.totalIncome,
      totalExpense = metrics.totalExpense,
      savings = savings,
      savingsRate = if (metrics.totalIncome > 0.0) savings / metrics.totalIncome * 100.0 else 0.0,
      budgetLimit = budgetLimit,
      budgetUsed = metrics.totalExpense,
      budgetRemaining = if (budgetLimit > 0.0) budgetLimit - metrics.totalExpense else 0.0,
      budgetUsagePercent = if (budgetLimit > 0.0) metrics.totalExpense / budgetLimit * 100.0 else 0.0,
      transactionCount = metrics.transactionCount
    )
  }

  fun monthlySummary(
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    selectedCalendar: Calendar
  ): MonthlyFinancialSummary = monthlySummary(monthlyMetrics(transactions, selectedCalendar), budgets)

  fun yearlySummary(
    transactions: List<TransactionItem>,
    selectedYear: Int
  ): YearlyFinancialSummary {
    val start = DateUtils.getStartOfYear(selectedYear)
    val end = DateUtils.getEndOfYear(selectedYear)
    val monthlyIncome = DoubleArray(12)
    val monthlyExpense = DoubleArray(12)
    val monthlyCounts = IntArray(12)
    val transactionCalendar = Calendar.getInstance()
    var totalIncome = 0.0
    var totalExpense = 0.0

    for (transaction in transactions) {
      if (transaction.timestamp !in start..end) continue
      transactionCalendar.timeInMillis = transaction.timestamp
      val month = transactionCalendar.get(Calendar.MONTH)
      monthlyCounts[month]++
      when (transaction.type) {
        TransactionType.INCOME -> {
          monthlyIncome[month] += transaction.amount
          totalIncome += transaction.amount
        }
        TransactionType.EXPENSE -> {
          monthlyExpense[month] += transaction.amount
          totalExpense += transaction.amount
        }
      }
    }

    val monthlyBreakdown = (Calendar.JANUARY..Calendar.DECEMBER).map { month ->
      val monthCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, selectedYear)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val income = monthlyIncome[month]
      val expense = monthlyExpense[month]
      MonthlyFinancialSummary(
        monthKey = DateUtils.getMonthKey(monthCalendar),
        monthLabel = DateUtils.getMonthLabel(monthCalendar),
        totalIncome = income,
        totalExpense = expense,
        savings = income - expense,
        transactionCount = monthlyCounts[month]
      )
    }

    val savings = totalIncome - totalExpense
    return YearlyFinancialSummary(
      year = selectedYear,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      savings = savings,
      savingsRate = if (totalIncome > 0.0) savings / totalIncome * 100.0 else 0.0,
      monthlyBreakdown = monthlyBreakdown
    )
  }

  fun categorySpending(
    transactions: List<TransactionItem>,
    selectedCalendar: Calendar
  ): List<CategorySpending> = monthlyMetrics(transactions, selectedCalendar).categorySpending

  fun dailyTrend(
    transactions: List<TransactionItem>,
    selectedCalendar: Calendar
  ): List<DailySpendingPoint> = monthlyMetrics(transactions, selectedCalendar).dailyTrend

}
