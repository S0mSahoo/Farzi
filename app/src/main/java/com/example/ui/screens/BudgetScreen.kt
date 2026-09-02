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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun BudgetScreen(
  viewModel: FinanceViewModel,
  onOpenSetBudget: () -> Unit
) {
  val selectedCalendar by viewModel.budgetCalendar.collectAsState()
  val allBudgets by viewModel.allBudgets.collectAsState()
  val allTransactions by viewModel.allTransactions.collectAsState()
  val monthlySummary by viewModel.budgetMonthSummary.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()

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

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("budget_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // 1. Month Navigation Header
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = { viewModel.previousBudgetMonth() }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.clickable { showMonthPicker = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = DateUtils.getMonthLabel(selectedCalendar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
          }

          IconButton(onClick = { viewModel.nextBudgetMonth() }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
          }
        }
      }
    }

    // 2. Main Budget Overview Card / Empty State
    if (currentBudget == null || currentBudget.totalBudget <= 0) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(56.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.AccountBalanceWallet,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(28.dp)
                )
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
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
              onClick = onOpenSetBudget,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.testTag("create_budget_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Set Budget for ${DateUtils.getMonthLabel(selectedCalendar).split(" ").firstOrNull() ?: ""}")
            }
          }
        }
      }
    } else {
      // Configured Monthly Budget
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
                IconButton(onClick = { showDeleteBudgetDialog = true }) {
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

      // 3. Category Budgets Allocations
      if (currentBudget.categoryBudgets.isNotEmpty()) {
        item {
          Text(
            text = "Category Budget Targets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        items(currentBudget.categoryBudgets.entries.toList(), key = { it.key.name }) { (category, limit) ->
          val spent = categoryActualSpending[category] ?: 0.0
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
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = category.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
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

              Spacer(modifier = Modifier.height(10.dp))

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
      }
    }
  }
}
