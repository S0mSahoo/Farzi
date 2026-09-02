package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.runtime.Immutable
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
import java.util.Currency
import java.util.Locale

enum class TransactionType(val displayName: String) {
  EXPENSE("Expense"),
  INCOME("Income")
}

enum class RecurrenceInterval(val displayName: String, val daysApprox: Int) {
  DAILY("Daily", 1),
  WEEKLY("Weekly", 7),
  MONTHLY("Monthly", 30),
  YEARLY("Yearly", 365)
}

enum class PaymentMethod(val displayName: String) {
  UPI("UPI (GPay / PhonePe / Paytm)"),
  CASH("Cash"),
  CREDIT_CARD("Credit Card"),
  DEBIT_CARD("Debit Card"),
  NET_BANKING("Net Banking / IMPS"),
  WALLET("Digital Wallet"),
  OTHER("Other")
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
  FITNESS("Fitness & Sports", TransactionType.EXPENSE, AccentTeal),
  PERSONAL_CARE("Personal Care", TransactionType.EXPENSE, AccentPink),
  SUBSCRIPTIONS("Subscriptions", TransactionType.EXPENSE, AccentPurple),
  OTHER_EXPENSE("Other Expense", TransactionType.EXPENSE, Slate500),

  // Income
  SALARY("Salary & Wages", TransactionType.INCOME, IncomeGreen),
  FREELANCE("Freelance & Gigs", TransactionType.INCOME, AccentTeal),
  BUSINESS("Business Income", TransactionType.INCOME, AccentIndigo),
  INVESTMENTS("Investments & Returns", TransactionType.INCOME, EmeraldPrimary),
  BONUS("Bonus & Incentives", TransactionType.INCOME, AccentAmber),
  GIFT("Gifts & Grants", TransactionType.INCOME, AccentPink),
  RENTAL_INCOME("Rental Income", TransactionType.INCOME, AccentSky),
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
      FITNESS -> Icons.Default.FitnessCenter
      PERSONAL_CARE -> Icons.Default.Spa
      SUBSCRIPTIONS -> Icons.Default.Subscriptions
      OTHER_EXPENSE -> Icons.Default.AttachMoney
      SALARY -> Icons.Default.Work
      FREELANCE -> Icons.Default.LaptopMac
      BUSINESS -> Icons.Default.AccountBalance
      INVESTMENTS -> Icons.Default.TrendingUp
      BONUS -> Icons.Default.CardGiftcard
      GIFT -> Icons.Default.CardGiftcard
      RENTAL_INCOME -> Icons.Default.AccountBalanceWallet
      OTHER_INCOME -> Icons.Default.AttachMoney
    }
}

