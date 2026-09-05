package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pro.engine.forecast.CashFlowForecastResult
import com.example.pro.engine.forecast.ForecastCategoryType
import com.example.pro.engine.forecast.ForecastConfidence
import com.example.pro.engine.forecast.ForecastLineItem
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastSheet(
    forecast: CashFlowForecastResult,
    currencySymbol: String,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = Modifier.testTag("forecast_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
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
                            color = MinimalBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = MinimalBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Cash-Flow Forecast",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Deterministic Period Projection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Confidence & Status Banner
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when (forecast.confidence) {
                        ForecastConfidence.HIGH -> IncomeGreen.copy(alpha = 0.12f)
                        ForecastConfidence.MEDIUM -> AccentAmber.copy(alpha = 0.12f)
                        ForecastConfidence.LOW, ForecastConfidence.INSUFFICIENT_DATA -> ExpenseRed.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (forecast.confidence) {
                            ForecastConfidence.HIGH -> IncomeGreen.copy(alpha = 0.3f)
                            ForecastConfidence.MEDIUM -> AccentAmber.copy(alpha = 0.3f)
                            ForecastConfidence.LOW, ForecastConfidence.INSUFFICIENT_DATA -> ExpenseRed.copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (forecast.confidence) {
                                ForecastConfidence.HIGH -> Icons.Default.Timeline
                                ForecastConfidence.MEDIUM -> Icons.Default.Info
                                ForecastConfidence.LOW, ForecastConfidence.INSUFFICIENT_DATA -> Icons.Default.WarningAmber
                            },
                            contentDescription = null,
                            tint = when (forecast.confidence) {
                                ForecastConfidence.HIGH -> IncomeGreen
                                ForecastConfidence.MEDIUM -> AccentAmber
                                ForecastConfidence.LOW, ForecastConfidence.INSUFFICIENT_DATA -> ExpenseRed
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Confidence: ${forecast.confidence.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (forecast.confidence) {
                                    ForecastConfidence.HIGH -> IncomeGreen
                                    ForecastConfidence.MEDIUM -> AccentAmber
                                    ForecastConfidence.LOW, ForecastConfidence.INSUFFICIENT_DATA -> ExpenseRed
                                }
                            )
                            Text(
                                text = forecast.confidenceReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Warnings if any
            if (forecast.warnings.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ExpenseRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                Text("Projection Notice", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                            forecast.warnings.forEach { warning ->
                                Text("• $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Main Projected End Position Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (forecast.projectedEndPeriodPosition < 0) ExpenseRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (forecast.projectedEndPeriodPosition < 0) ExpenseRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "PROJECTED END-OF-PERIOD BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = IndianCurrencyFormatter.formatWithSymbol(forecast.projectedEndPeriodPosition, currencySymbol),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (forecast.projectedEndPeriodPosition >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Projected Net Savings:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${IndianCurrencyFormatter.formatWithSymbol(forecast.projectedSavings, currencySymbol, includeSign = true)} (${String.format("%.1f", forecast.projectedSavingsRate)}%)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (forecast.projectedSavings >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // 3-Tier Horizon Breakdown
            item {
                Text(
                    text = "Forecast Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Layer 1: ACTUAL
            item {
                ForecastTierCard(
                    badge = "ACTUAL",
                    badgeColor = IncomeGreen,
                    title = "Already Occurred & Recorded",
                    subtitle = "Income: ${IndianCurrencyFormatter.formatWithSymbol(forecast.actualIncome, currencySymbol)} | Expenses: ${IndianCurrencyFormatter.formatWithSymbol(forecast.actualExpenses, currencySymbol)}",
                    netAmount = forecast.actualNet,
                    currencySymbol = currencySymbol
                )
            }

            // Layer 2: KNOWN FUTURE
            item {
                val knownNet = forecast.knownFutureIncome - forecast.knownFutureExpenses
                ForecastTierCard(
                    badge = "KNOWN",
                    badgeColor = MinimalBlue,
                    title = "Scheduled Recurring Transactions",
                    subtitle = "Upcoming recurring income & bills before month end",
                    netAmount = knownNet,
                    currencySymbol = currencySymbol
                )
            }

            // Layer 3: ESTIMATED REMAINING
            item {
                val estimatedNet = forecast.estimatedRemainingIncome - forecast.estimatedRemainingExpenses
                ForecastTierCard(
                    badge = "ESTIMATED",
                    badgeColor = AccentAmber,
                    title = "Estimated Discretionary Spending",
                    subtitle = "Projected remaining daily run rate",
                    netAmount = estimatedNet,
                    currencySymbol = currencySymbol
                )
            }

            // Line items if present
            if (forecast.lineItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Forecast Items",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(forecast.lineItems) { item ->
                    ForecastLineItemRow(item = item, currencySymbol = currencySymbol)
                }
            }

            // Educational / Truth in Labeling Footnote
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Estimates reflect historical spending patterns and scheduled recurring rules. They are projections and never represent guaranteed account balances.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ForecastTierCard(
    badge: String,
    badgeColor: Color,
    title: String,
    subtitle: String,
    netAmount: Double,
    currencySymbol: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = IndianCurrencyFormatter.formatWithSymbol(netAmount, currencySymbol, includeSign = true),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (netAmount >= 0) IncomeGreen else ExpenseRed
            )
        }
    }
}

@Composable
private fun ForecastLineItemRow(
    item: ForecastLineItem,
    currencySymbol: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
