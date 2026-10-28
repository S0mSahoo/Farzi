package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OccurrenceStatus
import com.example.data.model.RecurringOccurrence
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumIconButton
import com.example.ui.components.PremiumOutlinedButton
import com.example.ui.components.PremiumPageHeader
import com.example.ui.components.PremiumTonalButton
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun RecurringScreen(
  viewModel: FinanceViewModel,
  onOpenAddRecurringRule: () -> Unit,
  onEditRecurringRule: (rule: RecurringRule) -> Unit
) {
  val recurringRules by viewModel.allRecurringRules.collectAsStateWithLifecycle()
  val occurrences by viewModel.allScheduledOccurrences.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  var selectedTab by remember { mutableStateOf(0) }
  var ruleToDelete by remember { mutableStateOf<RecurringRule?>(null) }
  var occurrenceToCancel by remember { mutableStateOf<RecurringOccurrence?>(null) }
  val currencySymbol = userProfile.currencySymbol

  if (ruleToDelete != null) {
    ConfirmationDialog(
      title = "Delete recurring rule?",
      message = "Delete “${ruleToDelete?.title}”? Transactions already recorded from this rule will remain.",
      confirmButtonText = "Delete rule",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        ruleToDelete?.let { viewModel.deleteRecurringRule(it.id) }
        ruleToDelete = null
      },
      onDismiss = { ruleToDelete = null }
    )
  }
  if (occurrenceToCancel != null) {
    ConfirmationDialog(
      title = "Skip this payment?",
      message = "Skip only the ${occurrenceToCancel?.occurrenceDateKey} occurrence. The recurring rule will remain active for future payments.",
      confirmButtonText = "Skip payment",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        occurrenceToCancel?.let { viewModel.cancelRecurringOccurrence(it) }
        occurrenceToCancel = null
      },
      onDismiss = { occurrenceToCancel = null }
    )
  }

  val outstanding = remember(occurrences) {
    occurrences.filter {
      it.status == OccurrenceStatus.SCHEDULED ||
        it.status == OccurrenceStatus.DUE_TODAY ||
        it.status == OccurrenceStatus.OVERDUE
    }.sortedBy { it.occurrenceTimestamp }
  }
  val unpaidBills = remember(outstanding) {
    outstanding.filter { it.rule.type == TransactionType.EXPENSE }
  }
  val outstandingTotal = unpaidBills.sumOf { it.rule.amount }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize().testTag("recurring_screen"),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
      item {
        PremiumPageHeader(
          title = "Recurring",
          subtitle = "Bills, subscriptions and regular payments.",
          trailing = {
            PremiumTonalButton(onClick = onOpenAddRecurringRule) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(6.dp))
              Text("Add rule")
            }
          }
        )
      }

      item {
        val isLight = !com.example.ui.theme.isAppInDarkTheme()
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = Color.Transparent,
          border = androidx.compose.foundation.BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.28f)),
          modifier = Modifier
            .fillMaxWidth()
            .background(
              brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                 colors = if (isLight) {
                  listOf(Color(0xFFF3E8FF), Color(0xFFEDE9FE))
                } else {
                  listOf(Color(0xFF6D28D9), Color(0xFF4C1D95))
                }
              ),
              shape = RoundedCornerShape(28.dp)
            )
        ) {
          val textColor = if (isLight) Color(0xFF2E1065) else Color.White
          val subTextColor = if (isLight) Color(0xFF4C1D95).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.72f)
          Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
              Text("BILLS STILL TO PAY", style = MaterialTheme.typography.labelMedium, color = subTextColor, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
              Text(IndianCurrencyFormatter.formatWithSymbol(outstandingTotal, currencySymbol), style = MaterialTheme.typography.headlineSmall, color = textColor, fontWeight = FontWeight.Bold)
              Text("${unpaidBills.size} bills · ${outstanding.size} scheduled items", style = MaterialTheme.typography.bodySmall, color = subTextColor)
            }
            Surface(shape = RoundedCornerShape(17.dp), color = if (isLight) Color(0xFFE9D5FF) else Color.White.copy(alpha = 0.15f)) {
              Icon(Icons.Default.Repeat, contentDescription = null, tint = textColor, modifier = Modifier.padding(13.dp).size(24.dp))
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.selectableGroup(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text("Upcoming bills") })
          FilterChip(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text("Rules · ${recurringRules.size}") })
        }
      }

      item {
        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
          label = "recurring_tab_transition"
        ) { tab ->
          if (tab == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              com.example.ui.components.PremiumSectionHeading(
                title = "Payment schedule",
                subtitle = "Mark payments once they are completed."
              )
              if (outstanding.isEmpty()) {
                PremiumCard {
                  PremiumEmptyState(
                    title = "You're all caught up",
                    body = "No active scheduled or overdue payments are waiting for you.",
                    action = {
                      PremiumOutlinedButton(onClick = onOpenAddRecurringRule) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add a recurring rule")
                      }
                    }
                  )
                }
              } else {
                val grouped = outstanding.groupBy { it.occurrenceDateKey }
                grouped.forEach { (dateKey, list) ->
                  Text(
                    dateKey,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                  )
                  list.forEach { occurrence ->
                    OccurrencePremiumCard(
                      occurrence = occurrence,
                      currencySymbol = currencySymbol,
                      onMarkPaid = { viewModel.markRecurringPaymentPaid(occurrence) },
                      onSkip = { occurrenceToCancel = occurrence }
                    )
                  }
                }
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              com.example.ui.components.PremiumSectionHeading(
                title = "Your recurring rules",
                subtitle = "Pause or adjust a rule any time."
              )
              if (recurringRules.isEmpty()) {
                PremiumCard {
                  PremiumEmptyState(
                    title = "No rules yet",
                    body = "Add rent, subscriptions, utilities or regular income to make future payments easier to track.",
                    action = {
                      PremiumButton(onClick = onOpenAddRecurringRule) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create first rule")
                      }
                    }
                  )
                }
              } else {
                recurringRules.sortedBy { !it.isActive }.forEach { rule ->
                  RecurringRulePremiumCard(
                    rule = rule,
                    currencySymbol = currencySymbol,
                    onEdit = { onEditRecurringRule(rule) },
                    onDelete = { ruleToDelete = rule },
                    onToggle = { active -> viewModel.toggleRecurringRule(rule.id, active) }
                  )
                }
              }
            }
          }
        }
      }
      item { Spacer(Modifier.height(10.dp)) }
    }
  }
}

