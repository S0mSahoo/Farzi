package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumMonthNavigator
import com.example.ui.components.PremiumSectionHeading
import com.example.ui.components.PremiumTransactionRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TransactionsScreen(
  viewModel: FinanceViewModel,
  onEditTransaction: (item: TransactionItem) -> Unit
) {
  val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val filterType by viewModel.filterType.collectAsStateWithLifecycle()
  val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val selectedCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()

  var selectedDayOfMonth by remember { mutableStateOf<Int?>(null) } // null = All Month
  var showMonthPicker by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }
  var showCategoryFilters by remember { mutableStateOf(false) }

  val currencySymbol = userProfile.currencySymbol
  val startOfMonth = remember(selectedCalendar) { DateUtils.getStartOfMonth(selectedCalendar) }
  val endOfMonth = remember(selectedCalendar) { DateUtils.getEndOfMonth(selectedCalendar) }
  val monthLabel = remember(selectedCalendar) { DateUtils.getMonthLabel(selectedCalendar) }

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

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentCalendar = selectedCalendar,
      onDismiss = { showMonthPicker = false },
      onMonthYearSelected = { year, month ->
        viewModel.setDashboardMonthAndYear(year, month)
        selectedDayOfMonth = null
        showMonthPicker = false
      }
    )
  }

  // Filter transactions by selected month & selected day (if day selected)
  val monthTransactions = remember(transactions, startOfMonth, endOfMonth) {
    transactions.filter { it.timestamp in startOfMonth..endOfMonth }
  }

  val filteredByDate = remember(monthTransactions, selectedDayOfMonth, selectedCalendar) {
    if (selectedDayOfMonth == null) {
      monthTransactions
    } else {
      val dayCal = (selectedCalendar.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, selectedDayOfMonth!!)
      }
      val startOfDay = DateUtils.getStartOfDay(dayCal.timeInMillis)
      val endOfDay = DateUtils.getEndOfDay(dayCal.timeInMillis)
      monthTransactions.filter { it.timestamp in startOfDay..endOfDay }
    }
  }

  val sorted = remember(filteredByDate) { filteredByDate.sortedByDescending { it.timestamp } }
  val grouped = remember(sorted) {
    val todayKey = DateUtils.getDayKey(Calendar.getInstance())
    val yesterdayKey = DateUtils.getDayKey(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) })
    sorted.groupBy { tx ->
      when (DateUtils.getDayKey(tx.timestamp)) {
        todayKey -> "Today"
        yesterdayKey -> "Yesterday"
        else -> DateUtils.getDisplayDate(tx.timestamp)
      }
    }
  }

  val totalIncomeInView = remember(sorted) { sorted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
  val totalExpenseInView = remember(sorted) { sorted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }
  val netInView = totalIncomeInView - totalExpenseInView

  // Generate days for selected month
  val daysInMonth = remember(selectedCalendar) {
    val cal = selectedCalendar.clone() as Calendar
    cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 940.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        PremiumSectionHeading(
          title = "Transactions",
          subtitle = "Search, filter & track date by date."
        )
      }

      // Unified Month Navigator & Date Strip
      item {
        PremiumCard {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = "Select Month & Date",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.fillMaxWidth(),
              textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              PremiumMonthNavigator(
                label = monthLabel,
                onPrevious = {
                  viewModel.previousDashboardMonth()
                  selectedDayOfMonth = null
                },
                onNext = {
                  viewModel.nextDashboardMonth()
                  selectedDayOfMonth = null
                },
                onPick = { showMonthPicker = true }
              )
            }
            // Scrollable Day Pills
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val isDark = com.example.ui.theme.isAppInDarkTheme()

              // "All Month" Pill
              Surface(
                onClick = { selectedDayOfMonth = null },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedDayOfMonth == null) MaterialTheme.colorScheme.primary else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (selectedDayOfMonth == null) MaterialTheme.colorScheme.primary else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else Color(0xFFDDD6FE)
                )
              ) {
                Text(
                  text = "All Month",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (selectedDayOfMonth == null) Color.White else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
              }

              // Days of Month
              val dayCal = selectedCalendar.clone() as Calendar
              val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
              (1..daysInMonth).forEach { day ->
                dayCal.set(Calendar.DAY_OF_MONTH, day)
                val dayOfWeek = dayFormat.format(dayCal.time)
                val isSelected = selectedDayOfMonth == day

                val dayStart = DateUtils.getStartOfDay(dayCal.timeInMillis)
                val dayEnd = DateUtils.getEndOfDay(dayCal.timeInMillis)
                val hasTxOnDay = monthTransactions.any { it.timestamp in dayStart..dayEnd }

                Surface(
                  onClick = { selectedDayOfMonth = if (selectedDayOfMonth == day) null else day },
                  shape = RoundedCornerShape(16.dp),
                  color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    hasTxOnDay -> if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color(0xFFDDD6FE)
                    else -> if (isDark) Color(0xFF1A162B) else Color.White.copy(alpha = 0.75f)
                  },
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else Color(0xFFDDD6FE)
                  )
                ) {
                  Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Text(
                      text = day.toString(),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = when {
                        isSelected -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                      }
                    )
                    Text(
                      text = dayOfWeek,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = when {
                        isSelected -> Color.White.copy(alpha = 0.85f)
                        hasTxOnDay -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      }
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Financial Summary Header Card for active view
      item {
        PremiumCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (selectedDayOfMonth != null) "SUMMARY FOR DAY $selectedDayOfMonth" else "NET CASH FLOW IN VIEW",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              )
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "${sorted.size} ${if (sorted.size == 1) "record" else "records"}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Text(
              IndianCurrencyFormatter.formatWithSymbol(netInView, currencySymbol, includeSign = true),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = CircleShape, color = IncomeGreen.copy(alpha = 0.18f), modifier = Modifier.size(10.dp)) {}
                Text("Income: ${IndianCurrencyFormatter.formatCompact(totalIncomeInView, currencySymbol)}", style = MaterialTheme.typography.labelMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = CircleShape, color = ExpenseRed.copy(alpha = 0.18f), modifier = Modifier.size(10.dp)) {}
                Text("Spent: ${IndianCurrencyFormatter.formatCompact(totalExpenseInView, currencySymbol)}", style = MaterialTheme.typography.labelMedium, color = ExpenseRed, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Search Bar
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = viewModel::setSearchQuery,
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Search title, category or note...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
          )
        )
      }

      // Type & Category Filter Chips
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = filterType == null,
            onClick = { viewModel.setFilterType(null) },
            label = { Text("Everything", fontWeight = FontWeight.Bold) }
          )
          FilterChip(
            selected = filterType == TransactionType.EXPENSE,
            onClick = { viewModel.setFilterType(if (filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
            label = { Text("Expenses", fontWeight = FontWeight.Bold) }
          )
          FilterChip(
            selected = filterType == TransactionType.INCOME,
            onClick = { viewModel.setFilterType(if (filterType == TransactionType.INCOME) null else TransactionType.INCOME) },
            label = { Text("Income", fontWeight = FontWeight.Bold) }
          )
          FilterChip(
            selected = showCategoryFilters || filterCategory != null,
            onClick = { showCategoryFilters = !showCategoryFilters },
            label = { Text("Category", fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) }
          )
        }
      }

      if (showCategoryFilters || filterCategory != null) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
          ) {
            FilterChip(
              selected = filterCategory == null,
              onClick = { viewModel.setFilterCategory(null) },
              label = { Text("All categories") }
            )
            TransactionCategory.values()
              .filter { filterType == null || it.defaultType == filterType }
              .forEach { category ->
                FilterChip(
                  selected = filterCategory == category,
                  onClick = { viewModel.setFilterCategory(if (filterCategory == category) null else category) },
                  label = { Text(category.displayName) }
                )
              }
          }
        }
      }

      if (sorted.isEmpty()) {
        item {
          PremiumCard {
            PremiumEmptyState(
              title = if (searchQuery.isNotBlank() || filterType != null || filterCategory != null || selectedDayOfMonth != null) "No matching records" else "Nothing recorded yet",
              body = if (searchQuery.isNotBlank() || filterType != null || filterCategory != null || selectedDayOfMonth != null) {
                "Try picking another date or clear your active filters."
              } else {
                "Your recorded entries for $monthLabel will appear here."
              },
              action = if (searchQuery.isNotBlank() || filterType != null || filterCategory != null || selectedDayOfMonth != null) {
                {
                  TextButton(onClick = {
                    viewModel.setSearchQuery("")
                    viewModel.setFilterType(null)
                    viewModel.setFilterCategory(null)
                    selectedDayOfMonth = null
                    showCategoryFilters = false
                  }) { Text("Clear all filters", fontWeight = FontWeight.Bold) }
                }
              } else null
            )
          }
        }
      } else {
        grouped.entries.forEach { (dateLabel, itemsForDay) ->
          item(key = "header_$dateLabel") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                "${itemsForDay.size} ${if (itemsForDay.size == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          item(key = "group_$dateLabel") {
            PremiumCard {
              Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)) {
                itemsForDay.forEachIndexed { index, tx ->
                  PremiumTransactionRow(
                    transaction = tx,
                    currencySymbol = currencySymbol,
                    onClick = { onEditTransaction(tx) },
                    trailing = {
                      IconButton(onClick = { transactionToDelete = tx }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${tx.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                      }
                    }
                  )
                  if (index != itemsForDay.lastIndex) {
                    HorizontalDivider(
                      modifier = Modifier.padding(start = 58.dp),
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                  }
                }
              }
            }
          }
        }
      }

      item {
        Spacer(Modifier.height(8.dp))
        Text(
          "Tap any record to edit it. Scroll dates or pick a month to explore your full financial timeline.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 2.dp)
        )
      }
    }
  }
}
