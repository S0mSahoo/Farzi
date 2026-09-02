package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun CalendarScreen(
  viewModel: FinanceViewModel,
  onAddTransactionForDate: (timestamp: Long) -> Unit,
  onEditTransaction: (item: TransactionItem) -> Unit
) {
  val selectedCalendar by viewModel.selectedCalendar.collectAsState()
  val calendarDays by viewModel.calendarDays.collectAsState()
  val selectedDayMillis by viewModel.calendarSelectedDayMillis.collectAsState()
  val selectedDayTransactions by viewModel.selectedDayTransactions.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()

  var showMonthPicker by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }
  var swipeDirection by remember { mutableIntStateOf(1) } // 1 for next (leftwards drag), -1 for prev

  val currencySymbol = userProfile.currencySymbol
  val selectedDayKey = DateUtils.getDayKey(selectedDayMillis)

  val dayIncome = selectedDayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
  val dayExpense = selectedDayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
  val dayNet = dayIncome - dayExpense

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentCalendar = selectedCalendar,
      onDismiss = { showMonthPicker = false },
      onMonthYearSelected = { year, month ->
        viewModel.setSelectedMonth(year, month)
      }
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

  // Horizontal Swipe Drag State
  var totalDragX by remember { mutableFloatStateOf(0f) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("calendar_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Top Bar with Month Navigation
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Calendar",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Swipe horizontally to navigate months",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedButton(
          onClick = {
            val today = Calendar.getInstance()
            viewModel.setSelectedMonth(today.get(Calendar.YEAR), today.get(Calendar.MONTH))
            viewModel.setCalendarSelectedDay(today.timeInMillis)
          },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Today", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
      }
    }

    // Calendar Card Container (With Horizontal Swipe Gesture & Month Slider)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .pointerInput(selectedCalendar.timeInMillis) {
            detectHorizontalDragGestures(
              onDragStart = { totalDragX = 0f },
              onDragEnd = {
                if (totalDragX < -60f) {
                  // Swiped Left -> Move to Next Month
                  swipeDirection = 1
                  viewModel.nextMonth()
                } else if (totalDragX > 60f) {
                  // Swiped Right -> Move to Previous Month
                  swipeDirection = -1
                  viewModel.previousMonth()
                }
                totalDragX = 0f
              },
              onHorizontalDrag = { _, dragAmount ->
                totalDragX += dragAmount
              }
            )
          },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Month Header Row with Navigation Arrows
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                swipeDirection = -1
                viewModel.previousMonth()
              }
            ) {
              Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.onSurface)
            }

            Text(
              text = DateUtils.getMonthLabel(selectedCalendar),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showMonthPicker = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            IconButton(
              onClick = {
                swipeDirection = 1
                viewModel.nextMonth()
              }
            ) {
              Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Day of Week Header Row (Sun, Mon, Tue, Wed, Thu, Fri, Sat)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            daysOfWeek.forEach { dayName ->
              Text(
                text = dayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(8.dp))

          // Month Days Grid with Smooth Slide Transition
          AnimatedContent(
            targetState = calendarDays,
            transitionSpec = {
              if (swipeDirection > 0) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> -width } + fadeOut()
                )
              } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> width } + fadeOut()
                )
              }
            },
            label = "calendar_days_transition"
          ) { daysGrid ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              daysGrid.chunked(7).forEach { week ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceAround
                ) {
                  week.forEach { dayData ->
                    CalendarDayCell(
                      dayData = dayData,
                      isSelected = dayData.dateKey == selectedDayKey,
                      onClick = {
                        viewModel.setCalendarSelectedDay(dayData.epochMillis)
                      }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Selected Day Summary Card & Transaction List
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = DateUtils.getDayOfWeekLabel(selectedDayMillis),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${selectedDayTransactions.size} transaction${if (selectedDayTransactions.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Button(
              onClick = { onAddTransactionForDate(selectedDayMillis) },
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.height(36.dp)
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
          }

          if (selectedDayTransactions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Day Net: ${if (dayNet >= 0) "+" else ""}${IndianCurrencyFormatter.formatWithSymbol(dayNet, currencySymbol)}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (dayNet >= 0) IncomeGreen else ExpenseRed
                  )
                )

                Text(
                  text = "Spent: ${IndianCurrencyFormatter.formatWithSymbol(dayExpense, currencySymbol)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    // List of Transactions on Selected Day
    if (selectedDayTransactions.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Filled.EventNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No records for this date",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(selectedDayTransactions, key = { it.id }) { item ->
        CalendarTransactionCard(
          item = item,
          currencySymbol = currencySymbol,
          onEdit = { onEditTransaction(item) },
          onDelete = { transactionToDelete = item }
        )
      }
    }
  }
}

@Composable
fun CalendarDayCell(
  dayData: CalendarDayData,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val isCurrentMonth = dayData.isCurrentMonth
  val isToday = dayData.isToday

  val cellBackground = when {
    isSelected -> MaterialTheme.colorScheme.primary
    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else -> Color.Transparent
  }

  val textColor = when {
    isSelected -> Color.White
    !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    isToday -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(40.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(enabled = isCurrentMonth) { onClick() }
      .background(cellBackground)
      .padding(vertical = 6.dp)
  ) {
    Text(
      text = "${dayData.dayOfMonth}",
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
      ),
      color = textColor,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Dots for Income / Expense
    Row(
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.height(6.dp)
    ) {
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

@Composable
fun CalendarTransactionCard(
  item: TransactionItem,
  currencySymbol: String,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val isExpense = item.type == TransactionType.EXPENSE
  val amountColor = if (isExpense) ExpenseRed else IncomeGreen
  val prefix = if (isExpense) "- " else "+ "

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onEdit() }
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
            .background(item.category.color.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = item.category.icon,
            contentDescription = null,
            tint = item.category.color,
            modifier = Modifier.size(20.dp)
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${item.category.displayName} • ${item.paymentMethod.displayName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "$prefix${IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol)}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = amountColor
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}
