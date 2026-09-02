package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaidRecurringOccurrenceDao {
  @Query("SELECT * FROM paid_recurring_occurrences")
  fun getAllPaidOccurrencesFlow(): Flow<List<PaidRecurringOccurrenceEntity>>

  @Query("SELECT * FROM paid_recurring_occurrences")
  suspend fun getAllPaidOccurrences(): List<PaidRecurringOccurrenceEntity>

  @Query("SELECT * FROM paid_recurring_occurrences WHERE ruleId = :ruleId")
  suspend fun getPaidOccurrencesByRule(ruleId: Long): List<PaidRecurringOccurrenceEntity>

  @Query("SELECT EXISTS(SELECT 1 FROM paid_recurring_occurrences WHERE ruleId = :ruleId AND occurrenceDate = :dateKey)")
  suspend fun isOccurrencePaid(ruleId: Long, dateKey: String): Boolean

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun markOccurrencePaid(occurrence: PaidRecurringOccurrenceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(occurrences: List<PaidRecurringOccurrenceEntity>)

  @Query("DELETE FROM paid_recurring_occurrences WHERE ruleId = :ruleId AND occurrenceDate = :dateKey")
  suspend fun deleteOccurrence(ruleId: Long, dateKey: String)

  @Query("DELETE FROM paid_recurring_occurrences WHERE ruleId = :ruleId")
  suspend fun deleteOccurrencesByRule(ruleId: Long)

  @Query("DELETE FROM paid_recurring_occurrences")
  suspend fun clearAll()
}
