package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.drive.ConsentRequiredException
import com.example.data.drive.GoogleDriveBackupService
import com.example.data.drive.GoogleDriveState
import com.example.data.model.BudgetModel
import com.example.data.model.CalendarDayData
import com.example.data.model.CategoryDetailData
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpendingPoint
import com.example.data.model.DriveStorageInfo
import com.example.data.model.ExportPeriod
import com.example.data.model.FinancialRecommendation
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.OccurrenceStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.ScheduledRecurringOccurrence
import com.example.data.model.SecureNote
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.YearlyFinancialSummary
import com.example.data.repository.FinanceRepository
import com.example.ui.components.DateUtils
import com.example.util.JsonPortabilityManager
import com.example.util.JsonValidationResult
import com.example.util.NotificationHelper
import com.example.util.PaisaJsonBackup
import com.example.util.RecommendationEngine
import com.example.util.SecurityManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FinanceRepository(application.applicationContext)
  val driveService = GoogleDriveBackupService(application.applicationContext)
  val securityManager = SecurityManager(application.applicationContext)

  // ---------------- Theme Management ----------------
  private val _themeMode = MutableStateFlow(
    try {
      ThemeMode.valueOf(securityManager.getThemeMode())
    } catch (e: Exception) {
      ThemeMode.SYSTEM
    }
  )
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    securityManager.setThemeMode(mode.name)
  }

  // ---------------- App Lock & Biometrics ----------------
  private val _isAppLockEnabled = MutableStateFlow(securityManager.isAppLockEnabled())
  val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()
  val isAppLockConfigured: StateFlow<Boolean> = _isAppLockEnabled

  // If App Lock is enabled, start locked; otherwise unlocked
  private val _isAppLocked = MutableStateFlow(securityManager.isAppLockEnabled())
  val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

  // Secure Vault Lock State
  private val _isVaultUnlocked = MutableStateFlow(false)
  val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

  fun setAppLockEnabled(enabled: Boolean) {
    _isAppLockEnabled.value = enabled
    securityManager.setAppLockEnabled(enabled)
    if (!enabled) {
      _isAppLocked.value = false
    }
  }

  fun setAppLockEnabled(activity: FragmentActivity, enabled: Boolean) {
    if (enabled) {
      viewModelScope.launch {
        val success = securityManager.authenticate(
          activity = activity,
          title = "Enable App Lock",
          subtitle = "Authenticate to confirm biometric security"
        )
        if (success) {
          setAppLockEnabled(true)
        }
      }
    } else {
      setAppLockEnabled(false)
    }
  }

  fun authenticateAndUnlockApp(activity: FragmentActivity, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val success = securityManager.authenticate(
        activity = activity,
        title = "Unlock Paisa",
        subtitle = "Confirm biometric credential or device passcode to access your finances"
      )
      if (success) {
        _isAppLocked.value = false
      }
      onResult(success)
    }
  }

  fun authenticateForExport(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    if (!_isAppLockEnabled.value && !securityManager.canAuthenticateWithBiometrics()) {
      onResult(true)
      return
    }
    viewModelScope.launch {
      val success = securityManager.authenticate(
        activity = activity,
        title = "Confirm Export",
        subtitle = "Authenticate to generate financial export"
      )
      onResult(success)
    }
  }

  fun authenticateVaultAccess(activity: FragmentActivity, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val success = securityManager.authenticate(
        activity = activity,
        title = "Private Vault Access",
        subtitle = "Authenticate to view and manage encrypted sensitive notes"
      )
      if (success) {
        _isVaultUnlocked.value = true
      }
      onResult(success)
    }
  }

  fun lockVault() {
    _isVaultUnlocked.value = false
  }

  // ---------------- Google Drive & Cloud Sync ----------------
  private val _googleDriveState = MutableStateFlow<GoogleDriveState>(GoogleDriveState.NotConnected)
  val googleDriveState: StateFlow<GoogleDriveState> = _googleDriveState.asStateFlow()

  private val _driveStorageInfo = MutableStateFlow(DriveStorageInfo())
  val driveStorageInfo: StateFlow<DriveStorageInfo> = _driveStorageInfo.asStateFlow()
  val driveStorageUsageBytes: StateFlow<Long> = _driveStorageInfo.map { it.usedBytes }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    0L
  )

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _lastSyncTimestamp = MutableStateFlow<Long?>(driveService.getLastBackupTimestamp())
  val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

  private val _syncErrorMessage = MutableStateFlow<String?>(null)
  val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

  private val _syncStatusToast = MutableStateFlow<String?>(null)
  val syncStatusToast: StateFlow<String?> = _syncStatusToast.asStateFlow()

  fun clearSyncStatusToast() {
    _syncStatusToast.value = null
  }

  private val _driveConsentIntent = MutableStateFlow<Intent?>(null)
  val driveConsentIntent: StateFlow<Intent?> = _driveConsentIntent.asStateFlow()

  fun clearConsentIntent() {
    _driveConsentIntent.value = null
  }

  fun getDriveSignInIntent(): Intent {
    return driveService.getSignInIntent()
  }

  // ---------------- Universal Month/Year Period Selection ----------------
  private val _selectedCalendar = MutableStateFlow(Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 12)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  })
  val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

  // Aliases for screen compatibility
  val dashboardCalendar: StateFlow<Calendar> = _selectedCalendar
  val calendarMonth: StateFlow<Calendar> = _selectedCalendar
  val budgetCalendar: StateFlow<Calendar> = _selectedCalendar

  private val _calendarSelectedDayMillis = MutableStateFlow(System.currentTimeMillis())
  val calendarSelectedDayMillis: StateFlow<Long> = _calendarSelectedDayMillis.asStateFlow()
  val selectedDayTimestamp: StateFlow<Long> = _calendarSelectedDayMillis

  fun setCalendarSelectedDay(millis: Long) {
    _calendarSelectedDayMillis.value = millis
  }

  fun setSelectedMonth(year: Int, monthIndex: Int) {
    val updated = (_selectedCalendar.value.clone() as Calendar).apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthIndex)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    _selectedCalendar.value = updated
  }

  fun setBudgetMonthAndYear(year: Int, monthIndex: Int) {
    setSelectedMonth(year, monthIndex)
  }

  fun nextMonth() {
    val updated = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.MONTH, 1)
    }
    _selectedCalendar.value = updated
  }

  fun previousMonth() {
    val updated = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.MONTH, -1)
    }
    _selectedCalendar.value = updated
  }

  fun nextBudgetMonth() = nextMonth()
  fun previousBudgetMonth() = previousMonth()

  fun nextYear() {
    val updated = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.YEAR, 1)
    }
    _selectedCalendar.value = updated
  }

  fun previousYear() {
    val updated = (_selectedCalendar.value.clone() as Calendar).apply {
      add(Calendar.YEAR, -1)
    }
    _selectedCalendar.value = updated
  }

  // User Profile
  private val _userProfile = MutableStateFlow(repository.getUserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // Search & Filter in Transactions Screen
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _filterType = MutableStateFlow<TransactionType?>(null)
  val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

  private val _filterCategory = MutableStateFlow<TransactionCategory?>(null)
  val filterCategory: StateFlow<TransactionCategory?> = _filterCategory.asStateFlow()

  fun setSearchQuery(q: String) { _searchQuery.value = q }
  fun setFilterType(t: TransactionType?) { _filterType.value = t }
  fun setFilterCategory(c: TransactionCategory?) { _filterCategory.value = c }

  // ---------------- Base Database Collections ----------------
  val allTransactions: StateFlow<List<TransactionItem>> = repository.allTransactions.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
    allTransactions,
    _searchQuery,
    _filterType,
    _filterCategory
  ) { list, query, type, category ->
    list.filter { item ->
      val matchesQuery = if (query.isBlank()) true else {
        item.title.contains(query, ignoreCase = true) ||
          item.note.contains(query, ignoreCase = true) ||
          item.amount.toString().contains(query)
      }
      val matchesType = type == null || item.type == type
      val matchesCat = category == null || item.category == category
      matchesQuery && matchesType && matchesCat
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allBudgets: StateFlow<List<BudgetModel>> = repository.allBudgets.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allRecurringRules: StateFlow<List<RecurringRule>> = repository.allRecurringRules.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allSecureNotes: StateFlow<List<SecureNote>> = repository.allSecureNotes.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // ---------------- Scheduled Recurring Occurrences Flow ----------------
  private val _scheduledOccurrences = MutableStateFlow<List<ScheduledRecurringOccurrence>>(emptyList())
  val scheduledOccurrences: StateFlow<List<ScheduledRecurringOccurrence>> = _scheduledOccurrences.asStateFlow()

  fun refreshScheduledOccurrences() {
    viewModelScope.launch {
      _scheduledOccurrences.value = repository.getScheduledOccurrences()
    }
  }

  val dueTodayOccurrences: StateFlow<List<ScheduledRecurringOccurrence>> = _scheduledOccurrences.combine(allTransactions) { list, _ ->
    list.filter { it.status == OccurrenceStatus.DUE_TODAY && !it.isPaid }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val overdueOccurrences: StateFlow<List<ScheduledRecurringOccurrence>> = _scheduledOccurrences.combine(allTransactions) { list, _ ->
    list.filter { it.status == OccurrenceStatus.OVERDUE && !it.isPaid }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val upcomingOccurrences: StateFlow<List<ScheduledRecurringOccurrence>> = _scheduledOccurrences.combine(allTransactions) { list, _ ->
    list.filter { it.status == OccurrenceStatus.UPCOMING && !it.isPaid }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val paidOccurrences: StateFlow<List<ScheduledRecurringOccurrence>> = _scheduledOccurrences.combine(allTransactions) { list, _ ->
    list.filter { it.isPaid }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // ---------------- Universal Monthly & Yearly Summaries ----------------
  val dashboardMonthSummary: StateFlow<MonthlyFinancialSummary> = combine(
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

  val budgetMonthSummary: StateFlow<MonthlyFinancialSummary> = dashboardMonthSummary

  val dashboardYearSummary: StateFlow<YearlyFinancialSummary> = combine(
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

  // ---------------- Category Detail Data for Selected Month ----------------
  fun getCategoryDetailData(category: TransactionCategory): CategoryDetailData {
    val cal = _selectedCalendar.value
    val startOfMonth = DateUtils.getStartOfMonth(cal)
    val endOfMonth = DateUtils.getEndOfMonth(cal)
    val monthKey = DateUtils.getMonthKey(cal)
    val monthLabel = DateUtils.getMonthLabel(cal)

    val txList = allTransactions.value.filter {
      it.category == category && it.timestamp in startOfMonth..endOfMonth
    }.sortedByDescending { it.timestamp }

    val totalExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    return CategoryDetailData(
      category = category,
      monthKey = monthKey,
      monthLabel = monthLabel,
      totalExpense = totalExpense,
      totalIncome = totalIncome,
      netAmount = totalIncome - totalExpense,
      transactionCount = txList.size,
      transactions = txList
    )
  }

  // ---------------- Calendar Days Flow ----------------
  val calendarDays: StateFlow<List<CalendarDayData>> = combine(
    allTransactions,
    _selectedCalendar
  ) { transactions, cal ->
    DateUtils.buildMonthCalendarDays(cal, transactions)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val selectedDayTransactions: StateFlow<List<TransactionItem>> = combine(
    allTransactions,
    _calendarSelectedDayMillis
  ) { transactions, selectedMillis ->
    val startOfDay = DateUtils.getStartOfDay(selectedMillis)
    val endOfDay = DateUtils.getEndOfDay(selectedMillis)
    transactions.filter { it.timestamp in startOfDay..endOfDay }.sortedByDescending { it.timestamp }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // ---------------- Budget Screen Flows ----------------
  val currentMonthBudget: StateFlow<BudgetModel?> = combine(
    allBudgets,
    _selectedCalendar
  ) { budgets, cal ->
    val monthKey = DateUtils.getMonthKey(cal)
    budgets.find { it.monthKey == monthKey }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = null
  )

  // ---------------- Intelligent Financial Recommendations Flow ----------------
  val intelligentRecommendations: StateFlow<List<FinancialRecommendation>> = combine(
    dashboardMonthSummary,
    allTransactions,
    currentMonthBudget,
    _scheduledOccurrences,
    _selectedCalendar
  ) { monthSummary, txs, budget, occurrences, cal ->
    RecommendationEngine.evaluate(
      currentMonthSummary = monthSummary,
      historicalTransactions = txs,
      currentBudget = budget,
      recurringOccurrences = occurrences,
      selectedCalendar = cal
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // ---------------- Lifecycle & Init ----------------
  init {
    initGoogleDriveState()
    refreshScheduledOccurrences()
    triggerAutoSyncOnResume(force = true)
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
      refreshDriveStorageQuota(account)
    } else if (!savedEmail.isNullOrBlank()) {
      _googleDriveState.value = GoogleDriveState.Connected(
        email = savedEmail,
        lastBackupTimestampMillis = lastBackup
      )
    } else {
      _googleDriveState.value = GoogleDriveState.NotConnected
    }
  }

  fun refreshDriveStorageQuota(account: GoogleSignInAccount? = driveService.getLastSignedInAccount()) {
    if (account == null) return
    viewModelScope.launch {
      val quota = driveService.fetchStorageQuota(account)
      _driveStorageInfo.value = quota
    }
  }

  private var lastAutoSyncCheckTime = 0L

  fun triggerAutoSyncOnResume(force: Boolean = false) {
    val now = System.currentTimeMillis()
    if (!force && (now - lastAutoSyncCheckTime < 25_000L)) {
      return
    }
    lastAutoSyncCheckTime = now

    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount()
      if (account != null && account.email != null) {
        pullFromDriveAndMerge(account)
        refreshDriveStorageQuota(account)
      }
      refreshScheduledOccurrences()
    }
  }

  /**
   * Manual Sync Now action: cohesive and immediate UI state transition.
   */
  fun syncNow(
    onSuccessMessage: (String) -> Unit = {},
    onErrorMessage: (String) -> Unit = {}
  ) {
    val account = driveService.getLastSignedInAccount()
    if (account == null) {
      _syncErrorMessage.value = "Google Drive account not connected."
      onErrorMessage("Google Drive account not connected.")
      return
    }

    viewModelScope.launch {
      _isSyncing.value = true
      _syncErrorMessage.value = null
      try {
        val bundle = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "4.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = bundle.transactions,
          budgets = bundle.budgets,
          recurringRules = bundle.recurringRules,
          paidOccurrences = bundle.paidOccurrences,
          secureNotes = bundle.secureNotes
        )
        val ts = driveService.saveCloudData(account, payload)
        _lastSyncTimestamp.value = ts
        _googleDriveState.value = GoogleDriveState.BackupSuccess(
          email = account.email ?: "Google Drive",
          timestampMillis = ts
        )
        _syncStatusToast.value = "Synced just now"
        refreshDriveStorageQuota(account)
        onSuccessMessage("Successfully synchronized with Google Drive!")
      } catch (e: Exception) {
        _syncErrorMessage.value = e.localizedMessage ?: "Sync failed"
        onErrorMessage(e.localizedMessage ?: "Sync failed")
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun syncCurrentStateToDrive() {
    viewModelScope.launch {
      val account = driveService.getLastSignedInAccount() ?: return@launch
      try {
        val bundle = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "4.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = bundle.transactions,
          budgets = bundle.budgets,
          recurringRules = bundle.recurringRules,
          paidOccurrences = bundle.paidOccurrences,
          secureNotes = bundle.secureNotes
        )
        val ts = driveService.saveCloudData(account, payload)
        _lastSyncTimestamp.value = ts
        _googleDriveState.value = GoogleDriveState.Connected(
          email = account.email ?: "Google Drive",
          lastBackupTimestampMillis = ts
        )
      } catch (ignored: Exception) {
        // Safe offline fallback
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
        repository.replaceCacheWithCloudData(
          transactions = cloudPayload.transactions,
          budgets = cloudPayload.budgets,
          recurringRules = cloudPayload.recurringRules,
          paidOccurrences = cloudPayload.paidOccurrences,
          secureNotes = cloudPayload.secureNotes
        )
        _lastSyncTimestamp.value = cloudPayload.exportTimestamp
        _googleDriveState.value = GoogleDriveState.Connected(
          email = account.email ?: "Google Drive",
          lastBackupTimestampMillis = cloudPayload.exportTimestamp
        )
      } else {
        val bundle = repository.getAllLocalData()
        val payload = com.example.data.drive.BackupPayload(
          version = "4.0.0",
          exportTimestamp = System.currentTimeMillis(),
          userProfile = _userProfile.value,
          transactions = bundle.transactions,
          budgets = bundle.budgets,
          recurringRules = bundle.recurringRules,
          paidOccurrences = bundle.paidOccurrences,
          secureNotes = bundle.secureNotes
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
      _syncErrorMessage.value = "Google Drive access requires permission."
      false
    } catch (e: Exception) {
      _syncErrorMessage.value = e.localizedMessage ?: "Sync error"
      false
    } finally {
      _isSyncing.value = false
    }
  }

  // ---------------- Transaction CRUD & Actions ----------------
  fun saveTransaction(
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    timestamp: Long,
    note: String = "",
    paymentMethod: PaymentMethod = PaymentMethod.UPI,
    isRecurring: Boolean = false,
    recurringRuleId: Long? = null,
    onComplete: (Long) -> Unit = {}
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
      val id = repository.insertTransaction(item)
      syncCurrentStateToDrive()
      onComplete(id)
    }
  }

  fun addTransaction(
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    timestamp: Long,
    note: String = "",
    paymentMethod: PaymentMethod = PaymentMethod.UPI,
    isRecurring: Boolean = false,
    recurringRuleId: Long? = null,
    onComplete: (Long) -> Unit = {}
  ) = saveTransaction(title, amount, type, category, timestamp, note, paymentMethod, isRecurring, recurringRuleId, onComplete)

  fun updateTransaction(item: TransactionItem, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.updateTransaction(item.copy(updatedAt = System.currentTimeMillis()))
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun deleteTransaction(item: TransactionItem, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.deleteTransaction(item)
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun bulkDeleteTransactions(ids: List<Long>, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.deleteTransactionsByIds(ids)
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun deleteTransactionsByRange(startTime: Long, endTime: Long, onComplete: (Int) -> Unit = {}) {
    viewModelScope.launch {
      val count = repository.deleteTransactionsBetween(startTime, endTime)
      syncCurrentStateToDrive()
      onComplete(count)
    }
  }

  // ---------------- Recurring Payments & "Mark as Paid" Flow ----------------
  fun markRecurringOccurrenceAsPaid(
    occurrence: ScheduledRecurringOccurrence,
    onComplete: (TransactionItem) -> Unit = {}
  ) {
    viewModelScope.launch {
      val createdTx = repository.markRecurringOccurrenceAsPaid(occurrence)
      refreshScheduledOccurrences()
      syncCurrentStateToDrive()
      onComplete(createdTx)
    }
  }

  fun saveRecurringRule(
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    interval: RecurrenceInterval,
    startDate: Long,
    endDate: Long? = null,
    paymentMethod: PaymentMethod = PaymentMethod.UPI,
    note: String = "",
    onComplete: () -> Unit = {}
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
        paymentMethod = paymentMethod,
        note = note.trim()
      )
      repository.insertRecurringRule(rule)
      refreshScheduledOccurrences()
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun addRecurringRule(
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    interval: RecurrenceInterval,
    startDate: Long,
    endDate: Long? = null,
    paymentMethod: PaymentMethod = PaymentMethod.UPI,
    note: String = "",
    onComplete: () -> Unit = {}
  ) = saveRecurringRule(title, amount, type, category, interval, startDate, endDate, paymentMethod, note, onComplete)

  fun updateRecurringRule(rule: RecurringRule, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.updateRecurringRule(rule)
      refreshScheduledOccurrences()
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun deleteRecurringRule(id: Long, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.deleteRecurringRule(id)
      refreshScheduledOccurrences()
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun toggleRecurringRule(id: Long, isActive: Boolean) {
    viewModelScope.launch {
      repository.toggleRecurringRule(id, isActive)
      refreshScheduledOccurrences()
      syncCurrentStateToDrive()
    }
  }

  fun sendPaymentReminderNotification(occurrence: ScheduledRecurringOccurrence) {
    NotificationHelper.showDuePaymentReminder(getApplication(), occurrence)
  }

  // ---------------- Budgets ----------------
  fun saveBudget(
    monthKey: String,
    totalBudget: Double,
    categoryBudgets: Map<TransactionCategory, Double>,
    onComplete: () -> Unit = {}
  ) {
    viewModelScope.launch {
      val model = BudgetModel(
        monthKey = monthKey,
        totalBudget = totalBudget,
        categoryBudgets = categoryBudgets,
        updatedAt = System.currentTimeMillis()
      )
      repository.saveBudget(model)
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun saveMonthlyBudget(
    monthKey: String,
    totalBudget: Double,
    categoryBudgets: Map<TransactionCategory, Double>,
    onComplete: () -> Unit = {}
  ) = saveBudget(monthKey, totalBudget, categoryBudgets, onComplete)

  fun deleteBudgetForMonth(monthKey: String, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.deleteBudgetForMonth(monthKey)
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun deleteBudget(monthKey: String, onComplete: () -> Unit = {}) = deleteBudgetForMonth(monthKey, onComplete)

  // ---------------- Secure Notes Vault ----------------
  fun saveSecureNote(note: SecureNote, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      if (note.id > 0) {
        repository.updateSecureNote(note.copy(updatedAt = System.currentTimeMillis()))
      } else {
        repository.insertSecureNote(note)
      }
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  fun deleteSecureNote(id: Long, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.deleteSecureNote(id)
      syncCurrentStateToDrive()
      onComplete()
    }
  }

  // ---------------- Profile & Account ----------------
  fun saveUserProfile(name: String, email: String) {
    val updated = _userProfile.value.copy(
      name = name.trim(),
      email = email.trim(),
      hasCompletedOnboarding = true
    )
    _userProfile.value = updated
    repository.saveUserProfile(updated)
  }

  fun onGoogleSignInSuccess(
    account: GoogleSignInAccount,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) = handleGoogleSignInSuccess(account, onResult)

  fun handleGoogleSignInSuccess(
    account: GoogleSignInAccount,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      try {
        val updated = _userProfile.value.copy(
          name = account.displayName ?: _userProfile.value.name.ifBlank { "User" },
          email = account.email ?: "",
          photoUrl = account.photoUrl?.toString(),
          googleId = account.id,
          hasCompletedOnboarding = true
        )
        _userProfile.value = updated
        repository.saveUserProfile(updated)

        _googleDriveState.value = GoogleDriveState.Connected(
          email = account.email ?: "Google Account",
          lastBackupTimestampMillis = driveService.getLastBackupTimestamp()
        )

        pullFromDriveAndMerge(account)
        refreshDriveStorageQuota(account)
        refreshScheduledOccurrences()
        onResult(true, null)
      } catch (e: Exception) {
        onResult(false, e.localizedMessage)
      }
    }
  }

  fun signOut(onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      driveService.signOut()
      _googleDriveState.value = GoogleDriveState.NotConnected
      _driveStorageInfo.value = DriveStorageInfo()
      onComplete()
    }
  }

  fun clearAllData(onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.clearAllData()
      syncCurrentStateToDrive()
      refreshScheduledOccurrences()
      onComplete()
    }
  }

  fun disconnectGoogleDrive() {
    viewModelScope.launch {
      driveService.signOut()
      _googleDriveState.value = GoogleDriveState.NotConnected
      _driveStorageInfo.value = DriveStorageInfo()
    }
  }

  // ---------------- PDF & JSON Export Flows ----------------
  fun exportToPdfFile(
    period: ExportPeriod,
    specificYear: Int? = null,
    specificMonthCalendar: Calendar? = null,
    customStart: Long? = null,
    customEnd: Long? = null,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val file = repository.exportToPdf(
          period = period,
          selectedCalendar = _selectedCalendar.value,
          customStart = customStart,
          customEnd = customEnd,
          specificYear = specificYear,
          specificMonthCalendar = specificMonthCalendar
        )
        onSuccess(file)
      } catch (e: Exception) {
        onError(e.localizedMessage ?: "Failed to generate PDF report.")
      }
    }
  }

  fun exportToPdf(
    context: Context,
    period: ExportPeriod,
    specificYear: Int? = null,
    specificMonthCalendar: Calendar? = null,
    customStart: Long? = null,
    customEnd: Long? = null,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    exportToPdfFile(
      period = period,
      specificYear = specificYear,
      specificMonthCalendar = specificMonthCalendar,
      customStart = customStart,
      customEnd = customEnd,
      onSuccess = { file ->
        try {
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            val uri = FileProvider.getUriForFile(
              context,
              "${context.packageName}.fileprovider",
              file
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(shareIntent, "Share PDF Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
          onSuccess("PDF Report generated and shared successfully!")
        } catch (e: Exception) {
          onSuccess("PDF saved to ${file.name}")
        }
      },
      onError = onError
    )
  }

  fun exportToJsonFile(
    period: ExportPeriod = ExportPeriod.ALL_TIME,
    specificYear: Int? = null,
    specificMonthCalendar: Calendar? = null,
    customStart: Long? = null,
    customEnd: Long? = null,
    includeSecureNotes: Boolean = false,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val bundle = repository.getAllLocalData()
        val file = JsonPortabilityManager.exportToJsonFile(
          context = getApplication(),
          profile = _userProfile.value,
          allTransactions = bundle.transactions,
          budgets = bundle.budgets,
          recurringRules = bundle.recurringRules,
          secureNotes = bundle.secureNotes,
          period = period,
          specificYear = specificYear,
          specificMonthCalendar = specificMonthCalendar,
          customStart = customStart,
          customEnd = customEnd,
          includeSecureNotes = includeSecureNotes
        )
        onSuccess(file)
      } catch (e: Exception) {
        onError(e.localizedMessage ?: "Failed to export JSON backup.")
      }
    }
  }

  fun exportJson(
    context: Context,
    period: ExportPeriod = ExportPeriod.ALL_TIME,
    specificYear: Int? = null,
    specificMonthCalendar: Calendar? = null,
    customStart: Long? = null,
    customEnd: Long? = null,
    includeSecureNotes: Boolean = false,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    exportToJsonFile(
      period = period,
      specificYear = specificYear,
      specificMonthCalendar = specificMonthCalendar,
      customStart = customStart,
      customEnd = customEnd,
      includeSecureNotes = includeSecureNotes,
      onSuccess = { file ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
          type = "application/json"
          val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
          )
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share JSON Backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        onSuccess("JSON Backup exported successfully (${(file.length() / 1024).coerceAtLeast(1)} KB)")
      },
      onError = onError
    )
  }

  suspend fun validateImportFile(context: Context, uri: Uri): JsonValidationResult {
    return JsonPortabilityManager.validateImportFile(context, uri)
  }

  suspend fun validateJsonImport(uri: Uri): JsonValidationResult {
    return JsonPortabilityManager.validateImportFile(getApplication(), uri)
  }

  fun confirmAndApplyImport(
    backup: PaisaJsonBackup,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) = applyJsonImport(backup, onSuccess, onError)

  fun applyJsonImport(
    backup: PaisaJsonBackup,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val currentBundle = repository.getAllLocalData()
        val (mergedTxs, mergedBgs, mergedRcs, mergedNts) = JsonPortabilityManager.mergeData(
          currentTransactions = currentBundle.transactions,
          currentBudgets = currentBundle.budgets,
          currentRecurringRules = currentBundle.recurringRules,
          currentSecureNotes = currentBundle.secureNotes,
          importBackup = backup
        )

        repository.replaceCacheWithCloudData(
          transactions = mergedTxs,
          budgets = mergedBgs,
          recurringRules = mergedRcs,
          paidOccurrences = currentBundle.paidOccurrences,
          secureNotes = mergedNts
        )

        refreshScheduledOccurrences()
        syncCurrentStateToDrive()

        val msg = "Successfully imported ${backup.transactions.size} transactions, ${backup.budgets.size} budgets, and ${backup.recurringTransactions.size} recurring rules."
        onSuccess(msg)
      } catch (e: Exception) {
        onError("Import failed: ${e.localizedMessage ?: "Unknown error"}")
      }
    }
  }
}
