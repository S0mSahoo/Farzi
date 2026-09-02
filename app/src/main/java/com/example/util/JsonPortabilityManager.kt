package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BudgetModel
import com.example.data.model.ExportPeriod
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.SecureNote
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.ui.components.DateUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PaisaExportAccount(
  val name: String = "",
  val email: String = ""
)

data class PaisaJsonBackup(
  val schemaVersion: Int = 2,
  val appName: String = "Paisa",
  val version: String = "4.0.0",
  val exportedAt: String = "",
  val exportTimestamp: Long = 0L,
  val exportRange: String = "All Data",
  val account: PaisaExportAccount = PaisaExportAccount(),
  val transactions: List<TransactionItem> = emptyList(),
  val budgets: List<BudgetModel> = emptyList(),
  val recurringTransactions: List<RecurringRule> = emptyList(),
  val secureNotes: List<SecureNote> = emptyList()
)

data class ValidationSummary(
  val transactionCount: Int,
  val budgetCount: Int,
  val recurringCount: Int,
  val secureNoteCount: Int,
  val accountName: String,
  val accountEmail: String,
  val exportDate: String
)

sealed class JsonValidationResult {
  data class Success(val backup: PaisaJsonBackup, val summary: ValidationSummary) : JsonValidationResult()
  data class Error(val message: String) : JsonValidationResult()
}

object JsonPortabilityManager {

  private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val adapter = moshi.adapter(PaisaJsonBackup::class.java).indent("  ")

  /**
   * Generates a clean, schema-versioned JSON export of Paisa records based on range and options.
   */
  suspend fun exportToJsonFile(
    context: Context,
    profile: UserProfile,
    allTransactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>,
    secureNotes: List<SecureNote> = emptyList(),
    period: ExportPeriod = ExportPeriod.ALL_TIME,
    specificYear: Int? = null,
    specificMonthCalendar: Calendar? = null,
    customStart: Long? = null,
    customEnd: Long? = null,
    includeSecureNotes: Boolean = false
  ): File = withContext(Dispatchers.IO) {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()

    val now = Date()
    val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now)
    val isoDateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(now)

    val (filteredTransactions, rangeLabel) = when (period) {
      ExportPeriod.CURRENT_MONTH -> {
        val cal = specificMonthCalendar ?: Calendar.getInstance()
        val start = DateUtils.getStartOfMonth(cal)
        val end = DateUtils.getEndOfMonth(cal)
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        Pair(allTransactions.filter { it.timestamp in start..end }, "Month of $label")
      }
      ExportPeriod.SELECTED_YEAR -> {
        val year = specificYear ?: Calendar.getInstance().get(Calendar.YEAR)
        val start = DateUtils.getStartOfYear(year)
        val end = DateUtils.getEndOfYear(year)
        Pair(allTransactions.filter { it.timestamp in start..end }, "Year $year")
      }
      ExportPeriod.CUSTOM_RANGE -> {
        val start = customStart ?: 0L
        val end = customEnd ?: Long.MAX_VALUE
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startStr = if (customStart != null) df.format(Date(customStart)) else "Beginning"
        val endStr = if (customEnd != null) df.format(Date(customEnd)) else "Present"
        Pair(allTransactions.filter { it.timestamp in start..end }, "$startStr to $endStr")
      }
      ExportPeriod.ALL_TIME -> {
        Pair(allTransactions, "All Financial Records")
      }
    }

    val backupObject = PaisaJsonBackup(
      schemaVersion = 2,
      appName = "Paisa",
      version = "4.0.0",
      exportedAt = isoDateStr,
      exportTimestamp = now.time,
      exportRange = rangeLabel,
      account = PaisaExportAccount(
        name = profile.name,
        email = profile.email
      ),
      transactions = filteredTransactions.sortedByDescending { it.timestamp },
      budgets = budgets,
      recurringTransactions = recurringRules,
      secureNotes = if (includeSecureNotes) secureNotes else emptyList()
    )