@Immutable
data class TransactionItem(
  val id: Long = 0,
  val title: String,
  val amount: Double,
  val type: TransactionType,
  val category: TransactionCategory,
  val timestamp: Long, // Epoch millis for transaction date
  val note: String = "",
  val paymentMethod: PaymentMethod = PaymentMethod.UPI,
  val isRecurring: Boolean = false,
  val recurringRuleId: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
data class BudgetModel(
  val id: Long = 0,
  val monthKey: String, // "YYYY-MM", e.g. "2026-09"
  val totalBudget: Double = 0.0,
  val categoryBudgets: Map<TransactionCategory, Double> = emptyMap(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
data class RecurringRule(
  val id: Long = 0,
  val title: String,
  val amount: Double,
  val type: TransactionType,
  val category: TransactionCategory,
  val interval: RecurrenceInterval,
  val startDate: Long,
  val endDate: Long? = null,
  val lastGeneratedDate: Long = 0,
  val paymentMethod: PaymentMethod = PaymentMethod.UPI,
  val note: String = "",
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class UserProfile(
  val name: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  val googleId: String? = null,
  val hasCompletedOnboarding: Boolean = false
) {
  val currencySymbol: String
    get() = try {
      Currency.getInstance(Locale.getDefault())?.getSymbol(Locale.getDefault()) ?: "₹"
    } catch (e: Exception) {
      "₹"
    }

  val currencyCode: String
    get() = try {
      Currency.getInstance(Locale.getDefault())?.currencyCode ?: "INR"
    } catch (e: Exception) {
      "INR"
    }
}

@Immutable
data class MonthlyFinancialSummary(
  val monthKey: String,
  val monthLabel: String,
  val totalIncome: Double = 0.0,
  val totalExpense: Double = 0.0,
  val savings: Double = 0.0, // Income - Expense
  val savingsRate: Double = 0.0, // (Savings / Income) * 100
  val budgetLimit: Double = 0.0,
  val budgetUsed: Double = 0.0, // Same as totalExpense
  val budgetRemaining: Double = 0.0, // budgetLimit - totalExpense
  val budgetUsagePercent: Double = 0.0,
  val transactionCount: Int = 0
)

@Immutable
data class YearlyFinancialSummary(
  val year: Int,
  val totalIncome: Double = 0.0,
  val totalExpense: Double = 0.0,
  val savings: Double = 0.0, // Total Income - Total Expense
  val savingsRate: Double = 0.0,
  val monthlyBreakdown: List<MonthlyFinancialSummary> = emptyList()
)

@Immutable
data class CategorySpending(
  val category: TransactionCategory,
  val amount: Double,
  val percentage: Float,
  val count: Int,
  val color: Color
)

@Immutable
data class DailySpendingPoint(
  val dayOfMonth: Int,
  val dateKey: String, // "YYYY-MM-DD"
  val dayLabel: String,
  val expense: Double,
  val income: Double,
  val net: Double
)

@Immutable
data class CalendarDayData(
  val dayOfMonth: Int,
  val dateKey: String, // "YYYY-MM-DD"
  val epochMillis: Long,
  val isCurrentMonth: Boolean,
  val isToday: Boolean,
  val hasIncome: Boolean,
  val hasExpense: Boolean,
  val totalIncome: Double,
  val totalExpense: Double,
  val transactions: List<TransactionItem>
)

enum class ExportPeriod(val displayName: String) {
  CURRENT_MONTH("Particular Month"),
  SELECTED_YEAR("Particular Year"),
  CUSTOM_RANGE("Custom Date Range"),
  ALL_TIME("All Financial Records")
}

enum class ThemeMode(val displayName: String) {
  SYSTEM("System Default"),
  LIGHT("Light"),
  DARK("Dark (AMOLED)")
}

enum class OccurrenceStatus {
  DUE_TODAY,
  OVERDUE,
  UPCOMING,
  PAID
}

@Immutable
data class ScheduledRecurringOccurrence(
  val ruleId: Long,
  val ruleTitle: String,
  val amount: Double,
  val type: TransactionType,
  val category: TransactionCategory,
  val interval: RecurrenceInterval,
  val paymentMethod: PaymentMethod,
  val note: String,
  val scheduledDateKey: String, // "YYYY-MM-DD"
  val scheduledEpochMillis: Long,
  val status: OccurrenceStatus,
  val daysDiff: Int, // 0 = today, < 0 = overdue, > 0 = upcoming
  val relativeLabel: String,
  val isPaid: Boolean = false,
  val paidTransactionId: Long? = null
)

@Immutable
data class CategoryDetailData(
  val category: TransactionCategory,
  val monthKey: String,
  val monthLabel: String,
  val totalExpense: Double = 0.0,
  val totalIncome: Double = 0.0,
  val netAmount: Double = 0.0,
  val transactionCount: Int = 0,
  val transactions: List<TransactionItem> = emptyList()
)

enum class RecommendationSeverity {
  INFO,
  WARNING,
  SUCCESS,
  ALERT
}

@Immutable
data class FinancialRecommendation(
  val id: String,
  val title: String,
  val message: String,
  val severity: RecommendationSeverity = RecommendationSeverity.INFO,
  val category: TransactionCategory? = null,
  val actionLabel: String? = null
)

enum class SecureNoteType(val displayName: String) {
  GENERIC("Secure Note"),
  CREDIT_DEBIT_CARD("Card Details"),
  BANK_ACCOUNT("Bank Account"),
  CREDENTIAL("Credential / PIN")
}

@Immutable
data class SecureNote(
  val id: Long = 0,
  val title: String,
  val content: String = "",
  val type: SecureNoteType = SecureNoteType.GENERIC,
  val maskedNumber: String? = null,
  val expiryDate: String? = null,
  val cvv: String? = null,
  val ifscCode: String? = null,
  val accountNumber: String? = null,
  val additionalFields: Map<String, String> = emptyMap(),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
data class DriveStorageInfo(
  val totalBytes: Long = 0L,
  val usedBytes: Long = 0L,
  val availableBytes: Long = 0L,
  val formattedSummary: String = "Storage information unavailable",
  val isAvailable: Boolean = false
)


