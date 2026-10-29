package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.insight.FinanceInsight
import com.example.data.insight.InsightSeverity
import com.example.data.insight.InsightType
import com.example.data.model.TransactionCategory
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.resolveCardSurfaceColor
import com.example.ui.util.rememberPaisaHaptics

@Composable
fun FinancialInsightsSection(
  insights: List<FinanceInsight>,
  periodLabel: String,
  currencySymbol: String,
  onOpenSetBudget: () -> Unit = {},
  onCategoryClick: ((TransactionCategory) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  if (insights.isEmpty()) return

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("financial_insights_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    val warningCount = insights.count { it.severity == InsightSeverity.CRITICAL || it.severity == InsightSeverity.WARNING }

    PremiumSectionHeading(
      title = "Financial Insights",
      subtitle = "Smart analysis for $periodLabel",
      trailing = {
        if (warningCount > 0) {
          Surface(
            shape = CircleShape,
            color = ExpenseRed.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.30f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
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

    val isOnlyEmptyState = insights.all { it.severity == InsightSeverity.NEUTRAL }

    if (isOnlyEmptyState) {
      PremiumCard {
        PremiumEmptyState(
          title = "Awaiting financial activity",
          body = "Record your transactions and set budgets to unlock real-time financial insights for $periodLabel.",
          action = null
        )
      }
    } else {
      insights.forEach { insight ->
        if (insight.severity != InsightSeverity.NEUTRAL) {
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

  val (accentColor, icon, severityLabel) = when (insight.severity) {
    InsightSeverity.CRITICAL -> Triple(ExpenseRed, Icons.Default.ErrorOutline, "CRITICAL")
    InsightSeverity.WARNING -> Triple(Color(0xFFF59E0B), Icons.Default.WarningAmber, "ALERT")
    InsightSeverity.SUCCESS -> Triple(IncomeGreen, Icons.Default.CheckCircleOutline, "ON TRACK")
    InsightSeverity.INFO -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.AutoAwesome, "INSIGHT")
    InsightSeverity.NEUTRAL -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Info, "INFO")
  }

  val isClickable = insight.category != null && onCategoryClick != null

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = resolveCardSurfaceColor(),
    border = BorderStroke(1.dp, accentColor.copy(alpha = if (insight.severity == InsightSeverity.CRITICAL) 0.5f else 0.25f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("insight_card_${insight.id}")
      .then(
        if (isClickable) {
          Modifier.clickable {
            haptics.performLightHaptic()
            insight.category?.let { onCategoryClick?.invoke(it) }
          }
        } else Modifier
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
              imageVector = insight.category?.icon ?: getInsightTypeIcon(insight.type, icon),
              contentDescription = insight.title,
              tint = accentColor,
              modifier = Modifier
                .padding(8.dp)
                .size(20.dp)
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
              text = insight.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            insight.category?.let { cat ->
              Text(
                text = cat.displayName,
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

      Text(
        text = insight.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
      )

      // Action Row if budget action is relevant
      if (insight.type == InsightType.BUDGET_STATUS && (insight.severity == InsightSeverity.CRITICAL || insight.severity == InsightSeverity.WARNING)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          PremiumTextButton(onClick = onOpenSetBudget) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
              Text("Adjust Budget", fontWeight = FontWeight.Bold)
              Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(15.dp))
            }
          }
        }
      }
    }
  }
}

private fun getInsightTypeIcon(type: InsightType, defaultIcon: ImageVector): ImageVector {
  return when (type) {
    InsightType.CASH_FLOW -> Icons.Default.TrendingUp
    InsightType.SAVINGS_RATE -> Icons.Default.TrendingUp
    InsightType.PERIOD_COMPARISON -> Icons.Default.TrendingDown
    InsightType.TOP_CATEGORY -> Icons.Default.AutoAwesome
    InsightType.BUDGET_STATUS -> Icons.Default.AccountBalanceWallet
    InsightType.RECURRING_COMMITMENTS -> Icons.Default.Repeat
  }
}
