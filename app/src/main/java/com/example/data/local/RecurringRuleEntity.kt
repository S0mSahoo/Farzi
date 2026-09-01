package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val amount: Double,
  val type: String, // TransactionType.name
  val category: String, // TransactionCategory.name
  val interval: String, // RecurrenceInterval.name
  val startDate: Long, // Epoch millis
  val endDate: Long? = null,
  val lastGeneratedDate: Long = 0, // Epoch millis of the latest occurrence generated
  val paymentMethod: String = PaymentMethod.UPI.name,
  val note: String = "",
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toModel(): RecurringRule {
    val parsedType = try {
      TransactionType.valueOf(type)
    } catch (e: Exception) {
      TransactionType.EXPENSE
    }
    val parsedCategory = try {
      TransactionCategory.valueOf(category)
    } catch (e: Exception) {
      TransactionCategory.OTHER_EXPENSE
    }
    val parsedInterval = try {
      RecurrenceInterval.valueOf(interval)
    } catch (e: Exception) {
      RecurrenceInterval.MONTHLY
    }
    val parsedMethod = try {
      PaymentMethod.valueOf(paymentMethod)
    } catch (e: Exception) {
      PaymentMethod.UPI
    }

    return RecurringRule(
      id = id,
      title = title,
      amount = amount,
      type = parsedType,
      category = parsedCategory,
      interval = parsedInterval,
      startDate = startDate,
      endDate = endDate,
      lastGeneratedDate = lastGeneratedDate,
      paymentMethod = parsedMethod,
      note = note,
      isActive = isActive,
      createdAt = createdAt
    )
  }

  companion object {
    fun fromModel(model: RecurringRule): RecurringRuleEntity {
      return RecurringRuleEntity(
        id = model.id,
        title = model.title,
        amount = model.amount,
        type = model.type.name,
        category = model.category.name,
        interval = model.interval.name,
        startDate = model.startDate,
        endDate = model.endDate,
        lastGeneratedDate = model.lastGeneratedDate,
        paymentMethod = model.paymentMethod.name,
        note = model.note,
        isActive = model.isActive,
        createdAt = model.createdAt
      )
    }
  }
}
