package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalEmerald
import com.example.ui.theme.MinimalIndigo
import com.example.ui.theme.MinimalRose
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
  viewModel: FinanceViewModel,
  onEditTransaction: (item: TransactionItem) -> Unit
) {
  val transactions by viewModel.filteredTransactions.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val filterType by viewModel.filterType.collectAsState()
  val filterCategory by viewModel.filterCategory.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()

  var isMultiSelectMode by remember { mutableStateOf(false) }
  val selectedIds = remember { mutableStateListOf<Long>() }
  var showBulkDeleteConfirm by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }

  val currencySymbol = userProfile.currencySymbol

  if (transactionToDelete != null) {
    ConfirmationDialog(
      title = "Delete Transaction?",
      message = "Are you sure you want to delete \"${transactionToDelete?.title}\"?",
      onConfirm = {
        transactionToDelete?.let { viewModel.deleteTransaction(it) }
        transactionToDelete = null
      },
      onDismiss = { transactionToDelete = null }
    )
  }

  if (showBulkDeleteConfirm) {
    val totalAmount = transactions.filter { it.id in selectedIds }.sumOf { it.amount }
    ConfirmationDialog(
      title = "Bulk Delete Transactions?",
      message = "Are you sure you want to permanently delete ${selectedIds.size} transactions (totaling ${IndianCurrencyFormatter.formatWithSymbol(totalAmount, currencySymbol)})? This action cannot be undone.",
      onConfirm = {
        viewModel.bulkDeleteTransactions(selectedIds.toList()) {
          selectedIds.clear()
          isMultiSelectMode = false
          showBulkDeleteConfirm = false
        }
      },
      onDismiss = { showBulkDeleteConfirm = false }
    )
  }

  // Group transactions by date for clean chronological display
  val groupedTransactions = remember(transactions) {
    val todayKey = DateUtils.getDayKey(Calendar.getInstance())
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
    val yesterdayKey = DateUtils.getDayKey(yesterdayCal)

    transactions.groupBy { item ->
      val itemKey = DateUtils.getDayKey(item.timestamp)
      when (itemKey) {
        todayKey -> "Today"
        yesterdayKey -> "Yesterday"
        else -> DateUtils.getDisplayDate(item.timestamp)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("transactions_screen")
  ) {
    // 1. Top Section (Search / Multi-select Action Bar)
    Surface(
      color = MaterialTheme.colorScheme.surface,
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (isMultiSelectMode) {
          // Multi-Select Action Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              IconButton(onClick = {
                isMultiSelectMode = false
                selectedIds.clear()
              }) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
              }
              Text(
                text = "${selectedIds.size} selected",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(
                onClick = {
                  if (selectedIds.size == transactions.size) {
                    selectedIds.clear()
                  } else {
                    selectedIds.clear()
                    selectedIds.addAll(transactions.map { it.id })
                  }
                }
              ) {
                Text(if (selectedIds.size == transactions.size) "Deselect All" else "Select All")
              }

              Button(
                onClick = { showBulkDeleteConfirm = true },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MinimalRose),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete (${selectedIds.size})")
              }
            }
          }
        } else {
          // Standard Search Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { viewModel.setSearchQuery(it) },
              modifier = Modifier
                .weight(1f)
                .testTag("search_transactions_input"),
              placeholder = { Text("Search by title, note, or amount...", style = MaterialTheme.typography.bodyMedium) },
              leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { viewModel.setSearchQuery("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
              )
            )

            OutlinedButton(
              onClick = { isMultiSelectMode = true },
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text("Select", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Filter Chips Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // All Types
            FilterChip(
              selected = filterType == null,
              onClick = { viewModel.setFilterType(null) },
              label = { Text("All Types") },
              shape = RoundedCornerShape(10.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                selectedLabelColor = MaterialTheme.colorScheme.primary
              )
            )

            // Expense Chip
            FilterChip(
              selected = filterType == TransactionType.EXPENSE,
              onClick = { viewModel.setFilterType(if (filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
              label = { Text("Expenses") },
              shape = RoundedCornerShape(10.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                selectedLabelColor = ExpenseRed
              )
            )

            // Income Chip
            FilterChip(
              selected = filterType == TransactionType.INCOME,
              onClick = { viewModel.setFilterType(if (filterType == TransactionType.INCOME) null else TransactionType.INCOME) },
              label = { Text("Income") },
              shape = RoundedCornerShape(10.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                selectedLabelColor = IncomeGreen
              )
            )

            // Category Chips
            TransactionCategory.values().forEach { cat ->
              FilterChip(
                selected = filterCategory == cat,
                onClick = { viewModel.setFilterCategory(if (filterCategory == cat) null else cat) },
                label = { Text(cat.displayName) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = cat.color.copy(alpha = 0.15f),
                  selectedLabelColor = cat.color
                )
              )
            }
          }
        }
      }
    }

    // 2. Transaction List
    if (transactions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Filled.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = if (searchQuery.isNotEmpty() || filterType != null || filterCategory != null) "No matching transactions found" else "No transactions yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Try adjusting your filters or search keyword",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        groupedTransactions.forEach { (dateHeader, items) ->
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = dateHeader,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${items.size} items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
            }
          }

          items(items, key = { it.id }) { item ->
            val isSelected = selectedIds.contains(item.id)

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                  onClick = {
                    if (isMultiSelectMode) {
                      if (isSelected) selectedIds.remove(item.id) else selectedIds.add(item.id)
                    } else {
                      onEditTransaction(item)
                    }
                  },
                  onLongClick = {
                    if (!isMultiSelectMode) {
                      isMultiSelectMode = true
                      selectedIds.add(item.id)
                    }
                  }
                )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  if (isMultiSelectMode) {
                    Icon(
                      imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                      contentDescription = null,
                      tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(24.dp)
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(item.category.color.copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = item.category.icon,
                        contentDescription = null,
                        tint = item.category.color,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${item.category.displayName} • ${item.paymentMethod.displayName}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "${if (item.type == TransactionType.EXPENSE) "-" else "+"} ${IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (item.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                  )

                  if (!isMultiSelectMode) {
                    IconButton(
                      onClick = { transactionToDelete = item },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
