package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.insight.FinanceInsight
import com.example.data.insight.InsightSeverity
import com.example.data.insight.InsightType
import com.example.data.model.TransactionCategory
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.resolveCardSurfaceColor
import com.example.ui.util.rememberPaisaHaptics
import java.util.Locale

@Composable
fun FinancialInsightsSection(
  insights: List<FinanceInsight>,
  periodLabel: String,
  currencySymbol: String,
  onOpenSetBudget: () -> Unit = {},
  onCategoryClick: ((TransactionCategory) -> Unit)? = null,
  title: String = "Spending Insights & Analysis",
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("spending_insights_section"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    val nonNeutralInsights = insights.filter { it.severity != InsightSeverity.NEUTRAL }
    val warningCount = nonNeutralInsights.count {
      it.severity == InsightSeverity.CRITICAL || it.severity == InsightSeverity.WARNING
    }

    PremiumSectionHeading(
      title = title,
      subtitle = if (nonNeutralInsights.isEmpty()) "Awaiting activity for $periodLabel" else "Key observations for $periodLabel",
      trailing = {
        if (warningCount > 0) {
          Surface(
            shape = CircleShape,
            color = ExpenseRed.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.30f)),
            modifier = Modifier.semantics {
              contentDescription = "$warningCount critical alerts requiring attention"
            }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(ExpenseRed)
              )
              Text(
                text = "$warningCount Alert${if (warningCount > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
              )
            }
          }
        }
      }
    )

    if (nonNeutralInsights.isEmpty()) {
      PremiumCard(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("insights_empty_state")
      ) {
        PremiumEmptyState(
          title = "No spending insights yet",
          body = "Record transactions and set budgets to unlock real-time spending insights, trends, and health analysis for $periodLabel.",
          action = null
        )
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        nonNeutralInsights.forEach { insight ->
          InsightCardItem(
            insight = insight,
            currencySymbol = currencySymbol,
            onOpenSetBudget = onOpenSetBudget,
            onCategoryClick = onCategoryClick
          )
        }
      }
    }
  }
}

@Composable
private fun InsightCardItem(
  insight: FinanceInsight,
  currencySymbol: String,
  onOpenSetBudget: () -> Unit,
  onCategoryClick: ((TransactionCategory) -> Unit)?
) {
  val haptics = rememberPaisaHaptics()

  val (accentColor, fallbackIcon, severityLabel) = when (insight.severity) {
    InsightSeverity.CRITICAL -> Triple(ExpenseRed, Icons.Default.ErrorOutline, "CRITICAL")
    InsightSeverity.WARNING -> Triple(Color(0xFFF59E0B), Icons.Default.WarningAmber, "ALERT")
    InsightSeverity.SUCCESS -> Triple(IncomeGreen, Icons.Default.CheckCircleOutline, "ON TRACK")
    InsightSeverity.INFO -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.AutoAwesome, "INSIGHT")
    InsightSeverity.NEUTRAL -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Info, "INFO")
  }

  val isCategoryClickable = insight.category != null && onCategoryClick != null
  val iconToUse = insight.category?.icon ?: getInsightTypeIcon(insight.type, fallbackIcon)

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = resolveCardSurfaceColor(),
    border = BorderStroke(
      1.dp,
      accentColor.copy(alpha = if (insight.severity == InsightSeverity.CRITICAL) 0.55f else 0.22f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("insight_card_${insight.id}")
      .then(
        if (isCategoryClickable) {
          Modifier
            .semantics {
              role = Role.Button
              contentDescription = "${insight.title}: ${insight.description}. Tap to view ${insight.category?.displayName} details."
            }
            .clickable {
              haptics.performLightHaptic()
              insight.category?.let { onCategoryClick?.invoke(it) }
            }
        } else {
          Modifier.semantics {
            contentDescription = "${insight.title}: ${insight.description}"
          }
        }
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Icon + Title + Severity Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = accentColor.copy(alpha = 0.12f)
          ) {
            Icon(
              imageVector = iconToUse,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier
                .padding(8.dp)
                .size(20.dp)
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = insight.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (insight.category != null) {
              Text(
                text = insight.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = accentColor.copy(alpha = 0.12f)
        ) {
          Text(
            text = severityLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      // Metric Highlight Row (when amount or percentage is provided)
      val hasMetric = insight.amount != null || insight.percentage != null
      if (hasMetric) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (insight.percentage != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
              Text(
                text = formatMetricPercentage(insight.type, insight.percentage),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }

          if (insight.amount != null) {
            Text(
              text = IndianCurrencyFormatter.formatWithSymbol(
                amount = Math.abs(insight.amount),
                symbol = currencySymbol
              ),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // Explanation Body Text
      Text(
        text = insight.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp
      )

      // Actions & Interactive Affordances
      if (insight.type == InsightType.BUDGET_STATUS && (insight.severity == InsightSeverity.CRITICAL || insight.severity == InsightSeverity.WARNING)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          PremiumTextButton(
            onClick = onOpenSetBudget,
            modifier = Modifier.testTag("action_adjust_budget")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
              Text("Adjust Budget", fontWeight = FontWeight.Bold)
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(15.dp))
            }
          }
        }
      } else if (isCategoryClickable) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "View details",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

private fun formatMetricPercentage(type: InsightType, percentage: Double): String {
  val formatted = String.format(Locale.US, "%.1f", Math.abs(percentage))
  return when (type) {
    InsightType.PERIOD_COMPARISON -> if (percentage >= 0) "+$formatted%" else "-$formatted%"
    InsightType.TOP_CATEGORY -> "$formatted% share"
    InsightType.BUDGET_STATUS -> "$formatted% used"
    InsightType.CASH_FLOW, InsightType.SAVINGS_RATE -> "$formatted% saved"
    else -> "$formatted%"
  }
}

private fun getInsightTypeIcon(type: InsightType, defaultIcon: ImageVector): ImageVector {
  return when (type) {
    InsightType.CASH_FLOW -> Icons.AutoMirrored.Filled.TrendingUp
    InsightType.SAVINGS_RATE -> Icons.AutoMirrored.Filled.TrendingUp
    InsightType.PERIOD_COMPARISON -> Icons.AutoMirrored.Filled.TrendingDown
    InsightType.TOP_CATEGORY -> Icons.Default.PieChart
    InsightType.BUDGET_STATUS -> Icons.Default.AccountBalanceWallet
    InsightType.RECURRING_COMMITMENTS -> Icons.Default.Repeat
  }
}
