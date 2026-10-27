package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

/**
 * Rebuilds only the paid-recurring-occurrence table into the current known layout. Earlier
 * released builds used both occurrenceDate/transactionId/paidAt and
 * occurrenceDateKey/paidTransactionId/paidTimestamp column names.
 */
private fun normalizePaidRecurringOccurrences(db: SupportSQLiteDatabase) {
  val columns = mutableSetOf<String>()
  db.query("PRAGMA table_info(paid_recurring_occurrences)").use { cursor ->
    val nameIndex = cursor.getColumnIndex("name")
    while (cursor.moveToNext()) {
      if (nameIndex >= 0) columns += cursor.getString(nameIndex)
    }
  }

  if (columns.isEmpty()) {
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS paid_recurring_occurrences (" +
        "ruleId INTEGER NOT NULL, occurrenceDateKey TEXT NOT NULL, " +
        "paidTransactionId INTEGER NOT NULL, paidTimestamp INTEGER NOT NULL, " +
        "isCancelled INTEGER NOT NULL, PRIMARY KEY(ruleId, occurrenceDateKey))"
    )
    return
  }

  val dateColumn = when {
    "occurrenceDateKey" in columns -> "occurrenceDateKey"
    "occurrenceDate" in columns -> "occurrenceDate"
    else -> throw IllegalStateException(
      "Cannot migrate recurring-payment history because its date column is unknown. " +
        "Restore from a JSON backup and contact support before retrying."
    )
  }
  val transactionColumn = when {
    "paidTransactionId" in columns -> "paidTransactionId"
    "transactionId" in columns -> "transactionId"
    else -> "-1"
  }
  val paidAtColumn = when {
    "paidTimestamp" in columns -> "paidTimestamp"
    "paidAt" in columns -> "paidAt"
    else -> "0"
  }
  val cancelledColumn = if ("isCancelled" in columns) "isCancelled" else "0"

  db.execSQL("DROP TABLE IF EXISTS paid_recurring_occurrences_new")
  db.execSQL(
    "CREATE TABLE paid_recurring_occurrences_new (" +
      "ruleId INTEGER NOT NULL, occurrenceDateKey TEXT NOT NULL, " +
      "paidTransactionId INTEGER NOT NULL, paidTimestamp INTEGER NOT NULL, " +
      "isCancelled INTEGER NOT NULL, PRIMARY KEY(ruleId, occurrenceDateKey))"
  )
  db.execSQL(
    "INSERT OR REPLACE INTO paid_recurring_occurrences_new " +
      "(ruleId, occurrenceDateKey, paidTransactionId, paidTimestamp, isCancelled) " +
      "SELECT ruleId, $dateColumn, $transactionColumn, $paidAtColumn, $cancelledColumn " +
      "FROM paid_recurring_occurrences"
  )
  db.execSQL("DROP TABLE paid_recurring_occurrences")
  db.execSQL("ALTER TABLE paid_recurring_occurrences_new RENAME TO paid_recurring_occurrences")
}

/**
 * Migrate the original v1 tables to the schema that became v2 without dropping transactions.
 * The old transactions table predates recurringRuleId/updatedAt and the two new financial
 * tables, so rebuild it while carrying every existing value forward.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE transactions_new (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "title TEXT NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, " +
        "category TEXT NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL, " +
        "paymentMethod TEXT NOT NULL, isRecurring INTEGER NOT NULL, " +
        "recurringRuleId INTEGER, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
    )
    db.execSQL(
      "INSERT INTO transactions_new " +
        "(id, title, amount, type, category, timestamp, note, paymentMethod, isRecurring, " +
        "recurringRuleId, createdAt, updatedAt) " +
        "SELECT id, title, amount, type, category, timestamp, note, paymentMethod, isRecurring, " +
        "NULL, createdAt, createdAt FROM transactions"
    )
    db.execSQL("DROP TABLE transactions")
    db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions (timestamp)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions (type)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions (category)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_recurringRuleId ON transactions (recurringRuleId)")

    db.execSQL(
      "CREATE TABLE IF NOT EXISTS budgets (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, monthKey TEXT NOT NULL, " +
        "totalBudget REAL NOT NULL, categoryBudgetsJson TEXT NOT NULL, updatedAt INTEGER NOT NULL)"
    )
    db.execSQL(
      "CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_monthKey ON budgets (monthKey)"
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS recurring_rules (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, amount REAL NOT NULL, " +
        "type TEXT NOT NULL, category TEXT NOT NULL, interval TEXT NOT NULL, startDate INTEGER NOT NULL, " +
        "endDate INTEGER, lastGeneratedDate INTEGER NOT NULL, paymentMethod TEXT NOT NULL, " +
        "note TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
    )
  }
}

/**
 * The app originally used "dailydraft_database" in version 1, then switched the filename at
 * version 2. Reuse the legacy file if no current database exists, checkpointing SQLite WAL first
 * so previously committed transactions are not lost during the filename migration.
 */
