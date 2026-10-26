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
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.ui.input.pointer.pointerInput
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

  val adaptiveInfo = LocalAdaptiveInfo.current

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

  val horizontalPadding = if (adaptiveInfo.isLandscapePhone) 12.dp else 20.dp
  val verticalSpacing = if (adaptiveInfo.isLandscapePhone) 10.dp else 16.dp

  if (adaptiveInfo.useTwoPaneLayout) {
    // 2-Pane Adaptive Master-Detail for Tablets and Wide Windows
    Row(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = horizontalPadding, vertical = 12.dp)
        .testTag("calendar_screen"),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Left Pane: Calendar Month Nav & Grid
      Column(
        modifier = Modifier
          .weight(1.15f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        CalendarMonthHeader(
          selectedCalendar = selectedCalendar,
          onPrevious = { viewModel.previousCalendarMonth() },
          onNext = { viewModel.nextCalendarMonth() },
          onOpenDatePicker = { showDatePicker = true }
        )

        CalendarGridCard(
          selectedCalendar = selectedCalendar,
          calendarDays = calendarDays,
          selectedDayKey = selectedDayKey,
          onDaySelected = { viewModel.selectCalendarDate(it.epochMillis) },
          onPreviousMonth = { viewModel.previousCalendarMonth() },
          onNextMonth = { viewModel.nextCalendarMonth() }
        )
      }

      // Right Pane: Selected Day Details & Transactions
      Column(
        modifier = Modifier
          .weight(0.85f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        SelectedDaySummaryCard(
          selectedDayMillis = selectedDayMillis,
          transactionCount = selectedDayTransactions.size,
          dayIncome = dayIncome,
          dayExpense = dayExpense,
          dayNet = dayNet,
          currencySymbol = currencySymbol,
          onAddTransaction = { onAddTransactionForDate(selectedDayMillis) }
        )

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
        ) {
          if (selectedDayTransactions.isEmpty()) {
            item {
              DayTransactionsEmptyState()
            }
          } else {
            items(selectedDayTransactions, key = { it.id }) { item ->
              DayTransactionItem(
                item = item,
                currencySymbol = currencySymbol,
                onEdit = { onEditTransaction(item) },
                onDelete = { transactionToDelete = item }
              )
            }
          }
        }
      }
    }
  } else {
    // Single-Pane Column Layout for Phone Portrait & Landscape
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("calendar_screen"),
      contentPadding = PaddingValues(
        start = horizontalPadding,
        end = horizontalPadding,
        top = if (adaptiveInfo.isLandscapePhone) 8.dp else 16.dp,
        bottom = if (adaptiveInfo.isLandscapePhone) 32.dp else 100.dp
      ),
      verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
      // 1. Month Navigation Header
      item {
        CalendarMonthHeader(
          selectedCalendar = selectedCalendar,
          onPrevious = { viewModel.previousCalendarMonth() },
          onNext = { viewModel.nextCalendarMonth() },
          onOpenDatePicker = { showDatePicker = true }
        )
      }

      // 2. Calendar Grid Card
      item {
        CalendarGridCard(
          selectedCalendar = selectedCalendar,
          calendarDays = calendarDays,
          selectedDayKey = selectedDayKey,
          onDaySelected = { viewModel.selectCalendarDate(it.epochMillis) },
          onPreviousMonth = { viewModel.previousCalendarMonth() },
          onNextMonth = { viewModel.nextCalendarMonth() }
        )
      }

      // 3. Selected Day Summary Card
      item {
        SelectedDaySummaryCard(
          selectedDayMillis = selectedDayMillis,
          transactionCount = selectedDayTransactions.size,
          dayIncome = dayIncome,
          dayExpense = dayExpense,
          dayNet = dayNet,
          currencySymbol = currencySymbol,
          onAddTransaction = { onAddTransactionForDate(selectedDayMillis) }
        )
      }

      // 4. List of Transactions or Empty State
      if (selectedDayTransactions.isEmpty()) {
        item {
          DayTransactionsEmptyState()
        }
      } else {
        items(selectedDayTransactions, key = { it.id }) { item ->
          DayTransactionItem(
            item = item,
            currencySymbol = currencySymbol,
            onEdit = { onEditTransaction(item) },
            onDelete = { transactionToDelete = item }
          )
        }
      }
    }
  }
}

