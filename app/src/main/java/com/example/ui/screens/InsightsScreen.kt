package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategorySpending
import com.example.data.model.MonthlyFinancialSummary
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar
import java.util.Locale

/**
 * Explainable, rules-based insights from recorded values only.
 * No forecast, trend percentage, or recommendation is shown unless the data supports it.
 */
@Composable
fun InsightsScreen(viewModel: FinanceViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardMonthSummary.collectAsStateWithLifecycle()
    val categories by viewModel.dashboardCategorySpending.collectAsStateWithLifecycle()
    val currencySymbol = userProfile.currencySymbol
    val topCategory = categories.maxByOrNull { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Financial insights",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clear takeaways from your recorded activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp).size(25.dp)
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousDashboardMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = DateUtils.getMonthLabel(selectedCalendar),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Selected reporting month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.nextDashboardMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                    }
                }
            }
        }

        if (summary.transactionCount == 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(54.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Text(
                            text = "Insights start with your records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add income or expenses for this month to see meaningful totals and category insights here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                InsightHero(
                    summary = summary,
                    currencySymbol = currencySymbol
                )
            }

            if (topCategory != null) {
                item {
                    InsightDetailCard(
                        title = "Your largest expense category",
                        value = topCategory.category.displayName,
                        detail = "${IndianCurrencyFormatter.formatWithSymbol(topCategory.amount, currencySymbol)} recorded across ${topCategory.count} ${if (topCategory.count == 1) "transaction" else "transactions"} (${(topCategory.percentage * 100f).toInt()}% of recorded expenses).",
                        icon = Icons.Default.ArrowDownward,
                        tint = topCategory.color
                    )
                }
            }

            item {
                val budgetKnown = summary.budgetLimit > 0.0
                val overBudget = budgetKnown && summary.budgetUsed > summary.budgetLimit
                val remaining = summary.budgetLimit - summary.budgetUsed
                InsightDetailCard(
                    title = if (!budgetKnown) "Budget insight" else if (overBudget) "Budget limit exceeded" else "Budget remaining",
                    value = if (!budgetKnown) "No monthly budget" else if (overBudget) "Over budget" else "Within limit",
                    detail = if (!budgetKnown) {
                        "Set a budget for this month to compare your recorded expenses with a target."
                    } else if (overBudget) {
                        "Recorded expenses exceed the monthly limit by ${IndianCurrencyFormatter.formatWithSymbol(kotlin.math.abs(remaining), currencySymbol)}."
                    } else {
                        "${IndianCurrencyFormatter.formatWithSymbol(remaining, currencySymbol)} remains from your monthly budget."
                    },
                    icon = Icons.Default.Savings,
                    tint = if (overBudget) ExpenseRed else IncomeGreen
                )
            }

            item {
                InsightDetailCard(
                    title = "Monthly activity",
                    value = "${summary.transactionCount} ${if (summary.transactionCount == 1) "transaction" else "transactions"}",
                    detail = "Income: ${IndianCurrencyFormatter.formatWithSymbol(summary.totalIncome, currencySymbol)} · Expenses: ${IndianCurrencyFormatter.formatWithSymbol(summary.totalExpense, currencySymbol)}.",
                    icon = if (summary.totalIncome >= summary.totalExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    tint = if (summary.totalIncome >= summary.totalExpense) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun InsightHero(
    summary: MonthlyFinancialSummary,
    currencySymbol: String
) {
    val isPositive = summary.savings >= 0.0
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Net result this month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = IndianCurrencyFormatter.formatWithSymbol(summary.savings, currencySymbol, includeSign = true),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) IncomeGreen else ExpenseRed,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isPositive) {
                    "Recorded income was equal to or greater than expenses for this period."
                } else {
                    "Recorded expenses were greater than income for this period."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Savings rate: ${String.format(Locale.getDefault(), "%.1f", summary.savingsRate)}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InsightDetailCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = tint.copy(alpha = 0.13f),
                modifier = Modifier.size(46.dp)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
