package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetEntity
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.BudgetModel
import com.example.data.model.ExportPeriod
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.ui.components.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceRepository(private val context: Context) {
  private val database = AppDatabase.getDatabase(context)
  private val transactionDao = database.transactionDao()
  private val budgetDao = database.budgetDao()
  private val recurringRuleDao = database.recurringRuleDao()

  private val prefs: SharedPreferences =
    context.getSharedPreferences("paisa_user_preferences", Context.MODE_PRIVATE)

  // Reactive transaction flow
  val allTransactions: Flow<List<TransactionItem>> =
    transactionDao.getAllTransactionsFlow().map { entities ->
      entities.map { it.toModel() }
    }

  // Reactive budget flow
  val allBudgets: Flow<List<BudgetModel>> =
    budgetDao.getAllBudgetsFlow().map { entities ->
      entities.map { it.toModel() }
    }

  fun getBudgetForMonth(monthKey: String): Flow<BudgetModel?> =
    budgetDao.getBudgetForMonthFlow(monthKey).map { it?.toModel() }

  // Reactive recurring rules flow
  val allRecurringRules: Flow<List<RecurringRule>> =
    recurringRuleDao.getAllRulesFlow().map { entities ->
      entities.map { it.toModel() }
    }

  // ================= Transactions CRUD =================

  suspend fun insertTransaction(item: TransactionItem): Long = withContext(Dispatchers.IO) {
    transactionDao.insertTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun updateTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    transactionDao.updateTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun deleteTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    transactionDao.deleteTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
    transactionDao.deleteById(id)
  }

  suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
    transactionDao.clearAll()
  }

  // ================= Budgets CRUD =================

  suspend fun saveBudget(budget: BudgetModel): Long = withContext(Dispatchers.IO) {
    budgetDao.insertOrUpdateBudget(BudgetEntity.fromModel(budget))
  }

  suspend fun deleteBudgetForMonth(monthKey: String) = withContext(Dispatchers.IO) {
    budgetDao.deleteBudgetByMonth(monthKey)
  }

  // ================= Recurring Rules CRUD =================

  suspend fun insertRecurringRule(rule: RecurringRule): Long = withContext(Dispatchers.IO) {
    recurringRuleDao.insertRule(RecurringRuleEntity.fromModel(rule))
  }

  suspend fun updateRecurringRule(rule: RecurringRule) = withContext(Dispatchers.IO) {
    recurringRuleDao.updateRule(RecurringRuleEntity.fromModel(rule))
  }

  suspend fun deleteRecurringRule(id: Long) = withContext(Dispatchers.IO) {
    recurringRuleDao.deleteRuleById(id)
  }

  suspend fun toggleRecurringRule(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
    val existing = recurringRuleDao.getRuleById(id) ?: return@withContext
    recurringRuleDao.updateRule(existing.copy(isActive = isActive))
  }

  // ================= Idempotent Recurring Rule Engine =================

  /**
   * Evaluates all active recurring rules and generates missing transaction instances
   * up to the current timestamp without duplicating any records.
   * Safe across app restarts, offline periods, and long intervals.
   */
  suspend fun processDueRecurringRules(): Int = withContext(Dispatchers.IO) {
    val activeRules = recurringRuleDao.getActiveRules()
    if (activeRules.isEmpty()) return@withContext 0

    val now = Calendar.getInstance()
    val endOfToday = DateUtils.getEndOfDay(now.timeInMillis)
    var generatedCount = 0

    for (ruleEntity in activeRules) {
      val rule = ruleEntity.toModel()
      val startPoint = if (rule.lastGeneratedDate > 0) {
        getNextOccurrence(rule.lastGeneratedDate, rule.interval)
      } else {
        rule.startDate
      }

      var currentCheck = startPoint
      var latestGenerated = rule.lastGeneratedDate
      val newTransactions = mutableListOf<TransactionEntity>()

      while (currentCheck <= endOfToday && (rule.endDate == null || currentCheck <= rule.endDate)) {
        val newTx = TransactionEntity(
          title = rule.title,
          amount = rule.amount,
          type = rule.type.name,
          category = rule.category.name,
          timestamp = currentCheck,
          note = if (rule.note.isNotBlank()) rule.note else "Recurring (${rule.interval.displayName})",
          paymentMethod = rule.paymentMethod.name,
          isRecurring = true,
          recurringRuleId = rule.id
        )
        newTransactions.add(newTx)
        latestGenerated = currentCheck
        currentCheck = getNextOccurrence(currentCheck, rule.interval)
      }

      if (newTransactions.isNotEmpty()) {
        transactionDao.insertAll(newTransactions)
        recurringRuleDao.updateRule(ruleEntity.copy(lastGeneratedDate = latestGenerated))
        generatedCount += newTransactions.size
      }
    }

    generatedCount
  }

  private fun getNextOccurrence(currentDateMillis: Long, interval: RecurrenceInterval): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = currentDateMillis
    when (interval) {
      RecurrenceInterval.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
      RecurrenceInterval.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
      RecurrenceInterval.MONTHLY -> cal.add(Calendar.MONTH, 1)
      RecurrenceInterval.YEARLY -> cal.add(Calendar.YEAR, 1)
    }
    return cal.timeInMillis
  }

  // ================= User Profile & Preferences =================

  fun getUserProfile(): UserProfile {
    val name = prefs.getString("user_name", "") ?: ""
    val completed = prefs.getBoolean("has_completed_onboarding", false)

    return UserProfile(
      name = name,
      hasCompletedOnboarding = completed
    )
  }

  fun saveUserProfile(profile: UserProfile) {
    prefs.edit()
      .putString("user_name", profile.name)
      .putBoolean("has_completed_onboarding", profile.hasCompletedOnboarding)
      .apply()
  }

  // ================= PDF Report Export =================

  suspend fun exportToPdf(
    period: ExportPeriod,
    selectedCalendar: Calendar,
    customStart: Long? = null,
    customEnd: Long? = null
  ): File = withContext(Dispatchers.IO) {
    val (transactions, periodLabel) = when (period) {
      ExportPeriod.CURRENT_MONTH -> {
        val start = DateUtils.getStartOfMonth(selectedCalendar)
        val end = DateUtils.getEndOfMonth(selectedCalendar)
        val list = transactionDao.getTransactionsBetween(start, end)
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedCalendar.time)
        Pair(list, label)
      }
      ExportPeriod.SELECTED_YEAR -> {
        val year = selectedCalendar.get(Calendar.YEAR)
        val start = DateUtils.getStartOfYear(year)
        val end = DateUtils.getEndOfYear(year)
        val list = transactionDao.getTransactionsBetween(start, end)
        Pair(list, "Full Year $year")
      }
      ExportPeriod.CUSTOM_RANGE -> {
        val start = customStart ?: 0L
        val end = customEnd ?: Long.MAX_VALUE
        val list = transactionDao.getTransactionsBetween(start, end)
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startStr = if (customStart != null) df.format(Date(customStart)) else "Beginning"
        val endStr = if (customEnd != null) df.format(Date(customEnd)) else "Present"
        Pair(list, "$startStr to $endStr")
      }
      ExportPeriod.ALL_TIME -> {
        val list = transactionDao.getAllTransactions()
        Pair(list, "All Time Records")
      }
    }

    val profile = getUserProfile()
    val modelList = transactions.map { it.toModel() }

    com.example.util.PdfReportGenerator.generateReport(
      context = context,
      userName = profile.name,
      periodLabel = periodLabel,
      transactions = modelList,
      currencySymbol = profile.currencySymbol
    )
  }
}

