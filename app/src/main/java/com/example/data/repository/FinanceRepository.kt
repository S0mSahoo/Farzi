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
import com.example.data.model.LocalDataDump
import com.example.data.model.OccurrenceStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringOccurrence
import com.example.data.model.RecurringRule
import com.example.data.model.SecureNoteItem
import com.example.data.model.SecureNoteType
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.ui.components.DateUtils
import com.example.util.CryptoManager
import com.example.util.NotificationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FinanceRepository(private val context: Context) {
  private val database = AppDatabase.getDatabase(context)
  private val transactionDao = database.transactionDao()
  private val budgetDao = database.budgetDao()
  private val recurringRuleDao = database.recurringRuleDao()
  private val secureNoteDao = database.secureNoteDao()
  private val paidRecurringOccurrenceDao = database.paidRecurringOccurrenceDao()

  private val prefs: SharedPreferences =
    context.getSharedPreferences("paisa_user_preferences", Context.MODE_PRIVATE)

  private val localKey = "paisa_vault_local_key_v1"

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
    paidRecurringOccurrenceDao.getAllPaidOccurrencesFlow()

  // Reactive secure notes flow
  val allSecureNotes: Flow<List<SecureNoteItem>> =
    secureNoteDao.getAllNotesFlow().map { entities ->
      entities.mapNotNull { entity -> decryptNoteEntity(entity) }
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

  // ================= Bulk Delete =================

  suspend fun deleteTransactionsByIds(ids: List<Long>): Int = withContext(Dispatchers.IO) {
    if (ids.isEmpty()) return@withContext 0
    transactionDao.deleteByIds(ids)
  }

  suspend fun deleteTransactionsBetween(startTime: Long, endTime: Long): Int = withContext(Dispatchers.IO) {
    transactionDao.deleteBetween(startTime, endTime)
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
    paidRecurringOccurrenceDao.deleteForRule(id)
  }

  suspend fun toggleRecurringRule(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
    val existing = recurringRuleDao.getRuleById(id) ?: return@withContext
    recurringRuleDao.updateRule(existing.copy(isActive = isActive))
  }

  // ================= Scheduled Recurring Payments Logic =================

  /**
   * Evaluates active recurring rules and triggers due payment notifications.
   * CRITICAL: Recurring rules DO NOT automatically create transactions.
   * Transactions are only created when the user explicitly taps "Mark as Paid".
   */
  suspend fun processDueRecurringRules(): Int = withContext(Dispatchers.IO) {
    val activeRules = recurringRuleDao.getActiveRules()
    if (activeRules.isEmpty()) return@withContext 0

    val now = Calendar.getInstance()
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
    val startOfToday = DateUtils.getStartOfDay(now.timeInMillis)
    val endOfToday = DateUtils.getEndOfDay(now.timeInMillis)

    var dueTodayCount = 0
    for (ruleEntity in activeRules) {
      val rule = ruleEntity.toModel()
      val occurrences = computeRuleOccurrencesInWindow(
        rule = rule,
        windowStart = startOfToday - (30L * 86400000L),
        windowEnd = endOfToday + (30L * 86400000L)
      )

      for (occTime in occurrences) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(occTime))
        val isPaid = paidRecurringOccurrenceDao.getPaidOccurrence(rule.id, dateKey) != null
        if (!isPaid && dateKey == todayKey) {
          dueTodayCount++
          val notifId = (rule.id * 1000 + now.get(Calendar.DAY_OF_YEAR)).toInt()
          NotificationHelper.showPaymentDueReminder(
            context = context,
            notificationId = notifId,
            title = rule.title,
            amountFormatted = "₹${rule.amount.toLong()}"
          )
        }
      }
    }

    dueTodayCount
  }

  /**
   * Marks a scheduled recurring occurrence as paid.
   * ONLY here is an actual transaction created in the database and counted towards expenses/budgets.
   */
  suspend fun markRecurringPaymentPaid(rule: RecurringRule, occurrenceTimestamp: Long): Long = withContext(Dispatchers.IO) {
    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(occurrenceTimestamp))
    val existingPaid = paidRecurringOccurrenceDao.getPaidOccurrence(rule.id, dateKey)
    if (existingPaid != null && !existingPaid.isCancelled && existingPaid.paidTransactionId > 0) {
      return@withContext existingPaid.paidTransactionId
    }

    val newTx = TransactionEntity(
      title = rule.title,
      amount = rule.amount,
      type = rule.type.name,
      category = rule.category.name,
      timestamp = occurrenceTimestamp,
      note = if (rule.note.isNotBlank()) rule.note else "Recurring (${rule.interval.displayName})",
      paymentMethod = rule.paymentMethod.name,
      isRecurring = true,
      recurringRuleId = rule.id
    )

    val txId = transactionDao.insertTransaction(newTx)
    paidRecurringOccurrenceDao.markPaid(
      PaidRecurringOccurrenceEntity(
        ruleId = rule.id,
        occurrenceDateKey = dateKey,
        paidTransactionId = txId,
        paidTimestamp = System.currentTimeMillis(),
        isCancelled = false
      )
    )
    txId
  }

  suspend fun cancelRecurringOccurrence(ruleId: Long, dateKey: String) = withContext(Dispatchers.IO) {
    paidRecurringOccurrenceDao.markPaid(
      PaidRecurringOccurrenceEntity(
        ruleId = ruleId,
        occurrenceDateKey = dateKey,
        paidTransactionId = -1L,
        paidTimestamp = System.currentTimeMillis(),
        isCancelled = true
      )
    )
  }

  suspend fun restoreRecurringOccurrence(ruleId: Long, dateKey: String) = withContext(Dispatchers.IO) {
    paidRecurringOccurrenceDao.unmarkPaid(ruleId, dateKey)
  }

  fun computeRuleOccurrencesInWindow(
    rule: RecurringRule,
    windowStart: Long,
    windowEnd: Long
  ): List<Long> {
    val results = mutableListOf<Long>()
    var current = rule.startDate
    val maxLookahead = windowEnd

    while (current <= maxLookahead && (rule.endDate == null || current <= rule.endDate)) {
      if (current >= windowStart) {
        results.add(current)
      }
      current = getNextOccurrence(current, rule.interval)
      if (results.size > 200) break
    }
    return results
  }

  fun getNextOccurrence(currentDateMillis: Long, interval: RecurrenceInterval): Long {
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

  // ================= Secure Vault / Private Notes CRUD =================

  suspend fun insertSecureNote(note: SecureNoteItem): Long = withContext(Dispatchers.IO) {
    val jsonPayload = JSONObject().apply {
      put("notes", note.notes)
      put("bankName", note.bankName)
      put("accountNumber", note.accountNumber)
      put("ifscCode", note.ifscCode)
      put("holderName", note.holderName)
      put("cardNumber", note.cardNumber)
      put("cardExpiry", note.cardExpiry)
      put("cardCvv", note.cardCvv)
      put("username", note.username)
      put("passwordSecret", note.passwordSecret)
    }.toString()

    val (ciphertext, iv) = CryptoManager.encryptLocal(jsonPayload, localKey)
    val entity = SecureNoteEntity(
      id = note.id,
      title = note.title,
      type = note.type.name,
      encryptedContent = ciphertext,
      iv = iv,
      updatedAt = System.currentTimeMillis()
    )
    secureNoteDao.insertNote(entity)
  }

  suspend fun updateSecureNote(note: SecureNoteItem) = withContext(Dispatchers.IO) {
    insertSecureNote(note)
  }

  suspend fun deleteSecureNote(id: Long) = withContext(Dispatchers.IO) {
    secureNoteDao.deleteById(id)
  }

  private fun decryptNoteEntity(entity: SecureNoteEntity): SecureNoteItem? {
    return try {
      val plainJson = CryptoManager.decryptLocal(entity.encryptedContent, entity.iv, localKey)
      val json = JSONObject(plainJson)
      val noteType = try { SecureNoteType.valueOf(entity.type) } catch (e: Exception) { SecureNoteType.GENERAL_NOTE }

      SecureNoteItem(
        id = entity.id,
        title = entity.title,
        type = noteType,
        notes = json.optString("notes", ""),
        bankName = json.optString("bankName", ""),
        accountNumber = json.optString("accountNumber", ""),
        ifscCode = json.optString("ifscCode", ""),
        holderName = json.optString("holderName", ""),
        cardNumber = json.optString("cardNumber", ""),
        cardExpiry = json.optString("cardExpiry", ""),
        cardCvv = json.optString("cardCvv", ""),
        username = json.optString("username", ""),
        passwordSecret = json.optString("passwordSecret", ""),
        updatedAt = entity.updatedAt
      )
    } catch (e: Exception) {
      null
    }
  }

  // ================= Cloud Cache Synchronization =================

  suspend fun replaceCacheWithCloudData(
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>,
    secureNotes: List<SecureNoteItem> = emptyList(),
    paidOccurrences: List<PaidRecurringOccurrenceEntity> = emptyList()
  ) = withContext(Dispatchers.IO) {
    // Atomically replace local tables with cloud authoritative data
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

    paidRecurringOccurrenceDao.clearAll()
    if (paidOccurrences.isNotEmpty()) {
      paidRecurringOccurrenceDao.insertAll(paidOccurrences)
    }

    if (secureNotes.isNotEmpty()) {
      secureNoteDao.clearAll()
      for (sn in secureNotes) {
        insertSecureNote(sn)
      }
    }
  }

  suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
    transactionDao.clearAll()
    budgetDao.clearAll()
    recurringRuleDao.clearAll()
    paidRecurringOccurrenceDao.clearAll()
    secureNoteDao.clearAll()
  }

  suspend fun getAllLocalData(): LocalDataDump = withContext(Dispatchers.IO) {
    val txs = transactionDao.getAllTransactions().map { it.toModel() }
    val bgs = budgetDao.getAllBudgets().map { it.toModel() }
    val rcs = recurringRuleDao.getAllRules().map { it.toModel() }
    val paid = paidRecurringOccurrenceDao.getAllPaidOccurrences()
    val notes = secureNoteDao.getAllNotes().mapNotNull { decryptNoteEntity(it) }
    LocalDataDump(txs, bgs, rcs, notes, paid)
  }

  // ================= Theme & Security Preferences =================

  fun getThemeMode(): ThemeMode {
    val raw = prefs.getString("paisa_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
    return try { ThemeMode.valueOf(raw) } catch (e: Exception) { ThemeMode.SYSTEM }
  }

  fun saveThemeMode(mode: ThemeMode) {
    prefs.edit().putString("paisa_theme_mode", mode.name).apply()
  }

  fun isAppLockEnabled(): Boolean {
    return prefs.getBoolean("paisa_app_lock_enabled", false)
  }

  fun setAppLockEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("paisa_app_lock_enabled", enabled).apply()
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

