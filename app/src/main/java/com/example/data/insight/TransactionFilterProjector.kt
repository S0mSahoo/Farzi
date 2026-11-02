package com.example.data.insight

import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType

/**
 * Pure transaction search/filter projection. The ViewModel runs this from a Flow on
 * Dispatchers.Default so text matching never blocks Compose recomposition.
 */
object TransactionFilterProjector {
  fun filter(
    transactions: List<TransactionItem>,
    query: String,
    type: TransactionType?,
    category: TransactionCategory?
  ): List<TransactionItem> = transactions.filter { item ->
    val matchesQuery = query.isBlank() ||
      item.title.contains(query, ignoreCase = true) ||
      item.category.displayName.contains(query, ignoreCase = true) ||
      item.note.contains(query, ignoreCase = true) ||
      item.amount.toString().contains(query)
    val matchesType = type == null || item.type == type
    val matchesCategory = category == null || item.category == category
    matchesQuery && matchesType && matchesCategory
  }
}
