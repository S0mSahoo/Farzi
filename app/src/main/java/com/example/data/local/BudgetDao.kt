package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
  @Query("SELECT * FROM budgets WHERE monthKey = :monthKey LIMIT 1")
  fun getBudgetForMonthFlow(monthKey: String): Flow<BudgetEntity?>

  @Query("SELECT * FROM budgets WHERE monthKey = :monthKey LIMIT 1")
  suspend fun getBudgetForMonth(monthKey: String): BudgetEntity?

  @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
  fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

  @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
  suspend fun getAllBudgets(): List<BudgetEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(budgets: List<BudgetEntity>)

  @Update
  suspend fun updateBudget(budget: BudgetEntity)

  @Delete
  suspend fun deleteBudget(budget: BudgetEntity)

  @Query("DELETE FROM budgets WHERE monthKey = :monthKey")
  suspend fun deleteBudgetByMonth(monthKey: String)

  @Query("DELETE FROM budgets")
  suspend fun clearAll()
}
