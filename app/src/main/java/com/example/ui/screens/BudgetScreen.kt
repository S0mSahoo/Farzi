package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumIconButton
import com.example.ui.components.PremiumMonthNavigator
import com.example.ui.components.PremiumOutlinedButton
import com.example.ui.components.PremiumPageHeader
import com.example.ui.components.PremiumSectionHeading
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.PaisaMotion
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
  val summary by viewModel.budgetMonthSummary.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val coroutineScope = rememberCoroutineScope()
  var showMonthPicker by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var isDeletingBudget by remember { mutableStateOf(false) }
  var deleteBudgetError by remember { mutableStateOf<String?>(null) }
  val currencySymbol = userProfile.currencySymbol
  val twoPane = LocalAdaptiveInfo.current.useTwoPaneLayout
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
      isLoading = isDeletingBudget,
      errorMessage = deleteBudgetError,
      onConfirm = {
        isDeletingBudget = true
        deleteBudgetError = null
        coroutineScope.launch {
          try {
            viewModel.deleteBudgetSuspend(monthKey)
            isDeletingBudget = false
            showDeleteDialog = false
          } catch (e: Exception) {
            isDeletingBudget = false
            deleteBudgetError = e.localizedMessage ?: "Failed to delete budget."
          }
        }
      },
      onDismiss = {
        if (!isDeletingBudget) {
          showDeleteDialog = false
          deleteBudgetError = null
        }
      }
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
        PremiumPageHeader(
          title = "Budgets",
          subtitle = "Make room for what matters."
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          PremiumMonthNavigator(
            label = summary.monthLabel,
            onPrevious = { viewModel.previousBudgetMonth() },
            onNext = { viewModel.nextBudgetMonth() },
            onPick = { showMonthPicker = true }
          )
        }
      }

      item {
        AnimatedContent(
          targetState = (currentBudget != null && currentBudget.totalBudget > 0.0),
          transitionSpec = { PaisaMotion.DefaultContentTransform },
          label = "budget_hero_transition"
        ) { hasBudget ->
          if (!hasBudget) {
            PremiumCard {
              PremiumEmptyState(
                title = "Build your first monthly plan",
                body = "Set an overall limit, add category caps, and get a clearer view of what is left.",
                action = {
                  PremiumButton(onClick = onOpenSetBudget) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("Create budget")
                  }
                }
              )
            }
          } else {
            currentBudget?.let { budget ->
              BudgetHeroCard(
                budget = budget,
                summary = summary,
                currencySymbol = currencySymbol,
                selectedCalendar = selectedCalendar,
                onEdit = onOpenSetBudget,
                onDelete = { showDeleteDialog = true }
              )
            }
          }
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

      if (twoPane) {
        item(key = "budget_categories_two_pane") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              PremiumSectionHeading(
                title = "Category limits",
                subtitle = "Compare planned amounts with recorded spending.",
                trailing = {
                  PremiumOutlinedButton(onClick = onOpenSetBudget, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text(if (currentBudget == null) "Set up" else "Edit")
                  }
                }
              )
              if (configuredCategories.isEmpty()) {
                PremiumCard {
                  PremiumEmptyState(
                    title = "No category limits yet",
                    body = "Add caps for rent, groceries, transport, subscriptions and other categories to make the plan more useful.",
                    action = { PremiumOutlinedButton(onClick = onOpenSetBudget) { Text("Add category limits") } }
                  )
                }
              } else {
                configuredCategories.forEach { (category, limit) ->
                  CategoryLimitCard(
                    category = category,
                    limit = limit,
                    spent = categoryActual[category] ?: 0.0,
                    currencySymbol = currencySymbol
                  )
                }
              }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              PremiumSectionHeading(
                title = "Outside category caps",
                subtitle = "Spending without an individual limit."
              )
              if (unbudgeted.isEmpty()) {
                Surface(
                  shape = RoundedCornerShape(18.dp),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    "Every category with spending has a limit, or there is no spending outside the plan.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              } else {
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
          }
        }
      } else {
        item {
          PremiumSectionHeading(
            title = "Category limits",
            subtitle = "Compare planned amounts with recorded spending.",
            trailing = {
              PremiumOutlinedButton(onClick = onOpenSetBudget, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
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
                  PremiumOutlinedButton(onClick = onOpenSetBudget) { Text("Add category limits") }
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
  val isLight = !com.example.ui.theme.isAppInDarkTheme()
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = Color.Transparent,
    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.28f)),
    modifier = Modifier
      .fillMaxWidth()
      .background(
        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
          colors = if (isLight) {
            listOf(Color(0xFFF3E8FF), Color(0xFFEDE9FE))
          } else {
            listOf(Color(0xFF6D28D9), Color(0xFF4C1D95))
          }
        ),
        shape = RoundedCornerShape(28.dp)
      )
  ) {
    val textColor = if (isLight) Color(0xFF2E1065) else Color.White
    val subTextColor = if (isLight) Color(0xFF4C1D95).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.72f)
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
          Text("MONTHLY SPENDING LIMIT", style = MaterialTheme.typography.labelMedium, color = subTextColor, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
          com.example.ui.util.AnimatedFinancialText(IndianCurrencyFormatter.formatWithSymbol(limit, currencySymbol), style = MaterialTheme.typography.headlineMedium, color = textColor, fontWeight = FontWeight.Bold)
        }
        Row {
          PremiumIconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit budget", tint = textColor)
          }
          PremiumIconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete budget", tint = subTextColor)
          }
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.ui.components.PremiumProgressTrack(
          progress = used,
          color = if (used >= 0.9f) ExpenseRed else IncomeGreen,
          trackColor = if (isLight) Color(0xFFE9D5FF) else Color.White.copy(alpha = 0.16f),
          height = 10.dp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          com.example.ui.util.AnimatedFinancialText("${(used * 100).toInt()}% used", style = MaterialTheme.typography.labelMedium, color = subTextColor)
          com.example.ui.util.AnimatedFinancialText(
            text = if (remaining >= 0) "${IndianCurrencyFormatter.formatWithSymbol(remaining, currencySymbol)} remaining"
              else "${IndianCurrencyFormatter.formatWithSymbol(kotlin.math.abs(remaining), currencySymbol)} over",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (remaining >= 0) IncomeGreen else ExpenseRed
          )
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Spent", style = MaterialTheme.typography.labelMedium, color = subTextColor)
          com.example.ui.util.AnimatedFinancialText(IndianCurrencyFormatter.formatCompact(spent, currencySymbol), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Daily guide", style = MaterialTheme.typography.labelMedium, color = subTextColor)
          val now = Calendar.getInstance()
          val isCurrentMonth = now.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)
          val dayInPlan = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else 1
          val days = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
          val remainingDays = (days - dayInPlan + 1).coerceAtLeast(1)
          val daily = (limit - spent).coerceAtLeast(0.0) / remainingDays
          com.example.ui.util.AnimatedFinancialText(IndianCurrencyFormatter.formatCompact(daily, currencySymbol), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
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
          com.example.ui.util.AnimatedFinancialText(
            if (over) "Above your limit" else "${IndianCurrencyFormatter.formatWithSymbol((limit - spent).coerceAtLeast(0.0), currencySymbol)} left",
            style = MaterialTheme.typography.bodySmall,
            color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
          com.example.ui.util.AnimatedFinancialText(IndianCurrencyFormatter.formatWithSymbol(spent, currencySymbol), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurface)
          com.example.ui.util.AnimatedFinancialText("of ${IndianCurrencyFormatter.formatWithSymbol(limit, currencySymbol)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      com.example.ui.components.PremiumProgressTrack(
        progress = normalized,
        color = if (over) ExpenseRed else category.color,
        height = 7.dp
      )
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        com.example.ui.util.AnimatedFinancialText("${(fraction * 100).toInt()}% used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (over) {
          com.example.ui.util.AnimatedFinancialText("${(fraction * 100).toInt() - 100}% over", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
        }
      }
    }
  }
}
