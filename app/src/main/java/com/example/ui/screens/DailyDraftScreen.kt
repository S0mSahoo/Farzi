package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveModule
import com.example.data.model.MonthlySalaryLog
import com.example.data.model.TimeRangeFilter
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.FilterChipItem
import com.example.ui.components.SearchAndFilterBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DailyDraftScreen(
  uiState: FinanceUiState,
  onSearchQueryChange: (String) -> Unit,
  onTimeRangeSelected: (TimeRangeFilter) -> Unit,
  onActiveModuleSelected: (ActiveModule) -> Unit,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onResetCurrentMonth: () -> Unit,
  onTypeFilterSelected: (TransactionType?) -> Unit,
  onCategoryFilterSelected: (TransactionCategory?) -> Unit,
  onItemClick: (TransactionItem) -> Unit,
  onAddClick: () -> Unit,
  onOpenSalarySetup: () -> Unit,
  onOpenDraftMonthSalaryDialog: () -> Unit
) {
  val context = LocalContext.current
  val currencySymbol = uiState.salarySettings.currencySymbol
  var showCalendarFilterDialog by remember { mutableStateOf(false) }

  if (showCalendarFilterDialog) {
    val cal = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val chosenCal = Calendar.getInstance().apply {
          set(year, month, dayOfMonth)
        }
        val dateSearchStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(chosenCal.time)
        onSearchQueryChange(dateSearchStr)
        showCalendarFilterDialog = false
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.setOnDismissListener {
      showCalendarFilterDialog = false
    }
    DisposableEffect(showCalendarFilterDialog) {
      datePickerDialog.show()
      onDispose {
        datePickerDialog.dismiss()
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(bottom = 96.dp)
  ) {
    // 1. Month Navigation Header
    item(key = "month_nav_header", contentType = "header") {
      MonthNavigatorBar(
        selectedMonthLabel = uiState.selectedMonthLabel,
        isCurrentMonth = uiState.isCurrentMonthSelected,
        onPrevMonth = onPrevMonth,
        onNextMonth = onNextMonth,
        onResetCurrentMonth = onResetCurrentMonth,
        onPickDate = { showCalendarFilterDialog = true }
      )
    }

    // 2. Module Selector Tabs (All Drafts | Expenses Module | Income & Salary Module)
    item(key = "module_selector", contentType = "module_tabs") {
      ModuleSelectorRow(
        activeModule = uiState.activeModule,
        onModuleSelected = onActiveModuleSelected
      )
    }

    // 3. Module-Specific Hero Card
    item(key = "hero_summary_card", contentType = "hero") {
      when (uiState.activeModule) {
        ActiveModule.ALL -> {
          HeroCashflowCard(
            uiState = uiState,
            currencySymbol = currencySymbol,
            onOpenSalarySetup = onOpenSalarySetup
          )
        }
        ActiveModule.EXPENSE -> {
          ExpenseModuleHeroCard(
            uiState = uiState,
            currencySymbol = currencySymbol,
            onOpenSalarySetup = onOpenSalarySetup,
            onAddExpenseClick = onAddClick
          )
        }
        ActiveModule.INCOME -> {
          IncomeModuleHeroCard(
            uiState = uiState,
            currencySymbol = currencySymbol,
            onDraftSalaryClick = onOpenDraftMonthSalaryDialog,
            onAddIncomeClick = onAddClick
          )
        }
      }
    }

    // 4. Search & Filters
    item(key = "search_and_filters", contentType = "filters") {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SearchAndFilterBar(
          query = uiState.searchQuery,
          onQueryChange = onSearchQueryChange,
          placeholder = "Search drafts, notes, categories..."
        )

        // Category / Type filter pills when inside Expense or All module
        if (uiState.activeModule == ActiveModule.EXPENSE) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
          ) {
            item(key = "all_exp_cat") {
              FilterChipItem(
                label = "All Categories",
                isSelected = uiState.selectedCategory == null,
                onClick = { onCategoryFilterSelected(null) }
              )
            }
            TransactionCategory.entries
              .filter { it.defaultType == TransactionType.EXPENSE }
              .forEach { cat ->
                item(key = "cat_${cat.name}") {
                  FilterChipItem(
                    label = cat.displayName,
                    isSelected = uiState.selectedCategory == cat,
                    onClick = {
                      onCategoryFilterSelected(if (uiState.selectedCategory == cat) null else cat)
                    },
                    icon = cat.icon
                  )
                }
              }
          }
        } else if (uiState.activeModule == ActiveModule.INCOME) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
          ) {
            item(key = "all_inc_cat") {
              FilterChipItem(
                label = "All Income Streams",
                isSelected = uiState.selectedCategory == null,
                onClick = { onCategoryFilterSelected(null) }
              )
            }
            TransactionCategory.entries
              .filter { it.defaultType != TransactionType.EXPENSE }
              .forEach { cat ->
                item(key = "cat_${cat.name}") {
                  FilterChipItem(
                    label = cat.displayName,
                    isSelected = uiState.selectedCategory == cat,
                    onClick = {
                      onCategoryFilterSelected(if (uiState.selectedCategory == cat) null else cat)
                    },
                    icon = cat.icon,
                    activeColor = IncomeGreen
                  )
                }
              }
          }
        }
      }
    }

    // 5. Income Module Salary History section if in Income Module and we have multiple months
    if (uiState.activeModule == ActiveModule.INCOME && uiState.monthlySalaryLogs.isNotEmpty()) {
      item(key = "salary_history_section", contentType = "salary_history") {
        SalaryHistoryCard(
          salaryLogs = uiState.monthlySalaryLogs,
          currencySymbol = currencySymbol,
          onDraftNewMonthSalary = onOpenDraftMonthSalaryDialog
        )
      }
    }

    // 6. Draft List or Empty State
    if (uiState.groupedDrafts.isEmpty()) {
      item(key = "empty_state", contentType = "empty") {
        EmptyDraftsView(
          hasQuery = uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null,
          activeModule = uiState.activeModule,
          onAddDraftClick = onAddClick,
          onDraftSalaryClick = onOpenDraftMonthSalaryDialog
        )
      }
    } else {
      uiState.groupedDrafts.forEach { group ->
        // Date Group Header
        item(key = "header_${group.dateKey}", contentType = "header") {
          DateGroupHeader(
            headerTitle = group.headerTitle,
            dayExpense = group.totalExpense,
            dayIncome = group.totalIncome,
            count = group.count,
            currencySymbol = currencySymbol
          )
        }

        // Items in this date group (Ultra-fast scrolling: pre-formatted strings, stable keys)
        items(
          items = group.items,
          key = { it.id },
          contentType = { "transaction_item" }
        ) { item ->
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
fun MonthNavigatorBar(
  selectedMonthLabel: String,
  isCurrentMonth: Boolean,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onResetCurrentMonth: () -> Unit,
  onPickDate: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onPrevMonth,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ArrowBackIosNew,
          contentDescription = "Previous Month",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(16.dp)
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onPickDate)
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = "Calendar",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = selectedMonthLabel,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        if (!isCurrentMonth) {
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onResetCurrentMonth)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Today",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      IconButton(
        onClick = onNextMonth,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ArrowForwardIos,
          contentDescription = "Next Month",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
fun ModuleSelectorRow(
  activeModule: ActiveModule,
  onModuleSelected: (ActiveModule) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    ActiveModule.entries.forEach { module ->
      val isSelected = activeModule == module
      val bg = when {
        isSelected && module == ActiveModule.EXPENSE -> ExpenseRed
        isSelected && module == ActiveModule.INCOME -> IncomeGreen
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
      }
      val textCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .clickable { onModuleSelected(module) }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = module.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 12.sp
            ),
            color = textCol,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
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
            text = "NET CASHFLOW • ${uiState.selectedMonthLabel.uppercase(Locale.getDefault())}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 10.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenSalarySetup)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Savings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Base: ${formatCurrency(uiState.salarySettings.salaryAmount, currencySymbol)}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp
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
            fontSize = 30.sp,
            letterSpacing = (-0.5).sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (uiState.savingsRate > 0) "Savings rate: ${String.format(Locale.US, "%.1f", uiState.savingsRate)}% of cash drafted" else "All monthly drafts balanced",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Minimalist 3-Stat Container Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Income Mini Box
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = IncomeGreen,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Income",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
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
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = ExpenseRed,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Expense",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
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
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1.1f)
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              "Safe Daily",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
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
fun ExpenseModuleHeroCard(
  uiState: FinanceUiState,
  currencySymbol: String,
  onOpenSalarySetup: () -> Unit,
  onAddExpenseClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
              .background(ExpenseRed)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "EXPENSE MODULE • ${uiState.selectedMonthLabel.uppercase(Locale.getDefault())}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = ExpenseRed.copy(alpha = 0.12f),
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onAddExpenseClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Log Expense", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ExpenseRed)
          }
        }
      }

      // Big Expense Total
      Column {
        Text(
          text = formatCurrency(uiState.totalExpense, currencySymbol),
          style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            letterSpacing = (-0.5).sp
          ),
          color = ExpenseRed
        )
        Text(
          text = "Budget Goal: ${formatCurrency(uiState.salarySettings.monthlyBudgetGoal, currencySymbol)} • ${String.format(Locale.US, "%.0f", uiState.budgetProgress * 100)}% used",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Budget Progress Meter
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
          progress = { uiState.budgetProgress.coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = if (uiState.budgetProgress > 0.9f) ExpenseRed else AccentIndigo,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }

      // Safe Daily & Remaining Budget info row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Remaining Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            formatCurrency(uiState.remainingBudget, currencySymbol),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Daily Safe Burn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            formatCurrency(uiState.dailySafeSpend, currencySymbol) + "/day (${uiState.daysRemainingInMonth}d left)",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
fun IncomeModuleHeroCard(
  uiState: FinanceUiState,
  currencySymbol: String,
  onDraftSalaryClick: () -> Unit,
  onAddIncomeClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
              .background(IncomeGreen)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "INCOME MODULE • ${uiState.selectedMonthLabel.uppercase(Locale.getDefault())}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = IncomeGreen.copy(alpha = 0.12f),
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onAddIncomeClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Other Income", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = IncomeGreen)
          }
        }
      }

      // Big Income Total
      Column {
        Text(
          text = formatCurrency(uiState.totalIncome, currencySymbol),
          style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            letterSpacing = (-0.5).sp
          ),
          color = IncomeGreen
        )
        Text(
          text = if (uiState.isSalaryDraftedForSelectedMonth) {
            "Includes ${formatCurrency(uiState.currentMonthSalaryDrafted, currencySymbol)} salary drafted for ${uiState.selectedMonthLabel}"
          } else {
            "No salary drafted yet for ${uiState.selectedMonthLabel}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Per-Month Salary Action Card
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = IncomeGreen.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (uiState.isSalaryDraftedForSelectedMonth) Icons.Default.CheckCircle else Icons.Default.Work,
              contentDescription = null,
              tint = IncomeGreen,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "${uiState.selectedMonthLabel} Salary",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (uiState.isSalaryDraftedForSelectedMonth) "Drafted: ${formatCurrency(uiState.currentMonthSalaryDrafted, currencySymbol)}" else "Set custom salary for this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = IncomeGreen,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onDraftSalaryClick)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (uiState.isSalaryDraftedForSelectedMonth) "Edit Salary" else "Draft Salary",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SalaryHistoryCard(
  salaryLogs: List<MonthlySalaryLog>,
  currencySymbol: String,
  onDraftNewMonthSalary: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Month-by-Month Salary History",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Text(
          text = "+ Draft Other Month",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clickable(onClick = onDraftNewMonthSalary)
        )
      }

      salaryLogs.take(4).forEach { log ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = log.monthLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${log.dateFormatted} • ${log.paymentMethod.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "+${formatCurrency(log.amount, currencySymbol)}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = IncomeGreen
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
      .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = headerTitle,
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp
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
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = ExpenseRed
        )
      }
      if (dayIncome > 0) {
        Text(
          text = "+${formatCurrency(dayIncome, currencySymbol)}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
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
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 3.5.dp)
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
          size = 40,
          iconSize = 19
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
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Text(
              text = item.category.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
              text = item.formattedTime.ifEmpty { "Draft" },
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
              text = item.paymentMethod.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          if (item.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.note,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Right: Amount (using pre-formatted cache for smooth scrolling)
      Column(horizontalAlignment = Alignment.End) {
        val isExp = item.type == TransactionType.EXPENSE
        Text(
          text = (if (isExp) "- " else "+ ") + item.formattedAmount.ifEmpty { formatCurrency(item.amount, currencySymbol) },
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp
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
  activeModule: ActiveModule,
  onAddDraftClick: () -> Unit,
  onDraftSalaryClick: () -> Unit
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.size(68.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = when (activeModule) {
              ActiveModule.INCOME -> Icons.Default.Work
              ActiveModule.EXPENSE -> Icons.Default.ReceiptLong
              ActiveModule.ALL -> Icons.Default.ReceiptLong
            },
            contentDescription = null,
            tint = when (activeModule) {
              ActiveModule.INCOME -> IncomeGreen
              ActiveModule.EXPENSE -> ExpenseRed
              ActiveModule.ALL -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(32.dp)
          )
        }
      }

      Text(
        text = if (hasQuery) "No matching drafts found" else "No ${activeModule.shortName} Drafts for this Month",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = if (hasQuery) "Try adjusting your search keywords or category filters" else "Draft your first transaction for this month using the button below.",
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      if (!hasQuery) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (activeModule == ActiveModule.INCOME) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = IncomeGreen,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onDraftSalaryClick)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Draft Month Salary", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (activeModule == ActiveModule.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable(onClick = onAddDraftClick)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (activeModule == ActiveModule.EXPENSE) "Log Expense" else "Add Draft",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}
