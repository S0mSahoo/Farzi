package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategorySpending
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryDetailSheet
import com.example.ui.components.CategorySpendingBreakdown
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

/**
 * Overview of Paisa's built-in transaction categories for the selected month.
 * Categories remain enum-backed, so this screen does not imply unsupported custom editing.
 */
@Composable
fun CategoriesScreen(viewModel: FinanceViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val selectedCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedCategoryDetail by viewModel.selectedCategoryDetail.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val currencySymbol = userProfile.currencySymbol
    val monthLabel = DateUtils.getMonthLabel(selectedCalendar)
    val startOfMonth = DateUtils.getStartOfMonth(selectedCalendar)
    val endOfMonth = DateUtils.getEndOfMonth(selectedCalendar)

    val categoryItems = remember(allTransactions, selectedCalendar, selectedType) {
        val categories = TransactionCategory.values().filter { it.defaultType == selectedType }
        val monthTransactions = allTransactions.filter {
            it.timestamp in startOfMonth..endOfMonth && it.type == selectedType
        }
        val totals = categories.associateWith { category ->
            monthTransactions.filter { it.category == category }.sumOf { it.amount }
        }
        val total = totals.values.sum()
        categories.map { category ->
            val items = monthTransactions.filter { it.category == category }
            CategorySpending(
                category = category,
                amount = totals[category] ?: 0.0,
                percentage = if (total > 0.0) ((totals[category] ?: 0.0) / total).toFloat() else 0f,
                count = items.size,
                color = category.color
            )
        }.sortedWith(compareByDescending<CategorySpending> { it.amount }.thenBy { it.category.displayName })
    }

    selectedCategoryDetail?.let { detail ->
        CategoryDetailSheet(
            detail = detail,
            currencySymbol = currencySymbol,
            onDismiss = { viewModel.clearSelectedCategoryDetail() }
        )
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentCalendar = selectedCalendar,
            onDismiss = { showMonthPicker = false },
            onMonthYearSelected = { year, month ->
                viewModel.setDashboardMonthAndYear(year, month)
                showMonthPicker = false
            }
        )
    }

    val total = categoryItems.sumOf { it.amount }
    val totalCount = categoryItems.sumOf { it.count }
    val maxCategory = categoryItems.maxByOrNull { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Understand each part of your finances",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Choose month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp)
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.previousDashboardMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Expense and income categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showMonthPicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select month")
                    }
                    IconButton(onClick = { viewModel.nextDashboardMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    label = { Text("Expenses") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    label = { Text("Income") }
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (selectedType == TransactionType.EXPENSE) "Recorded expenses" else "Recorded income",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = IndianCurrencyFormatter.formatWithSymbol(total, currencySymbol),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedType == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$totalCount recorded ${if (totalCount == 1) "transaction" else "transactions"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (maxCategory != null && maxCategory.amount > 0.0) {
                        Text(
                            text = "Largest category: ${maxCategory.category.displayName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = maxCategory.color
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Category breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (totalCount > 0) {
            item {
                CategorySpendingBreakdown(
                    categories = categoryItems.filter { it.count > 0 },
                    currencySymbol = currencySymbol,
                    onCategoryClick = { viewModel.selectCategoryForDetail(it) }
                )
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nothing recorded in $monthLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Categories will show their totals as you record transactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
