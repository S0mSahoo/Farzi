package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryAnalytics
import com.example.data.model.DailySpendingTrend
import com.example.data.model.DayOfWeekBreakdown
import com.example.data.model.MonthlySalarySettings
import com.example.data.model.PaymentMethod
import com.example.data.model.TimeRangeFilter
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FinanceUiState(
  val allTransactions: List<TransactionItem> = emptyList(),
  val filteredTransactions: List<TransactionItem> = emptyList(),
  val timeRange: TimeRangeFilter = TimeRangeFilter.THIS_MONTH,
  val selectedType: TransactionType? = null,
  val selectedCategory: TransactionCategory? = null,
  val searchQuery: String = "",
  val salarySettings: MonthlySalarySettings = MonthlySalarySettings(),
  
  // Analytics
  val totalIncome: Double = 0.0,
  val totalExpense: Double = 0.0,
  val netSavings: Double = 0.0,
  val savingsRate: Float = 0f,
  val categoryAnalytics: List<CategoryAnalytics> = emptyList(),
  val dailyTrends: List<DailySpendingTrend> = emptyList(),
  val dayOfWeekBreakdown: List<DayOfWeekBreakdown> = emptyList(),
  val budgetSpent: Double = 0.0,
  val budgetProgress: Float = 0f,
  val remainingBudget: Double = 0.0,
  val daysRemainingInMonth: Int = 1,
  val dailySafeSpend: Double = 0.0,
  val topSpendingDay: String = "N/A",
  val topCategory: CategoryAnalytics? = null,
  val avgDailyExpense: Double = 0.0,

  // Navigation & Modals
  val currentTab: Int = 0, // 0 = Drafts, 1 = Analytics, 2 = Salary/Budget
  val isAddDraftSheetOpen: Boolean = false,
  val editingTransaction: TransactionItem? = null,
  val isSalaryModalOpen: Boolean = false,
  val selectedDetailItem: TransactionItem? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FinanceRepository(application)

  private val _timeRange = MutableStateFlow(TimeRangeFilter.THIS_MONTH)
  val timeRange: StateFlow<TimeRangeFilter> = _timeRange.asStateFlow()

  private val _selectedType = MutableStateFlow<TransactionType?>(null)
  val selectedType: StateFlow<TransactionType?> = _selectedType.asStateFlow()

  private val _selectedCategory = MutableStateFlow<TransactionCategory?>(null)
  val selectedCategory: StateFlow<TransactionCategory?> = _selectedCategory.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _salarySettings = MutableStateFlow(repository.getSalarySettings())
  val salarySettings: StateFlow<MonthlySalarySettings> = _salarySettings.asStateFlow()

  private val _currentTab = MutableStateFlow(0)
  val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

  private val _isAddDraftSheetOpen = MutableStateFlow(false)
  val isAddDraftSheetOpen: StateFlow<Boolean> = _isAddDraftSheetOpen.asStateFlow()

  private val _editingTransaction = MutableStateFlow<TransactionItem?>(null)
  val editingTransaction: StateFlow<TransactionItem?> = _editingTransaction.asStateFlow()

  private val _isSalaryModalOpen = MutableStateFlow(false)
  val isSalaryModalOpen: StateFlow<Boolean> = _isSalaryModalOpen.asStateFlow()

  private val _selectedDetailItem = MutableStateFlow<TransactionItem?>(null)
  val selectedDetailItem: StateFlow<TransactionItem?> = _selectedDetailItem.asStateFlow()

  val uiState: StateFlow<FinanceUiState> = combine(
    repository.allTransactions,
    _timeRange,
    _selectedType,
    _selectedCategory,
    _searchQuery,
    _salarySettings,
    _currentTab,
    _isAddDraftSheetOpen,
    _editingTransaction,
    _isSalaryModalOpen,
    _selectedDetailItem
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val allItems = args[0] as List<TransactionItem>
    val range = args[1] as TimeRangeFilter
    val type = args[2] as TransactionType?
    val category = args[3] as TransactionCategory?
    val query = args[4] as String
    val salary = args[5] as MonthlySalarySettings
    val tab = args[6] as Int
    val isAddOpen = args[7] as Boolean
    val editing = args[8] as TransactionItem?
    val isSalaryOpen = args[9] as Boolean
    val detailItem = args[10] as TransactionItem?

    computeUiState(
      allItems = allItems,
      range = range,
      type = type,
      category = category,
      query = query,
      salary = salary,
      tab = tab,
      isAddOpen = isAddOpen,
      editing = editing,
      isSalaryOpen = isSalaryOpen,
      detailItem = detailItem
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = FinanceUiState()
  )

  init {
    viewModelScope.launch {
      repository.checkAndSeedInitialData()
    }
  }

  private fun computeUiState(
    allItems: List<TransactionItem>,
    range: TimeRangeFilter,
    type: TransactionType?,
    category: TransactionCategory?,
    query: String,
    salary: MonthlySalarySettings,
    tab: Int,
    isAddOpen: Boolean,
    editing: TransactionItem?,
    isSalaryOpen: Boolean,
    detailItem: TransactionItem?
  ): FinanceUiState {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)
    val maxDaysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = now.get(Calendar.DAY_OF_MONTH)
    val daysRemaining = (maxDaysInMonth - currentDay + 1).coerceAtLeast(1)

    // Calculate time bounds
    val (startTime, endTime) = getTimeBounds(range, now)

    // Filtered by time range first for analytics
    val inRangeItems = allItems.filter { it.timestamp in startTime..endTime }

    // Analytics computation on in-range items
    var totalIncome = 0.0
    var totalExpense = 0.0
    val expenseCategoryMap = mutableMapOf<TransactionCategory, Double>()
    val expenseCategoryCountMap = mutableMapOf<TransactionCategory, Int>()
    val dayOfWeekSpendMap = mutableMapOf<Int, Double>() // Calendar.DAY_OF_WEEK -> spent
    val dayOfWeekCountMap = mutableMapOf<Int, Int>()

    inRangeItems.forEach { item ->
      when (item.type) {
        TransactionType.EXPENSE -> {
          totalExpense += item.amount
          expenseCategoryMap[item.category] = (expenseCategoryMap[item.category] ?: 0.0) + item.amount
          expenseCategoryCountMap[item.category] = (expenseCategoryCountMap[item.category] ?: 0) + 1

          val cal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
          val dow = cal.get(Calendar.DAY_OF_WEEK)
          dayOfWeekSpendMap[dow] = (dayOfWeekSpendMap[dow] ?: 0.0) + item.amount
          dayOfWeekCountMap[dow] = (dayOfWeekCountMap[dow] ?: 0) + 1
        }
        TransactionType.INCOME, TransactionType.SALARY -> {
          totalIncome += item.amount
        }
      }
    }

    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toFloat().coerceIn(0f, 100f) else 0f

    // Category breakdown list
    val categoryAnalyticsList = expenseCategoryMap.map { (cat, amount) ->
      val pct = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
      CategoryAnalytics(
        category = cat,
        totalAmount = amount,
        percentage = pct,
        count = expenseCategoryCountMap[cat] ?: 1,
        color = cat.color
      )
    }.sortedByDescending { it.totalAmount }

    // Daily spending trends (last 7 days or current month days)
    val dailyTrends = computeDailyTrends(inRangeItems, startTime, endTime, range)

    // Day of week breakdown
    val daysOfWeekNames = listOf(
      Calendar.SUNDAY to ("Sun" to "Sunday"),
      Calendar.MONDAY to ("Mon" to "Monday"),
      Calendar.TUESDAY to ("Tue" to "Tuesday"),
      Calendar.WEDNESDAY to ("Wed" to "Wednesday"),
      Calendar.THURSDAY to ("Thu" to "Thursday"),
      Calendar.FRIDAY to ("Fri" to "Friday"),
      Calendar.SATURDAY to ("Sat" to "Saturday")
    )
    val dayOfWeekList = daysOfWeekNames.map { (calDay, names) ->
      val spent = dayOfWeekSpendMap[calDay] ?: 0.0
      val count = (dayOfWeekCountMap[calDay] ?: 1).coerceAtLeast(1)
      val pct = if (totalExpense > 0) (spent / totalExpense).toFloat() else 0f
      DayOfWeekBreakdown(
        dayName = names.second,
        shortName = names.first,
        totalSpent = spent,
        averageSpent = spent / count,
        percentage = pct
      )
    }

    val topDay = dayOfWeekList.maxByOrNull { it.totalSpent }?.dayName ?: "N/A"
    val topCat = categoryAnalyticsList.firstOrNull()

    // Monthly Budget & Safe daily burn rate
    // Calculate expense for strictly THIS month for budget progress
    val thisMonthStart = Calendar.getInstance().apply {
      set(currentYear, currentMonth, 1, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val thisMonthEnd = Calendar.getInstance().apply {
      set(currentYear, currentMonth, maxDaysInMonth, 23, 59, 59)
      set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    val thisMonthExpenses = allItems
      .filter { it.timestamp in thisMonthStart..thisMonthEnd && it.type == TransactionType.EXPENSE }
      .sumOf { it.amount }

    val budgetGoal = salary.monthlyBudgetGoal
    val budgetProgress = if (budgetGoal > 0) (thisMonthExpenses / budgetGoal).toFloat() else 0f
    val remainingBudget = (budgetGoal - thisMonthExpenses).coerceAtLeast(0.0)
    val dailySafeSpend = remainingBudget / daysRemaining
    val avgDailyExpense = if (currentDay > 0) thisMonthExpenses / currentDay else 0.0

    // Filter list for display
    val filteredList = inRangeItems.filter { item ->
      val matchesType = type == null || (if (type == TransactionType.INCOME) (item.type == TransactionType.INCOME || item.type == TransactionType.SALARY) else item.type == type)
      val matchesCategory = category == null || item.category == category
      val matchesQuery = query.isBlank() ||
          item.title.contains(query, ignoreCase = true) ||
          item.note.contains(query, ignoreCase = true) ||
          item.category.displayName.contains(query, ignoreCase = true) ||
          item.paymentMethod.displayName.contains(query, ignoreCase = true)
      matchesType && matchesCategory && matchesQuery
    }

    return FinanceUiState(
      allTransactions = allItems,
      filteredTransactions = filteredList,
      timeRange = range,
      selectedType = type,
      selectedCategory = category,
      searchQuery = query,
      salarySettings = salary,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      netSavings = netSavings,
      savingsRate = savingsRate,
      categoryAnalytics = categoryAnalyticsList,
      dailyTrends = dailyTrends,
      dayOfWeekBreakdown = dayOfWeekList,
      budgetSpent = thisMonthExpenses,
      budgetProgress = budgetProgress,
      remainingBudget = remainingBudget,
      daysRemainingInMonth = daysRemaining,
      dailySafeSpend = dailySafeSpend,
      topSpendingDay = topDay,
      topCategory = topCat,
      avgDailyExpense = avgDailyExpense,
      currentTab = tab,
      isAddDraftSheetOpen = isAddOpen,
      editingTransaction = editing,
      isSalaryModalOpen = isSalaryOpen,
      selectedDetailItem = detailItem
    )
  }

  private fun getTimeBounds(range: TimeRangeFilter, now: Calendar): Pair<Long, Long> {
    val cal = now.clone() as Calendar
    return when (range) {
      TimeRangeFilter.THIS_MONTH -> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)
        start to endCal.timeInMillis
      }
      TimeRangeFilter.LAST_7_DAYS -> {
        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)
        start to endCal.timeInMillis
      }
      TimeRangeFilter.LAST_MONTH -> {
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)
        start to endCal.timeInMillis
      }
      TimeRangeFilter.THIS_YEAR -> {
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val endCal = now.clone() as Calendar
        endCal.set(Calendar.MONTH, Calendar.DECEMBER)
        endCal.set(Calendar.DAY_OF_MONTH, 31)
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)
        start to endCal.timeInMillis
      }
      TimeRangeFilter.ALL_TIME -> {
        0L to Long.MAX_VALUE
      }
    }
  }

  private fun computeDailyTrends(
    items: List<TransactionItem>,
    startTime: Long,
    endTime: Long,
    range: TimeRangeFilter
  ): List<DailySpendingTrend> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    
    // Group transactions by date key
    val dailyMap = mutableMapOf<String, Pair<Double, Double>>() // dateKey -> (expense, income)
    
    items.forEach { item ->
      val key = dateFormat.format(Date(item.timestamp))
      val (currExp, currInc) = dailyMap[key] ?: (0.0 to 0.0)
      if (item.type == TransactionType.EXPENSE) {
        dailyMap[key] = (currExp + item.amount) to currInc
      } else {
        dailyMap[key] = currExp to (currInc + item.amount)
      }
    }

    // Generate consecutive days for nice visual timeline
    val cal = Calendar.getInstance().apply { timeInMillis = startTime.coerceAtLeast(System.currentTimeMillis() - 30L * 86400000L) }
    val endCal = Calendar.getInstance().apply { timeInMillis = endTime.coerceAtMost(System.currentTimeMillis() + 86400000L) }
    val list = mutableListOf<DailySpendingTrend>()

    var safetyCount = 0
    while (cal.before(endCal) && safetyCount < 35) {
      safetyCount++
      val key = dateFormat.format(cal.time)
      val label = labelFormat.format(cal.time)
      val (exp, inc) = dailyMap[key] ?: (0.0 to 0.0)
      list.add(
        DailySpendingTrend(
          dateKey = key,
          dayLabel = label,
          expenseAmount = exp,
          incomeAmount = inc,
          netAmount = inc - exp
        )
      )
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    return if (list.size > 14 && range != TimeRangeFilter.LAST_7_DAYS) {
      // Sample or condense to last 14 days for optimal mobile chart rendering
      list.takeLast(14)
    } else {
      list
    }
  }

  // User Actions
  fun setTab(index: Int) {
    _currentTab.value = index
  }

  fun setTimeRange(range: TimeRangeFilter) {
    _timeRange.value = range
  }

  fun setTypeFilter(type: TransactionType?) {
    _selectedType.value = type
  }

  fun setCategoryFilter(category: TransactionCategory?) {
    _selectedCategory.value = category
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun openAddDraftSheet(itemToEdit: TransactionItem? = null) {
    _editingTransaction.value = itemToEdit
    _isAddDraftSheetOpen.value = true
  }

  fun closeAddDraftSheet() {
    _isAddDraftSheetOpen.value = false
    _editingTransaction.value = null
  }

  fun openSalaryModal() {
    _isSalaryModalOpen.value = true
  }

  fun closeSalaryModal() {
    _isSalaryModalOpen.value = false
  }

  fun openDetailDialog(item: TransactionItem) {
    _selectedDetailItem.value = item
  }

  fun closeDetailDialog() {
    _selectedDetailItem.value = null
  }

  fun saveTransaction(
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    timestamp: Long,
    note: String,
    paymentMethod: PaymentMethod,
    isRecurring: Boolean
  ) {
    viewModelScope.launch {
      val existing = _editingTransaction.value
      val item = TransactionItem(
        id = existing?.id ?: 0,
        title = title.ifBlank { category.displayName },
        amount = amount,
        type = type,
        category = category,
        timestamp = timestamp,
        note = note,
        paymentMethod = paymentMethod,
        isRecurring = isRecurring
      )

      if (existing != null && existing.id > 0) {
        repository.updateTransaction(item)
      } else {
        repository.insertTransaction(item)
      }
      closeAddDraftSheet()
    }
  }

  fun deleteTransaction(item: TransactionItem) {
    viewModelScope.launch {
      repository.deleteTransaction(item)
      if (_selectedDetailItem.value?.id == item.id) {
        _selectedDetailItem.value = null
      }
    }
  }

  fun updateSalarySettings(salary: Double, payDay: Int, budget: Double, symbol: String) {
    val newSettings = MonthlySalarySettings(
      salaryAmount = salary,
      payDayOfMonth = payDay,
      monthlyBudgetGoal = budget,
      currencySymbol = symbol
    )
    _salarySettings.value = newSettings
    repository.saveSalarySettings(newSettings)
    closeSalaryModal()
  }

  fun autoDraftSalaryForMonth() {
    viewModelScope.launch {
      val settings = _salarySettings.value
      val now = Calendar.getInstance()
      val payDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, settings.payDayOfMonth.coerceIn(1, 28))
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
      }

      val salaryItem = TransactionItem(
        title = "Monthly Salary (${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time)})",
        amount = settings.salaryAmount,
        type = TransactionType.SALARY,
        category = TransactionCategory.SALARY,
        timestamp = payDate.timeInMillis,
        note = "Auto-drafted recurring monthly earnings",
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        isRecurring = true
      )
      repository.insertTransaction(salaryItem)
    }
  }

  fun resetToSampleData() {
    viewModelScope.launch {
      repository.clearAll()
      repository.seedInitialDrafts()
    }
  }
}
