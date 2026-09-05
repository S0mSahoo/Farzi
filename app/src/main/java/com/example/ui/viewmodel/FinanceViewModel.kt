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
import com.example.data.local.PaidRecurringOccurrenceEntity
import com.example.data.model.BudgetModel
import com.example.data.model.CalendarDayData
import com.example.data.model.CategorySpending
import com.example.data.model.CategorySpendingDetail
import com.example.data.model.DailySpendingPoint
import com.example.data.model.DriveStorageQuota
import com.example.data.model.ExportPeriod
import com.example.data.model.FinancialInsight
import com.example.data.model.InsightType
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.OccurrenceStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringOccurrence
import com.example.data.model.RecurringRule
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.YearlyFinancialSummary
import com.example.data.repository.FinanceRepository
import com.example.pro.ai.CopilotMessage
import com.example.pro.ai.CopilotSender
import com.example.pro.ai.FinancialAiService
import com.example.pro.ai.IntentClassifier
import com.example.pro.engine.FinancialContextBuilder
import com.example.pro.engine.FinancialIntelligenceEngine
import com.example.pro.engine.forecast.CashFlowForecastEngine
import com.example.pro.engine.forecast.CashFlowForecastResult
import com.example.pro.engine.whatif.WhatIfScenario
import com.example.pro.engine.whatif.WhatIfSimulationResult
import com.example.pro.engine.whatif.WhatIfSimulatorEngine
import com.example.pro.entitlement.DevelopmentProEntitlementProvider
import com.example.pro.entitlement.EntitlementState
import com.example.pro.entitlement.ProEntitlementManager
import com.example.pro.entitlement.ProEntitlementManagerProvider
import com.example.pro.entitlement.ProFeature
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FinanceRepository(application.applicationContext)
  val driveService = GoogleDriveBackupService(application.applicationContext)

  // Paisa Pro Engines & Entitlement
  private val financialIntelligenceEngine = FinancialIntelligenceEngine()
  private val cashFlowForecastEngine = CashFlowForecastEngine()
  private val whatIfSimulatorEngine = WhatIfSimulatorEngine(cashFlowForecastEngine)
  private val financialContextBuilder = FinancialContextBuilder(financialIntelligenceEngine)
  private val financialAiService = FinancialAiService()
  val proEntitlementManager: ProEntitlementManager = ProEntitlementManagerProvider.get()

  val proEntitlementState: StateFlow<EntitlementState> = proEntitlementManager.entitlementStateFlow
  val isProUser: StateFlow<Boolean> = proEntitlementManager.entitlementStateFlow
    .map { it == EntitlementState.PRO }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), proEntitlementManager.getEntitlementState() == EntitlementState.PRO)

  // AI Copilot State
  private val _copilotMessages = MutableStateFlow<List<CopilotMessage>>(
    listOf(
      CopilotMessage(
        sender = CopilotSender.COPILOT,
        text = "Namaste! I'm your Paisa Financial Copilot. Ask me anything about your income, expenses, forecasts, or what-if spending decisions."
      )
    )
  )
  val copilotMessages: StateFlow<List<CopilotMessage>> = _copilotMessages.asStateFlow()

  private val _isCopilotLoading = MutableStateFlow(false)
  val isCopilotLoading: StateFlow<Boolean> = _isCopilotLoading.asStateFlow()

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
  var lastStopTime: Long = 0L

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
    if (_isSyncing.value) return
    lastAutoSyncCheckTime = now

    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount()
      if (account != null && account.email != null) {
        _isSyncing.value = true
        _syncErrorMessage.value = null
        try {
          pullFromDriveAndMerge(account)
          repository.processDueRecurringRules()
        } catch (e: Exception) {
          _syncErrorMessage.value = e.localizedMessage ?: "Sync error"
        } finally {
          _isSyncing.value = false
        }
      }
    }
  }

  /**
   * Persists the current local state to Google Drive automatically in background.
   */
  fun syncCurrentStateToDrive() {
    if (_isSyncing.value) return
    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount() ?: return@launch
      _isSyncing.value = true
      _syncErrorMessage.value = null
      try {
        val dump = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "4.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = dump.transactions,
          budgets = dump.budgets,
          recurringRules = dump.recurringRules,
          paidOccurrences = dump.paidOccurrences
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
    _syncErrorMessage.value = null
    return try {
      val cloudPayload = driveService.fetchCloudData(account)
      _driveConsentIntent.value = null
      if (cloudPayload != null) {
        // Cloud has existing authoritative data
        repository.replaceCacheWithCloudData(
          transactions = cloudPayload.transactions,
          budgets = cloudPayload.budgets,
          recurringRules = cloudPayload.recurringRules,
          paidOccurrences = cloudPayload.paidOccurrences
        )
        _lastSyncTimestamp.value = cloudPayload.exportTimestamp
        _googleDriveState.value = GoogleDriveState.Connected(
          email = account.email ?: "Google Drive",
          lastBackupTimestampMillis = cloudPayload.exportTimestamp
        )
      } else {
        // Cloud has no data yet: migrate existing local database data to Drive
        val dump = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "4.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = dump.transactions,
          budgets = dump.budgets,
          recurringRules = dump.recurringRules,
          paidOccurrences = dump.paidOccurrences
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
    }
  }

  // ================= Universal Application Period Scope =================
  // Selected month/year is treated as a universal application-level period selection
  // across Home, Calendar, Budget, History, and Report screens.
  private val _universalCalendar = MutableStateFlow(Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 12)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  })
  val selectedCalendar: StateFlow<Calendar> = _universalCalendar.asStateFlow()
  val dashboardCalendar: StateFlow<Calendar> = _universalCalendar.asStateFlow()
  val calendarMonth: StateFlow<Calendar> = _universalCalendar.asStateFlow()
  val budgetCalendar: StateFlow<Calendar> = _universalCalendar.asStateFlow()

  private val _calendarSelectedDayMillis = MutableStateFlow(System.currentTimeMillis())
  val calendarSelectedDayMillis: StateFlow<Long> = _calendarSelectedDayMillis.asStateFlow()
  val selectedDayTimestamp: StateFlow<Long> = _calendarSelectedDayMillis.asStateFlow()

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

  // All Paid Recurring Occurrences from DB
  val allPaidOccurrences: StateFlow<List<PaidRecurringOccurrenceEntity>> = repository.allPaidOccurrences.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Theme & App Lock States
  private val _themeMode = MutableStateFlow(repository.getThemeMode())
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  private val _isAppLockEnabled = MutableStateFlow(repository.isAppLockEnabled())
  val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

  private val _isAppUnlocked = MutableStateFlow(!repository.isAppLockEnabled())
  val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

  // Secure Vault Session State
  private val _isVaultUnlocked = MutableStateFlow(false)
  val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

  // Google Drive Available Storage Quota
  private val _driveStorageQuota = MutableStateFlow<DriveStorageQuota?>(null)
  val driveStorageQuota: StateFlow<DriveStorageQuota?> = _driveStorageQuota.asStateFlow()

  // Category Detail View Selection
  private val _selectedCategory = MutableStateFlow<TransactionCategory?>(null)
  val selectedCategory: StateFlow<TransactionCategory?> = _selectedCategory.asStateFlow()

  // ----------------- 1. Dashboard Flows (Scoped to _universalCalendar) -----------------

  val dashboardMonthSummary: StateFlow<MonthlyFinancialSummary> = combine(
    allTransactions,
    allBudgets,
    _universalCalendar
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

  val dashboardYearSummary: StateFlow<YearlyFinancialSummary> = combine(
    allTransactions,
    _universalCalendar
  ) { transactions, cal ->
    val year = cal.get(Calendar.YEAR)
    val startOfYear = DateUtils.getStartOfYear(year)
    val endOfYear = DateUtils.getEndOfYear(year)

    val yearTransactions = transactions.filter { it.timestamp in startOfYear..endOfYear }
    val totalIncome = yearTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = yearTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val savings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (savings / totalIncome) * 100.0 else 0.0

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

  val dashboardCategorySpending: StateFlow<List<CategorySpending>> = combine(
    allTransactions,
    _universalCalendar
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

  val dashboardDailyTrend: StateFlow<List<DailySpendingPoint>> = combine(
    allTransactions,
    _universalCalendar
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

  // ----------------- 2. Calendar Screen Flows (Scoped to _universalCalendar & _calendarSelectedDayMillis) -----------------

  fun computeCalendarDays(year: Int, month: Int, transactions: List<TransactionItem>): List<CalendarDayData> {
    val cal = Calendar.getInstance().apply {
      set(year, month, 1, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 7=Sat
    val leadDays = firstDayOfWeek - 1

    val todayKey = DateUtils.getDayKey(Calendar.getInstance())
    val gridDays = mutableListOf<CalendarDayData>()

    // Leading days from previous month
    val prevMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
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
    val nextMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
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

    return gridDays
  }

  val calendarDaysData: StateFlow<List<CalendarDayData>> = combine(
    allTransactions,
    _universalCalendar
  ) { transactions, cal ->
    computeCalendarDays(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), transactions)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val calendarDateTransactions: StateFlow<List<TransactionItem>> = combine(
    allTransactions,
    _calendarSelectedDayMillis
  ) { transactions, dayMillis ->
    val start = DateUtils.getStartOfDay(dayMillis)
    val end = DateUtils.getEndOfDay(dayMillis)
    transactions.filter { it.timestamp in start..end }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // ----------------- 3. Budget Screen Flows (Scoped to _universalCalendar) -----------------

  val budgetMonthSummary: StateFlow<MonthlyFinancialSummary> = combine(
    allTransactions,
    allBudgets,
    _universalCalendar
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

  // Category Detail View Flow (Respects universal calendar month)
  val selectedCategoryDetail: StateFlow<CategorySpendingDetail?> = combine(
    _selectedCategory,
    allTransactions,
    _universalCalendar
  ) { category, transactions, cal ->
    if (category == null) return@combine null
    val startOfMonth = DateUtils.getStartOfMonth(cal)
    val endOfMonth = DateUtils.getEndOfMonth(cal)
    val monthKey = DateUtils.getMonthKey(cal)
    val monthLabel = DateUtils.getMonthLabel(cal)

    val catTransactions = transactions.filter {
      it.category == category && it.timestamp in startOfMonth..endOfMonth
    }.sortedByDescending { it.timestamp }

    val totalSpent = catTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = catTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    CategorySpendingDetail(
      category = category,
      monthKey = monthKey,
      monthLabel = monthLabel,
      totalSpent = totalSpent,
      transactionCount = catTransactions.size,
      totalIncome = totalIncome,
      netAmount = totalIncome - totalSpent,
      transactions = catTransactions
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  fun selectCategoryForDetail(category: TransactionCategory) {
    _selectedCategory.value = category
  }

  fun clearSelectedCategoryDetail() {
    _selectedCategory.value = null
  }

  // ----------------- 4. Scheduled Recurring Payments Flows -----------------

  val allScheduledOccurrences: StateFlow<List<RecurringOccurrence>> = combine(
    allRecurringRules,
    allPaidOccurrences
  ) { rules, paidList ->
    val activeRules = rules.filter { it.isActive }
    val now = Calendar.getInstance()
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
    val startOfToday = DateUtils.getStartOfDay(now.timeInMillis)
    val endOfToday = DateUtils.getEndOfDay(now.timeInMillis)

    // Requirement 12: Restrict display to current and previous month only
    val prevMonthCal = (now.clone() as Calendar).apply {
      add(Calendar.MONTH, -1)
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val windowStart = prevMonthCal.timeInMillis
    val windowEnd = DateUtils.getEndOfMonth(now)

    val paidMap = paidList.associateBy { Pair(it.ruleId, it.occurrenceDateKey) }

    val results = mutableListOf<RecurringOccurrence>()
    for (rule in activeRules) {
      val occurrences = repository.computeRuleOccurrencesInWindow(
        rule = rule,
        windowStart = windowStart,
        windowEnd = windowEnd
      )
      for (occTime in occurrences) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(occTime))
        val paidEntity = paidMap[Pair(rule.id, dateKey)]
        val daysDiff = ((occTime - startOfToday) / 86400000L).toInt()

        val status = when {
          paidEntity != null && paidEntity.isCancelled -> OccurrenceStatus.CANCELLED
          paidEntity != null && paidEntity.paidTransactionId > 0 -> OccurrenceStatus.PAID
          dateKey == todayKey -> OccurrenceStatus.DUE_TODAY
          occTime < startOfToday -> OccurrenceStatus.OVERDUE
          else -> OccurrenceStatus.SCHEDULED
        }
        results.add(
          RecurringOccurrence(
            rule = rule,
            occurrenceTimestamp = occTime,
            occurrenceDateKey = dateKey,
            status = status,
            daysRelative = daysDiff,
            paidTransactionId = paidEntity?.paidTransactionId
          )
        )
      }
    }
    results.sortedBy { it.occurrenceTimestamp }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val todayRecurringPayments: StateFlow<List<RecurringOccurrence>> = allScheduledOccurrences.map { list ->
    list.filter { it.status == OccurrenceStatus.DUE_TODAY }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val overdueRecurringPayments: StateFlow<List<RecurringOccurrence>> = allScheduledOccurrences.map { list ->
    list.filter { it.status == OccurrenceStatus.OVERDUE }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val upcomingRecurringPayments: StateFlow<List<RecurringOccurrence>> = allScheduledOccurrences.map { list ->
    list.filter { it.status == OccurrenceStatus.SCHEDULED || it.status == OccurrenceStatus.UPCOMING }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun markRecurringPaymentPaid(occurrence: RecurringOccurrence) {
    viewModelScope.launch {
      repository.markRecurringPaymentPaid(occurrence.rule, occurrence.occurrenceTimestamp)
      syncCurrentStateToDrive()
    }
  }

  fun cancelRecurringOccurrence(occurrence: RecurringOccurrence) {
    viewModelScope.launch {
      repository.cancelRecurringOccurrence(occurrence.rule.id, occurrence.occurrenceDateKey)
      syncCurrentStateToDrive()
    }
  }

  fun restoreRecurringOccurrence(occurrence: RecurringOccurrence) {
    viewModelScope.launch {
      repository.restoreRecurringOccurrence(occurrence.rule.id, occurrence.occurrenceDateKey)
      syncCurrentStateToDrive()
    }
  }

  // ----------------- 5. Financial Insights Engine -----------------

  val financialInsights: StateFlow<List<FinancialInsight>> = combine(
    dashboardMonthSummary,
    dashboardCategorySpending,
    todayRecurringPayments,
    upcomingRecurringPayments
  ) { summary, categories, dueToday, upcoming ->
    val insights = mutableListOf<FinancialInsight>()
    val cal = _universalCalendar.value
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val daysLeft = (daysInMonth - currentDay).coerceAtLeast(0)

    // 1. Overall Budget Warning or Pacing
    if (summary.budgetLimit > 0) {
      val percent = summary.budgetUsagePercent
      if (percent >= 100) {
        insights.add(
          FinancialInsight(
            id = "budget_exceeded",
            title = "Monthly Budget Exceeded",
            description = "You have spent ₹${summary.totalExpense.toLong()} of your ₹${summary.budgetLimit.toLong()} budget (${percent.toInt()}% consumed).",
            type = InsightType.ALERT
          )
        )
      } else if (percent >= 80) {
        insights.add(
          FinancialInsight(
            id = "budget_pacing",
            title = "High Budget Utilization",
            description = "You have used ${percent.toInt()}% of your ₹${summary.budgetLimit.toLong()} monthly budget with $daysLeft days remaining in the month.",
            type = InsightType.WARNING
          )
        )
      }
    }

    // 2. Highest Category
    val topExpenseCat = categories.maxByOrNull { it.amount }
    if (topExpenseCat != null && topExpenseCat.amount > 0 && summary.totalExpense > 0) {
      val catPercent = ((topExpenseCat.amount / summary.totalExpense) * 100.0).toInt()
      if (catPercent >= 35) {
        insights.add(
          FinancialInsight(
            id = "top_category",
            title = "${topExpenseCat.category.displayName} Leading Expenses",
            description = "${topExpenseCat.category.displayName} accounts for $catPercent% (₹${topExpenseCat.amount.toLong()}) of your spending this month.",
            type = InsightType.INFO,
            category = topExpenseCat.category
          )
        )
      }
    }

    // 3. Due recurring bills
    if (dueToday.isNotEmpty()) {
      val dueSum = dueToday.sumOf { it.rule.amount }
      insights.add(
        FinancialInsight(
          id = "recurring_due_today",
          title = "Bills Due Today",
          description = "${dueToday.size} scheduled payment${if (dueToday.size > 1) "s" else ""} totaling ₹${dueSum.toLong()} are due today.",
          type = InsightType.ALERT
        )
      )
    }

    // 4. Savings rate insight
    if (summary.totalIncome > 0) {
      val rate = summary.savingsRate
      if (rate >= 30) {
        insights.add(
          FinancialInsight(
            id = "strong_savings",
            title = "Healthy Savings Rate",
            description = "Your savings rate is ${rate.toInt()}% this month (₹${summary.savings.toLong()} saved).",
            type = InsightType.POSITIVE
          )
        )
      } else if (rate < 10 && summary.savings > 0) {
        insights.add(
          FinancialInsight(
            id = "low_savings",
            title = "Low Savings Margin",
            description = "Your savings rate is only ${rate.toInt()}% this month. Consider reviewing non-essential expenses.",
            type = InsightType.WARNING
          )
        )
      }
    }

    if (insights.isEmpty()) {
      if (summary.transactionCount == 0) {
        insights.add(
          FinancialInsight(
            id = "no_transactions",
            title = "No Data Yet for This Period",
            description = "Add income and expenses for ${summary.monthLabel} to view personalized spending insights.",
            type = InsightType.INFO
          )
        )
      } else {
        insights.add(
          FinancialInsight(
            id = "balanced_month",
            title = "Spending On Track",
            description = "Your expenses are balanced and within healthy limits for ${summary.monthLabel}.",
            type = InsightType.POSITIVE
          )
        )
      }
    }

    insights
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Aliases for compatibility
  val currentMonthSummary: StateFlow<MonthlyFinancialSummary> = dashboardMonthSummary
  val currentYearSummary: StateFlow<YearlyFinancialSummary> = dashboardYearSummary
  val categorySpendingList: StateFlow<List<CategorySpending>> = dashboardCategorySpending
  val dailySpendingTrend: StateFlow<List<DailySpendingPoint>> = dashboardDailyTrend
  val selectedDateTransactions: StateFlow<List<TransactionItem>> = calendarDateTransactions

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
    val cal = (_universalCalendar.value.clone() as Calendar).apply {
      add(Calendar.MONTH, -1)
    }
    _universalCalendar.value = cal
  }

  fun nextMonth() {
    val cal = (_universalCalendar.value.clone() as Calendar).apply {
      add(Calendar.MONTH, 1)
    }
    _universalCalendar.value = cal
  }

  fun previousYear() {
    val cal = (_universalCalendar.value.clone() as Calendar).apply {
      add(Calendar.YEAR, -1)
    }
    _universalCalendar.value = cal
  }

  fun nextYear() {
    val cal = (_universalCalendar.value.clone() as Calendar).apply {
      add(Calendar.YEAR, 1)
    }
    _universalCalendar.value = cal
  }

  fun setYear(year: Int) {
    val cal = (_universalCalendar.value.clone() as Calendar).apply {
      set(Calendar.YEAR, year)
    }
    _universalCalendar.value = cal
  }

  fun setMonthAndYear(year: Int, month: Int) {
    val cal = (_universalCalendar.value.clone() as Calendar).apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
    }
    _universalCalendar.value = cal
  }

  // --- Screen-Specific Aliases Routing to Universal Period ---
  fun previousDashboardMonth() = previousMonth()
  fun nextDashboardMonth() = nextMonth()
  fun previousDashboardYear() = previousYear()
  fun nextDashboardYear() = nextYear()
  fun setDashboardYear(year: Int) = setYear(year)
  fun setDashboardMonthAndYear(year: Int, month: Int) = setMonthAndYear(year, month)

  fun previousCalendarMonth() = previousMonth()
  fun nextCalendarMonth() = nextMonth()
  fun setCalendarMonthAndYear(year: Int, month: Int) = setMonthAndYear(year, month)

  fun selectCalendarDate(timestamp: Long) {
    _calendarSelectedDayMillis.value = timestamp
    val cal = Calendar.getInstance().apply {
      timeInMillis = timestamp
      set(Calendar.HOUR_OF_DAY, 12)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    _universalCalendar.value = cal
  }

  // --- Budget Screen Navigation ---
  fun previousBudgetMonth() = previousMonth()
  fun nextBudgetMonth() = nextMonth()
  fun setBudgetMonthAndYear(year: Int, month: Int) = setMonthAndYear(year, month)

  // ================= Theme, Security & Storage =================

  fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    repository.saveThemeMode(mode)
  }

  fun setAppLockEnabled(enabled: Boolean) {
    _isAppLockEnabled.value = enabled
    repository.setAppLockEnabled(enabled)
    if (!enabled) {
      _isAppUnlocked.value = true
    }
  }

  fun unlockApp() {
    _isAppUnlocked.value = true
  }

  fun lockApp() {
    if (_isAppLockEnabled.value) {
      _isAppUnlocked.value = false
    }
  }

  fun unlockVault() {
    _isVaultUnlocked.value = true
  }

  fun lockVault() {
    _isVaultUnlocked.value = false
  }

  fun refreshDriveStorageQuota() {
    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount() ?: return@launch
      val quota = driveService.fetchStorageQuota(account)
      _driveStorageQuota.value = quota
    }
  }



  // ================= Bulk Delete Operations =================

  fun deleteTransactionsByIds(ids: List<Long>, onComplete: (Int) -> Unit = {}) {
    viewModelScope.launch {
      val count = repository.deleteTransactionsByIds(ids)
      syncCurrentStateToDrive()
      onComplete(count)
    }
  }

  fun deleteTransactionsForMonth(cal: Calendar, onComplete: (Int) -> Unit = {}) {
    viewModelScope.launch {
      val start = DateUtils.getStartOfMonth(cal)
      val end = DateUtils.getEndOfMonth(cal)
      val count = repository.deleteTransactionsBetween(start, end)
      syncCurrentStateToDrive()
      onComplete(count)
    }
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
    selectedCal: Calendar = _universalCalendar.value,
    customStart: Long? = null,
    customEnd: Long? = null,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val file = repository.exportToPdf(period, selectedCal, customStart, customEnd)

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
    period: ExportPeriod = ExportPeriod.ALL_TIME,
    selectedCal: Calendar = _universalCalendar.value,
    customStart: Long? = null,
    customEnd: Long? = null,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val dump = repository.getAllLocalData()
        val file = JsonPortabilityManager.exportToJsonFile(
          context = context,
          profile = _userProfile.value,
          transactions = dump.transactions,
          budgets = dump.budgets,
          recurringRules = dump.recurringRules,
          period = period,
          selectedCalendar = selectedCal,
          customStart = customStart,
          customEnd = customEnd
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
        val dump = repository.getAllLocalData()
        val (mergedTxs, mergedBgs, mergedRcs) = JsonPortabilityManager.mergeData(
          currentTransactions = dump.transactions,
          currentBudgets = dump.budgets,
          currentRecurringRules = dump.recurringRules,
          importBackup = backup
        )

        // 1. Atomically update local Room tables
        repository.replaceCacheWithCloudData(
          transactions = mergedTxs,
          budgets = mergedBgs,
          recurringRules = mergedRcs,
          paidOccurrences = dump.paidOccurrences
        )
        repository.processDueRecurringRules()

        // 2. Persist merged data to Google Drive
        val account = driveService.getLastSignedInAccount()
        if (account != null && account.email != null) {
          val payload = com.example.data.drive.BackupPayload(
            version = "4.0.0",
            exportTimestamp = System.currentTimeMillis(),
            userProfile = _userProfile.value,
            transactions = mergedTxs,
            budgets = mergedBgs,
            recurringRules = mergedRcs,
            paidOccurrences = dump.paidOccurrences
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
      if (_isSyncing.value) return@launch
      _isSyncing.value = true
      _googleDriveState.value = GoogleDriveState.Connecting
      _syncErrorMessage.value = null
      try {
        val success = pullFromDriveAndMerge(account)
        repository.processDueRecurringRules()
        if (success) {
          onResult?.invoke(true, null)
        } else {
          onResult?.invoke(false, _syncErrorMessage.value)
        }
      } catch (e: Exception) {
        val msg = e.localizedMessage ?: "Sign-in sync failed"
        _syncErrorMessage.value = msg
        onResult?.invoke(false, msg)
      } finally {
        _isSyncing.value = false
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
    if (_isSyncing.value) return
    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount()
      if (account == null) {
        onErrorMessage("Not signed in to Google Drive")
        return@launch
      }
      _isSyncing.value = true
      _googleDriveState.value = GoogleDriveState.BackingUp
      _syncErrorMessage.value = null
      try {
        val success = pullFromDriveAndMerge(account)
        if (success) {
          val dump = repository.getAllLocalData()
          val payload = com.example.data.drive.BackupPayload(
            version = "4.0.0",
            exportTimestamp = System.currentTimeMillis(),
            userProfile = _userProfile.value,
            transactions = dump.transactions,
            budgets = dump.budgets,
            recurringRules = dump.recurringRules,
            paidOccurrences = dump.paidOccurrences
          )
          val ts = driveService.saveCloudData(account, payload)
          _lastSyncTimestamp.value = ts
          _googleDriveState.value = GoogleDriveState.BackupSuccess(
            email = account.email ?: "Google Drive",
            timestampMillis = ts
          )
          onSuccessMessage("Successfully synchronized with Google Drive")
        } else {
          val msg = _syncErrorMessage.value ?: "Failed to connect to Google Drive"
          onErrorMessage(msg)
        }
      } catch (e: Exception) {
        val msg = e.localizedMessage ?: "Sync error"
        _syncErrorMessage.value = msg
        _googleDriveState.value = GoogleDriveState.BackupFailed(email = account.email, errorMessage = msg)
        onErrorMessage(msg)
      } finally {
        _isSyncing.value = false
      }
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

  // ================= Paisa Pro Operations =================

  fun hasProAccess(feature: ProFeature): Boolean {
    return proEntitlementManager.hasAccess(feature)
  }

  fun setDevProEnabled(enabled: Boolean) {
    val provider = proEntitlementManager.getProvider()
    if (provider is DevelopmentProEntitlementProvider) {
      provider.setProEnabled(enabled)
    }
  }

  fun getCashFlowForecast(yearMonth: YearMonth = YearMonth.now()): CashFlowForecastResult {
    val txs = allTransactions.value
    val rules = allRecurringRules.value
    val paids = allPaidOccurrences.value
    val monthKey = "%04d-%02d".format(Locale.US, yearMonth.year, yearMonth.monthValue)
    val budget = allBudgets.value.find { it.monthKey == monthKey }

    return cashFlowForecastEngine.generateForecast(
      allTransactions = txs,
      recurringRules = rules,
      paidOccurrences = paids,
      activeBudget = budget,
      yearMonth = yearMonth
    )
  }

  fun simulateWhatIfScenario(scenario: WhatIfScenario, yearMonth: YearMonth = YearMonth.now()): WhatIfSimulationResult {
    val txs = allTransactions.value
    val rules = allRecurringRules.value
    val paids = allPaidOccurrences.value
    val monthKey = "%04d-%02d".format(Locale.US, yearMonth.year, yearMonth.monthValue)
    val budget = allBudgets.value.find { it.monthKey == monthKey }

    return whatIfSimulatorEngine.simulate(
      allTransactions = txs,
      recurringRules = rules,
      paidOccurrences = paids,
      activeBudget = budget,
      yearMonth = yearMonth,
      scenario = scenario
    )
  }

  fun askCopilot(question: String) {
    if (question.isBlank()) return
    val userMsg = CopilotMessage(
      sender = CopilotSender.USER,
      text = question.trim()
    )
    _copilotMessages.value = _copilotMessages.value + userMsg
    _isCopilotLoading.value = true

    viewModelScope.launch {
      try {
        val txs = allTransactions.value
        val rules = allRecurringRules.value
        val paids = allPaidOccurrences.value
        val cal = _universalCalendar.value
        val yearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val monthKey = "%04d-%02d".format(Locale.US, yearMonth.year, yearMonth.monthValue)
        val budget = allBudgets.value.find { it.monthKey == monthKey }

        val snapshot = financialIntelligenceEngine.generateSnapshot(
          allTransactions = txs,
          currentYearMonth = yearMonth,
          recurringRules = rules,
          activeBudget = budget
        )
        val context = financialContextBuilder.buildContext(snapshot)
        val forecast = cashFlowForecastEngine.generateForecast(
          allTransactions = txs,
          recurringRules = rules,
          paidOccurrences = paids,
          activeBudget = budget,
          yearMonth = yearMonth
        )

        // If question is a What-If query, check if an amount can be parsed
        val simulation = if (IntentClassifier.classify(question) == com.example.pro.ai.AiIntent.WHAT_IF_QUERY) {
          val numbers = Regex("(\\d+([.,]\\d+)?)").findAll(question).mapNotNull {
            it.value.replace(",", "").toDoubleOrNull()
          }.toList()
          val amount = numbers.firstOrNull() ?: 5000.0
          val scenario = WhatIfScenario(
            type = com.example.pro.engine.whatif.ScenarioType.ONE_TIME_EXPENSE,
            change = com.example.pro.engine.whatif.ScenarioChange.OneTimeExpense(
              amount = amount,
              category = TransactionCategory.SHOPPING,
              date = LocalDate.now()
            )
          )
          whatIfSimulatorEngine.simulate(
            allTransactions = txs,
            recurringRules = rules,
            paidOccurrences = paids,
            activeBudget = budget,
            yearMonth = yearMonth,
            scenario = scenario
          )
        } else null

        val response = financialAiService.getAiExplanation(
          question = question,
          context = context,
          currencySymbol = _userProfile.value.currencySymbol,
          forecast = forecast,
          simulation = simulation
        )

        val copilotMsg = CopilotMessage(
          sender = CopilotSender.COPILOT,
          text = response.answer,
          keyFacts = response.keyFacts,
          warnings = response.warnings,
          confidence = response.confidence
        )
        _copilotMessages.value = _copilotMessages.value + copilotMsg
      } catch (e: Exception) {
        val errorMsg = CopilotMessage(
          sender = CopilotSender.COPILOT,
          text = "I encountered an issue analyzing your financial records. Please try asking again.",
          isError = true
        )
        _copilotMessages.value = _copilotMessages.value + errorMsg
      } finally {
        _isCopilotLoading.value = false
      }
    }
  }

  fun clearCopilotMessages() {
    _copilotMessages.value = listOf(
      CopilotMessage(
        sender = CopilotSender.COPILOT,
        text = "Namaste! I'm your Paisa Financial Copilot. Ask me anything about your income, expenses, forecasts, or what-if spending decisions."
      )
    )
  }
}
