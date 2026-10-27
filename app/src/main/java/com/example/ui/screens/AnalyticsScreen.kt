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
    val selectedCategoryDetail by viewModel.selectedCategoryDetail.collectAsStateWithLifecycle()

    var isAnnualView by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    val currencySymbol = userProfile.currencySymbol
    val currentYear = selectedCalendar.get(Calendar.YEAR)

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
            onYearSelected = { viewModel.setDashboardYear(it) }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Insights & analytics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Understand where your money goes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp).size(26.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
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
                    label = { Text("Annual") }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { if (isAnnualView) viewModel.previousDashboardYear() else viewModel.previousDashboardMonth() }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = if (isAnnualView) "Previous year" else "Previous month")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { if (isAnnualView) showYearPicker = true else showMonthPicker = true }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAnnualView) currentYear.toString() else monthlySummary.monthLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Choose period",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { if (isAnnualView) viewModel.nextDashboardYear() else viewModel.nextDashboardMonth() }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = if (isAnnualView) "Next year" else "Next month")
                }
            }
        }

        if (isAnnualView) {
            item {
                AnnualAnalyticsSummary(
                    summary = yearlySummary,
                    currencySymbol = currencySymbol
                )
            }
            item {
                AnnualMonthlyBreakdown(
                    summary = yearlySummary,
                    currencySymbol = currencySymbol
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsMetricCard(
                        title = "Income",
                        value = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.totalIncome, currencySymbol),
                        tint = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMetricCard(
                        title = "Expenses",
                        value = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.totalExpense, currencySymbol),
                        tint = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Net savings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = IndianCurrencyFormatter.formatWithSymbol(
                                monthlySummary.savings,
                                currencySymbol,
                                includeSign = true
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (monthlySummary.savings >= 0) IncomeGreen else ExpenseRed
                        )
                        Text(
                            text = "Savings rate: ${String.format(java.util.Locale.getDefault(), "%.1f", monthlySummary.savingsRate)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (monthlySummary.transactionCount > 0) {
                item {
                    MonthlyInsightCard(
                        summary = monthlySummary,
                        categorySpending = categorySpending,
                        currencySymbol = currencySymbol
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Income vs expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (monthlySummary.transactionCount == 0) {
                            AnalyticsEmptyState("Add a transaction to see your monthly comparison.")
                        } else {
                            IncomeExpenseComparisonBar(
                                income = monthlySummary.totalIncome,
                                expense = monthlySummary.totalExpense,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Spending by category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (categorySpending.isEmpty()) {
                            AnalyticsEmptyState("Category insights appear when you record expenses for this period.")
                        } else {
                            CategorySpendingBreakdown(
                                categories = categorySpending,
                                currencySymbol = currencySymbol,
                                onCategoryClick = { viewModel.selectCategoryForDetail(it) }
                            )
                        }
                    }
                }
            }
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
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
