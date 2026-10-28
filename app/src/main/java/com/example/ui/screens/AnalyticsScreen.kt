package com.example.ui.screens

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MonthlyFinancialSummary
import com.example.data.model.CategorySpending
import com.example.data.model.YearlyFinancialSummary
import com.example.ui.components.CategoryDetailSheet
import com.example.ui.components.CategorySpendingBreakdown
import com.example.ui.components.IncomeExpenseComparisonBar
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.YearPickerDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

/**
 * A dedicated analytics surface backed exclusively by the existing ViewModel flows.
 * It intentionally reuses the app's current financial summaries and category detail flow.
 */
@Composable
fun AnalyticsScreen(viewModel: FinanceViewModel) {
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val selectedCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()
  val monthlySummary by viewModel.dashboardMonthSummary.collectAsStateWithLifecycle()
  val yearlySummary by viewModel.dashboardYearSummary.collectAsStateWithLifecycle()
  val categorySpending by viewModel.dashboardCategorySpending.collectAsStateWithLifecycle()
  val dailyTrend by viewModel.dashboardDailyTrend.collectAsStateWithLifecycle()
  val selectedCategoryDetail by viewModel.selectedCategoryDetail.collectAsStateWithLifecycle()
  var isAnnualView by remember { mutableStateOf(false) }
  var showMonthPicker by remember { mutableStateOf(false) }
  var showYearPicker by remember { mutableStateOf(false) }
  val currencySymbol = userProfile.currencySymbol
  val currentYear = selectedCalendar.get(Calendar.YEAR)

  selectedCategoryDetail?.let { detail ->
    CategoryDetailSheet(detail = detail, currencySymbol = currencySymbol, onDismiss = { viewModel.clearSelectedCategoryDetail() })
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

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 1060.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      item {
        com.example.ui.components.PremiumSectionHeading(
          title = "Insights",
          subtitle = "Patterns that make your money make sense."
        )
      }
      item {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
          val periodSwitcher: @Composable () -> Unit = {
            Row(
              modifier = Modifier.selectableGroup(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = !isAnnualView,
                onClick = { isAnnualView = false },
                label = { Text("Monthly") }
              )
              FilterChip(
                selected = isAnnualView,
                onClick = { isAnnualView = true },
                label = { Text("Yearly") }
              )
            }
          }
          val periodNavigator: @Composable () -> Unit = {
            com.example.ui.components.PremiumMonthNavigator(
              label = if (isAnnualView) currentYear.toString() else monthlySummary.monthLabel,
              onPrevious = { if (isAnnualView) viewModel.previousDashboardYear() else viewModel.previousDashboardMonth() },
              onNext = { if (isAnnualView) viewModel.nextDashboardYear() else viewModel.nextDashboardMonth() },
              onPick = { if (isAnnualView) showYearPicker = true else showMonthPicker = true }
            )
          }
          if (maxWidth < 520.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              periodSwitcher()
              periodNavigator()
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              periodSwitcher()
              periodNavigator()
            }
          }
        }
      }

      if (!isAnnualView) {
        item {
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
            Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
              Text("NET CASH FLOW", style = MaterialTheme.typography.labelMedium, color = subTextColor, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
              Text(
                IndianCurrencyFormatter.formatWithSymbol(monthlySummary.savings, currencySymbol, includeSign = true),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
              )
              Text(
                if (monthlySummary.totalIncome > 0) "${String.format(java.util.Locale.getDefault(), "%.1f", monthlySummary.savingsRate)}% of recorded income retained"
                else "Add income to see your savings rate.",
                style = MaterialTheme.typography.bodySmall,
                color = subTextColor
              )
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Income", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                  Text(IndianCurrencyFormatter.formatCompact(monthlySummary.totalIncome, currencySymbol), style = MaterialTheme.typography.titleMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Expenses", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                  Text(IndianCurrencyFormatter.formatCompact(monthlySummary.totalExpense, currencySymbol), style = MaterialTheme.typography.titleMedium, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
              }
              com.example.ui.components.PremiumTrendChart(
                points = dailyTrend,
                lineColor = if (isLight) Color(0xFF8B5CF6) else Color(0xFFC084FC),
                trackColor = if (isLight) Color(0xFFE9D5FF) else Color.White.copy(alpha = 0.15f),
                height = 92.dp
              )
              Text("Daily spending trend", style = MaterialTheme.typography.labelSmall, color = subTextColor)
            }
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.example.ui.components.PremiumMetricTile(
              title = "Budget used",
              value = if (monthlySummary.budgetLimit > 0) "${monthlySummary.budgetUsagePercent.toInt()}%" else "Not set",
              caption = if (monthlySummary.budgetLimit > 0) "Of monthly limit" else "Create a budget to compare",
              positive = if (monthlySummary.budgetLimit > 0) monthlySummary.budgetUsagePercent < 90 else null,
              modifier = Modifier.weight(1f)
            )
            com.example.ui.components.PremiumMetricTile(
              title = "Transactions",
              value = monthlySummary.transactionCount.toString(),
              caption = monthlySummary.monthLabel,
              modifier = Modifier.weight(1f)
            )
          }
        }
        item {
          com.example.ui.components.PremiumSectionHeading(
            title = "Where it goes",
            subtitle = "Categories ranked by recorded spending."
          )
        }
        if (categorySpending.filter { it.amount > 0.0 }.isEmpty()) {
          item {
            com.example.ui.components.PremiumCard {
              com.example.ui.components.PremiumEmptyState(
                title = "No spending pattern yet",
                body = "Once you record expenses, you will see your largest categories here."
              )
            }
          }
        } else {
          val list = categorySpending.filter { it.amount > 0.0 }.take(6)
          items(list.size, key = { "insight_cat_${list[it].category.name}" }) { index ->
            val item = list[index]
            com.example.ui.components.PremiumCard {
              Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                val haptics = com.example.ui.util.rememberPaisaHaptics()
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      haptics.performLightHaptic()
                      viewModel.selectCategoryForDetail(item.category)
                    }
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Surface(shape = RoundedCornerShape(12.dp), color = item.category.color.copy(alpha = 0.13f)) {
                    Icon(item.category.icon, contentDescription = null, tint = item.category.color, modifier = Modifier.padding(9.dp).size(19.dp))
                  }
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.category.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${item.count} ${if (item.count == 1) "transaction" else "transactions"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${(item.percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                com.example.ui.components.PremiumProgressTrack(
                  progress = item.percentage,
                  color = item.category.color,
                  height = 7.dp
                )
              }
            }
          }
        }
        item {
          MonthlyInsightCard(summary = monthlySummary, categorySpending = categorySpending, currencySymbol = currencySymbol)
        }
      } else {
        item {
          val isLight = !com.example.ui.theme.isAppInDarkTheme()
          val textColor = if (isLight) Color(0xFF2E1065) else Color.White
          val subTextColor = if (isLight) Color(0xFF4C1D95).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.72f)
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
                    listOf(
                      Color(0xFF6D28D9),
                      Color(0xFF4C1D95)
                    )
                  }
                ),
                shape = RoundedCornerShape(28.dp)
              )
          ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
              Text("YEARLY OVERVIEW", style = MaterialTheme.typography.labelMedium, color = subTextColor, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
              Text(
                IndianCurrencyFormatter.formatWithSymbol(yearlySummary.savings, currencySymbol, includeSign = true),
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
              )
              Text("Net cash flow in $currentYear", style = MaterialTheme.typography.bodySmall, color = subTextColor)
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Income", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                  Text(IndianCurrencyFormatter.formatCompact(yearlySummary.totalIncome, currencySymbol), style = MaterialTheme.typography.titleMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Expenses", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                  Text(IndianCurrencyFormatter.formatCompact(yearlySummary.totalExpense, currencySymbol), style = MaterialTheme.typography.titleMedium, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
              }
              Text(
                if (yearlySummary.totalIncome > 0.0) "Savings rate: ${String.format(java.util.Locale.getDefault(), "%.1f", yearlySummary.savingsRate)}%"
                else "Record income and expenses to build your yearly overview.",
                style = MaterialTheme.typography.bodySmall,
                color = subTextColor
              )
            }
          }
        }
        item {
          com.example.ui.components.PremiumSectionHeading(
            title = "Month by month",
            subtitle = "A consistent view of the full year."
          )
        }
        items(yearlySummary.monthlyBreakdown, key = { "year_month_${it.monthKey}" }) { month ->
          val largest = maxOf(month.totalIncome, month.totalExpense).coerceAtLeast(1.0)
          com.example.ui.components.PremiumCard {
            Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(month.monthLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(IndianCurrencyFormatter.formatWithSymbol(month.savings, currencySymbol, includeSign = true), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (month.savings >= 0) IncomeGreen else ExpenseRed)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("In", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                  Box(Modifier.fillMaxWidth((month.totalIncome / largest).toFloat().coerceIn(0f, 1f)).height(7.dp).clip(CircleShape).background(IncomeGreen))
                }
                Text(IndianCurrencyFormatter.formatCompact(month.totalIncome, currencySymbol), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Out", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                  Box(Modifier.fillMaxWidth((month.totalExpense / largest).toFloat().coerceIn(0f, 1f)).height(7.dp).clip(CircleShape).background(ExpenseRed))
                }
                Text(IndianCurrencyFormatter.formatCompact(month.totalExpense, currencySymbol), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
        item {
          AnnualAnalyticsSummary(summary = yearlySummary, currencySymbol = currencySymbol)
        }
      }
      item { Spacer(Modifier.height(10.dp)) }
    }
  }
}
@Composable
private fun MonthlyInsightCard(
    summary: MonthlyFinancialSummary,
    categorySpending: List<CategorySpending>,
    currencySymbol: String
) {
    val topCategory = categorySpending.maxByOrNull { it.amount }
    val insightText = when {
        summary.savings > 0.0 -> "Recorded income was greater than expenses by ${IndianCurrencyFormatter.formatWithSymbol(summary.savings, currencySymbol)} this month."
        summary.savings < 0.0 -> "Recorded expenses exceeded income by ${IndianCurrencyFormatter.formatWithSymbol(kotlin.math.abs(summary.savings), currencySymbol)} this month."
        else -> "Recorded income and expenses are equal for this month."
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp).size(20.dp)
                    )
                }
                Text(
                    text = "Monthly insight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = insightText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (topCategory != null) {
                Text(
                    text = "Highest recorded expense category: ${topCategory.category.displayName} (${IndianCurrencyFormatter.formatWithSymbol(topCategory.amount, currencySymbol)}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (summary.budgetLimit > 0.0) {
                val budgetPercent = (summary.budgetUsed / summary.budgetLimit * 100.0).coerceAtLeast(0.0)
                Text(
                    text = "Budget used: ${String.format(java.util.Locale.getDefault(), "%.0f", budgetPercent)}%.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (budgetPercent > 100.0) ExpenseRed else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    title: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = tint.copy(alpha = 0.10f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnnualAnalyticsSummary(
    summary: YearlyFinancialSummary,
    currencySymbol: String
) {
    com.example.ui.components.PremiumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Year ${summary.year} overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AnalyticsMetricCard(
                title = "Annual income",
                value = IndianCurrencyFormatter.formatWithSymbol(summary.totalIncome, currencySymbol),
                tint = IncomeGreen,
                modifier = Modifier.fillMaxWidth()
            )
            AnalyticsMetricCard(
                title = "Annual expenses",
                value = IndianCurrencyFormatter.formatWithSymbol(summary.totalExpense, currencySymbol),
                tint = ExpenseRed,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Net savings: ${IndianCurrencyFormatter.formatWithSymbol(summary.savings, currencySymbol, includeSign = true)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (summary.savings >= 0) IncomeGreen else ExpenseRed
            )
            Text(
                text = "Savings rate: ${String.format(java.util.Locale.getDefault(), "%.1f", summary.savingsRate)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnnualMonthlyBreakdown(
    summary: YearlyFinancialSummary,
    currencySymbol: String
) {
    com.example.ui.components.PremiumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Month-by-month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            summary.monthlyBreakdown.forEachIndexed { index, month ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = month.monthLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = IndianCurrencyFormatter.formatWithSymbol(month.savings, currencySymbol, includeSign = true),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (month.savings >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "In ${IndianCurrencyFormatter.formatCompact(month.totalIncome, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen
                        )
                        Text(
                            text = "Out ${IndianCurrencyFormatter.formatCompact(month.totalExpense, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseRed
                        )
                    }
                }
                if (index < summary.monthlyBreakdown.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsEmptyState(message: String) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}
