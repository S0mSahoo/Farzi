package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: FinanceViewModel,
  onOpenAddTransaction: (prefilledType: TransactionType?) -> Unit,
  onOpenSetBudget: () -> Unit
) {
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val selectedCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()
  val monthlySummary by viewModel.dashboardMonthSummary.collectAsStateWithLifecycle()
  val yearlySummary by viewModel.dashboardYearSummary.collectAsStateWithLifecycle()
  val categorySpending by viewModel.dashboardCategorySpending.collectAsStateWithLifecycle()
  val dailyTrend by viewModel.dashboardDailyTrend.collectAsStateWithLifecycle()
  val selectedCategoryDetail by viewModel.selectedCategoryDetail.collectAsStateWithLifecycle()
  val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
  val recentTransactions = remember(allTransactions) {
    allTransactions.sortedByDescending { it.timestamp }.take(5)
  }

  val adaptiveInfo = LocalAdaptiveInfo.current

  var isAnnualView by remember { mutableStateOf(false) }
  var showMonthPicker by remember { mutableStateOf(false) }
  var showYearPicker by remember { mutableStateOf(false) }

  val currencySymbol = userProfile.currencySymbol
  val currentYear = selectedCalendar.get(Calendar.YEAR)

  val firstName = remember(userProfile.name) {
    if (userProfile.name.isNotBlank()) {
      userProfile.name.trim().split("\\s+".toRegex()).firstOrNull() ?: userProfile.name
    } else ""
  }

  selectedCategoryDetail?.let { detail ->
    CategoryDetailSheet(
      detail = detail,
      currencySymbol = currencySymbol,
      onDismiss = { viewModel.clearSelectedCategoryDetail() }
    )
  }

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentCalendar = selectedCalendar,
      onDismiss = { showMonthPicker = false },
      onMonthYearSelected = { year, month ->
        viewModel.setDashboardMonthAndYear(year, month)
      }
    )
  }

  if (showYearPicker) {
    YearPickerDialog(
      currentYear = currentYear,
      onDismiss = { showYearPicker = false },
      onYearSelected = { year ->
        viewModel.setDashboardYear(year)
      }
    )
  }

  val horizontalPadding = if (adaptiveInfo.isLandscapePhone) 12.dp else 20.dp
  val verticalSpacing = if (adaptiveInfo.isLandscapePhone) 12.dp else 16.dp

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("dashboard_screen"),
    contentPadding = PaddingValues(
      start = horizontalPadding,
      end = horizontalPadding,
      top = if (adaptiveInfo.isLandscapePhone) 8.dp else 16.dp,
      bottom = if (adaptiveInfo.isLandscapePhone) 48.dp else 112.dp
    ),
    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
  ) {
    if (adaptiveInfo.useTwoPaneLayout) {
      // Wide Tablet / Desktop 2-Pane Adaptive Layout
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(modifier = Modifier.weight(1.1f)) {
            DashboardHeader(
              firstName = firstName,
              isAnnualView = isAnnualView,
              currentYear = currentYear,
              onToggleAnnualView = { isAnnualView = !isAnnualView }
            )
          }
          Box(modifier = Modifier.weight(0.9f)) {
            PeriodSelectorBar(
              isAnnualView = isAnnualView,
              currentYear = currentYear,
              monthlySummary = monthlySummary,
              onPrevious = { if (isAnnualView) viewModel.previousDashboardYear() else viewModel.previousDashboardMonth() },
              onNext = { if (isAnnualView) viewModel.nextDashboardYear() else viewModel.nextDashboardMonth() },
              onOpenPicker = {
                if (isAnnualView) showYearPicker = true else showMonthPicker = true
              }
            )
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Left Column
          Column(
            modifier = Modifier.weight(1.05f),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
          ) {
            if (isAnnualView) {
              AnnualOverviewCard(yearlySummary = yearlySummary, currencySymbol = currencySymbol)
              AnnualBreakdownTable(yearlySummary = yearlySummary, currencySymbol = currencySymbol)
            } else {
              NetSavingsHeroCard(monthlySummary = monthlySummary, currencySymbol = currencySymbol, dailyTrend = dailyTrend)
              IncomeExpenseQuickCards(
                monthlySummary = monthlySummary,
                currencySymbol = currencySymbol,
                onAddIncome = { onOpenAddTransaction(TransactionType.INCOME) },
                onAddExpense = { onOpenAddTransaction(TransactionType.EXPENSE) }
              )
              if (monthlySummary.transactionCount > 0) {
                IncomeExpenseBarCard(monthlySummary = monthlySummary, currencySymbol = currencySymbol)
              }
            }
          }

          // Right Column
          Column(
            modifier = Modifier.weight(0.95f),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
          ) {
            if (!isAnnualView) {
              MonthlyBudgetCard(
                monthlySummary = monthlySummary,
                currencySymbol = currencySymbol,
                onOpenSetBudget = onOpenSetBudget
              )
              if (monthlySummary.transactionCount == 0) {
                EmptyTransactionsState(
                  monthLabel = monthlySummary.monthLabel,
                  onAddIncome = { onOpenAddTransaction(TransactionType.INCOME) },
                  onAddExpense = { onOpenAddTransaction(TransactionType.EXPENSE) }
                )
              } else if (categorySpending.isNotEmpty()) {
                CategorySpendingCard(
                  categorySpending = categorySpending,
                  currencySymbol = currencySymbol,
                  onCategoryClick = { viewModel.selectCategoryForDetail(it) }
                )
              }
            }
          }
        }
      }
    } else {
      // Phone / Compact Portrait & Landscape Column Layout
      item {
        DashboardHeader(
          firstName = firstName,
          isAnnualView = isAnnualView,
          currentYear = currentYear,
          onToggleAnnualView = { isAnnualView = !isAnnualView }
        )
      }

      item {
        PeriodSelectorBar(
          isAnnualView = isAnnualView,
          currentYear = currentYear,
          monthlySummary = monthlySummary,
          onPrevious = { if (isAnnualView) viewModel.previousDashboardYear() else viewModel.previousDashboardMonth() },
          onNext = { if (isAnnualView) viewModel.nextDashboardYear() else viewModel.nextDashboardMonth() },
          onOpenPicker = {
            if (isAnnualView) showYearPicker = true else showMonthPicker = true
          }
        )
      }

      if (isAnnualView) {
        item { AnnualOverviewCard(yearlySummary = yearlySummary, currencySymbol = currencySymbol) }
        item { AnnualBreakdownTable(yearlySummary = yearlySummary, currencySymbol = currencySymbol) }
      } else {
        item { NetSavingsHeroCard(monthlySummary = monthlySummary, currencySymbol = currencySymbol, dailyTrend = dailyTrend) }
        item {
          IncomeExpenseQuickCards(
            monthlySummary = monthlySummary,
            currencySymbol = currencySymbol,
            onAddIncome = { onOpenAddTransaction(TransactionType.INCOME) },
            onAddExpense = { onOpenAddTransaction(TransactionType.EXPENSE) }
          )
        }
        item {
          MonthlyBudgetCard(
            monthlySummary = monthlySummary,
            currencySymbol = currencySymbol,
            onOpenSetBudget = onOpenSetBudget
          )
        }

        if (monthlySummary.transactionCount == 0) {
          item {
            EmptyTransactionsState(
              monthLabel = monthlySummary.monthLabel,
              onAddIncome = { onOpenAddTransaction(TransactionType.INCOME) },
              onAddExpense = { onOpenAddTransaction(TransactionType.EXPENSE) }
            )
          }
        } else {
          item { IncomeExpenseBarCard(monthlySummary = monthlySummary, currencySymbol = currencySymbol) }
          if (categorySpending.isNotEmpty()) {
            item {
              CategorySpendingCard(
                categorySpending = categorySpending,
                currencySymbol = currencySymbol,
                onCategoryClick = { viewModel.selectCategoryForDetail(it) }
              )
            }
          }
        }
      }
    }

    // Recent activity is shared across monthly and annual views and uses persisted data.
    if (recentTransactions.isNotEmpty()) {
      item {
        RecentTransactionsCard(
          transactions = recentTransactions,
          currencySymbol = currencySymbol
        )
      }
    }
  }
}

