package com.example.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.BudgetModel
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumMonthNavigator
import com.example.ui.components.PremiumSectionHeading
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
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
  val summary by viewModel.budgetMonthSummary.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  var showMonthPicker by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  val currencySymbol = userProfile.currencySymbol
  val monthKey = DateUtils.getMonthKey(selectedCalendar)
  val currentBudget = remember(allBudgets, monthKey) { allBudgets.firstOrNull { it.monthKey == monthKey } }
  val start = DateUtils.getStartOfMonth(selectedCalendar)
  val end = DateUtils.getEndOfMonth(selectedCalendar)
  val categoryActual = remember(allTransactions, start, end) {
    allTransactions
      .filter { it.timestamp in start..end && it.type == TransactionType.EXPENSE }
      .groupBy { it.category }
      .mapValues { (_, rows) -> rows.sumOf { it.amount } }
  }
  val configuredCategories = remember(currentBudget) {
    currentBudget?.categoryBudgets?.entries
      ?.filter { it.value > 0.0 }
      ?.sortedByDescending { it.value }
      ?: emptyList()
  }
  val unbudgeted = remember(categoryActual, currentBudget) {
    categoryActual.entries
      .filter { it.value > 0.0 && (currentBudget?.categoryBudgets?.get(it.key) ?: 0.0) <= 0.0 }
      .sortedByDescending { it.value }
      .take(4)
  }

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentCalendar = selectedCalendar,
      onDismiss = { showMonthPicker = false },
      onMonthYearSelected = { year, month ->
        viewModel.setBudgetMonthAndYear(year, month)
        showMonthPicker = false
      }
    )
  }
  if (showDeleteDialog) {
    ConfirmationDialog(
      title = "Delete this budget?",
      message = "Remove the budget plan for ${summary.monthLabel}? Your transactions will remain untouched.",
      confirmButtonText = "Delete budget",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        viewModel.deleteBudget(monthKey)
        showDeleteDialog = false
      },
      onDismiss = { showDeleteDialog = false }
    )
  }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize().testTag("budget_screen"),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          PremiumSectionHeading(
            title = "Budgets",
            subtitle = "Make room for what matters.",
            modifier = Modifier.weight(1f)
          )
          PremiumMonthNavigator(
            label = summary.monthLabel,
            onPrevious = { viewModel.previousBudgetMonth() },
            onNext = { viewModel.nextBudgetMonth() },
            onPick = { showMonthPicker = true }
          )
        }
      }

      item {
        if (currentBudget == null || currentBudget.totalBudget <= 0.0) {
          PremiumCard(containerColor = MaterialTheme.colorScheme.surface) {
            PremiumEmptyState(
              title = "Build your first monthly plan",
              body = "Set an overall limit, add category caps, and get a clearer view of what is left.",
              action = {
                androidx.compose.material3.Button(onClick = onOpenSetBudget) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.size(7.dp))
                  Text("Create budget")
                }
              }
            )
          }
        } else {
          BudgetHeroCard(
            budget = currentBudget,
            summary = summary,
            currencySymbol = currencySymbol,
            selectedCalendar = selectedCalendar,
            onEdit = onOpenSetBudget,
            onDelete = { showDeleteDialog = true }
          )
        }
      }

      item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
          com.example.ui.components.PremiumMetricTile(
            title = "Spent so far",
            value = IndianCurrencyFormatter.formatCompact(summary.totalExpense, currencySymbol),
            caption = "${summary.transactionCount} recorded transactions",
            positive = false,
            modifier = Modifier.weight(1f)
          )
          com.example.ui.components.PremiumMetricTile(
            title = "Income recorded",
            value = IndianCurrencyFormatter.formatCompact(summary.totalIncome, currencySymbol),
            caption = summary.monthLabel,
            positive = true,
            modifier = Modifier.weight(1f)
          )
        }
      }

      item {
        PremiumSectionHeading(
          title = "Category limits",
          subtitle = "Compare planned amounts with recorded spending.",
          trailing = {
            OutlinedButton(onClick = onOpenSetBudget, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(6.dp))
              Text(if (currentBudget == null) "Set up" else "Edit")
            }
          }
        )
      }

      if (configuredCategories.isEmpty()) {
        item {
          PremiumCard {
            PremiumEmptyState(
              title = "No category limits yet",
              body = "Add caps for rent, groceries, transport, subscriptions and other categories to make the plan more useful.",
              action = {
                OutlinedButton(onClick = onOpenSetBudget) { Text("Add category limits") }
              }
            )
          }
        }
      } else {
        val list = configuredCategories
        items(list.size, key = { "budget_cat_${list[it].key.name}" }) { index ->
          val (category, limit) = list[index]
          val spent = categoryActual[category] ?: 0.0
          CategoryLimitCard(
            category = category,
            limit = limit,
            spent = spent,
            currencySymbol = currencySymbol
          )
        }
      }

      item {
        PremiumSectionHeading(
          title = "Spending outside category caps",
          subtitle = "Categories with transactions but no individual limit."
        )
      }
      if (unbudgeted.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              "Nice work — every category with spending has a limit, or there is no spending recorded outside the plan.",
              modifier = Modifier.padding(16.dp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      } else {
        item {
          PremiumCard {
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              unbudgeted.forEach { entry ->
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Surface(shape = RoundedCornerShape(12.dp), color = entry.key.color.copy(alpha = 0.12f)) {
                    Icon(entry.key.icon, contentDescription = null, tint = entry.key.color, modifier = Modifier.padding(9.dp).size(18.dp))
                  }
                  Text(entry.key.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                  Text(IndianCurrencyFormatter.formatWithSymbol(entry.value, currencySymbol), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
      item { Spacer(Modifier.height(10.dp)) }
    }
  }
}

@Composable
private fun BudgetHeroCard(
  budget: BudgetModel,
  summary: MonthlyFinancialSummary,
  currencySymbol: String,
  selectedCalendar: Calendar,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val limit = budget.totalBudget.coerceAtLeast(0.0)
  val spent = summary.totalExpense.coerceAtLeast(0.0)
  val used = if (limit > 0.0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
  val remaining = limit - spent
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = Color(0xFF163D2B),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text("MONTHLY SPENDING LIMIT", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
          Text(IndianCurrencyFormatter.formatWithSymbol(limit, currencySymbol), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit budget", tint = Color.White)
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete budget", tint = Color.White.copy(alpha = 0.85f))
          }
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.ui.components.PremiumProgressTrack(
          progress = used,
          color = if (used >= 0.9f) Color(0xFFFFB5A9) else Color(0xFFD4EF75),
          trackColor = Color.White.copy(alpha = 0.16f),
          height = 10.dp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("${(used * 100).toInt()}% used", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.78f))
          Text(
            text = if (remaining >= 0) "${IndianCurrencyFormatter.formatWithSymbol(remaining, currencySymbol)} remaining"
              else "${IndianCurrencyFormatter.formatWithSymbol(kotlin.math.abs(remaining), currencySymbol)} over",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (remaining >= 0) Color(0xFFD4EF75) else Color(0xFFFFB5A9)
          )
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Spent", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f))
          Text(IndianCurrencyFormatter.formatCompact(spent, currencySymbol), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Daily guide", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f))
          val now = Calendar.getInstance()
          val isCurrentMonth = now.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)
          val dayInPlan = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else 1
          val days = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
          val remainingDays = (days - dayInPlan + 1).coerceAtLeast(1)
          val daily = (limit - spent).coerceAtLeast(0.0) / remainingDays
          Text(IndianCurrencyFormatter.formatCompact(daily, currencySymbol), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFD4EF75))
        }
      }
    }
  }
}

@Composable
private fun CategoryLimitCard(
  category: TransactionCategory,
  limit: Double,
  spent: Double,
  currencySymbol: String
) {
  val fraction = if (limit > 0.0) (spent / limit).toFloat().coerceAtLeast(0f) else 0f
  val normalized = fraction.coerceIn(0f, 1f)
  val over = spent > limit
  PremiumCard {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Surface(shape = RoundedCornerShape(13.dp), color = category.color.copy(alpha = 0.13f)) {
          Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.padding(10.dp).size(21.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(category.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
          Text(
            if (over) "Above your limit" else "${IndianCurrencyFormatter.formatWithSymbol((limit - spent).coerceAtLeast(0.0), currencySymbol)} left",
            style = MaterialTheme.typography.bodySmall,
            color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(IndianCurrencyFormatter.formatWithSymbol(spent, currencySymbol), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurface)
          Text("of ${IndianCurrencyFormatter.formatWithSymbol(limit, currencySymbol)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      com.example.ui.components.PremiumProgressTrack(
        progress = normalized,
        color = if (over) ExpenseRed else category.color,
        height = 7.dp
      )
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${(fraction * 100).toInt()}% used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (over) {
          Text("${(fraction * 100).toInt() - 100}% over", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
        }
      }
    }
  }
}
