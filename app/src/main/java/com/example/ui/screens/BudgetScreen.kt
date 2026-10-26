package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetModel
import com.example.data.model.MonthlyFinancialSummary as MonthlySummary
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun BudgetScreen(
  viewModel: FinanceViewModel,
  onOpenSetBudget: () -> Unit
) {
  val selectedCalendar by viewModel.budgetCalendar.collectAsStateWithLifecycle()
  val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()
  val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
  val monthlySummary by viewModel.budgetMonthSummary.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

  val adaptiveInfo = LocalAdaptiveInfo.current

  var showMonthPicker by remember { mutableStateOf(false) }
  var showDeleteBudgetDialog by remember { mutableStateOf(false) }

  val currencySymbol = userProfile.currencySymbol
  val monthKey = DateUtils.getMonthKey(selectedCalendar)
  val currentBudget = remember(allBudgets, monthKey) {
    allBudgets.find { it.monthKey == monthKey }
  }

  // Calculate actual spending per category in current month
  val startOfMonth = DateUtils.getStartOfMonth(selectedCalendar)
  val endOfMonth = DateUtils.getEndOfMonth(selectedCalendar)
  val categoryActualSpending = remember(allTransactions, startOfMonth, endOfMonth) {
    allTransactions
      .filter { it.timestamp in startOfMonth..endOfMonth && it.type == TransactionType.EXPENSE }
      .groupBy { it.category }
      .mapValues { (_, list) -> list.sumOf { it.amount } }
  }

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentCalendar = selectedCalendar,
      onDismiss = { showMonthPicker = false },
      onMonthYearSelected = { year, month ->
        viewModel.setBudgetMonthAndYear(year, month)
      }
    )
  }

  if (showDeleteBudgetDialog) {
    ConfirmationDialog(
      title = "Delete Budget Plan?",
      message = "Are you sure you want to remove the budget plan for ${monthlySummary.monthLabel}?",
      onConfirm = {
        viewModel.deleteBudget(monthKey)
        showDeleteBudgetDialog = false
      },
      onDismiss = { showDeleteBudgetDialog = false }
    )
  }

  val horizontalPadding = if (adaptiveInfo.isLandscapePhone) 12.dp else 20.dp
  val verticalSpacing = if (adaptiveInfo.isLandscapePhone) 10.dp else 16.dp

  if (adaptiveInfo.useTwoPaneLayout) {
    // 2-Pane Adaptive Layout for Tablets and Wide Windows
    Row(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = horizontalPadding, vertical = 12.dp)
        .testTag("budget_screen"),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Left Pane: Month Nav & Main Budget Overview Card
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        BudgetMonthHeader(
          selectedCalendar = selectedCalendar,
          onPrevious = { viewModel.previousBudgetMonth() },
          onNext = { viewModel.nextBudgetMonth() },
          onOpenPicker = { showMonthPicker = true }
        )

        BudgetOverviewCard(
          currentBudget = currentBudget,
          monthlySummary = monthlySummary,
          currencySymbol = currencySymbol,
          onOpenSetBudget = onOpenSetBudget,
          onDeleteBudget = { showDeleteBudgetDialog = true }
        )
      }

      // Right Pane: Category Targets List
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Category Budget Targets",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        if (currentBudget != null && currentBudget.categoryBudgets.isNotEmpty()) {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
          ) {
            items(currentBudget.categoryBudgets.entries.toList(), key = { it.key.name }) { (category, limit) ->
              CategoryBudgetCard(
                category = category,
                limit = limit,
                spent = categoryActualSpending[category] ?: 0.0,
                currencySymbol = currencySymbol
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier.padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No category targets set for this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  } else {
    // Single-Pane Column Layout for Phone Portrait & Landscape
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("budget_screen"),
      contentPadding = PaddingValues(
        start = horizontalPadding,
        end = horizontalPadding,
        top = if (adaptiveInfo.isLandscapePhone) 8.dp else 16.dp,
        bottom = if (adaptiveInfo.isLandscapePhone) 32.dp else 96.dp
      ),
      verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
      // 1. Month Navigation Header
      item {
        BudgetMonthHeader(
          selectedCalendar = selectedCalendar,
          onPrevious = { viewModel.previousBudgetMonth() },
          onNext = { viewModel.nextBudgetMonth() },
          onOpenPicker = { showMonthPicker = true }
        )
      }

      // 2. Main Budget Overview Card / Empty State
      item {
        BudgetOverviewCard(
          currentBudget = currentBudget,
          monthlySummary = monthlySummary,
          currencySymbol = currencySymbol,
          onOpenSetBudget = onOpenSetBudget,
          onDeleteBudget = { showDeleteBudgetDialog = true }
        )
      }

      // 3. Category Budgets Allocations
      if (currentBudget != null && currentBudget.categoryBudgets.isNotEmpty()) {
        item {
          Text(
            text = "Category Budget Targets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }

        items(currentBudget.categoryBudgets.entries.toList(), key = { it.key.name }) { (category, limit) ->
          CategoryBudgetCard(
            category = category,
            limit = limit,
            spent = categoryActualSpending[category] ?: 0.0,
            currencySymbol = currencySymbol
          )
        }
      }
    }
  }
}

@Composable
private fun BudgetMonthHeader(
  selectedCalendar: Calendar,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onOpenPicker: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onPrevious) {
        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable { onOpenPicker() }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = DateUtils.getMonthLabel(selectedCalendar),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        }
      }

      IconButton(onClick = onNext) {
        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
      }
    }
  }
}

