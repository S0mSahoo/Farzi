package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpendingPoint
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlin.math.max

@Composable
fun BudgetProgressBar(
  budgetLimit: Double,
  budgetUsed: Double,
  currencySymbol: String = "₹",
  modifier: Modifier = Modifier
) {
  val remaining = budgetLimit - budgetUsed
  val ratio = if (budgetLimit > 0) (budgetUsed / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
  val percent = if (budgetLimit > 0) (budgetUsed / budgetLimit * 100.0) else 0.0
  val isExceeded = budgetUsed > budgetLimit && budgetLimit > 0
  val isNearLimit = ratio >= 0.8f && !isExceeded

  val statusColor = when {
    isExceeded -> ExpenseRed
    isNearLimit -> AccentAmber
    else -> IncomeGreen
  }

  val statusText = when {
    isExceeded -> "Exceeded by ${IndianCurrencyFormatter.formatWithSymbol(budgetUsed - budgetLimit, currencySymbol)}"
    isNearLimit -> "Warning: ${String.format("%.1f", percent)}% used"
    budgetLimit > 0 -> "${IndianCurrencyFormatter.formatWithSymbol(remaining, currencySymbol)} remaining"
    else -> "No budget set"
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Budget Utilization",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusColor.copy(alpha = 0.15f)
      ) {
        Text(
          text = if (budgetLimit > 0) "${String.format("%.0f", percent)}%" else "None",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = statusColor,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Progress Bar
    LinearProgressIndicator(
      progress = { ratio },
      modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .clip(RoundedCornerShape(5.dp)),
      color = statusColor,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
      strokeCap = StrokeCap.Round
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Spent: ${IndianCurrencyFormatter.formatWithSymbol(budgetUsed, currencySymbol)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = statusColor
      )
    }
  }
}

@Composable
fun IncomeExpenseComparisonBar(
  income: Double,
  expense: Double,
  currencySymbol: String = "₹",
  modifier: Modifier = Modifier
) {
  val total = income + expense
  val incomeRatio = if (total > 0) (income / total).toFloat() else 0.5f
  val expenseRatio = if (total > 0) (expense / total).toFloat() else 0.5f

  val animatedIncomeRatio by animateFloatAsState(targetValue = incomeRatio, label = "income_ratio")

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
        Text(
          text = "Income ${if (total > 0) "(${String.format("%.0f", incomeRatio * 100)}%)" else ""}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
        Text(
          text = "Expenses ${if (total > 0) "(${String.format("%.0f", expenseRatio * 100)}%)" else ""}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Multi-color segmented bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(12.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
      if (income > 0 || expense > 0) {
        if (animatedIncomeRatio > 0) {
          Box(
            modifier = Modifier
              .weight(animatedIncomeRatio.coerceAtLeast(0.01f))
              .fillMaxHeight()
              .background(IncomeGreen)
          )
        }
        if (1f - animatedIncomeRatio > 0) {
          Box(
            modifier = Modifier
              .weight((1f - animatedIncomeRatio).coerceAtLeast(0.01f))
              .fillMaxHeight()
              .background(ExpenseRed)
          )
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
        )
      }
    }
  }
}

@Composable
fun CategorySpendingBreakdown(
  categories: List<CategorySpending>,
  currencySymbol: String = "₹",
  onCategoryClick: ((CategorySpending) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    categories.forEach { item ->
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
          .fillMaxWidth()
          .then(
            if (onCategoryClick != null) {
              Modifier.clip(RoundedCornerShape(14.dp)).clickable { onCategoryClick(item) }
            } else Modifier
          )
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = item.color.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = item.category.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              Column {
                Text(
                  text = item.category.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${item.count} transaction${if (item.count != 1) "s" else ""} • Tap to view",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${String.format("%.1f", item.percentage * 100)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Progress indicator for category
          LinearProgressIndicator(
            progress = { item.percentage.coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = item.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun DailySpendingTrendBarChart(
  dailyPoints: List<DailySpendingPoint>,
  currencySymbol: String = "₹",
  modifier: Modifier = Modifier
) {
  val maxDailyExpense = remember(dailyPoints) {
    max(dailyPoints.maxOfOrNull { it.expense } ?: 1.0, 100.0)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "Daily Spending Trend",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      dailyPoints.forEach { point ->
        val heightRatio = (point.expense / maxDailyExpense).toFloat().coerceIn(0.05f, 1f)
        val hasActivity = point.expense > 0 || point.income > 0

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom,
          modifier = Modifier.width(28.dp)
        ) {
          // Bar
          Box(
            modifier = Modifier
              .height(80.dp)
              .width(14.dp),
            contentAlignment = Alignment.BottomCenter
          ) {
            // Track
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
            // Active Bar
            if (point.expense > 0) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .fillMaxHeight(heightRatio)
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (point.income > 0) IncomeGreen else ExpenseRed)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Day label
          Text(
            text = point.dayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (hasActivity) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = if (hasActivity) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }
  }
}
