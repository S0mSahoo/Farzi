package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "paisa_finance_database"
        )
          .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
