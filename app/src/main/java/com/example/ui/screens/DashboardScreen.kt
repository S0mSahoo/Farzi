package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.CategoryDetailData
import com.example.data.model.FinancialRecommendation
import com.example.data.model.RecommendationSeverity
import com.example.data.model.ScheduledRecurringOccurrence
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CategoryDetailSheet
import com.example.ui.components.CategorySpendingBreakdown
import com.example.ui.components.IncomeExpenseComparisonBar
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.YearPickerDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalBlue
import com.example.ui.theme.MinimalEmerald
import com.example.ui.theme.MinimalIndigo
import com.example.ui.theme.MinimalRose
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: FinanceViewModel,
  onOpenAddTransaction: (prefilledType: TransactionType?) -> Unit,
  onOpenSetBudget: () -> Unit,
  onNavigateToRecurring: () -> Unit = {}
) {
  val userProfile by viewModel.userProfile.collectAsState()
  val selectedCalendar by viewModel.selectedCalendar.collectAsState()
  val monthlySummary by viewModel.dashboardMonthSummary.collectAsState()
  val yearlySummary by viewModel.dashboardYearSummary.collectAsState()
  val categorySpending by viewModel.dashboardCategorySpending.collectAsState()
  val dueTodayList by viewModel.dueTodayOccurrences.collectAsState()
  val overdueList by viewModel.overdueOccurrences.collectAsState()
  val recommendations by viewModel.intelligentRecommendations.collectAsState()

  var isAnnualView by remember { mutableStateOf(false) }
  var showMonthPicker by remember { mutableStateOf(false) }
  var showYearPicker by remember { mutableStateOf(false) }
  var selectedCategoryForDetail by remember { mutableStateOf<CategoryDetailData?>(null) }
  var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

  val currencySymbol = userProfile.currencySymbol
  val currentYear = selectedCalendar.get(Calendar.YEAR)

  val firstName = remember(userProfile.name) {
    if (userProfile.name.isNotBlank()) {
      userProfile.name.trim().split("\\s+".toRegex()).firstOrNull() ?: userProfile.name
    } else ""
  }

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentCalendar = selectedCalendar,
      onDismiss = { showMonthPicker = false },
      onMonthYearSelected = { year, month ->
        viewModel.setSelectedMonth(year, month)
      }
    )
  }

  if (showYearPicker) {
    YearPickerDialog(
      currentYear = currentYear,
      onDismiss = { showYearPicker = false },
      onYearSelected = { year ->
        viewModel.setSelectedMonth(year, selectedCalendar.get(Calendar.MONTH))
      }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("dashboard_screen"),
    contentPadding = PaddingValues(bottom = 100.dp)
  ) {
    // Top Greeting & Mode Switcher
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 16.dp, bottom = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (firstName.isNotBlank()) "Hello, $firstName" else "Overview",
              style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
              )
            )
            Text(
              text = "Personal Finances",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
              ),
              color = MaterialTheme.colorScheme.onBackground
            )
          }

          // Monthly vs Annual Toggle
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          ) {
            Row(modifier = Modifier.padding(3.dp)) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (!isAnnualView) MaterialTheme.colorScheme.surface else Color.Transparent,
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { isAnnualView = false }
              ) {
                Text(
                  text = "Monthly",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (!isAnnualView) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (!isAnnualView) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }

              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isAnnualView) MaterialTheme.colorScheme.surface else Color.Transparent,
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { isAnnualView = true }
              ) {
                Text(
                  text = "Annual",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isAnnualView) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isAnnualView) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Period Navigator (Connected to Universal Period)
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                if (isAnnualView) viewModel.previousYear() else viewModel.previousMonth()
              }
            ) {
              Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Period", tint = MaterialTheme.colorScheme.onSurface)
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  if (isAnnualView) showYearPicker = true else showMonthPicker = true
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = if (isAnnualView) "$currentYear (Full Year)" else monthlySummary.monthLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            IconButton(
              onClick = {
                if (isAnnualView) viewModel.nextYear() else viewModel.nextMonth()
              }
            ) {
              Icon(Icons.Filled.ChevronRight, contentDescription = "Next Period", tint = MaterialTheme.colorScheme.onSurface)
            }
          }
        }
      }
    }

    // DUE TODAY / OVERDUE PAYMENTS ALERT CARD (Requirements 10-14)
    if (dueTodayList.isNotEmpty() || overdueList.isNotEmpty()) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (overdueList.isNotEmpty()) MinimalRose.copy(alpha = 0.08f) else MinimalIndigo.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (overdueList.isNotEmpty()) MinimalRose.copy(alpha = 0.3f) else MinimalIndigo.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(if (overdueList.isNotEmpty()) MinimalRose else MinimalIndigo),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (overdueList.isNotEmpty()) Icons.Filled.Warning else Icons.Filled.Notifications,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Text(
                    text = if (overdueList.isNotEmpty()) "Overdue & Due Payments" else "Scheduled Due Today",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                TextButton(onClick = onNavigateToRecurring) {
                  Text("View All", style = MaterialTheme.typography.labelMedium)
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              val itemsToShow = (overdueList + dueTodayList).take(3)
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsToShow.forEach { occ ->
                  DueTodayPaymentRow(
                    occurrence = occ,
                    onMarkPaid = { viewModel.markRecurringOccurrenceAsPaid(occ) },
                    onRemind = { viewModel.sendPaymentReminderNotification(occ) }
                  )
                }
              }
            }
          }
        }
      }
    }

    // INTELLIGENT FINANCIAL RECOMMENDATIONS (Requirements 39-43)
    if (recommendations.isNotEmpty()) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
          recommendations.take(2).forEach { rec ->
            RecommendationBannerCard(
              rec = rec,
              onActionClick = {
                if (rec.actionLabel?.contains("Budget", ignoreCase = true) == true) {
                  onOpenSetBudget()
                } else if (rec.actionLabel?.contains("Recurring", ignoreCase = true) == true) {
                  onNavigateToRecurring()
                }
              }
            )
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
    }

    // Main Summary Card (Income, Expense, Savings)
    item {
      val income = if (isAnnualView) yearlySummary.totalIncome else monthlySummary.totalIncome
      val expense = if (isAnnualView) yearlySummary.totalExpense else monthlySummary.totalExpense
      val savings = if (isAnnualView) yearlySummary.savings else monthlySummary.savings
      val savingsRate = if (isAnnualView) yearlySummary.savingsRate else monthlySummary.savingsRate

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          // Net Savings Banner
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Net Balance / Savings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(savings, currencySymbol),
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = (-0.5).sp
                ),
                color = if (savings >= 0) IncomeGreen else ExpenseRed
              )
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = (if (savingsRate >= 20) IncomeGreen else MinimalIndigo).copy(alpha = 0.12f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = if (savingsRate >= 0) Icons.Filled.Savings else Icons.Filled.TrendingDown,
                  contentDescription = null,
                  tint = if (savingsRate >= 20) IncomeGreen else MinimalIndigo,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "${String.format("%.1f", savingsRate)}% Saved",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (savingsRate >= 20) IncomeGreen else MinimalIndigo
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Income vs Expense Breakdown Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Income Box
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = IncomeGreen.copy(alpha = 0.08f),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                  Text("Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = IndianCurrencyFormatter.formatWithSymbol(income, currencySymbol),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            // Expense Box
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = ExpenseRed.copy(alpha = 0.08f),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                  Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = IndianCurrencyFormatter.formatWithSymbol(expense, currencySymbol),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Visual Ratio Bar
          IncomeExpenseComparisonBar(
            income = income,
            expense = expense,
            currencySymbol = currencySymbol
          )
        }
      }
    }

    // Budget Tracker Card (Monthly only)
    if (!isAnnualView) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  Icons.Filled.AccountBalanceWallet,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "Monthly Budget",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              TextButton(onClick = onOpenSetBudget) {
                Text(
                  text = if (monthlySummary.budgetLimit > 0) "Adjust" else "Set Budget",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            BudgetProgressBar(
              budgetLimit = monthlySummary.budgetLimit,
              budgetUsed = monthlySummary.budgetUsed,
              currencySymbol = currencySymbol
            )
          }
        }
      }
    }

    // Category Spending Breakdown (Clickable items -> CategoryDetailSheet)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
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
            Text(
              text = "Spending by Category",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${categorySpending.size} categories",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (categorySpending.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No expenses recorded in ${if (isAnnualView) currentYear else monthlySummary.monthLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            CategorySpendingBreakdown(
              categories = categorySpending,
              currencySymbol = currencySymbol,
              onCategoryClick = { catSpending ->
                val detailData = viewModel.getCategoryDetailData(catSpending.category)
                selectedCategoryForDetail = detailData
              }
            )
          }
        }
      }
    }
  }

  // Category Detail Bottom Sheet
  selectedCategoryForDetail?.let { detailData ->
    CategoryDetailSheet(
      data = detailData,
      onDismiss = { selectedCategoryForDetail = null },
      onEditTransaction = { tx ->
        selectedCategoryForDetail = null
        editingTransaction = tx
      },
      onDeleteTransaction = { tx ->
        viewModel.deleteTransaction(tx) {
          val updated = viewModel.getCategoryDetailData(detailData.category)
          selectedCategoryForDetail = updated
        }
      }
    )
  }

  // Edit Transaction Sheet if opened from category detail
  editingTransaction?.let { tx ->
    com.example.ui.components.AddEditTransactionSheet(
      initialTransaction = tx,
      currencySymbol = userProfile.currencySymbol,
      onDismiss = { editingTransaction = null },
      onSave = { title, amount, type, category, timestamp, note, paymentMethod, isRecurring ->
        val updated = tx.copy(
          title = title,
          amount = amount,
          type = type,
          category = category,
          timestamp = timestamp,
          note = note,
          paymentMethod = paymentMethod,
          isRecurring = isRecurring
        )
        viewModel.updateTransaction(updated) {
          editingTransaction = null
        }
      }
    )
  }
}

