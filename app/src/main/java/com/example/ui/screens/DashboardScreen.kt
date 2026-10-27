package com.example.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import com.example.ui.components.DateUtils
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpendingPoint
import com.example.data.model.MonthlyFinancialSummary as MonthlySummary
import com.example.data.model.TransactionType
import com.example.data.model.TransactionItem
import com.example.data.model.YearlyFinancialSummary as YearlySummary
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CategoryDetailSheet
import com.example.ui.components.IncomeExpenseComparisonBar
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.YearPickerDialog
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: FinanceViewModel,
  onOpenAddTransaction: (prefilledType: TransactionType?) -> Unit,
  onOpenSetBudget: () -> Unit,
  onEditTransaction: (item: TransactionItem) -> Unit
) {
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val selectedCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()
  val summary by viewModel.dashboardMonthSummary.collectAsStateWithLifecycle()
  val categorySpending by viewModel.dashboardCategorySpending.collectAsStateWithLifecycle()
  val dailyTrend by viewModel.dashboardDailyTrend.collectAsStateWithLifecycle()
  val selectedCategoryDetail by viewModel.selectedCategoryDetail.collectAsStateWithLifecycle()
  val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
  val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()

  var showMonthPicker by remember { mutableStateOf(false) }
  val currencySymbol = userProfile.currencySymbol
  val currentMonthKey = com.example.ui.components.DateUtils.getMonthKey(selectedCalendar)
  val currentBudget = remember(allBudgets, currentMonthKey) {
    allBudgets.firstOrNull { it.monthKey == currentMonthKey }
  }
  val recentTransactions = remember(allTransactions, selectedCalendar) {
    val start = com.example.ui.components.DateUtils.getStartOfMonth(selectedCalendar)
    val end = com.example.ui.components.DateUtils.getEndOfMonth(selectedCalendar)
    allTransactions.filter { it.timestamp in start..end }
      .sortedByDescending { it.timestamp }
      .take(5)
  }
  val firstName = userProfile.name.trim().substringBefore(" ").ifBlank { "there" }
  val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
  }

  selectedCategoryDetail?.let { detail ->
    com.example.ui.components.CategoryDetailSheet(
      detail = detail,
      currencySymbol = currencySymbol,
      onDismiss = { viewModel.clearSelectedCategoryDetail() }
    )
  }

  if (showMonthPicker) {
    com.example.ui.components.MonthYearPickerDialog(
      currentCalendar = selectedCalendar,
      onDismiss = { showMonthPicker = false },
      onMonthYearSelected = { year, month ->
        viewModel.setDashboardMonthAndYear(year, month)
        showMonthPicker = false
      }
    )
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    val wide = maxWidth >= 700.dp
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 1080.dp)
        .fillMaxSize()
        .testTag("dashboard_screen"),
      contentPadding = PaddingValues(
        start = if (maxWidth < 380.dp) 16.dp else 22.dp,
        end = if (maxWidth < 380.dp) 16.dp else 22.dp,
        top = 18.dp,
        bottom = 36.dp
      ),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = "$greeting, $firstName",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "Here is your money at a glance.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          com.example.ui.components.PremiumMonthNavigator(
            label = summary.monthLabel,
            onPrevious = { viewModel.previousDashboardMonth() },
            onNext = { viewModel.nextDashboardMonth() },
            onPick = { showMonthPicker = true }
          )
        }
      }

      item {
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
              Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                  text = "NET CASH FLOW",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.72f),
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 1.2.sp
                )
                Text(
                  text = com.example.ui.components.IndianCurrencyFormatter.formatWithSymbol(
                    summary.savings,
                    currencySymbol,
                    includeSign = true
                  ),
                  style = MaterialTheme.typography.headlineLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (summary.totalIncome > 0.0 && summary.savingsRate < 0.0) {
                    "Spending exceeded recorded income by ${String.format(java.util.Locale.getDefault(), "%.1f", kotlin.math.abs(summary.savingsRate).coerceAtMost(999.0))}%"
                  } else if (summary.totalIncome > 0.0) {
                    "${String.format(java.util.Locale.getDefault(), "%.1f", summary.savingsRate.coerceAtMost(999.0))}% of recorded income retained"
                  } else {
                    "Income and spending, in one clear picture"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.72f)
                )
              }
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.10f)
              ) {
                Icon(
                  imageVector = Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = Color(0xFFD4EF75),
                  modifier = Modifier.padding(12.dp).size(22.dp)
                )
              }
            }

            com.example.ui.components.PremiumTrendChart(
              points = dailyTrend,
              lineColor = Color(0xFFD4EF75),
              trackColor = Color.White.copy(alpha = 0.16f),
              height = if (wide) 104.dp else 76.dp
            )
            Text(
              "Daily spending trend",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.64f)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFD4EF75), modifier = Modifier.size(15.dp))
                  Text("Income", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f))
                }
                Text(
                  text = com.example.ui.components.IndianCurrencyFormatter.formatCompact(summary.totalIncome, currencySymbol),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false
                )
              }
              Box(Modifier.width(1.dp).height(42.dp).background(Color.White.copy(alpha = 0.18f)))
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFFFB5A9), modifier = Modifier.size(15.dp))
                  Text("Spent", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f))
                }
                Text(
                  text = com.example.ui.components.IndianCurrencyFormatter.formatCompact(summary.totalExpense, currencySymbol),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          PremiumQuickAction(
            label = "Add expense",
            icon = Icons.Default.ArrowUpward,
            color = Color(0xFFD64A46),
            modifier = Modifier.weight(1f),
            onClick = { onOpenAddTransaction(TransactionType.EXPENSE) }
          )
          PremiumQuickAction(
            label = "Add income",
            icon = Icons.Default.ArrowDownward,
            color = Color(0xFF17845C),
            modifier = Modifier.weight(1f),
            onClick = { onOpenAddTransaction(TransactionType.INCOME) }
          )
          PremiumQuickAction(
            label = "Set budget",
            icon = Icons.Default.AccountBalanceWallet,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = onOpenSetBudget
          )
        }
      }

      if (wide) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            PremiumBudgetOverview(
              summary = summary,
              currencySymbol = currencySymbol,
              hasBudget = currentBudget != null,
              onOpenSetBudget = onOpenSetBudget,
              modifier = Modifier.weight(1.15f)
            )
            PremiumCategoryOverview(
              categories = categorySpending.take(4),
              currencySymbol = currencySymbol,
              onCategoryClick = { viewModel.selectCategoryForDetail(it) },
              modifier = Modifier.weight(1f)
            )
          }
        }
      } else {
        item {
          PremiumBudgetOverview(
            summary = summary,
            currencySymbol = currencySymbol,
            hasBudget = currentBudget != null,
            onOpenSetBudget = onOpenSetBudget
          )
        }
        if (categorySpending.isNotEmpty()) {
          item {
            PremiumCategoryOverview(
              categories = categorySpending.take(4),
              currencySymbol = currencySymbol,
              onCategoryClick = { viewModel.selectCategoryForDetail(it) }
            )
          }
        }
      }

      item {
        com.example.ui.components.PremiumSectionHeading(
          title = "Recent activity",
          subtitle = "Your latest entries for ${summary.monthLabel}"
        )
      }
      if (recentTransactions.isEmpty()) {
        item {
          com.example.ui.components.PremiumCard {
            com.example.ui.components.PremiumEmptyState(
              title = "A fresh start",
              body = "Add your first transaction and your monthly picture will take shape.",
              action = {
                androidx.compose.material3.FilledTonalButton(onClick = { onOpenAddTransaction(null) }) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("Add transaction")
                }
              }
            )
          }
        }
      } else {
        item {
          com.example.ui.components.PremiumCard {
            Column(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              recentTransactions.forEachIndexed { index, transaction ->
                com.example.ui.components.PremiumTransactionRow(
                  transaction = transaction,
                  currencySymbol = currencySymbol,
                  onClick = { onEditTransaction(transaction) }
                )
                if (index != recentTransactions.lastIndex) {
                  HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                  )
                }
              }
            }
          }
        }
      }

      item {
        Text(
          text = "Numbers are based on transactions you have recorded. Keep your records up to date for a clearer picture.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun PremiumQuickAction(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 13.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(shape = RoundedCornerShape(13.dp), color = color.copy(alpha = 0.12f)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(9.dp).size(19.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun PremiumBudgetOverview(
  summary: com.example.data.model.MonthlyFinancialSummary,
  currencySymbol: String,
  hasBudget: Boolean,
  onOpenSetBudget: () -> Unit,
  modifier: Modifier = Modifier
) {
  com.example.ui.components.PremiumCard(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text("Monthly budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(summary.monthLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
          Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(19.dp))
        }
      }
      if (hasBudget && summary.budgetLimit > 0.0) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            com.example.ui.components.IndianCurrencyFormatter.formatWithSymbol(summary.budgetUsed, currencySymbol),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text("of ${com.example.ui.components.IndianCurrencyFormatter.formatWithSymbol(summary.budgetLimit, currencySymbol)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp))
        }
        val used = (summary.budgetUsed / summary.budgetLimit).toFloat().coerceIn(0f, 1f)
        com.example.ui.components.PremiumProgressTrack(
          progress = used,
          color = if (used >= 0.9f) Color(0xFFD64A46) else Color(0xFF176B4D),
          height = 9.dp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("${(used * 100).toInt()}% used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = if (summary.budgetRemaining >= 0) "${com.example.ui.components.IndianCurrencyFormatter.formatWithSymbol(summary.budgetRemaining, currencySymbol)} left"
              else "${com.example.ui.components.IndianCurrencyFormatter.formatWithSymbol(kotlin.math.abs(summary.budgetRemaining), currencySymbol)} over",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (summary.budgetRemaining >= 0) Color(0xFF17845C) else Color(0xFFD64A46)
          )
        }
      } else {
        Text(
          "Give this month a spending limit to see how you are tracking.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.OutlinedButton(onClick = onOpenSetBudget) {
          Text("Create monthly budget")
        }
      }
    }
  }
}

@Composable
private fun PremiumCategoryOverview(
  categories: List<com.example.data.model.CategorySpending>,
  currencySymbol: String,
  onCategoryClick: (com.example.data.model.TransactionCategory) -> Unit,
  modifier: Modifier = Modifier
) {
  com.example.ui.components.PremiumCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      com.example.ui.components.PremiumSectionHeading(
        title = "Spending mix",
        subtitle = "Top categories this month"
      )
      if (categories.isEmpty()) {
        Text("No spending recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else {
        val max = categories.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
        categories.forEach { item ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onCategoryClick(item.category) }
              .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Surface(shape = RoundedCornerShape(10.dp), color = item.category.color.copy(alpha = 0.12f)) {
                Icon(item.category.icon, contentDescription = null, tint = item.category.color, modifier = Modifier.padding(7.dp).size(16.dp))
              }
              Text(
                item.category.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                com.example.ui.components.IndianCurrencyFormatter.formatCompact(item.amount, currencySymbol),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
            com.example.ui.components.PremiumProgressTrack(
              progress = (item.amount / max).toFloat(),
              color = item.category.color,
              height = 6.dp
            )
          }
        }
      }
    }
  }
}

