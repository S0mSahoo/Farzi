package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.model.TransactionType
import com.example.pro.entitlement.ProFeature
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CategoryDetailSheet
import com.example.ui.components.CategorySpendingBreakdown
import com.example.ui.components.FinancialInsightsSection
import com.example.ui.components.IncomeExpenseComparisonBar
import com.example.ui.components.IndianCurrencyFormatter
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.PaisaProCard
import com.example.ui.components.YearPickerDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: FinanceViewModel,
  onOpenAddTransaction: (prefilledType: TransactionType?) -> Unit,
  onOpenSetBudget: () -> Unit,
  onOpenCopilot: () -> Unit = {},
  onOpenForecast: () -> Unit = {},
  onOpenWhatIf: () -> Unit = {},
  onOpenProUpgrade: (ProFeature?) -> Unit = {}
) {
  val userProfile by viewModel.userProfile.collectAsState()
  val isProUser by viewModel.isProUser.collectAsState()
  val selectedCalendar by viewModel.dashboardCalendar.collectAsState()
  val monthlySummary by viewModel.dashboardMonthSummary.collectAsState()
  val yearlySummary by viewModel.dashboardYearSummary.collectAsState()
  val categorySpending by viewModel.dashboardCategorySpending.collectAsState()
  val financialInsights by viewModel.financialInsights.collectAsState()
  val selectedCategoryDetail by viewModel.selectedCategoryDetail.collectAsState()

  var isAnnualView by remember { mutableStateOf(false) }
  var showMonthPicker by remember { mutableStateOf(false) }
  var showYearPicker by remember { mutableStateOf(false) }

  val currencySymbol = userProfile.currencySymbol
  val currentYear = selectedCalendar.get(Calendar.YEAR)

  val firstName = remember(userProfile.name) {
    if (userProfile.name.isNotBlank()) {
      userProfile.name.trim().split("\\s+".toRegex()).firstOrNull() ?: userProfile.name
    } else ""
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
      }
    )
  }

  if (showYearPicker) {
    YearPickerDialog(
      currentYear = currentYear,
      onDismiss = { showYearPicker = false },
      onYearSelected = { year ->
        viewModel.setDashboardYear(year)
      }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("dashboard_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Compact Header with First-Name Greeting
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (firstName.isNotBlank()) "Namaste, $firstName 👋" else "Namaste 👋",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Track your income, expenses & savings",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // View Mode Switcher (Monthly / Annual)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { isAnnualView = !isAnnualView }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = if (isAnnualView) "Year $currentYear" else "Month View",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // 2. Month / Year Selector Navigation Bar
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
            onClick = {
              if (isAnnualView) viewModel.previousDashboardYear() else viewModel.previousDashboardMonth()
            },
            modifier = Modifier.testTag("prev_period_button")
          ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = if (isAnnualView) "Previous Year" else "Previous Month")
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                if (isAnnualView) {
                  showYearPicker = true
                } else {
                  showMonthPicker = true
                }
              }
              .testTag("period_selector_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = if (isAnnualView) "Year $currentYear" else monthlySummary.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
          }

          IconButton(
            onClick = {
              if (isAnnualView) viewModel.nextDashboardYear() else viewModel.nextDashboardMonth()
            },
            modifier = Modifier.testTag("next_period_button")
          ) {
            Icon(Icons.Default.ChevronRight, contentDescription = if (isAnnualView) "Next Year" else "Next Month")
          }
        }
      }
    }

    // 2.5 Paisa Pro Feature Card (Forecast, What-If, Copilot)
    item {
      PaisaProCard(
        isPro = isProUser,
        onOpenCopilot = {
          if (viewModel.hasProAccess(ProFeature.AI_COPILOT)) onOpenCopilot() else onOpenProUpgrade(ProFeature.AI_COPILOT)
        },
        onOpenForecast = {
          if (viewModel.hasProAccess(ProFeature.CASH_FLOW_FORECAST)) onOpenForecast() else onOpenProUpgrade(ProFeature.CASH_FLOW_FORECAST)
        },
        onOpenWhatIf = {
          if (viewModel.hasProAccess(ProFeature.WHAT_IF_SIMULATOR)) onOpenWhatIf() else onOpenProUpgrade(ProFeature.WHAT_IF_SIMULATOR)
        }
      )
    }

    if (isAnnualView) {
      // ================= ANNUAL OVERVIEW =================
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Annual Financial Summary (${yearlySummary.year})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              // Annual Income
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(IncomeGreen.copy(alpha = 0.1f))
                  .padding(12.dp)
              ) {
                Text("Total Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = IndianCurrencyFormatter.formatWithSymbol(yearlySummary.totalIncome, currencySymbol),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = IncomeGreen
                )
              }

              // Annual Expense
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(ExpenseRed.copy(alpha = 0.1f))
                  .padding(12.dp)
              ) {
                Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = IndianCurrencyFormatter.formatWithSymbol(yearlySummary.totalExpense, currencySymbol),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = ExpenseRed
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Annual Savings
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Total Annual Savings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = IndianCurrencyFormatter.formatWithSymbol(yearlySummary.savings, currencySymbol, includeSign = true),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (yearlySummary.savings >= 0) IncomeGreen else ExpenseRed
                  )
                }

                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (yearlySummary.savings >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "Savings Rate: ${String.format("%.1f", yearlySummary.savingsRate)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (yearlySummary.savings >= 0) IncomeGreen else ExpenseRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }
      }

      // 12-Month Table Breakdown
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Month-by-Month Breakdown",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Explicit Column Headers Row
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Month",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1.2f)
                )
                Text(
                  text = "Income",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = IncomeGreen,
                  textAlign = TextAlign.End,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = "Expenses",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = ExpenseRed,
                  textAlign = TextAlign.End,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = "Savings",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.End,
                  modifier = Modifier.weight(1.1f)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            yearlySummary.monthlyBreakdown.forEach { monthItem ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = monthItem.monthLabel.split(" ").firstOrNull() ?: monthItem.monthLabel,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1.2f)
                )

                Text(
                  text = "+${IndianCurrencyFormatter.formatCompact(monthItem.totalIncome, currencySymbol)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = IncomeGreen,
                  fontWeight = FontWeight.Medium,
                  textAlign = TextAlign.End,
                  modifier = Modifier.weight(1f)
                )

                Text(
                  text = "-${IndianCurrencyFormatter.formatCompact(monthItem.totalExpense, currencySymbol)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = ExpenseRed,
                  fontWeight = FontWeight.Medium,
                  textAlign = TextAlign.End,
                  modifier = Modifier.weight(1f)
                )

                Text(
                  text = IndianCurrencyFormatter.formatCompact(monthItem.savings, currencySymbol),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = if (monthItem.savings >= 0) IncomeGreen else ExpenseRed,
                  textAlign = TextAlign.End,
                  modifier = Modifier.weight(1.1f)
                )
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            }
          }
        }
      }
    } else {
      // ================= MONTHLY VIEW =================

      // 3. Income / Expense / Savings Cards Grid
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Total Income Card
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = IncomeGreen.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.25f)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(20.dp))
              .clickable { onOpenAddTransaction(TransactionType.INCOME) }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = IncomeGreen.copy(alpha = 0.2f),
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                  }
                }
                Icon(Icons.Default.Add, contentDescription = "Add Income", tint = IncomeGreen, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text("Total Income", style = MaterialTheme.typography.labelMedium, color = IncomeGreen, fontWeight = FontWeight.Medium)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.totalIncome, currencySymbol),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen
              )
            }
          }

          // Total Expenses Card
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = ExpenseRed.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.25f)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(20.dp))
              .clickable { onOpenAddTransaction(TransactionType.EXPENSE) }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = ExpenseRed.copy(alpha = 0.2f),
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                  }
                }
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = ExpenseRed, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text("Total Expenses", style = MaterialTheme.typography.labelMedium, color = ExpenseRed, fontWeight = FontWeight.Medium)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.totalExpense, currencySymbol),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
              )
            }
          }
        }
      }

      // 4. Net Monthly Savings Card
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                  }
                }
                Column {
                  Text(
                    text = "Net Monthly Savings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Income - Expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (monthlySummary.savings >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "Rate: ${String.format("%.1f", monthlySummary.savingsRate)}%",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (monthlySummary.savings >= 0) IncomeGreen else ExpenseRed,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = IndianCurrencyFormatter.formatWithSymbol(monthlySummary.savings, currencySymbol, includeSign = true),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = if (monthlySummary.savings >= 0) IncomeGreen else ExpenseRed
            )
          }
        }
      }

      // 5. Monthly Budget Utilization Card
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            if (monthlySummary.budgetLimit > 0) {
              BudgetProgressBar(
                budgetLimit = monthlySummary.budgetLimit,
                budgetUsed = monthlySummary.budgetUsed,
                currencySymbol = currencySymbol
              )
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(onClick = onOpenSetBudget) {
                  Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Edit Budget")
                }
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "No Monthly Budget Set",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Set a budget for ${monthlySummary.monthLabel} to keep expenses in check.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                FilledTonalButton(
                  onClick = onOpenSetBudget,
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("Set Budget")
                }
              }
            }
          }
        }
      }

      // 6. Comparative Visual & Breakdown / Empty State
      if (monthlySummary.transactionCount == 0) {
        item {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "No transactions in ${monthlySummary.monthLabel}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "Tap below to log your first income or expense for this month.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(18.dp))

              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = { onOpenAddTransaction(TransactionType.INCOME) },
                  colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen, contentColor = Color.White),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add Income")
                }

                Button(
                  onClick = { onOpenAddTransaction(TransactionType.EXPENSE) },
                  colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add Expense")
                }
              }
            }
          }
        }
      } else {
        // Income vs Expense Comparison Bar
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "Income vs Expenses",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(12.dp))
              IncomeExpenseComparisonBar(
                income = monthlySummary.totalIncome,
                expense = monthlySummary.totalExpense,
                currencySymbol = currencySymbol
              )
            }
          }
        }

        // Financial Insights & Guidance
        if (financialInsights.isNotEmpty()) {
          item {
            FinancialInsightsSection(
              insights = financialInsights,
              onCategoryClick = { viewModel.selectCategoryForDetail(it) }
            )
          }
        }

        // Category Breakdown
        if (categorySpending.isNotEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Spending by Category",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategorySpendingBreakdown(
                  categories = categorySpending,
                  currencySymbol = currencySymbol,
                  onCategoryClick = { category ->
                    viewModel.selectCategoryForDetail(category)
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
