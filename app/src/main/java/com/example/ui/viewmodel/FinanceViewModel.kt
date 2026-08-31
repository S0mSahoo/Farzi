package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActiveModule
import com.example.data.model.AppThemeMode
import com.example.data.model.CategoryAnalytics
import com.example.data.model.DailySpendingTrend
import com.example.data.model.DateGroupedDrafts
import com.example.data.model.DayOfWeekBreakdown
import com.example.data.model.MonthlySalaryLog
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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FinanceUiState(
  val allTransactions: List<TransactionItem> = emptyList(),
  val filteredTransactions: List<TransactionItem> = emptyList(),
  val groupedDrafts: List<DateGroupedDrafts> = emptyList(),
  val timeRange: TimeRangeFilter = TimeRangeFilter.THIS_MONTH,
  val activeModule: ActiveModule = ActiveModule.ALL,
  val selectedYear: Int = 2026,
  val selectedMonth: Int = 7, // 0-indexed Calendar.MONTH
  val selectedMonthLabel: String = "August 2026",
  val isCurrentMonthSelected: Boolean = true,
  val selectedType: TransactionType? = null,
  val selectedCategory: TransactionCategory? = null,
  val searchQuery: String = "",
  val salarySettings: MonthlySalarySettings = MonthlySalarySettings(),
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  
  // Analytics & Module Metrics
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

  // Per-Month Salary Tracking
  val monthlySalaryLogs: List<MonthlySalaryLog> = emptyList(),
  val currentMonthSalaryDrafted: Double = 0.0,
  val isSalaryDraftedForSelectedMonth: Boolean = false,
  val isMonthSalaryDraftDialogOpen: Boolean = false,

  // Navigation & Modals
  val currentTab: Int = 0, // 0 = Drafts, 1 = Analytics, 2 = Salary/Budget
  val isAddDraftSheetOpen: Boolean = false,
  val editingTransaction: TransactionItem? = null,
  val isSalaryModalOpen: Boolean = false,
  val isThemeDialogOpen: Boolean = false,
  val isClearDataDialogOpen: Boolean = false,
  val selectedDetailItem: TransactionItem? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FinanceRepository(application)

  private val _themeMode = MutableStateFlow(repository.getThemeMode())
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  private val _timeRange = MutableStateFlow(TimeRangeFilter.THIS_MONTH)
  val timeRange: StateFlow<TimeRangeFilter> = _timeRange.asStateFlow()

  private val _activeModule = MutableStateFlow(ActiveModule.ALL)
  val activeModule: StateFlow<ActiveModule> = _activeModule.asStateFlow()

  // Selected Month (Year and Month)
  private val _selectedMonthOffset = MutableStateFlow(0) // 0 = current month, -1 = prev, +1 = next

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

  private val _isThemeDialogOpen = MutableStateFlow(false)
  val isThemeDialogOpen: StateFlow<Boolean> = _isThemeDialogOpen.asStateFlow()

  private val _isClearDataDialogOpen = MutableStateFlow(false)
  val isClearDataDialogOpen: StateFlow<Boolean> = _isClearDataDialogOpen.asStateFlow()

  private val _selectedDetailItem = MutableStateFlow<TransactionItem?>(null)
  val selectedDetailItem: StateFlow<TransactionItem?> = _selectedDetailItem.asStateFlow()

  private val _isMonthSalaryDraftDialogOpen = MutableStateFlow(false)
  val isMonthSalaryDraftDialogOpen: StateFlow<Boolean> = _isMonthSalaryDraftDialogOpen.asStateFlow()

  val uiState: StateFlow<FinanceUiState> = combine(
    repository.allTransactions,
    _timeRange,
    _activeModule,
    _selectedMonthOffset,
    _selectedType,
    _selectedCategory,
    _searchQuery,
    _salarySettings,
    _themeMode,
    _currentTab,
    _isAddDraftSheetOpen,
    _editingTransaction,
    _isSalaryModalOpen,
    _isThemeDialogOpen,
    _isClearDataDialogOpen,
    _selectedDetailItem,
    _isMonthSalaryDraftDialogOpen
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val allItems = args[0] as List<TransactionItem>
    val range = args[1] as TimeRangeFilter
    val activeMod = args[2] as ActiveModule
    val monthOffset = args[3] as Int
    val type = args[4] as TransactionType?
    val category = args[5] as TransactionCategory?
    val query = args[6] as String
    val salary = args[7] as MonthlySalarySettings
    val theme = args[8] as AppThemeMode
    val tab = args[9] as Int
    val isAddOpen = args[10] as Boolean
    val editing = args[11] as TransactionItem?
    val isSalaryOpen = args[12] as Boolean
    val isThemeOpen = args[13] as Boolean
    val isClearOpen = args[14] as Boolean
    val detailItem = args[15] as TransactionItem?
    val isMonthSalaryOpen = args[16] as Boolean

    computeUiState(
      allItems = allItems,
      range = range,
      activeMod = activeMod,
      monthOffset = monthOffset,
      type = type,
      category = category,
      query = query,
      salary = salary,
      theme = theme,
      tab = tab,
      isAddOpen = isAddOpen,
      editing = editing,
      isSalaryOpen = isSalaryOpen,
      isThemeOpen = isThemeOpen,
      isClearOpen = isClearOpen,
      detailItem = detailItem,
      isMonthSalaryOpen = isMonthSalaryOpen
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
    activeMod: ActiveModule,
    monthOffset: Int,
    type: TransactionType?,
    category: TransactionCategory?,
    query: String,
    salary: MonthlySalarySettings,
    theme: AppThemeMode,
    tab: Int,
    isAddOpen: Boolean,
    editing: TransactionItem?,
    isSalaryOpen: Boolean,
    isThemeOpen: Boolean,
    isClearOpen: Boolean,
    detailItem: TransactionItem?,
    isMonthSalaryOpen: Boolean
  ): FinanceUiState {
    val now = Calendar.getInstance()
    val actualYear = now.get(Calendar.YEAR)
    val actualMonth = now.get(Calendar.MONTH)

    val targetCal = Calendar.getInstance().apply {
      add(Calendar.MONTH, monthOffset)
    }
    val selYear = targetCal.get(Calendar.YEAR)
    val selMonth = targetCal.get(Calendar.MONTH)
    val isCurrentMonth = (selYear == actualYear && selMonth == actualMonth)
    val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val selectedMonthLabel = monthLabelFormat.format(targetCal.time)

    val maxDaysInTargetMonth = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else maxDaysInTargetMonth
    val daysRemaining = if (isCurrentMonth) {
      (maxDaysInTargetMonth - now.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
    } else 1

    // Pre-format all items for ultra-smooth 120fps scrolling
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dateDisplayFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val shortDisplayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val wholeCurrencyFormatter = DecimalFormat("#,##0")
    val decCurrencyFormatter = DecimalFormat("#,##0.00")

    val formattedAllItems = allItems.map { item ->
      val formattedAmount = if (item.amount % 1.0 == 0.0) {
        salary.currencySymbol + wholeCurrencyFormatter.format(item.amount)
      } else {
        salary.currencySymbol + decCurrencyFormatter.format(item.amount)
      }
      item.copy(
        formattedTime = timeFormat.format(Date(item.timestamp)),
        formattedDate = dateFormat.format(Date(item.timestamp)),
        formattedAmount = formattedAmount
      )
    }

    // Selected Month Bounds
    val monthStartCal = Calendar.getInstance().apply {
      set(selYear, selMonth, 1, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val monthEndCal = Calendar.getInstance().apply {
      set(selYear, selMonth, maxDaysInTargetMonth, 23, 59, 59)
      set(Calendar.MILLISECOND, 999)
    }
    val selMonthStart = monthStartCal.timeInMillis
    val selMonthEnd = monthEndCal.timeInMillis

    // Calculate time bounds based on range or month offset
    val (startTime, endTime) = if (monthOffset != 0) {
      selMonthStart to selMonthEnd
    } else {
      getTimeBounds(range, now)
    }

    // Filtered by time range
    val inRangeItems = formattedAllItems.filter { it.timestamp in startTime..endTime }

    // Salary logs grouped by month to show month-by-month salary drafts
    val salaryFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val salaryDisplayMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val salaryDateDisplayFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

    val monthlySalaryLogs = formattedAllItems
      .filter { it.type == TransactionType.SALARY }
      .sortedByDescending { it.timestamp }
      .map { item ->
        MonthlySalaryLog(
          transactionId = item.id,
          monthKey = salaryFormat.format(Date(item.timestamp)),
          monthLabel = salaryDisplayMonthFormat.format(Date(item.timestamp)),
          amount = item.amount,
          timestamp = item.timestamp,
          dateFormatted = salaryDateDisplayFormat.format(Date(item.timestamp)),
          paymentMethod = item.paymentMethod,
          note = item.note
        )
      }

    // Check salary drafted for the currently selected month
    val selectedMonthSalaryDrafts = formattedAllItems.filter {
      it.type == TransactionType.SALARY && it.timestamp in selMonthStart..selMonthEnd
    }
    val isSalaryDraftedForSelectedMonth = selectedMonthSalaryDrafts.isNotEmpty()
    val currentMonthSalaryDrafted = selectedMonthSalaryDrafts.sumOf { it.amount }

    // Analytics computation on in-range items
    var totalIncome = 0.0
    var totalExpense = 0.0
    val expenseCategoryMap = mutableMapOf<TransactionCategory, Double>()
    val expenseCategoryCountMap = mutableMapOf<TransactionCategory, Int>()
    val dayOfWeekSpendMap = mutableMapOf<Int, Double>()
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

    // Daily spending trends
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
    val budgetGoal = salary.monthlyBudgetGoal
    val budgetProgress = if (budgetGoal > 0) (totalExpense / budgetGoal).toFloat() else 0f
    val remainingBudget = (budgetGoal - totalExpense).coerceAtLeast(0.0)
    val dailySafeSpend = remainingBudget / daysRemaining
    val avgDailyExpense = if (currentDay > 0) totalExpense / currentDay else 0.0

    // Filter items based on active module and search
    val baseList = if (query.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) formattedAllItems else inRangeItems

    val filteredList = baseList.filter { item ->
      val matchesModule = when (activeMod) {
        ActiveModule.ALL -> true
        ActiveModule.EXPENSE -> item.type == TransactionType.EXPENSE
        ActiveModule.INCOME -> item.type == TransactionType.INCOME || item.type == TransactionType.SALARY
      }
      val matchesType = type == null || (if (type == TransactionType.INCOME) (item.type == TransactionType.INCOME || item.type == TransactionType.SALARY) else item.type == type)
      val matchesCategory = category == null || item.category == category
      val matchesQuery = query.isBlank() ||
          item.title.contains(query, ignoreCase = true) ||
          item.note.contains(query, ignoreCase = true) ||
          item.category.displayName.contains(query, ignoreCase = true) ||
          item.paymentMethod.displayName.contains(query, ignoreCase = true) ||
          item.formattedDate.contains(query, ignoreCase = true)
      matchesModule && matchesType && matchesCategory && matchesQuery
    }

    // Pre-group transactions for instant, lag-free UI rendering
    val todayKey = dateFormat.format(Date())
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterdayKey = dateFormat.format(yesterdayCal.time)

    val groupedDrafts = filteredList
      .groupBy { it.formattedDate }
      .map { (dateKey, items) ->
        val headerTitle = when (dateKey) {
          todayKey -> "Today • ${shortDisplayFormat.format(Date())}"
          yesterdayKey -> "Yesterday • ${shortDisplayFormat.format(yesterdayCal.time)}"
          else -> items.firstOrNull()?.let { dateDisplayFormat.format(Date(it.timestamp)) } ?: dateKey
        }
        val dayExpense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val dayIncome = items.filter { it.type != TransactionType.EXPENSE }.sumOf { it.amount }
        DateGroupedDrafts(
          dateKey = dateKey,
          headerTitle = headerTitle,
          totalExpense = dayExpense,
          totalIncome = dayIncome,
          count = items.size,
          items = items
        )
      }

    return FinanceUiState(
      allTransactions = formattedAllItems,
      filteredTransactions = filteredList,
      groupedDrafts = groupedDrafts,
      timeRange = range,
      activeModule = activeMod,
      selectedYear = selYear,
      selectedMonth = selMonth,
      selectedMonthLabel = selectedMonthLabel,
      isCurrentMonthSelected = isCurrentMonth,
      selectedType = type,
      selectedCategory = category,
      searchQuery = query,
      salarySettings = salary,
      themeMode = theme,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      netSavings = netSavings,
      savingsRate = savingsRate,
      categoryAnalytics = categoryAnalyticsList,
      dailyTrends = dailyTrends,
      dayOfWeekBreakdown = dayOfWeekList,
      budgetSpent = totalExpense,
      budgetProgress = budgetProgress,
      remainingBudget = remainingBudget,
      daysRemainingInMonth = daysRemaining,
      dailySafeSpend = dailySafeSpend,
      topSpendingDay = topDay,
      topCategory = topCat,
      avgDailyExpense = avgDailyExpense,
      monthlySalaryLogs = monthlySalaryLogs,
      currentMonthSalaryDrafted = currentMonthSalaryDrafted,
      isSalaryDraftedForSelectedMonth = isSalaryDraftedForSelectedMonth,
      isMonthSalaryDraftDialogOpen = isMonthSalaryOpen,
      currentTab = tab,
      isAddDraftSheetOpen = isAddOpen,
      editingTransaction = editing,
      isSalaryModalOpen = isSalaryOpen,
      isThemeDialogOpen = isThemeOpen,
      isClearDataDialogOpen = isClearOpen,
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
    start: Long,
    end: Long,
    range: TimeRangeFilter
  ): List<DailySpendingTrend> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormat = SimpleDateFormat("EEE d", Locale.getDefault())
    val numDays = when (range) {
      TimeRangeFilter.LAST_7_DAYS -> 7
      TimeRangeFilter.THIS_MONTH, TimeRangeFilter.LAST_MONTH -> 14
      else -> 10
    }

    val trends = mutableListOf<DailySpendingTrend>()

    for (i in (numDays - 1) downTo 0) {
      val dayCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -i)
      }
      val dateKey = dateFormat.format(dayCal.time)
      val dayLabel = labelFormat.format(dayCal.time)

      val startOfDay = dayCal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis

      val endOfDay = dayCal.apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }.timeInMillis

      val daysItems = items.filter { it.timestamp in startOfDay..endOfDay }
      val exp = daysItems.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
      val inc = daysItems.filter { it.type != TransactionType.EXPENSE }.sumOf { it.amount }

      trends.add(
        DailySpendingTrend(
          dateKey = dateKey,
          dayLabel = dayLabel,
          expenseAmount = exp,
          incomeAmount = inc,
          netAmount = inc - exp
        )
      )
    }

    return trends
  }

  // Navigation & Actions
  fun setTab(index: Int) {
    _currentTab.value = index
  }

  fun setActiveModule(module: ActiveModule) {
    _activeModule.value = module
  }

  fun nextMonth() {
    _selectedMonthOffset.value += 1
  }

  fun prevMonth() {
    _selectedMonthOffset.value -= 1
  }

  fun resetToCurrentMonth() {
    _selectedMonthOffset.value = 0
  }

  fun setThemeMode(mode: AppThemeMode) {
    _themeMode.value = mode
    repository.saveThemeMode(mode)
  }

  fun openThemeDialog() {
    _isThemeDialogOpen.value = true
  }

  fun closeThemeDialog() {
    _isThemeDialogOpen.value = false
  }

  fun openClearDataDialog() {
    _isClearDataDialogOpen.value = true
  }

  fun closeClearDataDialog() {
    _isClearDataDialogOpen.value = false
  }

  fun setTimeRange(range: TimeRangeFilter) {
    _timeRange.value = range
    _selectedMonthOffset.value = 0 // Reset month offset when using standard time ranges
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

  fun openMonthSalaryDraftDialog() {
    _isMonthSalaryDraftDialogOpen.value = true
  }

  fun closeMonthSalaryDraftDialog() {
    _isMonthSalaryDraftDialogOpen.value = false
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

  // Draft distinct salary for the selected month or any custom amount/month
  fun draftSalaryForSpecificMonth(
    year: Int,
    month: Int,
    amount: Double,
    payDay: Int = _salarySettings.value.payDayOfMonth,
    paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    note: String = "Monthly salary draft"
  ) {
    viewModelScope.launch {
      val payDate = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        val maxDays = getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, payDay.coerceIn(1, maxDays))
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
      }

      val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(payDate.time)
      val salaryItem = TransactionItem(
        title = "Monthly Salary ($monthLabel)",
        amount = amount,
        type = TransactionType.SALARY,
        category = TransactionCategory.SALARY,
        timestamp = payDate.timeInMillis,
        note = note.ifBlank { "Salary draft for $monthLabel" },
        paymentMethod = paymentMethod,
        isRecurring = true
      )
      repository.insertTransaction(salaryItem)
      closeMonthSalaryDraftDialog()
    }
  }

  fun autoDraftSalaryForMonth() {
    val targetCal = Calendar.getInstance().apply {
      add(Calendar.MONTH, _selectedMonthOffset.value)
    }
    draftSalaryForSpecificMonth(
      year = targetCal.get(Calendar.YEAR),
      month = targetCal.get(Calendar.MONTH),
      amount = _salarySettings.value.salaryAmount
    )
  }

  fun cleanAllData() {
    viewModelScope.launch {
      repository.clearAll()
      closeClearDataDialog()
    }
  }

  fun resetToSampleData() {
    viewModelScope.launch {
      repository.clearAll()
      repository.seedInitialDrafts()
    }
  }
}
