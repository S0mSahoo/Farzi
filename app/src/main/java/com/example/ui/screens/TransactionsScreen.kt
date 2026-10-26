package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import com.example.ui.util.LocalAdaptiveInfo
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

  val adaptiveInfo = LocalAdaptiveInfo.current
  val horizontalPadding = if (adaptiveInfo.isLandscapePhone) 12.dp else 20.dp
  val verticalPadding = if (adaptiveInfo.isLandscapePhone) 8.dp else 14.dp

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

  // Group transactions by date for a clean chronological display
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

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("transactions_screen"),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 860.dp)
    ) {
      // 1. Search Bar & Filters Section
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Find and manage your records",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
            ) {
              Text(
                text = transactions.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
              )
            }
          }

        Spacer(modifier = Modifier.height(12.dp))

        // Search input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.setSearchQuery(it) },
          placeholder = {
            Text(
              "Search transactions, categories, notes...",
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transactions_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Type row (All, Expenses, Income)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = filterType == null,
            onClick = { viewModel.setFilterType(null) },
            label = { Text("All", fontWeight = if (filterType == null) FontWeight.Bold else FontWeight.Normal) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("filter_all")
          )
          FilterChip(
            selected = filterType == TransactionType.EXPENSE,
            onClick = {
              viewModel.setFilterType(if (filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
            },
            label = { Text("Expenses", fontWeight = if (filterType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ExpenseRed.copy(alpha = 0.12f),
              selectedLabelColor = ExpenseRed
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("filter_expenses")
          )
          FilterChip(
            selected = filterType == TransactionType.INCOME,
            onClick = {
              viewModel.setFilterType(if (filterType == TransactionType.INCOME) null else TransactionType.INCOME)
            },
            label = { Text("Income", fontWeight = if (filterType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = IncomeGreen.copy(alpha = 0.12f),
              selectedLabelColor = IncomeGreen
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("filter_income")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category filter chips (Horizontal scrolling)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          TransactionCategory.values().forEach { category ->
            val isSelected = filterCategory == category
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) category.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                  viewModel.setFilterCategory(if (isSelected) null else category)
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = category.icon,
                  contentDescription = null,
                  tint = if (isSelected) Color.White else category.color,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = category.displayName,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }

    // 2. Transactions List / Empty State
    if (transactions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "No matching transactions found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Try clearing filters or search query.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(16.dp))
          if (searchQuery.isNotEmpty() || filterType != null || filterCategory != null) {
            OutlinedButton(
              onClick = {
                viewModel.setSearchQuery("")
                viewModel.setFilterType(null)
                viewModel.setFilterCategory(null)
              },
              shape = RoundedCornerShape(18.dp)
            ) {
              Text("Reset Filters")
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = horizontalPadding,
          end = horizontalPadding,
          top = 12.dp,
          bottom = if (adaptiveInfo.isLandscapePhone) 32.dp else 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        groupedTransactions.forEach { (dateHeader, itemsInGroup) ->
          item(key = "header_$dateHeader", contentType = "header") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = dateHeader,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              val groupTotal = itemsInGroup.sumOf {
                if (it.type == TransactionType.INCOME) it.amount else -it.amount
              }
              val groupTotalLabel = IndianCurrencyFormatter.formatWithSymbol(groupTotal, currencySymbol, includeSign = true)
              Text(
                text = if (groupTotalLabel.length > 13) IndianCurrencyFormatter.formatCompact(groupTotal, currencySymbol) else groupTotalLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (groupTotal >= 0) IncomeGreen else ExpenseRed,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          items(itemsInGroup, key = { it.id }, contentType = { "transaction_item" }) { item ->
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 0.dp,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable { onEditTransaction(item) }
                .testTag("transaction_item_${item.id}")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  // Category Icon badge
                  Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = item.category.color.copy(alpha = 0.14f),
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = item.category.icon,
                        contentDescription = null,
                        tint = item.category.color,
                        modifier = Modifier.size(22.dp)
                      )
                    }
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = "${item.category.displayName} • ${item.paymentMethod.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )

                      if (item.isRecurring) {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                          ) {
                            Icon(Icons.Default.Repeat, contentDescription = "Recurring", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                            Text("Recurring", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                          }
                        }
                      }
                    }

                    if (item.note.isNotBlank()) {
                      Text(
                        text = item.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = IndianCurrencyFormatter.formatWithSymbol(
                      amount = item.amount,
                      symbol = currencySymbol,
                      includeSign = true
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                    maxLines = 1,
                    softWrap = false
                  )

                  IconButton(
                    onClick = { transactionToDelete = item },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete",
                      tint = ExpenseRed.copy(alpha = 0.7f),
                      modifier = Modifier.size(18.dp)
                    )
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