private fun migrateLegacyDatabaseFile(context: Context) {
  val legacy = context.getDatabasePath("dailydraft_database")
  val current = context.getDatabasePath("paisa_finance_database")
  if (current.exists() || !legacy.exists()) return

  SQLiteDatabase.openDatabase(
    legacy.absolutePath,
    null,
    SQLiteDatabase.OPEN_READWRITE
  ).use { db ->
    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { cursor ->
      if (cursor.moveToFirst()) {
        val busy = cursor.getInt(0)
        val logFrames = cursor.getInt(1)
        val checkpointedFrames = cursor.getInt(2)
        if (busy != 0 || (logFrames >= 0 && checkpointedFrames >= 0 && checkpointedFrames < logFrames)) {
          throw IllegalStateException(
            "Could not safely checkpoint the old Paisa database. It was left in place; retry after closing the previous app version."
          )
        }
      }
    }
  }

  // The checkpoint is complete and the SQLite connection is closed, so sidecar files no longer
  // contain uncheckpointed records. Do not proceed if cleanup fails.
  listOf(File(legacy.absolutePath + "-wal"), File(legacy.absolutePath + "-shm")).forEach { sidecar ->
    if (sidecar.exists() && !sidecar.delete()) {
      throw IllegalStateException("Could not safely migrate the legacy Paisa database file.")
    }
  }
  if (!legacy.renameTo(current)) {
    throw IllegalStateException("Could not rename the legacy Paisa database. The original file remains untouched.")
  }
}

/**
 * A previous app revision created the new database filename but left the old v1 file behind.
 * When both files exist, merge legacy transactions into the already-current database, using a
 * transaction marker so a restart cannot import them a second time. The legacy file is archived,
 * not deleted, after the import.
 */
private fun importLegacyTransactionsIfNeeded(context: Context, target: SupportSQLiteDatabase) {
  val legacy = context.getDatabasePath("dailydraft_database")
  if (!legacy.exists()) return

  val markerTable = "paisa_migration_markers"
  val markerName = "legacy_v1_transactions_imported"
  target.execSQL(
    "CREATE TABLE IF NOT EXISTS $markerTable (" +
      "name TEXT NOT NULL PRIMARY KEY, completedAt INTEGER NOT NULL)"
  )

  val source = SQLiteDatabase.openDatabase(
    legacy.absolutePath,
    null,
    SQLiteDatabase.OPEN_READWRITE
  )
  var importedSuccessfully = false
  try {
    source.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { cursor ->
      if (cursor.moveToFirst()) {
        val busy = cursor.getInt(0)
        val logFrames = cursor.getInt(1)
        val checkpointedFrames = cursor.getInt(2)
        if (busy != 0 || (logFrames >= 0 && checkpointedFrames >= 0 && checkpointedFrames < logFrames)) {
          throw IllegalStateException("Could not safely checkpoint the legacy Paisa database.")
        }
      }
    }

    // Only import known v1 files. Keep any unexpected legacy schema untouched for manual recovery.
    if (source.version != 1) {
      Log.w("PaisaDatabase", "Legacy database has unexpected user_version=${source.version}; leaving it untouched.")
      return
    }

    target.beginTransaction()
    try {
      val alreadyImported = target.query(
        "SELECT name FROM $markerTable WHERE name = ?",
        arrayOf(markerName)
      ).use { it.moveToFirst() }

      if (!alreadyImported) {
        // Deduplicate only against rows which existed before this import. Do not deduplicate
        // repeated legacy rows against one another; two identical entries may be real purchases.
        val existingCounts = mutableMapOf<String, Int>()
        target.query(
          "SELECT title, amount, type, category, timestamp, note, paymentMethod, isRecurring, createdAt FROM transactions"
        ).use { cursor ->
          while (cursor.moveToNext()) {
            val signature = legacyTransactionSignature(
              title = cursor.getString(0),
              amount = cursor.getDouble(1),
              type = cursor.getString(2),
              category = cursor.getString(3),
              timestamp = cursor.getLong(4),
              note = cursor.getString(5),
              paymentMethod = cursor.getString(6),
              isRecurring = cursor.getInt(7) != 0,
              createdAt = cursor.getLong(8)
            )
            existingCounts[signature] = (existingCounts[signature] ?: 0) + 1
          }
        }

        source.rawQuery(
          "SELECT title, amount, type, category, timestamp, note, paymentMethod, isRecurring, createdAt FROM transactions ORDER BY id",
          null
        ).use { cursor ->
          while (cursor.moveToNext()) {
            val title = cursor.getString(0)
            val amount = cursor.getDouble(1)
            val type = cursor.getString(2)
            val category = cursor.getString(3)
            val timestamp = cursor.getLong(4)
            val note = cursor.getString(5)
            val paymentMethod = cursor.getString(6)
            val isRecurring = cursor.getInt(7) != 0
            val createdAt = cursor.getLong(8)
            val signature = legacyTransactionSignature(
              title, amount, type, category, timestamp, note, paymentMethod, isRecurring, createdAt
            )
            val priorCopies = existingCounts[signature] ?: 0
            if (priorCopies > 0) {
              existingCounts[signature] = priorCopies - 1
              continue
            }

            val values = ContentValues().apply {
              put("title", title)
              put("amount", amount)
              put("type", type)
              put("category", category)
              put("timestamp", timestamp)
              put("note", note)
              put("paymentMethod", paymentMethod)
              put("isRecurring", if (isRecurring) 1 else 0)
              putNull("recurringRuleId")
              put("createdAt", createdAt)
              put("updatedAt", createdAt)
            }
            val inserted = target.insert("transactions", SQLiteDatabase.CONFLICT_ABORT, values)
            if (inserted < 0) throw IllegalStateException("Could not import a legacy Paisa transaction.")
          }
        }
      }

      target.execSQL(
        "INSERT OR REPLACE INTO $markerTable (name, completedAt) VALUES (?, ?)",
        arrayOf(markerName, System.currentTimeMillis())
      )
      target.setTransactionSuccessful()
      importedSuccessfully = true
    } finally {
      target.endTransaction()
    }
  } finally {
    source.close()
  }

  if (importedSuccessfully) {
    // SQLite has checkpointed and closed the source DB; sidecars no longer contain unique data.
    listOf(File(legacy.absolutePath + "-wal"), File(legacy.absolutePath + "-shm")).forEach { sidecar ->
      if (sidecar.exists() && !sidecar.delete()) {
        Log.w("PaisaDatabase", "Could not remove old SQLite sidecar ${sidecar.name}; migration marker prevents duplicate import.")
      }
    }
    val archive = File(legacy.absolutePath + ".migrated")
    val destination = if (archive.exists()) File(legacy.absolutePath + ".migrated-" + System.currentTimeMillis()) else archive
    if (!legacy.renameTo(destination)) {
      Log.w("PaisaDatabase", "Legacy transaction import succeeded, but the original file could not be archived.")
    }
  }
}