@Composable
private fun OccurrencePremiumCard(
  occurrence: RecurringOccurrence,
  currencySymbol: String,
  onMarkPaid: () -> Unit,
  onSkip: () -> Unit
) {
  val isOverdue = occurrence.status == OccurrenceStatus.OVERDUE
  val isDue = occurrence.status == OccurrenceStatus.DUE_TODAY
  val statusColor = when {
    isOverdue -> ExpenseRed
    isDue -> Color(0xFFB87918)
    else -> MaterialTheme.colorScheme.primary
  }
  PremiumCard {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Surface(shape = RoundedCornerShape(13.dp), color = occurrence.rule.category.color.copy(alpha = 0.13f)) {
          Icon(occurrence.rule.category.icon, contentDescription = null, tint = occurrence.rule.category.color, modifier = Modifier.padding(10.dp).size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(occurrence.rule.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("${occurrence.rule.category.displayName} · ${occurrence.rule.interval.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(IndianCurrencyFormatter.formatWithSymbol(occurrence.rule.amount, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.12f)) {
            Text(occurrence.status.displayName, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
          }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PremiumButton(
          onClick = onMarkPaid,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen, contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(if (occurrence.rule.type == TransactionType.INCOME) "Mark received" else "Mark paid")
        }
        PremiumOutlinedButton(
          onClick = onSkip,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
        ) {
          Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Skip")
        }
      }
    }
  }
}

@Composable
private fun RecurringRulePremiumCard(
  rule: RecurringRule,
  currencySymbol: String,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onToggle: (Boolean) -> Unit
) {
  PremiumCard {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Surface(shape = RoundedCornerShape(13.dp), color = rule.category.color.copy(alpha = 0.13f)) {
          Icon(rule.category.icon, contentDescription = null, tint = rule.category.color, modifier = Modifier.padding(10.dp).size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(rule.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("${rule.category.displayName} · ${rule.interval.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(IndianCurrencyFormatter.formatWithSymbol(rule.amount, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          Text(if (rule.type == TransactionType.INCOME) "Income" else "Expense", style = MaterialTheme.typography.labelSmall, color = if (rule.type == TransactionType.INCOME) IncomeGreen else ExpenseRed)
        }
      }
      androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Switch(
            checked = rule.isActive,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
          )
          Text(if (rule.isActive) "Active" else "Paused", style = MaterialTheme.typography.labelMedium, color = if (rule.isActive) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          PremiumIconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit ${rule.title}") }
          PremiumIconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${rule.title}", tint = ExpenseRed) }
        }
      }
    }
  }
}