@Composable
fun DueTodayPaymentRow(
  occurrence: ScheduledRecurringOccurrence,
  onMarkPaid: () -> Unit,
  onRemind: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(occurrence.category.color.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = occurrence.category.icon,
            contentDescription = null,
            tint = occurrence.category.color,
            modifier = Modifier.size(18.dp)
          )
        }

        Column {
          Text(
            text = occurrence.ruleTitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${occurrence.relativeLabel} • ${IndianCurrencyFormatter.format(occurrence.amount)}",
            style = MaterialTheme.typography.labelSmall,
            color = if (occurrence.daysDiff < 0) MinimalRose else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onRemind, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Remind",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
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

@Composable
fun RecommendationBannerCard(
  rec: FinancialRecommendation,
  onActionClick: () -> Unit
) {
  val (accentColor, icon) = when (rec.severity) {
    RecommendationSeverity.ALERT -> Pair(MinimalRose, Icons.Filled.Warning)
    RecommendationSeverity.WARNING -> Pair(AccentAmber, Icons.Filled.Lightbulb)
    RecommendationSeverity.SUCCESS -> Pair(MinimalEmerald, Icons.Rounded.CheckCircle)
    RecommendationSeverity.INFO -> Pair(MinimalBlue, Icons.Rounded.Info)
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = accentColor.copy(alpha = 0.08f),
    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = rec.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = rec.message,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (rec.actionLabel != null) {
        FilledTonalButton(
          onClick = onActionClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Text(rec.actionLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }
}