private fun legacyTransactionSignature(
  title: String,
  amount: Double,
  type: String,
  category: String,
  timestamp: Long,
  note: String,
  paymentMethod: String,
  isRecurring: Boolean,
  createdAt: Long
): String = listOf(
  title, java.lang.Double.doubleToLongBits(amount).toString(), type, category,
  timestamp.toString(), note, paymentMethod, isRecurring.toString(), createdAt.toString()
).joinToString("\u0000")

/**
 * Version 2 only had the primary finance tables. Recreate the auxiliary tables introduced in
 * version 3 so the subsequent migration can normalize their structure without dropping records.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS paid_recurring_occurrences (" +
        "ruleId INTEGER NOT NULL, occurrenceDate TEXT NOT NULL, transactionId INTEGER NOT NULL, " +
        "paidAt INTEGER NOT NULL, PRIMARY KEY(ruleId, occurrenceDate))"
    )
    db.execSQL(
      "CREATE INDEX IF NOT EXISTS index_paid_recurring_occurrences_ruleId " +
        "ON paid_recurring_occurrences (ruleId)"
    )
    db.execSQL(
      "CREATE INDEX IF NOT EXISTS index_paid_recurring_occurrences_occurrenceDate " +
        "ON paid_recurring_occurrences (occurrenceDate)"
    )
    // Legacy secure notes were removed from the UI, but the v3 database still had this table.
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS secure_notes (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, " +
        "encryptedPayload TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
    )
  }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(db: SupportSQLiteDatabase) {
    normalizePaidRecurringOccurrences(db)
  }
}

/**
 * Version 5 ensures future installs/upgrades also normalize the inconsistent version-4 table.
 * No database is silently dropped when a migration path is missing.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
  override fun migrate(db: SupportSQLiteDatabase) {
    normalizePaidRecurringOccurrences(db)
  }
}

@Database(
  entities = [
    TransactionEntity::class,
    BudgetEntity::class,
    RecurringRuleEntity::class,
    PaidRecurringOccurrenceEntity::class
  ],
  version = 5,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun transactionDao(): TransactionDao
  abstract fun budgetDao(): BudgetDao
  abstract fun recurringRuleDao(): RecurringRuleDao
  abstract fun paidRecurringOccurrenceDao(): PaidRecurringOccurrenceDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        migrateLegacyDatabaseFile(context.applicationContext)
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "paisa_finance_database"
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
          .build()

        // When the old filename still exists alongside the active DB, import its transactions
        // once after Room has migrated/validated the current database schema.
        if (context.getDatabasePath("dailydraft_database").exists()) {
          importLegacyTransactionsIfNeeded(
            context.applicationContext,
            instance.openHelper.writableDatabase
          )
        }

        INSTANCE = instance
        instance
      }
    }
  }
}
