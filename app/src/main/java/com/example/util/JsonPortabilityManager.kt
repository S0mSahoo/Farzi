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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PaisaExportAccount(
  val name: String = "",
  val email: String = ""
)

data class PaisaJsonBackup(
  val schemaVersion: Int = 1,
  val appName: String = "Paisa",
  val version: String = "4.0.0",
  val exportedAt: String = "",
  val exportTimestamp: Long = 0L,
  val periodLabel: String = "All Financial Records",
  val account: PaisaExportAccount = PaisaExportAccount(),
  val transactions: List<TransactionItem> = emptyList(),
  val budgets: List<BudgetModel> = emptyList(),
  val recurringTransactions: List<RecurringRule> = emptyList()
)

data class ValidationSummary(
  val transactionCount: Int,
  val budgetCount: Int,
  val recurringCount: Int,
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
  private const val CURRENT_SCHEMA_VERSION = 2
  private const val MAX_IMPORT_BYTES = 15 * 1024 * 1024

  /**
   * Generates a clean, schema-versioned JSON export of all Paisa application records,
   * completely stripped of any OAuth tokens, passwords, or client secrets.
   */
  suspend fun exportToJsonFile(
    context: Context,
    profile: UserProfile,
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>,
    period: ExportPeriod = ExportPeriod.ALL_TIME,
    selectedCalendar: Calendar = Calendar.getInstance(),
    customStart: Long? = null,
    customEnd: Long? = null
  ): File = withContext(Dispatchers.IO) {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()

    val (filteredTxs, periodLabel) = when (period) {
      ExportPeriod.CURRENT_MONTH -> {
        val start = DateUtils.getStartOfMonth(selectedCalendar)
        val end = DateUtils.getEndOfMonth(selectedCalendar)
        val list = transactions.filter { it.timestamp in start..end }
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedCalendar.time)
        Pair(list, label)
      }
      ExportPeriod.SELECTED_YEAR -> {
        val year = selectedCalendar.get(Calendar.YEAR)
        val start = DateUtils.getStartOfYear(year)
        val end = DateUtils.getEndOfYear(year)
        val list = transactions.filter { it.timestamp in start..end }
        Pair(list, "Full Year $year")
      }
      ExportPeriod.CUSTOM_RANGE -> {
        val start = customStart ?: 0L
        val end = customEnd ?: Long.MAX_VALUE
        val list = transactions.filter { it.timestamp in start..end }
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startStr = if (customStart != null) df.format(Date(customStart)) else "Beginning"
        val endStr = if (customEnd != null) df.format(Date(customEnd)) else "Present"
        Pair(list, "$startStr to $endStr")
      }
      ExportPeriod.ALL_TIME -> {
        Pair(transactions, "All Financial Records")
      }
    }

    val now = Date()
    val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now)
    val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val isoDateStr = isoFormatter.format(now)

    val backupObject = PaisaJsonBackup(
      schemaVersion = CURRENT_SCHEMA_VERSION,
      appName = "Paisa",
      version = "5.0.0",
      exportedAt = isoDateStr,
      exportTimestamp = now.time,
      periodLabel = periodLabel,
      account = PaisaExportAccount(
        name = profile.name,
        email = profile.email
      ),
      transactions = filteredTxs.sortedByDescending { it.timestamp },
      budgets = budgets,
      recurringTransactions = recurringRules
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

      val bytes = inputStream.use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
          val count = stream.read(buffer)
          if (count < 0) break
          total += count
          if (total > MAX_IMPORT_BYTES) {
            return@withContext JsonValidationResult.Error("Backup file is too large. Maximum supported size is 15 MB.")
          }
          output.write(buffer, 0, count)
        }
        output.toByteArray()
      }
      val jsonString = String(bytes, Charsets.UTF_8)

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
      if (backup.appName != "Paisa" || backup.schemaVersion !in 1..CURRENT_SCHEMA_VERSION) {
        return@withContext JsonValidationResult.Error(
          "Unsupported Paisa backup format (app='${backup.appName}', schema=${backup.schemaVersion}). Update Paisa or select a valid Paisa backup."
        )
      }

      // Sanitize and validate transactions
      for (tx in backup.transactions) {
        if (tx.title.isBlank()) {
          return@withContext JsonValidationResult.Error("Data contains transactions with empty titles.")
        }
        if (!tx.amount.isFinite() || tx.amount <= 0) {
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
        if (!b.totalBudget.isFinite() || b.totalBudget < 0 ||
          b.categoryBudgets.values.any { !it.isFinite() || it < 0 }) {
          return@withContext JsonValidationResult.Error("Data contains invalid budget amounts.")
        }
      }

      for (rule in backup.recurringTransactions) {
        if (rule.title.isBlank() || !rule.amount.isFinite() || rule.amount <= 0 ||
          rule.startDate <= 0 || (rule.endDate != null && rule.endDate < rule.startDate)) {
          return@withContext JsonValidationResult.Error("Data contains invalid recurring-payment rules.")
        }
      }

      val summary = ValidationSummary(
        transactionCount = backup.transactions.size,
        budgetCount = backup.budgets.size,
        recurringCount = backup.recurringTransactions.size,
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
    importBackup: PaisaJsonBackup
  ): Triple<List<TransactionItem>, List<BudgetModel>, List<RecurringRule>> {
    // Local Room IDs are only local primary keys. Never match an imported row by ID alone:
    // two different devices can assign the same ID to completely different financial records.
    fun transactionSignature(tx: TransactionItem): String = listOf(
      tx.title.trim().lowercase(Locale.ROOT),
      java.lang.Double.doubleToLongBits(tx.amount).toString(),
      tx.type.name,
      tx.category.name,
      tx.timestamp.toString(),
      tx.note.trim().lowercase(Locale.ROOT),
      tx.paymentMethod.name,
      tx.isRecurring.toString(),
      tx.createdAt.toString()
    ).joinToString("|")

    val mergedTransactions = currentTransactions.toMutableList()
    val indexBySignature = mutableMapOf<String, Int>()
    mergedTransactions.forEachIndexed { index, tx ->
      indexBySignature.putIfAbsent(transactionSignature(tx), index)
    }

    for (incoming in importBackup.transactions) {
      val signature = transactionSignature(incoming)
      val existingIndex = indexBySignature[signature]
      if (existingIndex == null) {
        val normalized = incoming.copy(id = 0)
        indexBySignature[signature] = mergedTransactions.size
        mergedTransactions.add(normalized)
      } else {
        val existing = mergedTransactions[existingIndex]
        if (incoming.updatedAt > existing.updatedAt) {
          mergedTransactions[existingIndex] = incoming.copy(id = existing.id)
        }
      }
    }

    // A budget is one month-scoped record. Replace the complete record only when the imported
    // version is newer, including zero-valued limits and intentionally removed category limits.
    val mergedBudgets = currentBudgets.associateBy { it.monthKey }.toMutableMap()
    for (incoming in importBackup.budgets) {
      val existing = mergedBudgets[incoming.monthKey]
      if (existing == null) {
        mergedBudgets[incoming.monthKey] = incoming.copy(id = 0)
      } else if (incoming.updatedAt > existing.updatedAt) {
        mergedBudgets[incoming.monthKey] = incoming.copy(id = existing.id)
      }
    }

    // Recurring-rule IDs are also local database IDs. Only treat a rule as duplicate when the
    // identifying fields match; do not collapse different rules merely because title/amount match.
    fun recurringSignature(rule: RecurringRule): String = listOf(
      rule.title.trim().lowercase(Locale.ROOT),
      java.lang.Double.doubleToLongBits(rule.amount).toString(),
      rule.type.name,
      rule.category.name,
      rule.interval.name,
      rule.startDate.toString(),
      rule.endDate?.toString().orEmpty(),
      rule.paymentMethod.name,
      rule.note.trim().lowercase(Locale.ROOT)
    ).joinToString("|")

    val mergedRules = currentRecurringRules.toMutableList()
    val seenRules = currentRecurringRules.map(::recurringSignature).toMutableSet()
    for (incoming in importBackup.recurringTransactions) {
      if (seenRules.add(recurringSignature(incoming))) {
        mergedRules.add(incoming.copy(id = 0, lastGeneratedDate = 0))
      }
    }

    return Triple(
      mergedTransactions.sortedByDescending { it.timestamp },
      mergedBudgets.values.toList(),
      mergedRules
    )
  }

}
