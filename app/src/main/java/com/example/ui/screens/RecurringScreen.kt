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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.RecurringRule
import com.example.data.model.ScheduledRecurringOccurrence
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.theme.MinimalBlue
import com.example.ui.theme.MinimalEmerald
import com.example.ui.theme.MinimalIndigo
import com.example.ui.theme.MinimalRose
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun RecurringScreen(
  viewModel: FinanceViewModel,
  onOpenAddRecurringRule: () -> Unit,
  onEditRecurringRule: (rule: RecurringRule) -> Unit
) {
  val recurringRules by viewModel.allRecurringRules.collectAsState()
  val dueTodayList by viewModel.dueTodayOccurrences.collectAsState()
  val overdueList by viewModel.overdueOccurrences.collectAsState()
  val upcomingList by viewModel.upcomingOccurrences.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()

  var ruleToDelete by remember { mutableStateOf<RecurringRule?>(null) }
  var selectedTab by remember { mutableStateOf(0) } // 0 = Due & Scheduled, 1 = Active Rules

  val currencySymbol = userProfile.currencySymbol

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

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("recurring_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
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
            text = "Recurring Payments",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Scheduled subscriptions, bills & commitments",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Button(
          onClick = onOpenAddRecurringRule,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("add_recurring_rule_button")
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Rule", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
      }
    }

    // 2. Tab Switcher (Scheduled Payments vs Rule Definitions)
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedTab = 0 }
          ) {
            val totalPending = overdueList.size + dueTodayList.size
            Row(
              modifier = Modifier.padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Scheduled",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (totalPending > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = CircleShape,
                  color = if (overdueList.isNotEmpty()) MinimalRose else MinimalEmerald
                ) {
                  Text(
                    text = "$totalPending",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedTab = 1 }
          ) {
            Text(
              text = "Active Rules (${recurringRules.size})",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
              ),
              color = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 10.dp),
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }

    if (selectedTab == 0) {
      // ---------------- SCHEDULED / DUE PAYMENTS TAB ----------------

      // Section A: Overdue Payments
      if (overdueList.isNotEmpty()) {
        item {
          Text(
            text = "Overdue Payments",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MinimalRose
          )
        }

        items(overdueList, key = { "${it.ruleId}_${it.scheduledDateKey}" }) { occ ->
          ScheduledOccurrenceCard(
            occurrence = occ,
            currencySymbol = currencySymbol,
            onMarkPaid = { viewModel.markRecurringOccurrenceAsPaid(occ) },
            onRemind = { viewModel.sendPaymentReminderNotification(occ) }
          )
        }
      }

      // Section B: Due Today
      item {
        Text(
          text = "Due Today",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (dueTodayList.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MinimalEmerald.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MinimalEmerald, modifier = Modifier.size(20.dp))
              }
              Column {
                Text(
                  text = "No payments due today",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "All scheduled obligations for today are completed.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      } else {
        items(dueTodayList, key = { "${it.ruleId}_${it.scheduledDateKey}" }) { occ ->
          ScheduledOccurrenceCard(
            occurrence = occ,
            currencySymbol = currencySymbol,
            onMarkPaid = { viewModel.markRecurringOccurrenceAsPaid(occ) },
            onRemind = { viewModel.sendPaymentReminderNotification(occ) }
          )
        }
      }

      // Section C: Upcoming Scheduled Payments
      if (upcomingList.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Upcoming Payments",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        items(upcomingList.take(10), key = { "${it.ruleId}_${it.scheduledDateKey}" }) { occ ->
          ScheduledOccurrenceCard(
            occurrence = occ,
            currencySymbol = currencySymbol,
            onMarkPaid = { viewModel.markRecurringOccurrenceAsPaid(occ) },
            onRemind = { viewModel.sendPaymentReminderNotification(occ) }
          )
        }
      }
    } else {
      // ---------------- ACTIVE RULES DEFINITIONS TAB ----------------
      if (recurringRules.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(MinimalIndigo.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Filled.Repeat, contentDescription = null, tint = MinimalIndigo, modifier = Modifier.size(32.dp))
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "No recurring rules yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Add rules for subscriptions, rent, bills or salaries.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        items(recurringRules, key = { it.id }) { rule ->
          RecurringRuleCard(
            rule = rule,
            currencySymbol = currencySymbol,
            onToggleActive = { isActive -> viewModel.toggleRecurringRule(rule.id, isActive) },
            onEdit = { onEditRecurringRule(rule) },
            onDelete = { ruleToDelete = rule }
          )
        }
      }
    }
  }
}

@Composable
fun ScheduledOccurrenceCard(
  occurrence: ScheduledRecurringOccurrence,
  currencySymbol: String,
  onMarkPaid: () -> Unit,
  onRemind: () -> Unit
) {
  val isOverdue = occurrence.status == OccurrenceStatus.OVERDUE
  val isDueToday = occurrence.status == OccurrenceStatus.DUE_TODAY

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      when {
        isOverdue -> MinimalRose.copy(alpha = 0.35f)
        isDueToday -> MinimalEmerald.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
      }
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(occurrence.category.color.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = occurrence.category.icon,
            contentDescription = null,
            tint = occurrence.category.color,
            modifier = Modifier.size(22.dp)
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = occurrence.ruleTitle,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = when {
                isOverdue -> MinimalRose.copy(alpha = 0.12f)
                isDueToday -> MinimalEmerald.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant
              }
            ) {
              Text(
                text = occurrence.relativeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = when {
                  isOverdue -> MinimalRose
                  isDueToday -> MinimalEmerald
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            Text(
              text = "• ${occurrence.interval.displayName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = IndianCurrencyFormatter.formatWithSymbol(occurrence.amount, currencySymbol),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = if (occurrence.type == TransactionType.EXPENSE) MinimalRose else MinimalEmerald
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = onRemind, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Notifications, contentDescription = "Remind", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          }

          Button(
            onClick = onMarkPaid,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MinimalEmerald),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mark Paid", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }
  }
}

@Composable
fun RecurringRuleCard(
  rule: RecurringRule,
  currencySymbol: String,
  onToggleActive: (Boolean) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val isExpense = rule.type == TransactionType.EXPENSE
  val amountColor = if (isExpense) MinimalRose else MinimalEmerald

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(rule.category.color.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = rule.category.icon,
            contentDescription = null,
            tint = rule.category.color,
            modifier = Modifier.size(20.dp)
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = rule.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (rule.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
          Text(
            text = "${rule.interval.displayName} • ${rule.category.displayName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = IndianCurrencyFormatter.formatWithSymbol(rule.amount, currencySymbol),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = if (rule.isActive) amountColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Switch(
            checked = rule.isActive,
            onCheckedChange = onToggleActive,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(width = 36.dp, height = 24.dp)
          )

          IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
          }

          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MinimalRose, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}
