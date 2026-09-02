package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetEntity
import com.example.data.local.PaidRecurringOccurrenceEntity
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.SecureNoteEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.BudgetModel
import com.example.data.model.ExportPeriod
import com.example.data.model.OccurrenceStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.ScheduledRecurringOccurrence
import com.example.data.model.SecureNote
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
  private val paidOccurrenceDao = database.paidRecurringOccurrenceDao()
  private val secureNoteDao = database.secureNoteDao()

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

  // Reactive paid occurrences flow
  val allPaidOccurrences: Flow<List<PaidRecurringOccurrenceEntity>> =
    paidOccurrenceDao.getAllPaidOccurrencesFlow()

  // Reactive secure notes flow
  val allSecureNotes: Flow<List<SecureNote>> =
    secureNoteDao.getAllNotesFlow().map { entities ->
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

  suspend fun deleteTransactionsByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
    for (id in ids) {
      transactionDao.deleteById(id)
    }
  }

  suspend fun deleteTransactionsBetween(startTime: Long, endTime: Long): Int = withContext(Dispatchers.IO) {
    val txs = transactionDao.getTransactionsBetween(startTime, endTime)
    for (tx in txs) {
      transactionDao.deleteById(tx.id)
    }
    txs.size
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
    paidOccurrenceDao.deleteOccurrencesByRule(id)
  }

  suspend fun toggleRecurringRule(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
    val existing = recurringRuleDao.getRuleById(id) ?: return@withContext
    recurringRuleDao.updateRule(existing.copy(isActive = isActive))
  }

  // ================= Scheduled Occurrences & Explicit Pay Flow =================

  /**
   * Generates scheduled occurrences for all active recurring rules from 60 days in the past
   * up to 60 days into the future.
   * Does NOT auto-create transactions.
   */
  suspend fun getScheduledOccurrences(): List<ScheduledRecurringOccurrence> = withContext(Dispatchers.IO) {
    val activeRules = recurringRuleDao.getActiveRules().map { it.toModel() }
    if (activeRules.isEmpty()) return@withContext emptyList()

    val paidList = paidOccurrenceDao.getAllPaidOccurrences()
    val paidMap = paidList.associateBy { "${it.ruleId}_${it.occurrenceDate}" }

    val calNow = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val todayMillis = calNow.timeInMillis
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayKey = sdf.format(Date(todayMillis))

    val windowStartCal = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -60) }
    val windowEndCal = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 60) }
    val windowStartMillis = windowStartCal.timeInMillis
    val windowEndMillis = windowEndCal.timeInMillis

    val occurrences = mutableListOf<ScheduledRecurringOccurrence>()

    for (rule in activeRules) {
      var currentOccurrence = rule.startDate
      // Advance to reasonable start if rule started long ago
      while (currentOccurrence < windowStartMillis && (rule.endDate == null || currentOccurrence <= rule.endDate)) {
        currentOccurrence = getNextOccurrence(currentOccurrence, rule.interval)
      }

      while (currentOccurrence <= windowEndMillis && (rule.endDate == null || currentOccurrence <= rule.endDate)) {
        val occKey = sdf.format(Date(currentOccurrence))
        val paidRecord = paidMap["${rule.id}_$occKey"]
        val isPaid = paidRecord != null

        val occCal = Calendar.getInstance().apply {
          timeInMillis = currentOccurrence
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        val occMidnight = occCal.timeInMillis
        val daysDiff = ((occMidnight - todayMillis) / (24 * 60 * 60 * 1000)).toInt()

        val status = when {
          isPaid -> OccurrenceStatus.PAID
          daysDiff == 0 -> OccurrenceStatus.DUE_TODAY
          daysDiff < 0 -> OccurrenceStatus.OVERDUE
          else -> OccurrenceStatus.UPCOMING
        }

        val relativeLabel = when {
          isPaid -> "Paid on ${sdf.format(Date(paidRecord.paidAt))}"
          daysDiff == 0 -> "Due today"
          daysDiff == -1 -> "Due yesterday"
          daysDiff < -1 -> "Due ${-daysDiff} days ago"
          daysDiff == 1 -> "Tomorrow"
          daysDiff in 2..6 -> "In $daysDiff days"
          else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(currentOccurrence))
        }

        occurrences.add(
          ScheduledRecurringOccurrence(
            ruleId = rule.id,
            ruleTitle = rule.title,
            amount = rule.amount,
            type = rule.type,
            category = rule.category,
            interval = rule.interval,
            paymentMethod = rule.paymentMethod,
            note = rule.note,
            scheduledDateKey = occKey,
            scheduledEpochMillis = currentOccurrence,
            status = status,
            daysDiff = daysDiff,
            relativeLabel = relativeLabel,
            isPaid = isPaid,
            paidTransactionId = paidRecord?.transactionId
          )
        )

        currentOccurrence = getNextOccurrence(currentOccurrence, rule.interval)
      }
    }

    occurrences.sortedWith(compareBy<ScheduledRecurringOccurrence> { it.daysDiff }.thenBy { it.ruleTitle })
  }

  /**
   * Explicitly marks a scheduled recurring occurrence as paid.
   * Creates the real transaction and records the occurrence as paid to prevent duplicate charges.
   */
  suspend fun markRecurringOccurrenceAsPaid(
    occurrence: ScheduledRecurringOccurrence
  ): TransactionItem = withContext(Dispatchers.IO) {
    // Check if already paid
    if (paidOccurrenceDao.isOccurrencePaid(occurrence.ruleId, occurrence.scheduledDateKey)) {
      val existingPaid = paidOccurrenceDao.getPaidOccurrencesByRule(occurrence.ruleId)
        .firstOrNull { it.occurrenceDate == occurrence.scheduledDateKey }
      if (existingPaid != null) {
        val tx = transactionDao.getTransactionById(existingPaid.transactionId)
        if (tx != null) return@withContext tx.toModel()
      }
    }

    // Create the actual transaction with the scheduled date
    val newTxEntity = TransactionEntity(
      title = occurrence.ruleTitle,
      amount = occurrence.amount,
      type = occurrence.type.name,
      category = occurrence.category.name,
      timestamp = occurrence.scheduledEpochMillis,
      note = if (occurrence.note.isNotBlank()) occurrence.note else "Recurring (${occurrence.interval.displayName})",
      paymentMethod = occurrence.paymentMethod.name,
      isRecurring = true,
      recurringRuleId = occurrence.ruleId
    )

    val txId = transactionDao.insertTransaction(newTxEntity)

    // Record the occurrence as paid
    paidOccurrenceDao.markOccurrencePaid(
      PaidRecurringOccurrenceEntity(
        ruleId = occurrence.ruleId,
        occurrenceDate = occurrence.scheduledDateKey,
        transactionId = txId,
        paidAt = System.currentTimeMillis()
      )
    )

    // Update rule's lastGeneratedDate
    val ruleEntity = recurringRuleDao.getRuleById(occurrence.ruleId)
    if (ruleEntity != null && occurrence.scheduledEpochMillis > ruleEntity.lastGeneratedDate) {
      recurringRuleDao.updateRule(ruleEntity.copy(lastGeneratedDate = occurrence.scheduledEpochMillis))
    }

    newTxEntity.copy(id = txId).toModel()
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

  // ================= Secure Notes CRUD =================

  suspend fun insertSecureNote(note: SecureNote): Long = withContext(Dispatchers.IO) {
    secureNoteDao.insertNote(SecureNoteEntity.fromModel(note))
  }

  suspend fun updateSecureNote(note: SecureNote) = withContext(Dispatchers.IO) {
    secureNoteDao.updateNote(SecureNoteEntity.fromModel(note))
  }

  suspend fun deleteSecureNote(id: Long) = withContext(Dispatchers.IO) {
    secureNoteDao.deleteNoteById(id)
  }

  suspend fun getAllSecureNotes(): List<SecureNote> = withContext(Dispatchers.IO) {
    secureNoteDao.getAllNotes().map { it.toModel() }
  }

  // ================= Cloud Cache Synchronization =================

  suspend fun replaceCacheWithCloudData(
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>,
    paidOccurrences: List<PaidRecurringOccurrenceEntity> = emptyList(),
    secureNotes: List<SecureNote> = emptyList()
  ) = withContext(Dispatchers.IO) {
    transactionDao.clearAll()
    if (transactions.isNotEmpty()) {
      transactionDao.insertAll(transactions.map { TransactionEntity.fromModel(it) })
    }

    budgetDao.clearAll()
    if (budgets.isNotEmpty()) {
      budgetDao.insertAll(budgets.map { BudgetEntity.fromModel(it) })
    }

    recurringRuleDao.clearAll()
    if (recurringRules.isNotEmpty()) {
      recurringRuleDao.insertAll(recurringRules.map { RecurringRuleEntity.fromModel(it) })
    }

    if (paidOccurrences.isNotEmpty()) {
      paidOccurrenceDao.clearAll()
      paidOccurrenceDao.insertAll(paidOccurrences)
    }

    if (secureNotes.isNotEmpty()) {
      secureNoteDao.clearAll()
      secureNoteDao.insertAll(secureNotes.map { SecureNoteEntity.fromModel(it) })
    }
  }

  suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
    transactionDao.clearAll()
    budgetDao.clearAll()
    recurringRuleDao.clearAll()
    paidOccurrenceDao.clearAll()
    secureNoteDao.clearAll()
  }

  suspend fun clearAllData() = withContext(Dispatchers.IO) {
    clearLocalCache()
    clearUserProfile()
  }

  suspend fun getAllLocalData(): LocalDataBundle = withContext(Dispatchers.IO) {
    val txs = transactionDao.getAllTransactions().map { it.toModel() }
    val bgs = budgetDao.getAllBudgets().map { it.toModel() }
    val rcs = recurringRuleDao.getAllRules().map { it.toModel() }
    val pds = paidOccurrenceDao.getAllPaidOccurrences()
    val nts = secureNoteDao.getAllNotes().map { it.toModel() }
    LocalDataBundle(txs, bgs, rcs, pds, nts)
  }

  // ================= User Profile & Preferences =================

  fun getUserProfile(): UserProfile {
    val name = prefs.getString("user_name", "") ?: ""
    val email = prefs.getString("user_email", "") ?: ""
    val photoUrl = prefs.getString("user_photo_url", null)
    val googleId = prefs.getString("user_google_id", null)
    val completed = prefs.getBoolean("has_completed_onboarding", false)

    return UserProfile(
      name = name,
      email = email,
      photoUrl = photoUrl,
      googleId = googleId,
      hasCompletedOnboarding = completed
    )
  }

  fun saveUserProfile(profile: UserProfile) {
    prefs.edit()
      .putString("user_name", profile.name)
      .putString("user_email", profile.email)
      .putString("user_photo_url", profile.photoUrl)
      .putString("user_google_id", profile.googleId)
      .putBoolean("has_completed_onboarding", profile.hasCompletedOnboarding)
      .apply()
  }

  fun clearUserProfile() {
    prefs.edit().clear().apply()
  }

  // ================= PDF Report Export =================

  suspend fun exportToPdf(
    period: ExportPeriod,
    selectedCalendar: Calendar,
    customStart: Long? = null,
    customEnd: Long? = null,
    specificYear: Int? = null,
    specificMonthCalendar: Calendar? = null
  ): File = withContext(Dispatchers.IO) {
    val (transactions, periodLabel) = when (period) {
      ExportPeriod.CURRENT_MONTH -> {
        val targetCal = specificMonthCalendar ?: selectedCalendar
        val start = DateUtils.getStartOfMonth(targetCal)
        val end = DateUtils.getEndOfMonth(targetCal)
        val list = transactionDao.getTransactionsBetween(start, end)
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(targetCal.time)
        Pair(list, label)
      }
      ExportPeriod.SELECTED_YEAR -> {
        val year = specificYear ?: selectedCalendar.get(Calendar.YEAR)
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

data class LocalDataBundle(
  val transactions: List<TransactionItem>,
  val budgets: List<BudgetModel>,
  val recurringRules: List<RecurringRule>,
  val paidOccurrences: List<PaidRecurringOccurrenceEntity> = emptyList(),
  val secureNotes: List<SecureNote> = emptyList()
)
