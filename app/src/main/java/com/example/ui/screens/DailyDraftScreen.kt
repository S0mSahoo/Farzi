package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimeRangeFilter
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.FilterChipItem
import com.example.ui.components.SearchAndFilterBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyDraftScreen(
  uiState: FinanceUiState,
  onSearchQueryChange: (String) -> Unit,
  onTimeRangeSelected: (TimeRangeFilter) -> Unit,
  onTypeFilterSelected: (TransactionType?) -> Unit,
  onCategoryFilterSelected: (TransactionCategory?) -> Unit,
  onItemClick: (TransactionItem) -> Unit,
  onAddClick: () -> Unit,
  onOpenSalarySetup: () -> Unit
) {
  val currencySymbol = uiState.salarySettings.currencySymbol

  // Group filtered transactions by Date header (e.g. "Today - Aug 31", "Yesterday - Aug 30", "Aug 25, 2026")
  val groupedTransactions = remember(uiState.filteredTransactions) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayKey = dateFormat.format(Date())
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterdayKey = dateFormat.format(yesterdayCal.time)
    val displayFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    uiState.filteredTransactions
      .groupBy { dateFormat.format(Date(it.timestamp)) }
      .map { (dateKey, items) ->
        val headerTitle = when (dateKey) {
          todayKey -> "Today • ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())}"
          yesterdayKey -> "Yesterday • ${SimpleDateFormat("MMM d", Locale.getDefault()).format(yesterdayCal.time)}"
          else -> items.firstOrNull()?.let { displayFormat.format(Date(it.timestamp)) } ?: dateKey
        }
        val dayExpense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val dayIncome = items.filter { it.type != TransactionType.EXPENSE }.sumOf { it.amount }
        Triple(headerTitle, Triple(dayExpense, dayIncome, items.size), items)
      }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(bottom = 96.dp)
  ) {
    // 1. Hero Summary Card (Net cashflow, income, expense & safe daily allowance)
    item {
      HeroCashflowCard(
        uiState = uiState,
        currencySymbol = currencySymbol,
        onOpenSalarySetup = onOpenSalarySetup
      )
    }

    // 2. Search & Time/Type Filters
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        SearchAndFilterBar(
          query = uiState.searchQuery,
          onQueryChange = onSearchQueryChange,
          placeholder = "Search drafts, notes, merchants..."
        )

        // Type filter pills (All | Expenses | Incomes | Salary)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 2.dp)
        ) {
          item {
            FilterChipItem(
              label = "All Drafts (${uiState.filteredTransactions.size})",
              isSelected = uiState.selectedType == null,
              onClick = { onTypeFilterSelected(null) }
            )
          }
          item {
            FilterChipItem(
              label = "Expenses",
              isSelected = uiState.selectedType == TransactionType.EXPENSE,
              onClick = {
                onTypeFilterSelected(if (uiState.selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
              },
              activeColor = ExpenseRed
            )
          }
          item {
            FilterChipItem(
              label = "Income",
              isSelected = uiState.selectedType == TransactionType.INCOME,
              onClick = {
                onTypeFilterSelected(if (uiState.selectedType == TransactionType.INCOME) null else TransactionType.INCOME)
              },
              activeColor = IncomeGreen
            )
          }
          item {
            FilterChipItem(
              label = "Salary",
              isSelected = uiState.selectedType == TransactionType.SALARY,
              onClick = {
                onTypeFilterSelected(if (uiState.selectedType == TransactionType.SALARY) null else TransactionType.SALARY)
              },
              activeColor = AccentIndigo
            )
          }
        }

        // Time Range Pills (This Month, 7 Days, Last Month, This Year, All Time)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(vertical = 2.dp)
        ) {
          TimeRangeFilter.entries.forEach { range ->
            item {
              val isSel = uiState.timeRange == range
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onTimeRangeSelected(range) }
              ) {
                Text(
                  text = range.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                  ),
                  color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }
        }
      }
    }

    // 3. Draft List or Empty State
    if (groupedTransactions.isEmpty()) {
      item {
        EmptyDraftsView(
          hasQuery = uiState.searchQuery.isNotBlank() || uiState.selectedType != null,
          onAddDraftClick = onAddClick
        )
      }
    } else {
      groupedTransactions.forEach { (header, stats, items) ->
        val (dayExpense, dayIncome, count) = stats

        // Date Group Header
        item {
          DateGroupHeader(
            headerTitle = header,
            dayExpense = dayExpense,
            dayIncome = dayIncome,
            count = count,
            currencySymbol = currencySymbol
          )
        }

        // Items in this date group
        items(items, key = { it.id }) { item ->
          DraftTransactionCard(
            item = item,
            currencySymbol = currencySymbol,
            onClick = { onItemClick(item) }
          )
        }
      }
    }
  }
}

