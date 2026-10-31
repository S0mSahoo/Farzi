package com.example.ui.screens

import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.DateUtils
import java.util.Calendar

data class TransactionFeedEntry(
  val key: String,
  val dateLabel: String? = null,
  val transaction: TransactionItem? = null,
  val entryCount: Int = 0
)

data class TransactionScreenProjection(
  val feedEntries: List<TransactionFeedEntry> = emptyList(),
  val recordCount: Int = 0,
  val totalIncome: Double = 0.0,
  val totalExpense: Double = 0.0,
  val daysWithTransactions: Set<Int> = emptySet(),
  val visibleTransactions: List<TransactionItem> = emptyList()
)

/**
 * Pure projection for the transaction history screen. The caller executes this off the main
 * thread; this function performs one month scan and derives daily markers, totals, sort order,
 * grouped headers and virtualized list entries from that pass.
 */
object TransactionHistoryProjector {
  fun project(
    transactions: List<TransactionItem>,
    startOfMonthMillis: Long,
    endOfMonthMillis: Long,
    selectedDayOfMonth: Int?,
    referenceTimestamp: Long
  ): TransactionScreenProjection {
    val dayCalendar = Calendar.getInstance()
    val daysWithTransactions = LinkedHashSet<Int>()
    val visibleTransactions = ArrayList<TransactionItem>()
    var totalIncome = 0.0
    var totalExpense = 0.0

    for (transaction in transactions) {
      if (transaction.timestamp !in startOfMonthMillis..endOfMonthMillis) continue
      dayCalendar.timeInMillis = transaction.timestamp
      val day = dayCalendar.get(Calendar.DAY_OF_MONTH)
      daysWithTransactions.add(day)
      if (selectedDayOfMonth != null && selectedDayOfMonth != day) continue

      visibleTransactions.add(transaction)
      when (transaction.type) {
        TransactionType.INCOME -> totalIncome += transaction.amount
        TransactionType.EXPENSE -> totalExpense += transaction.amount
      }
    }

    visibleTransactions.sortWith(
      compareByDescending<TransactionItem> { it.timestamp }.thenByDescending { it.id }
    )
    val todayKey = DateUtils.getDayKey(referenceTimestamp)
    val yesterdayKey = DateUtils.getDayKey(
      Calendar.getInstance().apply {
        timeInMillis = referenceTimestamp
        add(Calendar.DAY_OF_MONTH, -1)
      }.timeInMillis
    )
    val grouped = visibleTransactions.groupBy { transaction ->
      when (DateUtils.getDayKey(transaction.timestamp)) {
        todayKey -> "Today"
        yesterdayKey -> "Yesterday"
        else -> DateUtils.getDisplayDate(transaction.timestamp)
      }
    }
    val entries = buildList {
      grouped.forEach { (dateLabel, dayTransactions) ->
        add(
          TransactionFeedEntry(
            key = "header_$dateLabel",
            dateLabel = dateLabel,
            entryCount = dayTransactions.size
          )
        )
        dayTransactions.forEach { transaction ->
          add(
            TransactionFeedEntry(
              key = "tx_${transaction.id}_${transaction.timestamp}",
              transaction = transaction
            )
          )
        }
      }
    }

    return TransactionScreenProjection(
      feedEntries = entries,
      recordCount = visibleTransactions.size,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      daysWithTransactions = daysWithTransactions,
      visibleTransactions = visibleTransactions
    )
  }
}
