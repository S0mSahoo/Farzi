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

  @Query("SELECT * FROM paid_recurring_occurrences WHERE ruleId = :ruleId AND occurrenceDateKey = :dateKey LIMIT 1")
  suspend fun getPaidOccurrence(ruleId: Long, dateKey: String): PaidRecurringOccurrenceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun markPaid(occurrence: PaidRecurringOccurrenceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(occurrences: List<PaidRecurringOccurrenceEntity>)

  @Query("DELETE FROM paid_recurring_occurrences WHERE ruleId = :ruleId AND occurrenceDateKey = :dateKey")
  suspend fun unmarkPaid(ruleId: Long, dateKey: String)

  @Query("DELETE FROM paid_recurring_occurrences WHERE ruleId = :ruleId")
  suspend fun deleteForRule(ruleId: Long)

  @Query("DELETE FROM paid_recurring_occurrences")
  suspend fun clearAll()
}
