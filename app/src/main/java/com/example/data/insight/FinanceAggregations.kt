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
object FinanceAggregations {
  fun monthlySummary(
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    selectedCalendar: Calendar
  ): MonthlyFinancialSummary {
    val calendar = selectedCalendar.clone() as Calendar
    val monthKey = DateUtils.getMonthKey(calendar)
    val start = DateUtils.getStartOfMonth(calendar)
    val end = DateUtils.getEndOfMonth(calendar)
    var income = 0.0
    var expense = 0.0
    var count = 0

    for (transaction in transactions) {
      if (transaction.timestamp !in start..end) continue
      count++
      when (transaction.type) {
        TransactionType.INCOME -> income += transaction.amount
        TransactionType.EXPENSE -> expense += transaction.amount
      }
    }

    val savings = income - expense
    val budgetLimit = budgets.firstOrNull { it.monthKey == monthKey }?.totalBudget ?: 0.0
    return MonthlyFinancialSummary(
      monthKey = monthKey,
      monthLabel = DateUtils.getMonthLabel(calendar),
      totalIncome = income,
      totalExpense = expense,
      savings = savings,
      savingsRate = if (income > 0.0) savings / income * 100.0 else 0.0,
      budgetLimit = budgetLimit,
      budgetUsed = expense,
      budgetRemaining = if (budgetLimit > 0.0) budgetLimit - expense else 0.0,
      budgetUsagePercent = if (budgetLimit > 0.0) expense / budgetLimit * 100.0 else 0.0,
      transactionCount = count
    )
  }

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
  ): List<CategorySpending> {
    val start = DateUtils.getStartOfMonth(selectedCalendar)
    val end = DateUtils.getEndOfMonth(selectedCalendar)
    val amounts = DoubleArray(TransactionCategory.values().size)
    val counts = IntArray(TransactionCategory.values().size)
    var totalExpense = 0.0

    for (transaction in transactions) {
      if (transaction.timestamp !in start..end || transaction.type != TransactionType.EXPENSE) continue
      val index = transaction.category.ordinal
      amounts[index] += transaction.amount
      counts[index]++
      totalExpense += transaction.amount
    }
    if (totalExpense <= 0.0) return emptyList()

    return TransactionCategory.values().mapIndexedNotNull { index, category ->
      val amount = amounts[index]
      if (amount <= 0.0) null else CategorySpending(
        category = category,
        amount = amount,
        percentage = (amount / totalExpense).toFloat(),
        count = counts[index],
        color = category.color
      )
    }.sortedByDescending { it.amount }
  }

  fun dailyTrend(
    transactions: List<TransactionItem>,
    selectedCalendar: Calendar
  ): List<DailySpendingPoint> {
    val year = selectedCalendar.get(Calendar.YEAR)
    val month = selectedCalendar.get(Calendar.MONTH)
    val daysInMonth = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val start = DateUtils.getStartOfMonth(selectedCalendar)
    val end = DateUtils.getEndOfMonth(selectedCalendar)
    val incomes = DoubleArray(daysInMonth)
    val expenses = DoubleArray(daysInMonth)
    val transactionCalendar = Calendar.getInstance()

    for (transaction in transactions) {
      if (transaction.timestamp !in start..end) continue
      transactionCalendar.timeInMillis = transaction.timestamp
      val index = transactionCalendar.get(Calendar.DAY_OF_MONTH) - 1
      when (transaction.type) {
        TransactionType.INCOME -> incomes[index] += transaction.amount
        TransactionType.EXPENSE -> expenses[index] += transaction.amount
      }
    }

    return (1..daysInMonth).map { day ->
      val dayCalendar = Calendar.getInstance().apply {
        set(year, month, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val income = incomes[day - 1]
      val expense = expenses[day - 1]
      DailySpendingPoint(
        dayOfMonth = day,
        dateKey = DateUtils.getDayKey(dayCalendar),
        dayLabel = day.toString(),
        expense = expense,
        income = income,
        net = income - expense
      )
    }
  }
}
