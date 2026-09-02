package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    TransactionEntity::class,
    BudgetEntity::class,
    RecurringRuleEntity::class,
    PaidRecurringOccurrenceEntity::class,
    SecureNoteEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun transactionDao(): TransactionDao
  abstract fun budgetDao(): BudgetDao
  abstract fun recurringRuleDao(): RecurringRuleDao
  abstract fun paidRecurringOccurrenceDao(): PaidRecurringOccurrenceDao
  abstract fun secureNoteDao(): SecureNoteDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "paisa_finance_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
