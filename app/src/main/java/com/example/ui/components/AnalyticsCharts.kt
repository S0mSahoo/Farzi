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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import com.example.ui.util.AnimatedFinancialText
import com.example.ui.util.PaisaMotion
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
    val rawProgress = if (hasBudget) (budgetUsed / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(PaisaMotion.EnterFadeDurationMillis, easing = PaisaMotion.MotionEasing),
        label = "budget_progress"
    )
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
                AnimatedFinancialText(
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
                AnimatedFinancialText(
                    text = IndianCurrencyFormatter.formatWithSymbol(budgetLimit, currencySymbol),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = when {
                isOverBudget -> ExpenseRed
                rawProgress >= 0.85f -> MaterialTheme.colorScheme.tertiary
                else -> IncomeGreen
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedFinancialText(
                text = if (hasBudget) "${(budgetUsed / budgetLimit * 100.0).roundToInt()}% used" else "No budget set",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasBudget) {
                AnimatedFinancialText(
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
            val animatedProgress by animateFloatAsState(
                targetValue = item.percentage.coerceIn(0f, 1f),
                animationSpec = tween(PaisaMotion.EnterFadeDurationMillis, easing = PaisaMotion.MotionEasing),
                label = "category_progress_${item.category.name}"
            )
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
                            AnimatedFinancialText(
                                text = "${item.count} ${if (item.count == 1) "transaction" else "transactions"} · $percent%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedFinancialText(
                            text = IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
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
    val targetIncomeProgress = (income / maxValue).toFloat().coerceIn(0f, 1f)
    val targetExpenseProgress = (expense / maxValue).toFloat().coerceIn(0f, 1f)

    val animatedIncomeProgress by animateFloatAsState(
        targetValue = targetIncomeProgress,
        animationSpec = tween(PaisaMotion.EnterFadeDurationMillis, easing = PaisaMotion.MotionEasing),
        label = "income_progress"
    )
    val animatedExpenseProgress by animateFloatAsState(
        targetValue = targetExpenseProgress,
        animationSpec = tween(PaisaMotion.EnterFadeDurationMillis, easing = PaisaMotion.MotionEasing),
        label = "expense_progress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedFinancialText(
                    text = IndianCurrencyFormatter.formatWithSymbol(income, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }
            LinearProgressIndicator(
                progress = { animatedIncomeProgress },
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
                AnimatedFinancialText(
                    text = IndianCurrencyFormatter.formatWithSymbol(expense, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }
            LinearProgressIndicator(
                progress = { animatedExpenseProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = ExpenseRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
