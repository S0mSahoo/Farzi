package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumIconButton
import com.example.ui.components.PremiumMonthNavigator
import com.example.ui.components.PremiumSectionHeading
import com.example.ui.components.PremiumTextButton
import com.example.ui.components.PremiumTransactionRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.PaisaMotion
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class TransactionFeedEntry(
  val key: String,
  val dateLabel: String? = null,
  val transaction: TransactionItem? = null,
  val entryCount: Int = 0
)

private data class TransactionScreenProjection(
  val feedEntries: List<TransactionFeedEntry> = emptyList(),
  val recordCount: Int = 0,
  val totalIncome: Double = 0.0,
  val totalExpense: Double = 0.0,
  val daysWithTransactions: Set<Int> = emptySet(),
  val visibleTransactions: List<TransactionItem> = emptyList()
)

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

  val coroutineScope = rememberCoroutineScope()
  val useTwoPane = LocalAdaptiveInfo.current.useTwoPaneLayout
  var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
  var selectedDayOfMonth by remember { mutableStateOf<Int?>(null) } // null = All Month
  var showMonthPicker by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }
  var showCategoryFilters by remember { mutableStateOf(false) }
  var isDeletingTransaction by remember { mutableStateOf(false) }
  var deleteTransactionError by remember { mutableStateOf<String?>(null) }

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
      isLoading = isDeletingTransaction,
      errorMessage = deleteTransactionError,
      onConfirm = {
        val item = transactionToDelete ?: return@ConfirmationDialog
        isDeletingTransaction = true
        deleteTransactionError = null
        coroutineScope.launch {
          try {
            viewModel.deleteTransactionSuspend(item)
            isDeletingTransaction = false
            transactionToDelete = null
          } catch (e: Exception) {
            isDeletingTransaction = false
            deleteTransactionError = e.localizedMessage ?: "Failed to delete transaction."
          }
        }
      },
      onDismiss = {
        if (!isDeletingTransaction) {
          transactionToDelete = null
          deleteTransactionError = null
        }
      }
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

  // Filter, aggregate and sort transaction history off the main thread.
  // The same pass creates the daily markers so rendering the month strip is O(days), not
  // O(days * transactions).
  val projection by produceState(
    initialValue = TransactionScreenProjection(),
    transactions,
    startOfMonth,
    endOfMonth,
    selectedDayOfMonth,
    selectedCalendar.get(Calendar.YEAR),
    selectedCalendar.get(Calendar.MONTH)
  ) {
    value = withContext(Dispatchers.Default) {
      val dayCalendar = Calendar.getInstance()
      val daysWithTransactions = LinkedHashSet<Int>()
      val visibleTransactions = ArrayList<TransactionItem>()
      var totalIncome = 0.0
      var totalExpense = 0.0

      for (transaction in transactions) {
        if (transaction.timestamp !in startOfMonth..endOfMonth) continue
        dayCalendar.timeInMillis = transaction.timestamp
        val day = dayCalendar.get(Calendar.DAY_OF_MONTH)
        daysWithTransactions.add(day)
        if (selectedDayOfMonth != null && selectedDayOfMonth != day) continue

        visibleTransactions.add(transaction)
        when (transaction.type) {
          TransactionType.INCOME -> totalIncome += transaction.amount
          TransactionType.EXPENSE -> totalExpense += transaction.amount
        }
      }

      visibleTransactions.sortWith(
        compareByDescending<TransactionItem> { it.timestamp }.thenByDescending { it.id }
      )
      val todayKey = DateUtils.getDayKey(Calendar.getInstance())
      val yesterdayKey = DateUtils.getDayKey(Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -1)
      })
      val grouped = visibleTransactions.groupBy { transaction ->
        when (DateUtils.getDayKey(transaction.timestamp)) {
          todayKey -> "Today"
          yesterdayKey -> "Yesterday"
          else -> DateUtils.getDisplayDate(transaction.timestamp)
        }
      }
      val entries = buildList {
        grouped.forEach { (dateLabel, dayTransactions) ->
          add(
            TransactionFeedEntry(
              key = "header_$dateLabel",
              dateLabel = dateLabel,
              entryCount = dayTransactions.size
            )
          )
          dayTransactions.forEach { transaction ->
            add(
              TransactionFeedEntry(
                key = "tx_${transaction.id}_${transaction.timestamp}",
                transaction = transaction
              )
            )
          }
        }
      }

      TransactionScreenProjection(
        feedEntries = entries,
        recordCount = visibleTransactions.size,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        daysWithTransactions = daysWithTransactions,
        visibleTransactions = visibleTransactions
      )
    }
  }
  val feedEntries = projection.feedEntries
  val totalIncomeInView = projection.totalIncome
  val totalExpenseInView = projection.totalExpense
  val netInView = totalIncomeInView - totalExpenseInView

  val selectedTransaction = remember(projection.visibleTransactions, selectedTransactionId) {
    projection.visibleTransactions.firstOrNull { it.id == selectedTransactionId }
      ?: projection.visibleTransactions.firstOrNull()
  }

  val transactionDetailPanel: @Composable () -> Unit = {
    if (selectedTransaction == null) {
      PremiumCard {
        PremiumEmptyState(
          title = "Select a transaction",
          body = "Choose a record from the history pane to inspect its category, payment method and notes."
        )
      }
    } else {
      val transaction = selectedTransaction
      PremiumCard {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            "TRANSACTION DETAILS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
          )
          Text(
            transaction.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            IndianCurrencyFormatter.formatWithSymbol(transaction.amount, currencySymbol, includeSign = true),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          DetailLine("Type", if (transaction.type == TransactionType.INCOME) "Income" else "Expense")
          DetailLine("Category", transaction.category.displayName)
          DetailLine("Date", DateUtils.getDisplayDate(transaction.timestamp))
          DetailLine("Payment method", transaction.paymentMethod.displayName)
          if (transaction.note.isNotBlank()) {
            Text("Note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(transaction.note, style = MaterialTheme.typography.bodyMedium)
          }
          Spacer(Modifier.height(4.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PremiumTextButton(
              onClick = { onEditTransaction(transaction) },
              modifier = Modifier.weight(1f)
            ) {
              Text("Edit transaction")
            }
            PremiumTextButton(
              onClick = { transactionToDelete = transaction },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ExpenseRed)
              Spacer(Modifier.width(5.dp))
              Text("Delete", color = ExpenseRed)
            }
          }
        }
      }
    }
  }

  // Generate days for selected month
  val daysInMonth = remember(selectedCalendar) {
    val cal = selectedCalendar.clone() as Calendar
    cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  val transactionListContent: @Composable () -> Unit = {
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
                val hasTxOnDay = day in projection.daysWithTransactions

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
              AnimatedContent(
                targetState = selectedDayOfMonth,
                transitionSpec = { PaisaMotion.DefaultContentTransform },
                label = "transactions_summary_heading_transition"
              ) { day ->
                Text(
                  text = if (day != null) "SUMMARY FOR DAY $day" else "NET CASH FLOW IN VIEW",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.2.sp
                )
              }
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
              ) {
                if (searchQuery.isNotBlank()) {
                  Text(
                    text = "${projection.recordCount} ${if (sorted.size == 1) "record" else "records"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                } else {
                  com.example.ui.util.AnimatedFinancialText(
                    text = "${sorted.size} ${if (sorted.size == 1) "record" else "records"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }
            }

            if (searchQuery.isNotBlank()) {
              Text(
                IndianCurrencyFormatter.formatWithSymbol(netInView, currencySymbol, includeSign = true),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            } else {
              com.example.ui.util.AnimatedFinancialText(
                IndianCurrencyFormatter.formatWithSymbol(netInView, currencySymbol, includeSign = true),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = CircleShape, color = com.example.ui.theme.IncomeGreen.copy(alpha = 0.18f), modifier = Modifier.size(10.dp)) {}
                if (searchQuery.isNotBlank()) {
                  Text("Income: ${IndianCurrencyFormatter.formatCompact(totalIncomeInView, currencySymbol)}", style = MaterialTheme.typography.labelMedium, color = com.example.ui.theme.IncomeGreen, fontWeight = FontWeight.Bold)
                } else {
                  com.example.ui.util.AnimatedFinancialText("Income: ${IndianCurrencyFormatter.formatCompact(totalIncomeInView, currencySymbol)}", style = MaterialTheme.typography.labelMedium, color = com.example.ui.theme.IncomeGreen, fontWeight = FontWeight.Bold)
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = CircleShape, color = com.example.ui.theme.ExpenseRed.copy(alpha = 0.18f), modifier = Modifier.size(10.dp)) {}
                if (searchQuery.isNotBlank()) {
                  Text("Spent: ${IndianCurrencyFormatter.formatCompact(totalExpenseInView, currencySymbol)}", style = MaterialTheme.typography.labelMedium, color = com.example.ui.theme.ExpenseRed, fontWeight = FontWeight.Bold)
                } else {
                  com.example.ui.util.AnimatedFinancialText("Spent: ${IndianCurrencyFormatter.formatCompact(totalExpenseInView, currencySymbol)}", style = MaterialTheme.typography.labelMedium, color = com.example.ui.theme.ExpenseRed, fontWeight = FontWeight.Bold)
                }
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

      item {
        AnimatedVisibility(
          visible = showCategoryFilters || filterCategory != null,
          label = "category_filters_visibility"
        ) {
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

      if (projection.recordCount == 0) {
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
                  PremiumTextButton(onClick = {
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
        items(feedEntries, key = { it.key }) { entry ->
          val dateLabel = entry.dateLabel
          val transaction = entry.transaction
          if (dateLabel != null) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                "${entry.entryCount} ${if (entry.entryCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else if (transaction != null) {
            PremiumCard {
              PremiumTransactionRow(
                transaction = transaction,
                currencySymbol = currencySymbol,
                onClick = { if (useTwoPane) selectedTransactionId = transaction.id else onEditTransaction(transaction) },
                trailing = {
                  PremiumIconButton(onClick = { transactionToDelete = transaction }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${transaction.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                  }
                },
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
              )
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

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    if (useTwoPane) {
      Row(
        modifier = Modifier.fillMaxSize().widthIn(max = 1500.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
      ) {
        Box(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
          transactionListContent()
        }
        Column(
          modifier = Modifier
            .weight(0.85f)
            .fillMaxHeight()
            .padding(end = 20.dp, top = 18.dp, bottom = 18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          transactionDetailPanel()
        }
      }
    } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        transactionListContent()
      }
    }
  }
}
@Composable
private fun DetailLine(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
  }
}


