package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumSectionHeading
import com.example.ui.components.PremiumTransactionRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun TransactionsScreen(
  viewModel: FinanceViewModel,
  onEditTransaction: (item: TransactionItem) -> Unit
) {
  val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val filterType by viewModel.filterType.collectAsStateWithLifecycle()
  val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }
  var showCategoryFilters by remember { mutableStateOf(false) }
  val currencySymbol = userProfile.currencySymbol

  if (transactionToDelete != null) {
    ConfirmationDialog(
      title = "Delete transaction?",
      message = "Delete “${transactionToDelete?.title}”? This cannot be undone.",
      confirmButtonText = "Delete",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        transactionToDelete?.let { viewModel.deleteTransaction(it) }
        transactionToDelete = null
      },
      onDismiss = { transactionToDelete = null }
    )
  }

  val sorted = remember(transactions) { transactions.sortedByDescending { it.timestamp } }
  val grouped = remember(sorted) {
    val todayKey = DateUtils.getDayKey(Calendar.getInstance())
    val yesterdayKey = DateUtils.getDayKey(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) })
    sorted.groupBy { tx ->
      when (DateUtils.getDayKey(tx.timestamp)) {
        todayKey -> "Today"
        yesterdayKey -> "Yesterday"
        else -> DateUtils.getDisplayDate(tx.timestamp)
      }
    }
  }
  val net = remember(sorted) {
    sorted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
      sorted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
  }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 940.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        PremiumSectionHeading(
          title = "Transactions",
          subtitle = "Every rupee, accounted for."
        )
      }

      item {
        PremiumCard(containerColor = Color(0xFF163D2B)) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text("NET IN THIS VIEW", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.SemiBold)
            Text(
              IndianCurrencyFormatter.formatWithSymbol(net, currencySymbol, includeSign = true),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("${sorted.size} records", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.72f))
              Text(
                "Filtered results",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD4EF75),
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = viewModel::setSearchQuery,
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Search merchant, category or note", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          )
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = filterType == null,
            onClick = { viewModel.setFilterType(null) },
            label = { Text("Everything") }
          )
          FilterChip(
            selected = filterType == TransactionType.EXPENSE,
            onClick = { viewModel.setFilterType(if (filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
            label = { Text("Expenses") }
          )
          FilterChip(
            selected = filterType == TransactionType.INCOME,
            onClick = { viewModel.setFilterType(if (filterType == TransactionType.INCOME) null else TransactionType.INCOME) },
            label = { Text("Income") }
          )
          FilterChip(
            selected = showCategoryFilters || filterCategory != null,
            onClick = { showCategoryFilters = !showCategoryFilters },
            label = { Text("Category") },
            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) }
          )
        }
      }

      if (showCategoryFilters || filterCategory != null) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
          ) {
            FilterChip(
              selected = filterCategory == null,
              onClick = { viewModel.setFilterCategory(null) },
              label = { Text("All categories") }
            )
            TransactionCategory.values()
              .filter { filterType == null || it.defaultType == filterType }
              .forEach { category ->
                FilterChip(
                  selected = filterCategory == category,
                  onClick = { viewModel.setFilterCategory(if (filterCategory == category) null else category) },
                  label = { Text(category.displayName) }
                )
              }
          }
        }
      }

      if (sorted.isEmpty()) {
        item {
          PremiumCard {
            PremiumEmptyState(
              title = if (searchQuery.isNotBlank() || filterType != null || filterCategory != null) "No matching records" else "Nothing recorded yet",
              body = if (searchQuery.isNotBlank() || filterType != null || filterCategory != null) {
                "Try a different search or remove one of the filters."
              } else {
                "Your day-to-day spending and income will appear here."
              },
              action = if (searchQuery.isNotBlank() || filterType != null || filterCategory != null) {
                {
                  androidx.compose.material3.TextButton(onClick = {
                    viewModel.setSearchQuery("")
                    viewModel.setFilterType(null)
                    viewModel.setFilterCategory(null)
                    showCategoryFilters = false
                  }) { Text("Clear all filters") }
                }
              } else null
            )
          }
        }
      } else {
        grouped.entries.forEach { (dateLabel, itemsForDay) ->
          item(key = "header_$dateLabel") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                "${itemsForDay.size} ${if (itemsForDay.size == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          item(key = "group_$dateLabel") {
            PremiumCard {
              Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)) {
                itemsForDay.forEachIndexed { index, tx ->
                  PremiumTransactionRow(
                    transaction = tx,
                    currencySymbol = currencySymbol,
                    onClick = { onEditTransaction(tx) },
                    trailing = {
                      IconButton(onClick = { transactionToDelete = tx }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${tx.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                      }
                    }
                  )
                  if (index != itemsForDay.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                      modifier = Modifier.padding(start = 58.dp),
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                  }
                }
              }
            }
          }
        }
      }

      item {
        Spacer(Modifier.height(8.dp))
        Text(
          "Tap any record to edit it. Use search and filters to narrow your history.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 2.dp)
        )
      }
    }
  }
}