@Composable
private fun BudgetOverviewCard(
  currentBudget: BudgetModel?,
  monthlySummary: MonthlySummary,
  currencySymbol: String,
  onOpenSetBudget: () -> Unit,
  onDeleteBudget: () -> Unit
) {
  if (currentBudget == null || currentBudget.totalBudget <= 0) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(56.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "No Budget Configured",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Set a total monthly spending limit and allocate category budgets for ${monthlySummary.monthLabel}.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onOpenSetBudget,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("create_budget_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Set Budget for ${monthlySummary.monthLabel.split(" ").firstOrNull() ?: ""}")
        }
      }
    }
  } else {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Monthly Spending Budget",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Limit: ${IndianCurrencyFormatter.formatWithSymbol(currentBudget.totalBudget, currencySymbol)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onOpenSetBudget) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Budget", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDeleteBudget) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Budget", tint = ExpenseRed)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        BudgetProgressBar(
          budgetLimit = currentBudget.totalBudget,
          budgetUsed = monthlySummary.totalExpense,
          currencySymbol = currencySymbol
        )
      }
    }
  }
}

@Composable
private fun CategoryBudgetCard(
  category: TransactionCategory,
  limit: Double,
  spent: Double,
  currencySymbol: String
) {
  val ratio = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
  val isExceeded = spent > limit
  val isWarning = ratio >= 0.8f && !isExceeded
  val barColor = when {
    isExceeded -> ExpenseRed
    isWarning -> AccentAmber
    else -> category.color
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = category.color.copy(alpha = 0.12f),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(20.dp))
            }
          }

          Column {
            Text(
              text = category.displayName,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Target: ${IndianCurrencyFormatter.formatWithSymbol(limit, currencySymbol)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = IndianCurrencyFormatter.formatWithSymbol(spent, currencySymbol),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (isExceeded) "Exceeded by ${IndianCurrencyFormatter.formatWithSymbol(spent - limit, currencySymbol)}"
            else "${IndianCurrencyFormatter.formatWithSymbol(limit - spent, currencySymbol)} left",
            style = MaterialTheme.typography.labelSmall,
            color = if (isExceeded) ExpenseRed else if (isWarning) AccentAmber else IncomeGreen,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LinearProgressIndicator(
        progress = { ratio },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = barColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round
      )
    }
  }
}