@Composable
fun HeroCashflowCard(
  uiState: FinanceUiState,
  currencySymbol: String,
  onOpenSalarySetup: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top row: Label + Time Range + Salary setup tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (uiState.netSavings >= 0) IncomeGreen else ExpenseRed)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "NET CASHFLOW • ${uiState.timeRange.displayName.uppercase(Locale.getDefault())}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenSalarySetup)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Savings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Salary: ${formatCurrency(uiState.salarySettings.salaryAmount, currencySymbol)}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // Net Amount Big Number
      Column {
        Text(
          text = (if (uiState.netSavings >= 0) "+" else "-") + formatCurrency(Math.abs(uiState.netSavings), currencySymbol),
          style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            letterSpacing = (-1).sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (uiState.savingsRate > 0) "Healthy savings rate: ${String.format("%.1f", uiState.savingsRate)}% of cash drafted" else "All daily drafts balanced",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Minimalist 3-Stat Container Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Income Mini Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = IncomeGreen,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Income",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              formatCurrency(uiState.totalIncome, currencySymbol),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
              color = IncomeGreen,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Expense Mini Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = ExpenseRed,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Expense",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              formatCurrency(uiState.totalExpense, currencySymbol),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
              color = ExpenseRed,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Safe Daily Spend Mini Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1.1f)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              "Safe Daily",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              formatCurrency(uiState.dailySafeSpend, currencySymbol) + "/d",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

@Composable
fun DateGroupHeader(
  headerTitle: String,
  dayExpense: Double,
  dayIncome: Double,
  count: Int,
  currencySymbol: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = headerTitle,
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      ),
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (dayExpense > 0) {
        Text(
          text = "-${formatCurrency(dayExpense, currencySymbol)}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
          ),
          color = ExpenseRed
        )
      }
      if (dayIncome > 0) {
        Text(
          text = "+${formatCurrency(dayIncome, currencySymbol)}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
          ),
          color = IncomeGreen
        )
      }
    }
  }
}

@Composable
fun DraftTransactionCard(
  item: TransactionItem,
  currencySymbol: String,
  onClick: () -> Unit
) {
  val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.timestamp))

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Icon + Title & Category & Note
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        CategoryIconBadge(
          category = item.category,
          size = 42,
          iconSize = 20
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = item.title,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            if (item.isRecurring) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = "Recurring",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = item.category.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Text(
              text = timeStr,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Text(
              text = item.paymentMethod.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.primary
            )
          }

          if (item.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.note,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Right: Amount
      Column(horizontalAlignment = Alignment.End) {
        val isExp = item.type == TransactionType.EXPENSE
        Text(
          text = (if (isExp) "- " else "+ ") + formatCurrency(item.amount, currencySymbol),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          ),
          color = if (isExp) ExpenseRed else IncomeGreen
        )
      }
    }
  }
}

@Composable
fun EmptyDraftsView(
  hasQuery: Boolean,
  onAddDraftClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(72.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
          )
        }
      }

      Text(
        text = if (hasQuery) "No matching drafts found" else "No draft entries yet",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = if (hasQuery) "Try adjusting your filters or search keywords" else "Tap + to quickly draft an expense, income, or monthly salary",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      if (!hasQuery) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onAddDraftClick)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create First Draft", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
          }
        }
      }
    }
  }
}
