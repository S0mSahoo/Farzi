package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.BudgetModel
import com.example.data.model.TransactionCategory
import org.json.JSONObject

@Entity(
  tableName = "budgets",
  indices = [Index(value = ["monthKey"], unique = true)]
)
data class BudgetEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val monthKey: String, // "YYYY-MM"
  val totalBudget: Double,
  val categoryBudgetsJson: String = "{}", // JSON map: {"FOOD_DINING": 5000.0, ...}
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun toModel(): BudgetModel {
    val catMap = mutableMapOf<TransactionCategory, Double>()
    try {
      val json = JSONObject(categoryBudgetsJson)
      val keys = json.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        try {
          val cat = TransactionCategory.valueOf(key)
          val amount = json.optDouble(key, 0.0)
          if (amount > 0.0) {
            catMap[cat] = amount
          }
        } catch (ignored: Exception) {}
      }
    } catch (ignored: Exception) {}

    return BudgetModel(
      id = id,
      monthKey = monthKey,
      totalBudget = totalBudget,
      categoryBudgets = catMap,
      updatedAt = updatedAt
    )
  }

  companion object {
    fun fromModel(model: BudgetModel): BudgetEntity {
      val json = JSONObject()
      model.categoryBudgets.forEach { (cat, amount) ->
        json.put(cat.name, amount)
      }
      return BudgetEntity(
        id = model.id,
        monthKey = model.monthKey,
        totalBudget = model.totalBudget,
        categoryBudgetsJson = json.toString(),
        updatedAt = model.updatedAt
      )
    }
  }
}
