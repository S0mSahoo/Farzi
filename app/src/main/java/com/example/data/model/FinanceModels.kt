package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentSky
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Slate500

enum class TransactionType(val displayName: String) {
  EXPENSE("Expense"),
  INCOME("Income"),
  SALARY("Monthly Salary")
}

enum class PaymentMethod(val displayName: String) {
  CASH("Cash"),
  CREDIT_CARD("Credit Card"),
  DEBIT_CARD("Debit Card"),
  BANK_TRANSFER("Bank Transfer"),
  UPI_WALLET("UPI / Wallet")
}

enum class TransactionCategory(
  val displayName: String,
  val defaultType: TransactionType,
  val color: Color
) {
  // Expenses
  FOOD_DINING("Food & Dining", TransactionType.EXPENSE, AccentAmber),
  GROCERIES("Groceries", TransactionType.EXPENSE, EmeraldPrimary),
  TRANSPORTATION("Transportation", TransactionType.EXPENSE, AccentSky),
  HOUSING("Housing & Rent", TransactionType.EXPENSE, AccentIndigo),
  UTILITIES("Bills & Utilities", TransactionType.EXPENSE, AccentPurple),
  SHOPPING("Shopping", TransactionType.EXPENSE, AccentPink),
  ENTERTAINMENT("Entertainment", TransactionType.EXPENSE, AccentTeal),
  HEALTHCARE("Healthcare", TransactionType.EXPENSE, ExpenseRed),
  EDUCATION("Education", TransactionType.EXPENSE, AccentIndigo),
  TRAVEL("Travel", TransactionType.EXPENSE, AccentSky),
  PERSONAL_CARE("Personal Care", TransactionType.EXPENSE, AccentPink),
  SUBSCRIPTIONS("Subscriptions", TransactionType.EXPENSE, AccentPurple),
  OTHER_EXPENSE("Other Expense", TransactionType.EXPENSE, Slate500),

  // Income
  SALARY("Salary / Wages", TransactionType.SALARY, IncomeGreen),
  FREELANCE("Freelance & Gigs", TransactionType.INCOME, AccentTeal),
  BUSINESS("Business", TransactionType.INCOME, AccentIndigo),
  INVESTMENTS("Investments & Divs", TransactionType.INCOME, EmeraldPrimary),
  BONUS("Bonus & Rewards", TransactionType.INCOME, AccentAmber),
  GIFT("Gifts", TransactionType.INCOME, AccentPink),
  OTHER_INCOME("Other Income", TransactionType.INCOME, Slate500);

  val icon: ImageVector
    get() = when (this) {
      FOOD_DINING -> Icons.Default.Fastfood
      GROCERIES -> Icons.Default.ShoppingCart
      TRANSPORTATION -> Icons.Default.DirectionsCar
      HOUSING -> Icons.Default.Home
      UTILITIES -> Icons.Default.Receipt
      SHOPPING -> Icons.Default.LocalMall
      ENTERTAINMENT -> Icons.Default.Movie
      HEALTHCARE -> Icons.Default.Healing
      EDUCATION -> Icons.Default.School
      TRAVEL -> Icons.Default.Flight
      PERSONAL_CARE -> Icons.Default.Spa
      SUBSCRIPTIONS -> Icons.Default.Subscriptions
      OTHER_EXPENSE -> Icons.Default.AttachMoney
      SALARY -> Icons.Default.Work
      FREELANCE -> Icons.Default.LaptopMac
      BUSINESS -> Icons.Default.AccountBalance
      INVESTMENTS -> Icons.Default.TrendingUp
      BONUS -> Icons.Default.CardGiftcard
      GIFT -> Icons.Default.CardGiftcard
      OTHER_INCOME -> Icons.Default.AttachMoney
    }
}

data class TransactionItem(
  val id: Long = 0,
  val title: String,
  val amount: Double,
  val type: TransactionType,
  val category: TransactionCategory,
  val timestamp: Long, // Epoch milliseconds
  val note: String = "",
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val isRecurring: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

data class MonthlySalarySettings(
  val salaryAmount: Double = 3500.0,
  val payDayOfMonth: Int = 1,
  val monthlyBudgetGoal: Double = 2200.0,
  val currencySymbol: String = "$"
)

data class CategoryAnalytics(
  val category: TransactionCategory,
  val totalAmount: Double,
  val percentage: Float,
  val count: Int,
  val color: Color
)

data class DailySpendingTrend(
  val dateKey: String, // e.g. "2026-08-30"
  val dayLabel: String, // e.g. "Mon 30"
  val expenseAmount: Double,
  val incomeAmount: Double,
  val netAmount: Double
)

data class DayOfWeekBreakdown(
  val dayName: String,
  val shortName: String,
  val totalSpent: Double,
  val averageSpent: Double,
  val percentage: Float
)

enum class TimeRangeFilter(val displayName: String) {
  THIS_MONTH("This Month"),
  LAST_7_DAYS("7 Days"),
  LAST_MONTH("Last Month"),
  THIS_YEAR("This Year"),
  ALL_TIME("All Time")
}
