package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val amount: Double,
  val type: String, // TransactionType name
  val category: String, // TransactionCategory name
  val timestamp: Long,
  val note: String = "",
  val paymentMethod: String = PaymentMethod.CASH.name,
  val isRecurring: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toModel(): TransactionItem {
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

    val parsedMethod = try {
      PaymentMethod.valueOf(paymentMethod)
    } catch (e: Exception) {
      PaymentMethod.CASH
    }

    return TransactionItem(
      id = id,
      title = title,
      amount = amount,
      type = parsedType,
      category = parsedCategory,
      timestamp = timestamp,
      note = note,
      paymentMethod = parsedMethod,
      isRecurring = isRecurring,
      createdAt = createdAt
    )
  }

  companion object {
    fun fromModel(model: TransactionItem): TransactionEntity {
      return TransactionEntity(
        id = model.id,
        title = model.title,
        amount = model.amount,
        type = model.type.name,
        category = model.category.name,
        timestamp = model.timestamp,
        note = model.note,
        paymentMethod = model.paymentMethod.name,
        isRecurring = model.isRecurring,
        createdAt = model.createdAt
      )
    }
  }
}