@Composable
private fun RecentTransactionsCard(
  transactions: List<TransactionItem>,
  currencySymbol: String
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = "Recent activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Your latest recorded transactions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        ) {
          Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(10.dp).size(20.dp)
          )
        }
      }

      transactions.forEachIndexed { index, transaction ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(13.dp),
            color = transaction.category.color.copy(alpha = 0.15f),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = transaction.category.icon,
                contentDescription = transaction.category.displayName,
                tint = transaction.category.color,
                modifier = Modifier.size(21.dp)
              )
            }
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = transaction.title.ifBlank { transaction.category.displayName },
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${transaction.category.displayName} · ${DateUtils.getDisplayDate(transaction.timestamp)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          val isIncome = transaction.type == TransactionType.INCOME
          val signedAmount = if (isIncome) kotlin.math.abs(transaction.amount) else -kotlin.math.abs(transaction.amount)
          val exactAmount = IndianCurrencyFormatter.formatWithSymbol(
            amount = signedAmount,
            symbol = currencySymbol,
            includeSign = true
          )
          val compactAmount = if (exactAmount.length > 12) {
            val compact = IndianCurrencyFormatter.formatCompact(signedAmount, currencySymbol)
            if (signedAmount > 0.0) "+$compact" else compact
          } else exactAmount
          Text(
            text = compactAmount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) IncomeGreen else ExpenseRed,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (index < transactions.lastIndex) {
          androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardHeader(
  firstName: String,
  isAnnualView: Boolean,
  currentYear: Int,
  onToggleAnnualView: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Text(
        text = if (firstName.isNotBlank()) "Namaste, $firstName 👋" else "Namaste 👋",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Track your income, expenses & savings",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .clickable { onToggleAnnualView() }
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = if (isAnnualView) "Year $currentYear" else "Month View",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

@Composable
private fun PeriodSelectorBar(
  isAnnualView: Boolean,
  currentYear: Int,
  monthlySummary: MonthlySummary,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onOpenPicker: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
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
      IconButton(
        onClick = onPrevious,
        modifier = Modifier.testTag("prev_period_button")
      ) {
        Icon(Icons.Default.ChevronLeft, contentDescription = if (isAnnualView) "Previous Year" else "Previous Month")
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable { onOpenPicker() }
          .testTag("period_selector_chip")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = if (isAnnualView) "Year $currentYear" else monthlySummary.monthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Icon(
            Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }

      IconButton(
        onClick = onNext,
        modifier = Modifier.testTag("next_period_button")
      ) {
        Icon(Icons.Default.ChevronRight, contentDescription = if (isAnnualView) "Next Year" else "Next Month")
      }
    }
  }
}

@Composable
private fun IncomeExpenseQuickCards(
  monthlySummary: MonthlySummary,
  currencySymbol: String,
  onAddIncome: () -> Unit,
  onAddExpense: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Total Income Card
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = IncomeGreen.copy(alpha = 0.08f),
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(22.dp))
        .clickable { onAddIncome() }
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = IncomeGreen.copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
            }
          }
          Icon(Icons.Default.Add, contentDescription = "Add Income", tint = IncomeGreen, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Total Income", style = MaterialTheme.typography.labelMedium, color = IncomeGreen, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.totalIncome, currencySymbol),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = IncomeGreen
        )
      }
    }

    // Total Expenses Card
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = ExpenseRed.copy(alpha = 0.08f),
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(22.dp))
        .clickable { onAddExpense() }
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = ExpenseRed.copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.TrendingDown, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
            }
          }
          Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = ExpenseRed, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Total Expenses", style = MaterialTheme.typography.labelMedium, color = ExpenseRed, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.totalExpense, currencySymbol),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = ExpenseRed
        )
      }
    }
  }
}

