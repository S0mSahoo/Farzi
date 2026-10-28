package com.example.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategorySpending
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryDetailSheet
import com.example.ui.components.DateUtils
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumEmptyState
import com.example.ui.components.PremiumMonthNavigator
import com.example.ui.components.PremiumPageHeader
import com.example.ui.components.PremiumSectionHeading
import com.example.ui.viewmodel.FinanceViewModel

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
  val start = DateUtils.getStartOfMonth(selectedCalendar)
  val end = DateUtils.getEndOfMonth(selectedCalendar)

  val categoryItems = remember(allTransactions, selectedCalendar, selectedType) {
    val monthTransactions = allTransactions.filter {
      it.timestamp in start..end && it.type == selectedType
    }
    TransactionCategory.values()
      .filter { it.defaultType == selectedType }
      .map { category ->
        val matching = monthTransactions.filter { it.category == category }
        val amount = matching.sumOf { it.amount }
        CategorySpending(
          category = category,
          amount = amount,
          percentage = if (monthTransactions.sumOf { it.amount } > 0.0) {
            (amount / monthTransactions.sumOf { it.amount }).toFloat()
          } else 0f,
          count = matching.size,
          color = category.color
        )
      }
      .sortedByDescending { it.amount }
  }
  val populated = categoryItems.filter { it.amount > 0.0 }
  val total = categoryItems.sumOf { it.amount }
  val count = categoryItems.sumOf { it.count }

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

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
      item {
        PremiumPageHeader(
          title = "Categories",
          subtitle = "See where your money flows.",
          trailing = {
            PremiumMonthNavigator(
              label = monthLabel,
              onPrevious = { viewModel.previousDashboardMonth() },
              onNext = { viewModel.nextDashboardMonth() },
              onPick = { showMonthPicker = true }
            )
          }
        )
      }
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = selectedType == TransactionType.EXPENSE, onClick = { selectedType = TransactionType.EXPENSE }, label = { Text("Spending") })
          FilterChip(selected = selectedType == TransactionType.INCOME, onClick = { selectedType = TransactionType.INCOME }, label = { Text("Income") })
        }
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
              Text(if (selectedType == TransactionType.EXPENSE) "TOTAL SPENDING" else "TOTAL INCOME", style = MaterialTheme.typography.labelMedium, color = subTextColor, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
              Text(IndianCurrencyFormatter.formatWithSymbol(total, currencySymbol), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
              Text("${populated.size} active categories · $count entries", style = MaterialTheme.typography.bodySmall, color = subTextColor)
            }
            Surface(shape = RoundedCornerShape(16.dp), color = if (isLight) Color(0xFFE9D5FF) else Color.White.copy(alpha = 0.15f)) {
              Text(
                if (selectedType == TransactionType.EXPENSE) "OUT" else "IN",
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
      item {
        PremiumSectionHeading(
          title = if (selectedType == TransactionType.EXPENSE) "Your spending mix" else "Income sources",
          subtitle = "Tap a category to see the underlying transactions."
        )
      }
      if (populated.isEmpty()) {
        item {
          PremiumCard {
            PremiumEmptyState(
              title = "Nothing in these categories yet",
              body = "When you record ${if (selectedType == TransactionType.EXPENSE) "spending" else "income"}, your category breakdown will appear here."
            )
          }
        }
      } else {
        items(populated.size, key = { "category_${populated[it].category.name}" }) { index ->
          val item = populated[index]
          CategoryPremiumRow(
            item = item,
            currencySymbol = currencySymbol,
            onClick = { viewModel.selectCategoryForDetail(item.category) }
          )
        }
      }
      item { Spacer(Modifier.height(10.dp)) }
    }
  }
}

@Composable
private fun CategoryPremiumRow(
  item: CategorySpending,
  currencySymbol: String,
  onClick: () -> Unit
) {
  PremiumCard {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .clickable(onClick = onClick)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = item.color.copy(alpha = 0.13f)) {
          Icon(item.category.icon, contentDescription = null, tint = item.color, modifier = Modifier.padding(11.dp).size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(item.category.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("${item.count} ${if (item.count == 1) "entry" else "entries"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(IndianCurrencyFormatter.formatWithSymbol(item.amount, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text("${(item.percentage * 100).toInt()}% of total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      com.example.ui.components.PremiumProgressTrack(
        progress = item.percentage,
        color = item.color,
        height = 7.dp
      )
    }
  }
}