@Composable
private fun CalendarMonthHeader(
  selectedCalendar: Calendar,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onOpenDatePicker: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
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
        onClick = onPrevious,
        modifier = Modifier.testTag("cal_prev_month_btn")
      ) {
        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable { onOpenDatePicker() }
          .testTag("cal_jump_date_chip")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
          Text(
            text = DateUtils.getMonthLabel(selectedCalendar),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Icon(Icons.Default.ChevronRight, contentDescription = "Pick Date", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        }
      }

      IconButton(
        onClick = onNext,
        modifier = Modifier.testTag("cal_next_month_btn")
      ) {
        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
      }
    }
  }
}

@Composable
private fun CalendarGridCard(
  selectedCalendar: Calendar,
  calendarDays: List<CalendarDayData>,
  selectedDayKey: String,
  onDaySelected: (CalendarDayData) -> Unit,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit
) {
  val year = selectedCalendar.get(Calendar.YEAR)
  val month = selectedCalendar.get(Calendar.MONTH)
  val monthIndex = year * 12 + month

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .pointerInput(monthIndex) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
          onDragStart = { totalDrag = 0f },
          onDragEnd = {
            if (totalDrag < -50f) {
              onNextMonth()
            } else if (totalDrag > 50f) {
              onPreviousMonth()
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
      // Weekdays Header Row
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
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Animated Month Grid
      AnimatedContent(
        targetState = monthIndex,
        transitionSpec = {
          if (targetState > initialState) {
            slideInHorizontally { width -> width / 3 } togetherWith slideOutHorizontally { width -> -width / 3 }
          } else {
            slideInHorizontally { width -> -width / 3 } togetherWith slideOutHorizontally { width -> width / 3 }
          }
        },
        label = "month_grid_animation"
      ) { _ ->
        val weeks = calendarDays.chunked(7)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          weeks.forEach { weekDays ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              weekDays.forEach { dayData ->
                val isSelected = dayData.dateKey == selectedDayKey

                Box(
                  modifier = Modifier
                    .size(width = 44.dp, height = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                      if (isSelected) {
                        Modifier
                          .background(MaterialTheme.colorScheme.primaryContainer)
                          .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                      } else if (dayData.isToday) {
                        Modifier
                          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                      } else {
                        Modifier
                      }
                    )
                    .clickable { onDaySelected(dayData) }
                    .padding(2.dp),
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
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        dayData.isToday -> MaterialTheme.colorScheme.primary
                        dayData.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                      }
                    )

                    // Dots indicator
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(3.dp),
                      modifier = Modifier.height(4.dp)
                    ) {
                      if (dayData.hasIncome) {
                        Box(
                          modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen)
                        )
                      }
                      if (dayData.hasExpense) {
                        Box(
                          modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed)
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

@Composable
private fun SelectedDaySummaryCard(
  selectedDayMillis: Long,
  transactionCount: Int,
  dayIncome: Double,
  dayExpense: Double,
  dayNet: Double,
  currencySymbol: String,
  onAddTransaction: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = DateUtils.getDayOfWeekLabel(selectedDayMillis),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$transactionCount transactions logged",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedButton(
          onClick = onAddTransaction,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("add_to_selected_date_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Income
        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(IncomeGreen.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
          Text("Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen, maxLines = 1, fontWeight = FontWeight.Medium)
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
            .background(ExpenseRed.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
          Text("Expense", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, maxLines = 1, fontWeight = FontWeight.Medium)
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
          Text("Net", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontWeight = FontWeight.Medium)
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

@Composable
private fun DayTransactionsEmptyState() {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        Icons.Default.EventNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(36.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "No transactions recorded on this day",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun DayTransactionItem(
  item: TransactionItem,
  currencySymbol: String,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onEdit() }
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
          color = item.category.color.copy(alpha = 0.12f),
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}