@Composable
private fun NetSavingsHeroCard(
  monthlySummary: MonthlySummary,
  currencySymbol: String,
  dailyTrend: List<DailySpendingPoint>
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
          }
          Column {
            Text(text = "Net monthly savings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Income less recorded expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (monthlySummary.savings >= 0) IncomeGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)
        ) {
          Text(
            text = "Rate: ${String.format("%.1f", monthlySummary.savingsRate)}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (monthlySummary.savings >= 0) IncomeGreen else ExpenseRed,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.savings, currencySymbol, includeSign = true),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = if (monthlySummary.savings >= 0) IncomeGreen else ExpenseRed,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Daily net movement",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Recorded days",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      DailyNetSparkline(
        points = dailyTrend,
        modifier = Modifier.fillMaxWidth().height(48.dp)
      )
    }
  }
}

@Composable
private fun DailyNetSparkline(
  points: List<DailySpendingPoint>,
  modifier: Modifier = Modifier
) {
  val activePoints = remember(points) {
    points.filter { it.income != 0.0 || it.expense != 0.0 || it.net != 0.0 }
  }
  val positive = activePoints.any { it.net > 0.0 }
  val lineColor = if (positive) IncomeGreen else ExpenseRed
  val baselineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

  Canvas(modifier = modifier) {
    if (activePoints.isEmpty()) {
      drawLine(
        color = baselineColor,
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
      )
    } else {
      val values = activePoints.map { it.net }
      val minValue = minOf(0.0, values.minOrNull() ?: 0.0)
      val maxValue = maxOf(0.0, values.maxOrNull() ?: 0.0)
      val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
      val chartPoints = values.mapIndexed { index, value ->
        val x = if (values.size <= 1) size.width / 2f else size.width * index / (values.size - 1).toFloat()
        val y = size.height - (((value - minValue) / range).toFloat() * (size.height - 6.dp.toPx())) - 3.dp.toPx()
        Offset(x, y.coerceIn(2.dp.toPx(), size.height - 2.dp.toPx()))
      }

      val zeroY = size.height - (((0.0 - minValue) / range).toFloat() * (size.height - 6.dp.toPx())) - 3.dp.toPx()
      drawLine(
        color = baselineColor,
        start = Offset(0f, zeroY),
        end = Offset(size.width, zeroY),
        strokeWidth = 1.dp.toPx()
      )
      chartPoints.zipWithNext().forEach { (start, end) ->
        drawLine(
          color = lineColor,
          start = start,
          end = end,
          strokeWidth = 2.5.dp.toPx(),
          cap = StrokeCap.Round
        )
      }
      chartPoints.lastOrNull()?.let { last ->
        drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = last)
      }
    }
  }
}