    val jsonString = adapter.toJson(backupObject)
    val file = File(exportDir, "Paisa_Backup_${dateStamp}_$timeStamp.json")
    FileOutputStream(file).use { out ->
      out.write(jsonString.toByteArray(Charsets.UTF_8))
      out.flush()
    }
    file
  }

  /**
   * Creates an Android native Share Intent for the JSON export.
   */
  fun createShareIntent(context: Context, jsonFile: File): Intent {
    val uri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      jsonFile
    )

    return Intent(Intent.ACTION_SEND).apply {
      type = "application/json"
      putExtra(Intent.EXTRA_SUBJECT, "Paisa Financial Data Backup (${jsonFile.name})")
      putExtra(Intent.EXTRA_TEXT, "Here is my machine-readable Paisa financial records backup.")
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  /**
   * Reads, validates syntax, schema version, and sanity of data structures from a chosen file Uri.
   */
  suspend fun validateImportFile(
    context: Context,
    uri: Uri
  ): JsonValidationResult = withContext(Dispatchers.IO) {
    try {
      val inputStream = context.contentResolver.openInputStream(uri)
        ?: return@withContext JsonValidationResult.Error("Unable to open the selected file.")

      val jsonString = inputStream.use { stream ->
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
      }

      if (jsonString.isBlank()) {
        return@withContext JsonValidationResult.Error("The selected file is empty.")
      }

      // Check basic Paisa schema indicators before deep parsing
      if (!jsonString.contains("transactions") && !jsonString.contains("schemaVersion")) {
        return@withContext JsonValidationResult.Error("This doesn't appear to be a valid Paisa data file.")
      }

      val backup = try {
        adapter.fromJson(jsonString)
      } catch (e: Exception) {
        return@withContext JsonValidationResult.Error("Invalid JSON structure: ${e.localizedMessage ?: "Parsing failed"}")
      }

      if (backup == null) {
        return@withContext JsonValidationResult.Error("This doesn't appear to be a valid Paisa data file.")
      }

      // Validate schema version
      if (backup.schemaVersion < 1) {
        return@withContext JsonValidationResult.Error("Unsupported Paisa schema version (${backup.schemaVersion}).")
      }

      // Sanitize and validate transactions
      for (tx in backup.transactions) {
        if (tx.title.isBlank()) {
          return@withContext JsonValidationResult.Error("Data contains transactions with empty titles.")
        }
        if (tx.amount.isNaN() || tx.amount.isInfinite() || tx.amount < 0) {
          return@withContext JsonValidationResult.Error("Data contains invalid transaction amounts.")
        }
        if (tx.timestamp <= 0) {
          return@withContext JsonValidationResult.Error("Data contains invalid transaction dates.")
        }
      }

      // Validate budgets
      for (b in backup.budgets) {
        if (!b.monthKey.matches(Regex("^\\d{4}-\\d{2}$"))) {
          return@withContext JsonValidationResult.Error("Data contains invalid budget month format: ${b.monthKey}")
        }
        if (b.totalBudget < 0 || b.totalBudget.isNaN()) {
          return@withContext JsonValidationResult.Error("Data contains invalid budget amounts.")
        }
      }

      val summary = ValidationSummary(
        transactionCount = backup.transactions.size,
        budgetCount = backup.budgets.size,
        recurringCount = backup.recurringTransactions.size,
        secureNoteCount = backup.secureNotes.size,
        accountName = backup.account.name.ifBlank { "Unspecified" },
        accountEmail = backup.account.email.ifBlank { "Unspecified" },
        exportDate = if (backup.exportedAt.isNotBlank()) backup.exportedAt else "Unknown Date"
      )

      JsonValidationResult.Success(backup, summary)
    } catch (e: Exception) {
      JsonValidationResult.Error("Error reading file: ${e.localizedMessage ?: "Unknown error"}")
    }
  }

  /**
   * Deterministically and idempotently merges imported data into current dataset without duplicates.
   */
  fun mergeData(
    currentTransactions: List<TransactionItem>,
    currentBudgets: List<BudgetModel>,
    currentRecurringRules: List<RecurringRule>,
    currentSecureNotes: List<SecureNote> = emptyList(),
    importBackup: PaisaJsonBackup
  ): Quadruple<List<TransactionItem>, List<BudgetModel>, List<RecurringRule>, List<SecureNote>> {
    // 1. Transactions merge: deduplicate by ID and unique signature
    val existingSignatures = currentTransactions.associateBy {
      "${it.title.trim().lowercase()}_${it.amount}_${it.type}_${it.category}_${it.timestamp}"
    }.toMutableMap()

    val existingById = currentTransactions.filter { it.id > 0 }.associateBy { it.id }.toMutableMap()
    val mergedTransactions = currentTransactions.toMutableList()

    for (incoming in importBackup.transactions) {
      val signature = "${incoming.title.trim().lowercase()}_${incoming.amount}_${incoming.type}_${incoming.category}_${incoming.timestamp}"
      val matchById = if (incoming.id > 0) existingById[incoming.id] else null
      val matchBySig = existingSignatures[signature]

      if (matchById == null && matchBySig == null) {
        val newTx = incoming.copy(id = 0)
        mergedTransactions.add(newTx)
        existingSignatures[signature] = newTx
      } else {
        val target = matchById ?: matchBySig!!
        val index = mergedTransactions.indexOfFirst {
          (target.id > 0 && it.id == target.id) ||
            ("${it.title.trim().lowercase()}_${it.amount}_${it.type}_${it.category}_${it.timestamp}" == signature)
        }
        if (index >= 0 && incoming.updatedAt > target.updatedAt) {
          mergedTransactions[index] = incoming.copy(id = target.id)
        }
      }
    }

    // 2. Budgets merge: keyed by monthKey ("YYYY-MM")
    val mergedBudgetsMap = currentBudgets.associateBy { it.monthKey }.toMutableMap()
    for (incomingBudget in importBackup.budgets) {
      val existing = mergedBudgetsMap[incomingBudget.monthKey]
      if (existing == null) {
        mergedBudgetsMap[incomingBudget.monthKey] = incomingBudget.copy(id = 0)
      } else {
        val combinedCatBudgets = existing.categoryBudgets.toMutableMap().apply {
          putAll(incomingBudget.categoryBudgets)
        }
        val newerTotal = if (incomingBudget.updatedAt >= existing.updatedAt && incomingBudget.totalBudget > 0) {
          incomingBudget.totalBudget
        } else {
          existing.totalBudget
        }
        mergedBudgetsMap[incomingBudget.monthKey] = existing.copy(
          totalBudget = newerTotal,
          categoryBudgets = combinedCatBudgets,
          updatedAt = maxOf(existing.updatedAt, incomingBudget.updatedAt)
        )
      }
    }

    // 3. Recurring rules merge
    val existingRuleSigs = currentRecurringRules.associateBy {
      "${it.title.trim().lowercase()}_${it.amount}_${it.type}_${it.interval}"
    }.toMutableMap()
    val mergedRules = currentRecurringRules.toMutableList()

    for (incomingRule in importBackup.recurringTransactions) {
      val sig = "${incomingRule.title.trim().lowercase()}_${incomingRule.amount}_${incomingRule.type}_${incomingRule.interval}"
      if (!existingRuleSigs.containsKey(sig)) {
        mergedRules.add(incomingRule.copy(id = 0))
        existingRuleSigs[sig] = incomingRule
      }
    }

    // 4. Secure Notes merge
    val existingNotes = currentSecureNotes.toMutableList()
    for (incomingNote in importBackup.secureNotes) {
      if (existingNotes.none { it.title.equals(incomingNote.title, ignoreCase = true) && it.content == incomingNote.content }) {
        existingNotes.add(incomingNote.copy(id = 0))
      }
    }

    return Quadruple(
      mergedTransactions.sortedByDescending { it.timestamp },
      mergedBudgetsMap.values.toList(),
      mergedRules,
      existingNotes
    )
  }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
