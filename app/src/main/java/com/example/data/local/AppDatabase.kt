package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE paid_recurring_occurrences ADD COLUMN isCancelled INTEGER NOT NULL DEFAULT 0")
  }
}

@Database(
  entities = [
    TransactionEntity::class,
    BudgetEntity::class,
    RecurringRuleEntity::class,
    PaidRecurringOccurrenceEntity::class
  ],
  version = 4,
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
          .addMigrations(MIGRATION_3_4)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