@Composable
private fun MonthlyBudgetCard(
  monthlySummary: MonthlySummary,
  currencySymbol: String,
  onOpenSetBudget: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      if (monthlySummary.budgetLimit > 0) {
        BudgetProgressBar(
          budgetLimit = monthlySummary.budgetLimit,
          budgetUsed = monthlySummary.budgetUsed,
          currencySymbol = currencySymbol
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onOpenSetBudget) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit Budget")
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "No Monthly Budget Set",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Set a budget for ${monthlySummary.monthLabel} to keep expenses in check.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          FilledTonalButton(
            onClick = onOpenSetBudget,
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Set Budget")
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyTransactionsState(
  monthLabel: String,
  onAddIncome: () -> Unit,
  onAddExpense: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.size(56.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "No transactions in $monthLabel",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Tap below to log your first income or expense for this month.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackActions = maxWidth < 340.dp
        val actionSpacing = if (stackActions) 8.dp else 12.dp

        if (stackActions) {
          Column(verticalArrangement = Arrangement.spacedBy(actionSpacing)) {
            EmptyStateActionButton("Add Income", true, onAddIncome, Modifier.fillMaxWidth())
            EmptyStateActionButton("Add Expense", false, onAddExpense, Modifier.fillMaxWidth())
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(actionSpacing)
          ) {
            EmptyStateActionButton("Add Income", true, onAddIncome, Modifier.weight(1f))
            EmptyStateActionButton("Add Expense", false, onAddExpense, Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun IncomeExpenseBarCard(
  monthlySummary: MonthlySummary,
  currencySymbol: String
) {
  Card(
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Income vs Expenses",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(14.dp))
      IncomeExpenseComparisonBar(
        income = monthlySummary.totalIncome,
        expense = monthlySummary.totalExpense,
        currencySymbol = currencySymbol
      )
    }
  }
}

@Composable
private fun CategorySpendingCard(
  categorySpending: List<CategorySpending>,
  currencySymbol: String,
  onCategoryClick: (com.example.data.model.TransactionCategory) -> Unit
) {
  val sortedCategories = remember(categorySpending) {
    categorySpending.sortedByDescending { it.amount }
  }
  val totalSpent = remember(sortedCategories) { sortedCategories.sumOf { it.amount } }

  Card(
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = "Spending overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Breakdown by category",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.TrendingDown,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier.size(124.dp),
          contentAlignment = Alignment.Center
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            sortedCategories.forEach { item ->
              val sweepAngle = (item.percentage.coerceIn(0f, 1f) * 360f - 3f).coerceAtLeast(0f)
              if (sweepAngle > 0f) {
                drawArc(
                  color = item.color,
                  startAngle = startAngle,
                  sweepAngle = sweepAngle,
                  useCenter = false,
                  style = Stroke(width = 17.dp.toPx(), cap = StrokeCap.Round)
                )
              }
              startAngle += item.percentage.coerceIn(0f, 1f) * 360f
            }
          }

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth(0.74f)
          ) {
            Text(
              text = "Spent",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
            Text(
              text = IndianCurrencyFormatter.formatCompact(totalSpent, currencySymbol),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          sortedCategories.take(4).forEach { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onCategoryClick(item.category) }
                .padding(vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = item.color,
                modifier = Modifier.size(9.dp)
              ) {}
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = item.category.displayName,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${IndianCurrencyFormatter.formatCompact(item.amount, currencySymbol)} · ${(item.percentage * 100f).toInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      if (sortedCategories.size > 4) {
        Text(
          text = "${sortedCategories.size - 4} more categories recorded",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun AnnualOverviewCard(
  yearlySummary: YearlySummary,
  currencySymbol: String
) {
  Card(
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Annual Financial Summary (${yearlySummary.year})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(IncomeGreen.copy(alpha = 0.1f))
            .padding(14.dp)
        ) {
          Text("Total Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = IndianCurrencyFormatter.formatWithSymbol(yearlySummary.totalIncome, currencySymbol),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = IncomeGreen
          )
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(ExpenseRed.copy(alpha = 0.1f))
            .padding(14.dp)
        ) {
          Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = IndianCurrencyFormatter.formatWithSymbol(yearlySummary.totalExpense, currencySymbol),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ExpenseRed
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Total Annual Savings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = IndianCurrencyFormatter.formatWithSymbol(yearlySummary.savings, currencySymbol, includeSign = true),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = if (yearlySummary.savings >= 0) IncomeGreen else ExpenseRed
            )
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (yearlySummary.savings >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
          ) {
            Text(
              text = "Savings Rate: ${String.format("%.1f", yearlySummary.savingsRate)}%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (yearlySummary.savings >= 0) IncomeGreen else ExpenseRed,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AnnualBreakdownTable(
  yearlySummary: YearlySummary,
  currencySymbol: String
) {
  Card(
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Month-by-Month Breakdown",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Month", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
          Text("Income", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = IncomeGreen, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
          Text("Expenses", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ExpenseRed, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
          Text("Savings", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      yearlySummary.monthlyBreakdown.forEach { monthItem ->
        MonthBreakdownRowItem(monthItem = monthItem, currencySymbol = currencySymbol)
      }
    }
  }
}

@Composable
private fun MonthBreakdownRowItem(
  monthItem: MonthlySummary,
  currencySymbol: String
) {
  Column {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = monthItem.monthLabel.split(" ").firstOrNull() ?: monthItem.monthLabel,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1.2f)
      )

      Text(
        text = "+${IndianCurrencyFormatter.formatCompact(monthItem.totalIncome, currencySymbol)}",
        style = MaterialTheme.typography.bodySmall,
        color = IncomeGreen,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f)
      )

      Text(
        text = "-${IndianCurrencyFormatter.formatCompact(monthItem.totalExpense, currencySymbol)}",
        style = MaterialTheme.typography.bodySmall,
        color = ExpenseRed,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f)
      )

      Text(
        text = IndianCurrencyFormatter.formatCompact(monthItem.savings, currencySymbol),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = if (monthItem.savings >= 0) IncomeGreen else ExpenseRed,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1.1f)
      )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  }
}

@Composable
private fun EmptyStateActionButton(
  label: String,
  isIncome: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isIncome) IncomeGreen else ExpenseRed,
      contentColor = Color.White
    ),
    shape = RoundedCornerShape(20.dp),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    modifier = modifier.heightIn(min = 48.dp)
  ) {
    Icon(
      imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
      contentDescription = null,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = label,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Clip,
      style = MaterialTheme.typography.labelLarge
    )
  }
}
