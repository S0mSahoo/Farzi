package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategorySpending
import com.example.data.model.TransactionCategory
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlin.math.roundToInt

/**
 * Reusable budget summary with a bounded progress indicator and an explicit
 * remaining/over-budget message. It displays only the values supplied by the
 * existing financial calculations.
 */
@Composable
fun BudgetProgressBar(
    budgetLimit: Double,
    budgetUsed: Double,
    currencySymbol: String
) {
    val hasBudget = budgetLimit > 0.0
    val progress = if (hasBudget) (budgetUsed / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = budgetLimit - budgetUsed
    val isOverBudget = hasBudget && remaining < 0.0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Budget used",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = IndianCurrencyFormatter.formatWithSymbol(budgetUsed, currencySymbol),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Monthly limit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = IndianCurrencyFormatter.formatWithSymbol(budgetLimit, currencySymbol),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = when {
                isOverBudget -> ExpenseRed
                progress >= 0.85f -> MaterialTheme.colorScheme.tertiary
                else -> IncomeGreen
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasBudget) "${(budgetUsed / budgetLimit * 100.0).roundToInt()}% used" else "No budget set",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasBudget) {
                Text(
                    text = if (isOverBudget) {
                        "Over by ${IndianCurrencyFormatter.formatWithSymbol(-remaining, currencySymbol)}"
                    } else {
                        "${IndianCurrencyFormatter.formatWithSymbol(remaining, currencySymbol)} left"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverBudget) ExpenseRed else IncomeGreen
                )
            }
        }
    }
}

/**
 * Category spending list with proportional bars, transaction counts, and
 * clickable rows that preserve the existing category-detail flow.
 */
@Composable
fun CategorySpendingBreakdown(
    categories: List<CategorySpending>,
    currencySymbol: String,
    onCategoryClick: (TransactionCategory) -> Unit
) {
    if (categories.isEmpty()) {
        Text(
            text = "Category spending will appear here once expenses are recorded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categories.sortedByDescending { it.amount }.forEach { item ->
            val percent = (item.percentage * 100f).roundToInt().coerceIn(0, 100)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCategoryClick(item.category) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = item.color.copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.category.icon,
                                    contentDescription = item.category.displayName,
                                    tint = item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.category.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.count} ${if (item.count == 1) "transaction" else "transactions"} · $percent%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LinearProgressIndicator(
                        progress = { item.percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = item.color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Side-by-side income and expense comparison. Bars are scaled against the
 * larger of the two values so neither can exceed the track.
 */
@Composable
fun IncomeExpenseComparisonBar(
    income: Double,
    expense: Double,
    currencySymbol: String
) {
    val maxValue = maxOf(income, expense, 1.0)
    val incomeProgress = (income / maxValue).toFloat().coerceIn(0f, 1f)
    val expenseProgress = (expense / maxValue).toFloat().coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    IndianCurrencyFormatter.formatWithSymbol(income, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }
            LinearProgressIndicator(
                progress = { incomeProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = IncomeGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    IndianCurrencyFormatter.formatWithSymbol(expense, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }
            LinearProgressIndicator(
                progress = { expenseProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = ExpenseRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
