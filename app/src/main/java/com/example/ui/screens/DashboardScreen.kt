package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategorySpending
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryDetailSheet
import com.example.ui.components.CategorySpendingBreakdown
import com.example.ui.components.DateUtils
import com.example.ui.components.IncomeExpenseComparisonBar
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumMonthNavigator
import com.example.ui.components.PremiumOutlinedButton
import com.example.ui.components.PremiumProgressTrack
import com.example.ui.components.PremiumSectionHeading
import com.example.ui.components.PremiumSegmentedToggle
import com.example.ui.components.PremiumTextButton
import com.example.ui.components.PremiumTonalButton
import com.example.ui.components.PremiumTransactionRow
import com.example.ui.components.PremiumTrendChart
import com.example.ui.components.PremiumYearlyComparisonChart
import com.example.ui.components.YearPickerDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: FinanceViewModel,
  onOpenAddTransaction: (prefilledType: TransactionType?) -> Unit,
  onOpenSetBudget: () -> Unit,
  onEditTransaction: (item: TransactionItem) -> Unit,
  onOpenAllTransactions: () -> Unit,
  onOpenPaisaAi: () -> Unit = {}
) {
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val selectedCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()
  val monthlySummary by viewModel.dashboardMonthSummary.collectAsStateWithLifecycle()
  val yearlySummary by viewModel.dashboardYearSummary.collectAsStateWithLifecycle()
  val categorySpending by viewModel.dashboardCategorySpending.collectAsStateWithLifecycle()
  val dailyTrend by viewModel.dashboardDailyTrend.collectAsStateWithLifecycle()
  val selectedCategoryDetail by viewModel.selectedCategoryDetail.collectAsStateWithLifecycle()
  val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
  val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()

  var isAnnualView by remember { mutableStateOf(false) }
  var showMonthPicker by remember { mutableStateOf(false) }
  var showYearPicker by remember { mutableStateOf(false) }

  val currencySymbol = userProfile.currencySymbol
  val currentMonthKey = DateUtils.getMonthKey(selectedCalendar)
  val currentYear = selectedCalendar.get(Calendar.YEAR)
  val currentBudget = remember(allBudgets, currentMonthKey) {
    allBudgets.firstOrNull { it.monthKey == currentMonthKey }
  }
  val recentTransactions = remember(allTransactions, selectedCalendar) {
    val start = DateUtils.getStartOfMonth(selectedCalendar)
    val end = DateUtils.getEndOfMonth(selectedCalendar)
    allTransactions.filter { it.timestamp in start..end }
      .sortedByDescending { it.timestamp }
      .take(5)
  }
  val firstName = userProfile.name.trim().substringBefore(" ").ifBlank { "there" }
  val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
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
        showMonthPicker = false
      }
    )
  }

  if (showYearPicker) {
    YearPickerDialog(
      currentYear = currentYear,
      onDismiss = { showYearPicker = false },
      onYearSelected = { year ->
        viewModel.setDashboardYear(year)
        showYearPicker = false
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
        val periodSwitcher: @Composable () -> Unit = {
          PremiumSegmentedToggle(
            options = listOf("Monthly", "Yearly"),
            selectedIndex = if (isAnnualView) 1 else 0,
            onSelect = { isAnnualView = (it == 1) }
          )
        }

        val periodNavigator: @Composable () -> Unit = {
          PremiumMonthNavigator(
            label = if (isAnnualView) currentYear.toString() else monthlySummary.monthLabel,
            onPrevious = { if (isAnnualView) viewModel.previousDashboardYear() else viewModel.previousDashboardMonth() },
            onNext = { if (isAnnualView) viewModel.nextDashboardYear() else viewModel.nextDashboardMonth() },
            onPick = { if (isAnnualView) showYearPicker = true else showMonthPicker = true }
          )
        }

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = "$greeting, $firstName",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "Here is your overall financial dashboard.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            periodSwitcher()
          }

          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            periodNavigator()
          }
        }
      }

      // Net Cash Flow Hero Card
      item {
        val displaySavings = if (isAnnualView) yearlySummary.savings else monthlySummary.savings
        val displayIncome = if (isAnnualView) yearlySummary.totalIncome else monthlySummary.totalIncome
        val displayExpense = if (isAnnualView) yearlySummary.totalExpense else monthlySummary.totalExpense
        val displayRate = if (isAnnualView) yearlySummary.savingsRate else monthlySummary.savingsRate

        Surface(
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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
              AnimatedContent(
                targetState = isAnnualView,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "dashboard_hero_summary_transition",
                modifier = Modifier.weight(1f)
              ) { annual ->
                val displaySavings = if (annual) yearlySummary.savings else monthlySummary.savings
                val displayIncome = if (annual) yearlySummary.totalIncome else monthlySummary.totalIncome
                val displayRate = if (annual) yearlySummary.savingsRate else monthlySummary.savingsRate

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                  Text(
                    text = if (annual) "ANNUAL NET SAVINGS" else "NET CASH FLOW",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                  )
                  Text(
                    text = IndianCurrencyFormatter.formatWithSymbol(
                      displaySavings,
                      currencySymbol,
                      includeSign = true
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (displayIncome > 0.0 && displayRate < 0.0) {
                      "Spending exceeded recorded income by ${String.format(java.util.Locale.getDefault(), "%.1f", kotlin.math.abs(displayRate).coerceAtMost(999.0))}%"
                    } else if (displayIncome > 0.0) {
                      "${String.format(java.util.Locale.getDefault(), "%.1f", displayRate.coerceAtMost(999.0))}% of recorded income retained"
                    } else {
                      "Income and spending, in one clear picture"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
              ) {
                Icon(
                  imageVector = Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(12.dp).size(22.dp)
                )
              }
            }

            AnimatedContent(
              targetState = isAnnualView,
              transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
              label = "dashboard_hero_chart_transition"
            ) { annual ->
              if (!annual) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  PremiumTrendChart(
                    points = dailyTrend,
                    lineColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    height = if (wide) 104.dp else 76.dp
                  )
                  Text(
                    "Daily spending trend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                  )
                }
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  PremiumYearlyComparisonChart(
                    monthlyBreakdown = yearlySummary.monthlyBreakdown,
                    incomeColor = IncomeGreen,
                    expenseColor = ExpenseRed,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    height = if (wide) 104.dp else 76.dp
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      "Monthly income vs expenses",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen)
                        )
                        Text(
                          text = "Income",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                          fontWeight = FontWeight.SemiBold
                        )
                      }
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed)
                        )
                        Text(
                          text = "Expenses",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                          fontWeight = FontWeight.SemiBold
                        )
                      }
                    }
                  }
                }
              }
            }

            AnimatedContent(
              targetState = isAnnualView,
              transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
              label = "dashboard_hero_totals_transition"
            ) { annual ->
              val displayIncome = if (annual) yearlySummary.totalIncome else monthlySummary.totalIncome
              val displayExpense = if (annual) yearlySummary.totalExpense else monthlySummary.totalExpense

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Text("Total Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                  }
                  Text(
                    text = IndianCurrencyFormatter.formatCompact(displayIncome, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false
                  )
                }
                Box(Modifier.width(1.dp).height(42.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)))
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(15.dp))
                    Text("Total Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                  }
                  Text(
                    text = IndianCurrencyFormatter.formatCompact(displayExpense, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }
        }
      }

      // Quick Actions
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          PremiumQuickAction(
            label = "Add expense",
            icon = Icons.Default.ArrowUpward,
            color = Color(0xFFF43F5E),
            modifier = Modifier.weight(1f),
            onClick = { onOpenAddTransaction(TransactionType.EXPENSE) }
          )
          PremiumQuickAction(
            label = "Add income",
            icon = Icons.Default.ArrowDownward,
            color = Color(0xFF10B981),
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

      // Income vs Expense Comparison Bar
      item {
        AnimatedContent(
          targetState = isAnnualView,
          transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
          label = "dashboard_comparison_bar_transition"
        ) { annual ->
          val income = if (annual) yearlySummary.totalIncome else monthlySummary.totalIncome
          val expense = if (annual) yearlySummary.totalExpense else monthlySummary.totalExpense
          IncomeExpenseComparisonBar(
            income = income,
            expense = expense,
            currencySymbol = currencySymbol
          )
        }
      }

      // Budget & Category Overview Cards
      if (wide) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            PremiumBudgetOverview(
              summary = monthlySummary,
              currencySymbol = currencySymbol,
              hasBudget = currentBudget != null,
              onOpenSetBudget = onOpenSetBudget,
              modifier = Modifier.weight(1.15f)
            )
            PremiumCategoryOverview(
              categories = categorySpending.filter { it.amount > 0.0 }.take(4),
              currencySymbol = currencySymbol,
              onCategoryClick = { viewModel.selectCategoryForDetail(it) },
              modifier = Modifier.weight(1f)
            )
          }
        }
      } else {
        item {
          PremiumBudgetOverview(
            summary = monthlySummary,
            currencySymbol = currencySymbol,
            hasBudget = currentBudget != null,
            onOpenSetBudget = onOpenSetBudget
          )
        }
      }

      // Full Category Spending Analytics Breakdown
      item {
        PremiumCard {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            PremiumSectionHeading(
              title = "Spending Analytics & Categories",
              subtitle = if (isAnnualView) "Category distribution for $currentYear" else "Category distribution for ${monthlySummary.monthLabel}"
            )
            CategorySpendingBreakdown(
              categories = categorySpending,
              currencySymbol = currencySymbol,
              onCategoryClick = { category -> viewModel.selectCategoryForDetail(category) }
            )
          }
        }
      }

      // Recent Activity
      item {
        PremiumSectionHeading(
          title = "Recent activity",
          subtitle = "Your latest entries for ${monthlySummary.monthLabel}",
          trailing = {
            PremiumTextButton(onClick = onOpenAllTransactions) {
              Text("See all", fontWeight = FontWeight.Bold)
            }
          }
        )
      }
      if (recentTransactions.isEmpty()) {
        item {
          PremiumCard {
            PremiumEmptyState(
              title = if (allTransactions.isEmpty()) "A fresh start" else "No activity this month",
              body = if (allTransactions.isEmpty()) {
                "Add your first transaction and your monthly picture will take shape."
              } else {
                "There are no entries for ${monthlySummary.monthLabel}. Choose another month or add a transaction for this period."
              },
              action = {
                PremiumTonalButton(onClick = { onOpenAddTransaction(null) }) {
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
          PremiumCard {
            Column(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              recentTransactions.forEachIndexed { index, transaction ->
                PremiumTransactionRow(
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
  val haptics = com.example.ui.util.rememberPaisaHaptics()
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

  Surface(
    onClick = {
        haptics.performLightHaptic()
        onClick()
    },
    modifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    },
    interactionSource = interactionSource,
    shape = RoundedCornerShape(18.dp),
    color = com.example.ui.theme.resolveCardSurfaceColor(),
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
  PremiumCard(modifier = modifier) {
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
            IndianCurrencyFormatter.formatWithSymbol(summary.budgetUsed, currencySymbol),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text("of ${IndianCurrencyFormatter.formatWithSymbol(summary.budgetLimit, currencySymbol)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp))
        }
        val used = (summary.budgetUsed / summary.budgetLimit).toFloat().coerceIn(0f, 1f)
        PremiumProgressTrack(
          progress = used,
          color = if (used >= 0.9f) Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary,
          height = 9.dp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("${(used * 100).toInt()}% used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = if (summary.budgetRemaining >= 0) "${IndianCurrencyFormatter.formatWithSymbol(summary.budgetRemaining, currencySymbol)} left"
              else "${IndianCurrencyFormatter.formatWithSymbol(kotlin.math.abs(summary.budgetRemaining), currencySymbol)} over",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (summary.budgetRemaining >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
          )
        }
      } else {
        Text(
          "Give this month a spending limit to see how you are tracking.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PremiumOutlinedButton(onClick = onOpenSetBudget) {
          Text("Create monthly budget")
        }
      }
    }
  }
}

@Composable
private fun PremiumCategoryOverview(
  categories: List<CategorySpending>,
  currencySymbol: String,
  onCategoryClick: (com.example.data.model.TransactionCategory) -> Unit,
  modifier: Modifier = Modifier
) {
  val haptics = com.example.ui.util.rememberPaisaHaptics()
  PremiumCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      PremiumSectionHeading(
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
              .clickable {
                haptics.performLightHaptic()
                onCategoryClick(item.category)
              }
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
                IndianCurrencyFormatter.formatCompact(item.amount, currencySymbol),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
            PremiumProgressTrack(
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
