package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.drive.ConsentRequiredException
import com.example.data.drive.GoogleDriveBackupService
import com.example.data.drive.GoogleDriveState
import com.example.data.model.BudgetModel
import com.example.data.model.CalendarDayData
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpendingPoint
import com.example.data.model.ExportPeriod
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.YearlyFinancialSummary
import com.example.data.repository.FinanceRepository
import com.example.ui.components.DateUtils
import com.example.util.JsonPortabilityManager
import com.example.util.JsonValidationResult
import com.example.util.PaisaJsonBackup
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FinanceRepository(application.applicationContext)
  val driveService = GoogleDriveBackupService(application.applicationContext)

  // Google Drive Real State Flow
  private val _googleDriveState = MutableStateFlow<GoogleDriveState>(GoogleDriveState.NotConnected)
  val googleDriveState: StateFlow<GoogleDriveState> = _googleDriveState.asStateFlow()

  // Cloud Sync Status Flows
  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _lastSyncTimestamp = MutableStateFlow<Long?>(driveService.getLastBackupTimestamp())
  val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

  private val _syncErrorMessage = MutableStateFlow<String?>(null)
  val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

  private val _driveConsentIntent = MutableStateFlow<Intent?>(null)
  val driveConsentIntent: StateFlow<Intent?> = _driveConsentIntent.asStateFlow()

  fun clearConsentIntent() {
    _driveConsentIntent.value = null
  }

  init {
    initGoogleDriveState()
    checkAndAutoSync()
  }

  private fun initGoogleDriveState() {
    val account = driveService.getLastSignedInAccount()
    val savedEmail = driveService.getSavedEmail()
    val lastBackup = driveService.getLastBackupTimestamp()

    if (account != null && account.email != null) {
      _googleDriveState.value = GoogleDriveState.Connected(
        email = account.email ?: savedEmail ?: "Connected",
        lastBackupTimestampMillis = lastBackup
      )
      val currentProfile = repository.getUserProfile()
      if (currentProfile.email.isBlank() || currentProfile.googleId == null) {
        val updated = currentProfile.copy(
          name = account.displayName ?: currentProfile.name.ifBlank { "User" },
          email = account.email ?: "",
          photoUrl = account.photoUrl?.toString(),
          googleId = account.id,
          hasCompletedOnboarding = true
        )
        _userProfile.value = updated
        repository.saveUserProfile(updated)
      }
    } else if (!savedEmail.isNullOrBlank()) {
      _googleDriveState.value = GoogleDriveState.Connected(
        email = savedEmail,
        lastBackupTimestampMillis = lastBackup
      )
    } else {
      _googleDriveState.value = GoogleDriveState.NotConnected
    }
  }

  private var lastAutoSyncCheckTime = 0L

  private fun checkAndAutoSync() {
    triggerAutoSyncOnResume(force = true)
  }

  /**
   * Automatically synchronizes with Google Drive on app open or when returning to foreground.
   * Throttled to avoid excessive calls within 30 seconds.
   */
  fun triggerAutoSyncOnResume(force: Boolean = false) {
    val now = System.currentTimeMillis()
    if (!force && (now - lastAutoSyncCheckTime < 30_000L)) {
      return
    }
    lastAutoSyncCheckTime = now

    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount()
      if (account != null && account.email != null) {
        pullFromDriveAndMerge(account)
        repository.processDueRecurringRules()
      }
    }
  }

  /**
   * Persists the current local state to Google Drive automatically in background.
   */
  fun syncCurrentStateToDrive() {
    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount() ?: return@launch
      _isSyncing.value = true
      _syncErrorMessage.value = null
      try {
        val (txs, bgs, rcs) = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "3.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = txs,
          budgets = bgs,
          recurringRules = rcs
        )
        val ts = driveService.saveCloudData(account, payload)
        _lastSyncTimestamp.value = ts
        _googleDriveState.value = GoogleDriveState.BackupSuccess(
          email = account.email ?: "Google Drive",
          timestampMillis = ts
        )
      } catch (e: Exception) {
        _syncErrorMessage.value = "Offline: Changes saved locally and will sync when reconnected."
      } finally {
        _isSyncing.value = false
      }
    }
  }

  private suspend fun pullFromDriveAndMerge(account: GoogleSignInAccount): Boolean {
    _isSyncing.value = true
    _syncErrorMessage.value = null
    return try {
      val cloudPayload = driveService.fetchCloudData(account)
      _driveConsentIntent.value = null
      if (cloudPayload != null) {
        // Cloud has existing authoritative data
        repository.replaceCacheWithCloudData(
          transactions = cloudPayload.transactions,
          budgets = cloudPayload.budgets,
          recurringRules = cloudPayload.recurringRules
        )
        _lastSyncTimestamp.value = cloudPayload.exportTimestamp
        _googleDriveState.value = GoogleDriveState.Connected(
          email = account.email ?: "Google Drive",
          lastBackupTimestampMillis = cloudPayload.exportTimestamp
        )
      } else {
        // Cloud has no data yet: migrate existing local database data to Drive
        val (txs, bgs, rcs) = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "3.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = txs,
          budgets = bgs,
          recurringRules = rcs
        )
        val ts = driveService.saveCloudData(account, payload)
        _lastSyncTimestamp.value = ts
        _googleDriveState.value = GoogleDriveState.Connected(
          email = account.email ?: "Google Drive",
          lastBackupTimestampMillis = ts
        )
      }
      true
    } catch (e: ConsentRequiredException) {
      _driveConsentIntent.value = e.consentIntent
      _syncErrorMessage.value = "Google Drive access requires permission. Tap 'Grant Permission' below."
      _googleDriveState.value = GoogleDriveState.BackupFailed(
        email = account.email,
        errorMessage = "Permission required"
      )
      false
    } catch (e: Exception) {
      _syncErrorMessage.value = e.localizedMessage ?: "Failed to sync with Google Drive"
      false
    } finally {
      _isSyncing.value = false
    }
  }

  // Selected Month & Year for Dashboard & Analysis
  private val _selectedCalendar = MutableStateFlow(Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 12)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  })
  val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

  // Selected Day in Calendar Screen
  private val _selectedDayTimestamp = MutableStateFlow(System.currentTimeMillis())
  val selectedDayTimestamp: StateFlow<Long> = _selectedDayTimestamp.asStateFlow()

  // Search & Filter in Transactions Screen
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _filterType = MutableStateFlow<TransactionType?>(null)
  val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

  private val _filterCategory = MutableStateFlow<TransactionCategory?>(null)
  val filterCategory: StateFlow<TransactionCategory?> = _filterCategory.asStateFlow()

  // User Profile
  private val _userProfile = MutableStateFlow(repository.getUserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // All Transactions from DB
  val allTransactions: StateFlow<List<TransactionItem>> = repository.allTransactions.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // All Budgets from DB
  val allBudgets: StateFlow<List<BudgetModel>> = repository.allBudgets.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // All Recurring Rules from DB
  val allRecurringRules: StateFlow<List<RecurringRule>> = repository.allRecurringRules.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Current Month Summary: Income, Expenses, Savings, Budget
  val currentMonthSummary: StateFlow<MonthlyFinancialSummary> = combine(
    allTransactions,
    allBudgets,
    _selectedCalendar
  ) { transactions, budgets, cal ->
    val monthKey = DateUtils.getMonthKey(cal)
    val monthLabel = DateUtils.getMonthLabel(cal)
    val startOfMonth = DateUtils.getStartOfMonth(cal)
    val endOfMonth = DateUtils.getEndOfMonth(cal)

    val monthTransactions = transactions.filter { it.timestamp in startOfMonth..endOfMonth }
    val totalIncome = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val savings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (savings / totalIncome) * 100.0 else 0.0

    val budgetObj = budgets.find { it.monthKey == monthKey }
    val budgetLimit = budgetObj?.totalBudget ?: 0.0
    val budgetRemaining = if (budgetLimit > 0) budgetLimit - totalExpense else 0.0
    val budgetUsagePercent = if (budgetLimit > 0) (totalExpense / budgetLimit) * 100.0 else 0.0

    MonthlyFinancialSummary(
      monthKey = monthKey,
      monthLabel = monthLabel,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      savings = savings,
      savingsRate = savingsRate,
      budgetLimit = budgetLimit,
      budgetUsed = totalExpense,
      budgetRemaining = budgetRemaining,
      budgetUsagePercent = budgetUsagePercent,
      transactionCount = monthTransactions.size
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = MonthlyFinancialSummary(
      monthKey = DateUtils.getMonthKey(Calendar.getInstance()),
      monthLabel = DateUtils.getMonthLabel(Calendar.getInstance())
    )
  )

  // Current Year Summary
  val currentYearSummary: StateFlow<YearlyFinancialSummary> = combine(
    allTransactions,
    _selectedCalendar
  ) { transactions, cal ->
    val year = cal.get(Calendar.YEAR)
    val startOfYear = DateUtils.getStartOfYear(year)
    val endOfYear = DateUtils.getEndOfYear(year)

    val yearTransactions = transactions.filter { it.timestamp in startOfYear..endOfYear }
    val totalIncome = yearTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = yearTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val savings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (savings / totalIncome) * 100.0 else 0.0

    // Monthly breakdown
    val monthlyList = (0..11).map { monthIdx ->
      val monthCal = Calendar.getInstance().apply {
        set(year, monthIdx, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val mStart = DateUtils.getStartOfMonth(monthCal)
      val mEnd = DateUtils.getEndOfMonth(monthCal)
      val mTx = yearTransactions.filter { it.timestamp in mStart..mEnd }
      val mIncome = mTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
      val mExpense = mTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
      MonthlyFinancialSummary(
        monthKey = DateUtils.getMonthKey(monthCal),
        monthLabel = DateUtils.getMonthLabel(monthCal),
        totalIncome = mIncome,
        totalExpense = mExpense,
        savings = mIncome - mExpense,
        transactionCount = mTx.size
      )
    }

    YearlyFinancialSummary(
      year = year,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      savings = savings,
      savingsRate = savingsRate,
      monthlyBreakdown = monthlyList
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = YearlyFinancialSummary(year = Calendar.getInstance().get(Calendar.YEAR))
  )

  // Category-wise Spending in Selected Month
  val categorySpendingList: StateFlow<List<CategorySpending>> = combine(
    allTransactions,
    _selectedCalendar
  ) { transactions, cal ->
    val startOfMonth = DateUtils.getStartOfMonth(cal)
    val endOfMonth = DateUtils.getEndOfMonth(cal)
    val monthExpenses = transactions.filter { it.timestamp in startOfMonth..endOfMonth && it.type == TransactionType.EXPENSE }
    val totalMonthExpense = monthExpenses.sumOf { it.amount }

    if (totalMonthExpense == 0.0) {
      emptyList()
    } else {
      monthExpenses.groupBy { it.category }.map { (category, list) ->
        val amount = list.sumOf { it.amount }
        val pct = (amount / totalMonthExpense).toFloat()
        CategorySpending(
          category = category,
          amount = amount,
          percentage = pct,
          count = list.size,
          color = category.color
        )
      }.sortedByDescending { it.amount }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Daily Spending Trend in Selected Month
  val dailySpendingTrend: StateFlow<List<DailySpendingPoint>> = combine(
    allTransactions,
    _selectedCalendar
  ) { transactions, cal ->
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)

    val points = mutableListOf<DailySpendingPoint>()
    for (day in 1..daysInMonth) {
      val dayCal = Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val startDay = dayCal.timeInMillis
      val endDay = DateUtils.getEndOfDay(startDay)
      val dayTx = transactions.filter { it.timestamp in startDay..endDay }
      val exp = dayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
      val inc = dayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

      points.add(
        DailySpendingPoint(
          dayOfMonth = day,
          dateKey = DateUtils.getDayKey(dayCal),
          dayLabel = day.toString(),
          expense = exp,
          income = inc,
          net = inc - exp
        )
      )
    }
    points
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Calendar Grid Data for Calendar Screen
  val calendarDaysData: StateFlow<List<CalendarDayData>> = combine(
    allTransactions,
    _selectedCalendar
  ) { transactions, cal ->
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val firstDayCal = Calendar.getInstance().apply {
      set(year, month, 1, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    // Sunday = 1, Monday = 2, etc. Offset for grid alignment
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 7=Sat
    val leadDays = firstDayOfWeek - 1

    val todayKey = DateUtils.getDayKey(Calendar.getInstance())
    val gridDays = mutableListOf<CalendarDayData>()

    // Leading days from previous month
    val prevMonthCal = (firstDayCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val prevDaysCount = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in (prevDaysCount - leadDays + 1)..prevDaysCount) {
      val c = (prevMonthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, i) }
      val start = DateUtils.getStartOfDay(c.timeInMillis)
      val end = DateUtils.getEndOfDay(start)
      val dayTx = transactions.filter { it.timestamp in start..end }
      val inc = dayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
      val exp = dayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

      gridDays.add(
        CalendarDayData(
          dayOfMonth = i,
          dateKey = DateUtils.getDayKey(c),
          epochMillis = start,
          isCurrentMonth = false,
          isToday = DateUtils.getDayKey(c) == todayKey,
          hasIncome = inc > 0,
          hasExpense = exp > 0,
          totalIncome = inc,
          totalExpense = exp,
          transactions = dayTx
        )
      )
    }

    // Current month days
    for (day in 1..daysInMonth) {
      val c = Calendar.getInstance().apply {
        set(year, month, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val start = DateUtils.getStartOfDay(c.timeInMillis)
      val end = DateUtils.getEndOfDay(start)
      val dayTx = transactions.filter { it.timestamp in start..end }
      val inc = dayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
      val exp = dayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

      gridDays.add(
        CalendarDayData(
          dayOfMonth = day,
          dateKey = DateUtils.getDayKey(c),
          epochMillis = start,
          isCurrentMonth = true,
          isToday = DateUtils.getDayKey(c) == todayKey,
          hasIncome = inc > 0,
          hasExpense = exp > 0,
          totalIncome = inc,
          totalExpense = exp,
          transactions = dayTx
        )
      )
    }

    // Trailing days to fill 35 or 42 grid cells
    val remaining = (7 - (gridDays.size % 7)) % 7
    val nextMonthCal = (firstDayCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    for (day in 1..remaining) {
      val c = (nextMonthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
      val start = DateUtils.getStartOfDay(c.timeInMillis)
      val end = DateUtils.getEndOfDay(start)
      val dayTx = transactions.filter { it.timestamp in start..end }
      val inc = dayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
      val exp = dayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

      gridDays.add(
        CalendarDayData(
          dayOfMonth = day,
          dateKey = DateUtils.getDayKey(c),
          epochMillis = start,
          isCurrentMonth = false,
          isToday = DateUtils.getDayKey(c) == todayKey,
          hasIncome = inc > 0,
          hasExpense = exp > 0,
          totalIncome = inc,
          totalExpense = exp,
          transactions = dayTx
        )
      )
    }

    gridDays
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Transactions on the specifically selected calendar date
  val selectedDateTransactions: StateFlow<List<TransactionItem>> = combine(
    allTransactions,
    _selectedDayTimestamp
  ) { transactions, dayMillis ->
    val start = DateUtils.getStartOfDay(dayMillis)
    val end = DateUtils.getEndOfDay(dayMillis)
    transactions.filter { it.timestamp in start..end }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Filtered Transactions for Transactions Screen
  val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
    allTransactions,
    _searchQuery,
    _filterType,
    _filterCategory
  ) { transactions, query, type, cat ->
    transactions.filter { item ->
      val matchesQuery = query.isBlank() ||
          item.title.contains(query, ignoreCase = true) ||
          item.category.displayName.contains(query, ignoreCase = true) ||
          item.note.contains(query, ignoreCase = true) ||
          item.amount.toString().contains(query)
      val matchesType = type == null || item.type == type
      val matchesCat = cat == null || item.category == cat
      matchesQuery && matchesType && matchesCat
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  init {
    // Check and process due recurring transactions idempotently on startup
    processRecurringRules()
  }

  // ================= Navigation & Period =================

  fun previousMonth() {
    val cal = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.MONTH, -1)
    }
    _selectedCalendar.value = cal
  }

  fun nextMonth() {
    val cal = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.MONTH, 1)
    }
    _selectedCalendar.value = cal
  }

  fun previousYear() {
    val cal = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.YEAR, -1)
    }
    _selectedCalendar.value = cal
  }

  fun nextYear() {
    val cal = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.YEAR, 1)
    }
    _selectedCalendar.value = cal
  }

  fun setYear(year: Int) {
    val cal = (_selectedCalendar.value.clone() as Calendar).apply {
      set(Calendar.YEAR, year)
    }
    _selectedCalendar.value = cal
  }

  fun setMonthAndYear(year: Int, month: Int) {
    val cal = (_selectedCalendar.value.clone() as Calendar).apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
    }
    _selectedCalendar.value = cal
  }

  fun selectCalendarDate(timestamp: Long) {
    _selectedDayTimestamp.value = timestamp
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    _selectedCalendar.value = cal
  }

  // ================= Filters =================

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setFilterType(type: TransactionType?) {
    _filterType.value = type
  }

  fun setFilterCategory(category: TransactionCategory?) {
    _filterCategory.value = category
  }

  // ================= User Profile =================

  fun completeOnboarding(name: String) {
    val updated = _userProfile.value.copy(
      name = name.trim(),
      hasCompletedOnboarding = true
    )
    _userProfile.value = updated
    repository.saveUserProfile(updated)
    syncCurrentStateToDrive()
  }

  // ================= Transaction Operations =================

  fun addTransaction(
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    timestamp: Long,
    note: String,
    paymentMethod: PaymentMethod,
    isRecurring: Boolean = false,
    recurringRuleId: Long? = null
  ) {
    viewModelScope.launch {
      val item = TransactionItem(
        title = title.trim(),
        amount = amount,
        type = type,
        category = category,
        timestamp = timestamp,
        note = note.trim(),
        paymentMethod = paymentMethod,
        isRecurring = isRecurring,
        recurringRuleId = recurringRuleId
      )
      repository.insertTransaction(item)
      syncCurrentStateToDrive()
    }
  }

  fun updateTransaction(item: TransactionItem) {
    viewModelScope.launch {
      repository.updateTransaction(item)
      syncCurrentStateToDrive()
    }
  }

  fun deleteTransaction(item: TransactionItem) {
    viewModelScope.launch {
      repository.deleteTransaction(item)
      syncCurrentStateToDrive()
    }
  }

  fun deleteTransactionById(id: Long) {
    viewModelScope.launch {
      repository.deleteTransactionById(id)
      syncCurrentStateToDrive()
    }
  }

  // ================= Budget Operations =================

  fun saveMonthlyBudget(
    monthKey: String,
    totalBudget: Double,
    categoryBudgets: Map<TransactionCategory, Double>
  ) {
    viewModelScope.launch {
      val existing = allBudgets.value.find { it.monthKey == monthKey }
      val budget = BudgetModel(
        id = existing?.id ?: 0,
        monthKey = monthKey,
        totalBudget = totalBudget,
        categoryBudgets = categoryBudgets,
        updatedAt = System.currentTimeMillis()
      )
      repository.saveBudget(budget)
      syncCurrentStateToDrive()
    }
  }

  fun deleteBudget(monthKey: String) {
    viewModelScope.launch {
      repository.deleteBudgetForMonth(monthKey)
      syncCurrentStateToDrive()
    }
  }

  // ================= Recurring Rule Operations =================

  fun addRecurringRule(
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    interval: RecurrenceInterval,
    startDate: Long,
    endDate: Long?,
    paymentMethod: PaymentMethod,
    note: String
  ) {
    viewModelScope.launch {
      val rule = RecurringRule(
        title = title.trim(),
        amount = amount,
        type = type,
        category = category,
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        lastGeneratedDate = 0,
        paymentMethod = paymentMethod,
        note = note.trim(),
        isActive = true
      )
      repository.insertRecurringRule(rule)
      repository.processDueRecurringRules()
      syncCurrentStateToDrive()
    }
  }

  fun updateRecurringRule(rule: RecurringRule) {
    viewModelScope.launch {
      repository.updateRecurringRule(rule)
      repository.processDueRecurringRules()
      syncCurrentStateToDrive()
    }
  }

  fun toggleRecurringRule(id: Long, isActive: Boolean) {
    viewModelScope.launch {
      repository.toggleRecurringRule(id, isActive)
      if (isActive) {
        repository.processDueRecurringRules()
      }
      syncCurrentStateToDrive()
    }
  }

  fun deleteRecurringRule(id: Long) {
    viewModelScope.launch {
      repository.deleteRecurringRule(id)
      syncCurrentStateToDrive()
    }
  }

  fun processRecurringRules() {
    viewModelScope.launch {
      val generated = repository.processDueRecurringRules()
      if (generated > 0) {
        syncCurrentStateToDrive()
      }
    }
  }

  // ================= PDF Export & Sharing =================

  fun exportToPdf(
    context: Context,
    period: ExportPeriod,
    customStart: Long? = null,
    customEnd: Long? = null,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val file = repository.exportToPdf(period, _selectedCalendar.value, customStart, customEnd)

        val uri: Uri = FileProvider.getUriForFile(
          context,
          "${context.packageName}.fileprovider",
          file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
          type = "application/pdf"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_SUBJECT, "Paisa Financial Statement (${file.name})")
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share or Save Financial PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)

        onSuccess("PDF Statement generated: ${file.name}")
      } catch (e: Exception) {
        e.printStackTrace()
        onError(e.localizedMessage ?: "Failed to generate PDF report")
      }
    }
  }

  /**
   * Generates a complete machine-readable Paisa JSON backup file and opens the Android native Share flow.
   */
  fun exportJson(
    context: Context,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val (txs, bgs, rcs) = repository.getAllLocalData()
        val file = JsonPortabilityManager.exportToJsonFile(
          context = context,
          profile = _userProfile.value,
          transactions = txs,
          budgets = bgs,
          recurringRules = rcs
        )
        val shareIntent = JsonPortabilityManager.createShareIntent(context, file)
        val chooser = Intent.createChooser(shareIntent, "Share or Save Paisa JSON Backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        onSuccess("JSON Backup created: ${file.name}")
      } catch (e: Exception) {
        e.printStackTrace()
        onError(e.localizedMessage ?: "Failed to export JSON data")
      }
    }
  }

  /**
   * Validates a selected JSON file for proper Paisa schema and valid records.
   */
  suspend fun validateImportFile(
    context: Context,
    uri: Uri
  ): JsonValidationResult {
    return JsonPortabilityManager.validateImportFile(context, uri)
  }

  /**
   * Idempotently merges imported backup records, replaces local Room cache, and pushes to Google Drive.
   */
  fun confirmAndApplyImport(
    backup: PaisaJsonBackup,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val (currentTxs, currentBgs, currentRcs) = repository.getAllLocalData()
        val (mergedTxs, mergedBgs, mergedRcs) = JsonPortabilityManager.mergeData(
          currentTransactions = currentTxs,
          currentBudgets = currentBgs,
          currentRecurringRules = currentRcs,
          importBackup = backup
        )

        // 1. Atomically update local Room tables
        repository.replaceCacheWithCloudData(
          transactions = mergedTxs,
          budgets = mergedBgs,
          recurringRules = mergedRcs
        )
        repository.processDueRecurringRules()

        // 2. Persist merged data to Google Drive
        val account = driveService.getLastSignedInAccount()
        if (account != null && account.email != null) {
          val payload = com.example.data.drive.BackupPayload(
            version = "3.0.0",
            exportTimestamp = System.currentTimeMillis(),
            userProfile = _userProfile.value,
            transactions = mergedTxs,
            budgets = mergedBgs,
            recurringRules = mergedRcs
          )
          val ts = driveService.saveCloudData(account, payload)
          _lastSyncTimestamp.value = ts
          _googleDriveState.value = GoogleDriveState.BackupSuccess(
            email = account.email ?: "Google Drive",
            timestampMillis = ts
          )
        }
        onSuccess("Successfully imported ${backup.transactions.size} transactions and synced with Cloud.")
      } catch (e: Exception) {
        e.printStackTrace()
        onError(e.localizedMessage ?: "Failed to apply JSON import.")
      }
    }
  }

  fun clearAllData(onComplete: () -> Unit) {
    viewModelScope.launch {
      repository.clearAllTransactions()
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  // ================= Google Sign-In & Drive Sync Actions =================

  fun getDriveSignInIntent(): Intent = driveService.getSignInIntent()

  fun onGoogleSignInSuccess(
    account: GoogleSignInAccount,
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    val email = account.email ?: ""
    val name = account.displayName ?: email.substringBefore("@").ifBlank { "User" }
    val photo = account.photoUrl?.toString()
    val id = account.id

    val profile = UserProfile(
      name = name,
      email = email,
      photoUrl = photo,
      googleId = id,
      hasCompletedOnboarding = true
    )
    _userProfile.value = profile
    repository.saveUserProfile(profile)

    driveService.saveConnectedEmail(email)

    viewModelScope.launch {
      _isSyncing.value = true
      _googleDriveState.value = GoogleDriveState.Connecting
      val success = pullFromDriveAndMerge(account)
      repository.processDueRecurringRules()
      if (success) {
        onResult?.invoke(true, null)
      } else {
        onResult?.invoke(false, _syncErrorMessage.value)
      }
    }
  }

  fun onDriveSignInFailure(errorMessage: String) {
    val savedEmail = driveService.getSavedEmail()
    _googleDriveState.value = GoogleDriveState.BackupFailed(
      email = savedEmail,
      errorMessage = errorMessage
    )
    _syncErrorMessage.value = errorMessage
  }

  fun syncNow(onSuccessMessage: (String) -> Unit, onErrorMessage: (String) -> Unit) {
    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount()
      if (account == null) {
        onErrorMessage("Not signed in to Google Drive")
        return@launch
      }
      _isSyncing.value = true
      _googleDriveState.value = GoogleDriveState.BackingUp
      val success = pullFromDriveAndMerge(account)
      if (success) {
        val (txs, bgs, rcs) = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "3.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = txs,
          budgets = bgs,
          recurringRules = rcs
        )
        try {
          val ts = driveService.saveCloudData(account, payload)
          _lastSyncTimestamp.value = ts
          _googleDriveState.value = GoogleDriveState.BackupSuccess(
            email = account.email ?: "Google Drive",
            timestampMillis = ts
          )
          onSuccessMessage("Successfully synchronized with Google Drive")
        } catch (e: Exception) {
          val msg = e.localizedMessage ?: "Sync error"
          _syncErrorMessage.value = msg
          _googleDriveState.value = GoogleDriveState.BackupFailed(email = account.email, errorMessage = msg)
          onErrorMessage(msg)
        }
      } else {
        val msg = _syncErrorMessage.value ?: "Failed to connect to Google Drive"
        onErrorMessage(msg)
      }
      _isSyncing.value = false
    }
  }

  fun signOut(onComplete: () -> Unit) {
    viewModelScope.launch {
      driveService.signOut()
      repository.clearLocalCache()
      repository.clearUserProfile()
      _userProfile.value = UserProfile()
      _googleDriveState.value = GoogleDriveState.NotConnected
      _lastSyncTimestamp.value = null
      onComplete()
    }
  }

  fun resetDriveStateToConnected() {
    val account = driveService.getLastSignedInAccount()
    val savedEmail = driveService.getSavedEmail()
    val lastBackup = driveService.getLastBackupTimestamp()
    if (account != null || !savedEmail.isNullOrBlank()) {
      _googleDriveState.value = GoogleDriveState.Connected(
        email = account?.email ?: savedEmail ?: "Google Drive",
        lastBackupTimestampMillis = lastBackup
      )
    } else {
      _googleDriveState.value = GoogleDriveState.NotConnected
    }
  }
}
