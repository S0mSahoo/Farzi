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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OccurrenceStatus
import com.example.data.model.RecurringOccurrence
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecurringScreen(
  viewModel: FinanceViewModel,
  onOpenAddRecurringRule: () -> Unit,
  onEditRecurringRule: (rule: RecurringRule) -> Unit
) {
  val recurringRules by viewModel.allRecurringRules.collectAsState()
  val scheduledOccurrences by viewModel.allScheduledOccurrences.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()
  var ruleToDelete by remember { mutableStateOf<RecurringRule?>(null) }
  var selectedTab by remember { mutableStateOf(0) } // 0: Upcoming Bills, 1: Recurring Rules

  val currencySymbol = userProfile.currencySymbol
  var occurrenceToCancel by remember { mutableStateOf<RecurringOccurrence?>(null) }

  if (ruleToDelete != null) {
    ConfirmationDialog(
      title = "Delete Recurring Rule?",
      message = "Are you sure you want to delete the recurring rule \"${ruleToDelete?.title}\"? Existing transactions generated from it will remain intact.",
      onConfirm = {
        ruleToDelete?.let { viewModel.deleteRecurringRule(it.id) }
        ruleToDelete = null
      },
      onDismiss = { ruleToDelete = null }
    )
  }

  if (occurrenceToCancel != null) {
    ConfirmationDialog(
      title = "Cancel Payment Occurrence?",
      message = "This will cancel the payment for ${occurrenceToCancel?.occurrenceDateKey} only. Your recurring rule \"${occurrenceToCancel?.rule?.title}\" will stay active for subsequent months. No transaction will be created.",
      confirmText = "Cancel Occurrence",
      onConfirm = {
        occurrenceToCancel?.let { viewModel.cancelRecurringOccurrence(it) }
        occurrenceToCancel = null
      },
      onDismiss = { occurrenceToCancel = null }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("recurring_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Recurring & Bills",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Track upcoming dues & manage subscriptions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Button(
          onClick = onOpenAddRecurringRule,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("add_recurring_rule_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Rule")
        }
      }
    }

    // 2. Segmented Navigation (Upcoming Bills vs Rules)
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(modifier = Modifier.padding(4.dp)) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
            tonalElevation = if (selectedTab == 0) 2.dp else 0.dp,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedTab = 0 }
          ) {
            Box(
              modifier = Modifier.padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Upcoming Bills (${scheduledOccurrences.count { it.status != OccurrenceStatus.PAID }})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
            tonalElevation = if (selectedTab == 1) 2.dp else 0.dp,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedTab = 1 }
          ) {
            Box(
              modifier = Modifier.padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Active Rules (${recurringRules.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    if (selectedTab == 0) {
      // UPCOMING BILLS TAB with Mark As Paid
      if (scheduledOccurrences.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = IncomeGreen,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No Upcoming Bills",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Add recurring rules like rent, electricity, or subscriptions to track upcoming payments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      } else {
        items(scheduledOccurrences, key = { "${it.rule.id}_${it.occurrenceDateKey}" }) { occ ->
          val isExpense = occ.rule.type == TransactionType.EXPENSE
          val dueDateLabel = DateUtils.getFriendlyRelativeDate(occ.occurrenceTimestamp)
          val isPaid = occ.status == OccurrenceStatus.PAID

          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isPaid) IncomeGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = occ.rule.category.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        occ.rule.category.icon,
                        contentDescription = null,
                        tint = occ.rule.category.color,
                        modifier = Modifier.size(22.dp)
                      )
                    }
                  }

                  Column {
                    Text(
                      text = occ.rule.title,
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "$dueDateLabel • ${occ.rule.paymentMethod.displayName}",
                      style = MaterialTheme.typography.bodySmall,
                      color = if (!isPaid && occ.occurrenceTimestamp <= System.currentTimeMillis()) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Text(
                  text = "${if (isExpense) "-" else "+"}${IndianCurrencyFormatter.formatWithSymbol(occ.rule.amount, currencySymbol)}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (isExpense) ExpenseRed else IncomeGreen
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Action Row
              val isCancelled = occ.status == OccurrenceStatus.CANCELLED
              val isOverdue = occ.status == OccurrenceStatus.OVERDUE
              val isDueToday = occ.status == OccurrenceStatus.DUE_TODAY

              val statusColor = when {
                isPaid -> IncomeGreen
                isCancelled -> MaterialTheme.colorScheme.outline
                isOverdue -> ExpenseRed
                isDueToday -> MaterialTheme.colorScheme.primary
                else -> AccentAmber
              }

              val statusText = when {
                isPaid -> "Paid"
                isCancelled -> "Cancelled"
                isOverdue -> "Overdue"
                isDueToday -> "Due Today"
                else -> "Scheduled"
              }

              val statusIcon = when {
                isPaid -> Icons.Default.CheckCircle
                isCancelled -> Icons.Default.Block
                isOverdue -> Icons.Default.Warning
                isDueToday -> Icons.Default.Schedule
                else -> Icons.Default.CalendarToday
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = statusColor.copy(alpha = 0.15f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      statusIcon,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = statusColor
                    )
                    Text(
                      text = statusText,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = statusColor
                    )
                  }
                }

                when {
                  isCancelled -> {
                    OutlinedButton(
                      onClick = { viewModel.restoreRecurringOccurrence(occ) },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.testTag("restore_occ_${occ.rule.id}_${occ.occurrenceDateKey}")
                    ) {
                      Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Restore", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                  !isPaid -> {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedButton(
                        onClick = { occurrenceToCancel = occ },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("cancel_occ_${occ.rule.id}_${occ.occurrenceDateKey}")
                      ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skip", style = MaterialTheme.typography.labelSmall)
                      }

                      Button(
                        onClick = { viewModel.markRecurringPaymentPaid(occ) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen, contentColor = Color.White),
                        modifier = Modifier.testTag("pay_occ_${occ.rule.id}_${occ.occurrenceDateKey}")
                      ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Paid", style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                  else -> {
                    Text(
                      text = "Transaction recorded",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // ACTIVE RULES TAB
      // Offline Automatic Engine Status Card
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
              }

              Column {
                Text(
                  text = "Idempotent Generation",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Rules auto-generate transactions safely on due dates without duplicates.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            IconButton(onClick = { viewModel.processRecurringRules() }) {
              Icon(Icons.Default.Refresh, contentDescription = "Sync now", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }

      if (recurringRules.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "No Recurring Rules Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "Create rules for regular monthly salary, house rent, Netflix, gym fees, electricity bills, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                onClick = onOpenAddRecurringRule,
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Your First Rule")
              }
            }
          }
        }
      } else {
        items(recurringRules, key = { it.id }) { rule ->
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onEditRecurringRule(rule) }
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = rule.category.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(rule.category.icon, contentDescription = null, tint = rule.category.color, modifier = Modifier.size(22.dp))
                    }
                  }

                  Column {
                    Text(
                      text = rule.title,
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                      ) {
                        Text(
                          text = rule.interval.displayName,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }

                      Text(
                        text = "${rule.category.displayName} • ${rule.paymentMethod.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }

                // Amount
                Text(
                  text = IndianCurrencyFormatter.formatWithSymbol(
                    amount = rule.amount,
                    symbol = currencySymbol,
                    includeSign = true
                  ),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (rule.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Footer row with Active switch and Delete button
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Switch(
                    checked = rule.isActive,
                    onCheckedChange = { viewModel.toggleRecurringRule(rule.id, it) },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                  )
                  Text(
                    text = if (rule.isActive) "Active" else "Paused",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (rule.isActive) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  IconButton(onClick = { onEditRecurringRule(rule) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                  }
                  IconButton(onClick = { ruleToDelete = rule }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

