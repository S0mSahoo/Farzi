package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarDayData
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun CalendarScreen(
  viewModel: FinanceViewModel,
  onAddTransactionForDate: (timestamp: Long) -> Unit,
  onEditTransaction: (item: TransactionItem) -> Unit
) {
  val selectedCalendar by viewModel.calendarMonth.collectAsState()
  val calendarDays by viewModel.calendarDaysData.collectAsState()
  val selectedDayMillis by viewModel.calendarSelectedDayMillis.collectAsState()
  val selectedDayTransactions by viewModel.calendarDateTransactions.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()

  var showDatePicker by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }

  val currencySymbol = userProfile.currencySymbol
  val selectedDayKey = DateUtils.getDayKey(selectedDayMillis)

  val dayIncome = selectedDayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
  val dayExpense = selectedDayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
  val dayNet = dayIncome - dayExpense

  if (showDatePicker) {
    AppDatePickerDialog(
      initialDateMillis = selectedDayMillis,
      onDateSelected = { selectedMillis ->
        viewModel.selectCalendarDate(selectedMillis)
      },
      onDismiss = { showDatePicker = false }
    )
  }

  if (transactionToDelete != null) {
    ConfirmationDialog(
      title = "Delete Transaction?",
      message = "Are you sure you want to delete \"${transactionToDelete?.title}\"? This action cannot be undone.",
      onConfirm = {
        transactionToDelete?.let { viewModel.deleteTransaction(it) }
        transactionToDelete = null
      },
      onDismiss = { transactionToDelete = null }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("calendar_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // 1. Month Navigation Header with Date Jump
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { viewModel.previousCalendarMonth() },
            modifier = Modifier.testTag("cal_prev_month_btn")
          ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { showDatePicker = true }
              .testTag("cal_jump_date_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
              Text(
                text = DateUtils.getMonthLabel(selectedCalendar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Icon(Icons.Default.Edit, contentDescription = "Pick Date", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
          }

          IconButton(
            onClick = { viewModel.nextCalendarMonth() },
            modifier = Modifier.testTag("cal_next_month_btn")
          ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
          }
        }
      }
    }

    // 2. Calendar Grid Card with Horizontal Swipe Navigation
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
          .fillMaxWidth()
          .pointerInput(selectedCalendar.get(java.util.Calendar.YEAR), selectedCalendar.get(java.util.Calendar.MONTH)) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
              onDragStart = { totalDrag = 0f },
              onDragEnd = {
                if (totalDrag < -60f) {
                  viewModel.nextCalendarMonth()
                } else if (totalDrag > 60f) {
                  viewModel.previousCalendarMonth()
                }
              },
              onDragCancel = { totalDrag = 0f },
              onHorizontalDrag = { change, dragAmount ->
                change.consume()
                totalDrag += dragAmount
              }
            )
          }
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Weekdays header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
              Text(
                text = dayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(10.dp))

          // Animated Days Grid
          AnimatedContent(
            targetState = "${selectedCalendar.get(java.util.Calendar.YEAR)}_${selectedCalendar.get(java.util.Calendar.MONTH)}",
            transitionSpec = {
              slideInHorizontally { width -> width / 3 } togetherWith slideOutHorizontally { width -> -width / 3 }
            },
            label = "calendar_month_grid"
          ) { _ ->
            Column {
              val rows = calendarDays.chunked(7)
              rows.forEach { week ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  horizontalArrangement = Arrangement.SpaceAround
                ) {
                  week.forEach { dayData ->
                    val isSelected = dayData.dateKey == selectedDayKey

                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                          when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            dayData.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> Color.Transparent
                          }
                        )
                        .border(
                          width = if (dayData.isToday && !isSelected) 1.dp else 0.dp,
                          color = if (dayData.isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                          shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectCalendarDate(dayData.epochMillis) }
                        .padding(2.dp)
                        .testTag("cal_day_${dayData.dateKey}"),
                      contentAlignment = Alignment.Center
                    ) {
                      Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                      ) {
                        Text(
                          text = "${dayData.dayOfMonth}",
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = if (isSelected || dayData.isToday) FontWeight.Bold else FontWeight.Normal,
                          color = when {
                            isSelected -> Color.White
                            !dayData.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            else -> MaterialTheme.colorScheme.onSurface
                          }
                        )

                        // Dots for Income / Expense activity
                        if (dayData.hasIncome || dayData.hasExpense) {
                          Spacer(modifier = Modifier.height(2.dp))
                          Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (dayData.hasIncome) {
                              Box(
                                modifier = Modifier
                                  .size(4.dp)
                                  .clip(CircleShape)
                                  .background(if (isSelected) Color.White else IncomeGreen)
                              )
                            }
                            if (dayData.hasExpense) {
                              Box(
                                modifier = Modifier
                                  .size(4.dp)
                                  .clip(CircleShape)
                                  .background(if (isSelected) Color.White else ExpenseRed)
                              )
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
        }
      }
    }

    // 3. Selected Date Details Header & Day Summary
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = DateUtils.getDayOfWeekLabel(selectedDayMillis),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${selectedDayTransactions.size} transactions logged",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            OutlinedButton(
              onClick = { onAddTransactionForDate(selectedDayMillis) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("add_to_selected_date_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add")
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Day Totals Row (Income, Expense, Net)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Income
            Column(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(IncomeGreen.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
              Text("Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen, maxLines = 1)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(dayIncome, currencySymbol),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen,
                maxLines = 1,
                softWrap = false
              )
            }

            // Expense
            Column(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(ExpenseRed.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
              Text("Expense", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, maxLines = 1)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(dayExpense, currencySymbol),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed,
                maxLines = 1,
                softWrap = false
              )
            }

            // Net
            Column(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
              Text("Net", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(dayNet, currencySymbol, includeSign = true),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (dayNet >= 0) IncomeGreen else ExpenseRed,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }
    }

    // 4. List of Transactions on Selected Day / Empty State
    if (selectedDayTransactions.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No transactions recorded on this day",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(selectedDayTransactions, key = { it.id }) { item ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEditTransaction(item) }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = item.category.color.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(item.category.icon, contentDescription = null, tint = item.category.color, modifier = Modifier.size(20.dp))
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${item.category.displayName} • ${item.paymentMethod.displayName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(
                  amount = item.amount,
                  symbol = currencySymbol,
                  includeSign = true
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                maxLines = 1,
                softWrap = false
              )

              IconButton(
                onClick = { transactionToDelete = item },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }
}
