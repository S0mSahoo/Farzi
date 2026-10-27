package com.example.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CalendarDayData
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumPageHeader
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun CalendarScreen(
  viewModel: FinanceViewModel,
  onAddTransactionForDate: (timestamp: Long) -> Unit,
  onEditTransaction: (item: TransactionItem) -> Unit
) {
  val selectedCalendar by viewModel.calendarMonth.collectAsStateWithLifecycle()
  val calendarDays by viewModel.calendarDaysData.collectAsStateWithLifecycle()
  val selectedDayMillis by viewModel.calendarSelectedDayMillis.collectAsStateWithLifecycle()
  val selectedDayTransactions by viewModel.calendarDateTransactions.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  var showDatePicker by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }
  val currencySymbol = userProfile.currencySymbol
  val selectedDayKey = DateUtils.getDayKey(selectedDayMillis)
  val dayIncome = selectedDayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
  val dayExpense = selectedDayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
  val dayNet = dayIncome - dayExpense
  val monthLabel = DateUtils.getMonthLabel(selectedCalendar)
  val rows = remember(calendarDays) { calendarDays.chunked(7) }

  if (showDatePicker) {
    AppDatePickerDialog(
      initialDateMillis = selectedDayMillis,
      onDateSelected = { viewModel.selectCalendarDate(it); showDatePicker = false },
      onDismiss = { showDatePicker = false }
    )
  }
  if (transactionToDelete != null) {
    ConfirmationDialog(
      title = "Delete transaction?",
      message = "Delete “${transactionToDelete?.title}”? This cannot be undone.",
      confirmButtonText = "Delete",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        transactionToDelete?.let { viewModel.deleteTransaction(it) }
        transactionToDelete = null
      },
      onDismiss = { transactionToDelete = null }
    )
  }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 1060.dp).fillMaxSize().testTag("calendar_screen"),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
      item {
        PremiumPageHeader(
          title = "Calendar",
          subtitle = "Your money, day by day.",
          trailing = {
            com.example.ui.components.PremiumMonthNavigator(
              label = monthLabel,
              onPrevious = { viewModel.previousCalendarMonth() },
              onNext = { viewModel.nextCalendarMonth() },
              onPick = { showDatePicker = true }
            )
          }
        )
      }

      item {
        PremiumCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                Text(
                  day,
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.Center,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (index >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
            rows.forEach { week ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                week.forEach { day ->
                  val selected = day.dateKey == selectedDayKey
                  val fill = when {
                    selected -> MaterialTheme.colorScheme.primary
                    day.isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                  }
                  val content = when {
                    selected -> Color.White
                    day.isToday -> MaterialTheme.colorScheme.primary
                    day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                  }
                  Surface(
                    onClick = { viewModel.selectCalendarDate(day.epochMillis) },
                    modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = fill,
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (day.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                    )
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                      Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selected || day.isToday) FontWeight.Bold else FontWeight.Medium,
                        color = content
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (day.hasIncome) Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color.White else IncomeGreen))
                        if (day.hasExpense) Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color(0xFFF87171) else ExpenseRed))
                      }
                    }
                  }
                }
                repeat((7 - week.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(IncomeGreen))
                Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ExpenseRed))
                Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      item {
        val isLight = !androidx.compose.foundation.isSystemInDarkTheme()
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
          Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(DateUtils.getDisplayDate(selectedDayMillis), style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
                Text(
                  if (selectedDayTransactions.isEmpty()) "No recorded activity" else "${selectedDayTransactions.size} ${if (selectedDayTransactions.size == 1) "transaction" else "transactions"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = subTextColor
                )
              }
              IconButton(onClick = { onAddTransactionForDate(selectedDayMillis) }) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction for selected date", tint = textColor)
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Money in", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                Text(IndianCurrencyFormatter.formatCompact(dayIncome, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreen, maxLines = 1, softWrap = false)
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Money out", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                Text(IndianCurrencyFormatter.formatCompact(dayExpense, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed, maxLines = 1, softWrap = false)
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Net", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                Text(IndianCurrencyFormatter.formatCompact(dayNet, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, softWrap = false)
              }
            }
          }
        }
      }

      item {
        com.example.ui.components.PremiumSectionHeading(
          title = "Day activity",
          subtitle = if (selectedDayTransactions.isEmpty()) "Nothing recorded on this date." else "Select an entry to see or edit its details."
        )
      }

      if (selectedDayTransactions.isEmpty()) {
        item {
          PremiumCard {
            com.example.ui.components.PremiumEmptyState(
              title = "A quiet day",
              body = "Nothing has been recorded for this date yet.",
              action = {
                androidx.compose.material3.OutlinedButton(onClick = { onAddTransactionForDate(selectedDayMillis) }) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(Modifier.width(7.dp))
                  Text("Add to this day")
                }
              }
            )
          }
        }
      } else {
        item {
          PremiumCard {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)) {
              selectedDayTransactions.sortedByDescending { it.timestamp }.forEachIndexed { index, transaction ->
                com.example.ui.components.PremiumTransactionRow(
                  transaction = transaction,
                  currencySymbol = currencySymbol,
                  onClick = { onEditTransaction(transaction) },
                  trailing = {
                    IconButton(onClick = { transactionToDelete = transaction }, modifier = Modifier.size(36.dp)) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete ${transaction.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                  }
                )
                if (index != selectedDayTransactions.lastIndex) {
                  HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                }
              }
            }
          }
        }
      }
    }
  }
}
