package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
  @Query("SELECT * FROM recurring_rules ORDER BY id DESC")
  fun getAllRulesFlow(): Flow<List<RecurringRuleEntity>>

  @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
  suspend fun getActiveRules(): List<RecurringRuleEntity>

  @Query("SELECT * FROM recurring_rules ORDER BY id DESC")
  suspend fun getAllRules(): List<RecurringRuleEntity>

  @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
  suspend fun getRuleById(id: Long): RecurringRuleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRule(rule: RecurringRuleEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(rules: List<RecurringRuleEntity>)

  @Update
  suspend fun updateRule(rule: RecurringRuleEntity)

  @Delete
  suspend fun deleteRule(rule: RecurringRuleEntity)

  @Query("DELETE FROM recurring_rules WHERE id = :id")
  suspend fun deleteRuleById(id: Long)

  @Query("DELETE FROM recurring_rules")
  suspend fun clearAll()
}
