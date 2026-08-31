package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.model.MonthlySalarySettings
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepository(private val context: Context) {
  private val database = AppDatabase.getDatabase(context)
  private val dao = database.transactionDao()
  private val prefs: SharedPreferences = context.getSharedPreferences("daily_draft_prefs", Context.MODE_PRIVATE)

  val allTransactions: Flow<List<TransactionItem>> = dao.getAllTransactionsFlow().map { entities ->
    entities.map { it.toModel() }
  }

  suspend fun insertTransaction(item: TransactionItem): Long = withContext(Dispatchers.IO) {
    dao.insertTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun updateTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    dao.updateTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun deleteTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    dao.deleteTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
    dao.deleteById(id)
  }

  suspend fun clearAll() = withContext(Dispatchers.IO) {
    dao.clearAll()
  }

  fun getSalarySettings(): MonthlySalarySettings {
    val salary = prefs.getFloat("salary_amount", 3800.0f).toDouble()
    val payDay = prefs.getInt("pay_day", 1)
    val budget = prefs.getFloat("monthly_budget", 2200.0f).toDouble()
    val symbol = prefs.getString("currency_symbol", "$") ?: "$"
    return MonthlySalarySettings(
      salaryAmount = salary,
      payDayOfMonth = payDay,
      monthlyBudgetGoal = budget,
      currencySymbol = symbol
    )
  }

  fun saveSalarySettings(settings: MonthlySalarySettings) {
    prefs.edit()
      .putFloat("salary_amount", settings.salaryAmount.toFloat())
      .putInt("pay_day", settings.payDayOfMonth)
      .putFloat("monthly_budget", settings.monthlyBudgetGoal.toFloat())
      .putString("currency_symbol", settings.currencySymbol)
      .apply()
  }

  suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
    val count = dao.getCount()
    if (count == 0) {
      seedInitialDrafts()
    }
  }

  suspend fun seedInitialDrafts() = withContext(Dispatchers.IO) {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)
    val currentDay = now.get(Calendar.DAY_OF_MONTH)

    fun createTimestamp(day: Int, hour: Int = 12, min: Int = 0): Long {
      val cal = Calendar.getInstance()
      val targetDay = day.coerceIn(1, 28)
      cal.set(currentYear, currentMonth, targetDay, hour, min, 0)
      return cal.timeInMillis
    }

    val sampleList = mutableListOf<TransactionItem>()

    // Monthly Salary on Day 1
    sampleList.add(
      TransactionItem(
        title = "Monthly Base Salary",
        amount = 3800.0,
        type = TransactionType.SALARY,
        category = TransactionCategory.SALARY,
        timestamp = createTimestamp(1, 9, 0),
        note = "Direct deposit from company payroll",
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        isRecurring = true
      )
    )

    // Housing rent
    sampleList.add(
      TransactionItem(
        title = "Apartment Rent & Parking",
        amount = 950.0,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.HOUSING,
        timestamp = createTimestamp(2, 10, 30),
        note = "Monthly lease payment",
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        isRecurring = true
      )
    )

    // Freelance Project
    sampleList.add(
      TransactionItem(
        title = "UI/UX Mobile Redesign Client",
        amount = 750.0,
        type = TransactionType.INCOME,
        category = TransactionCategory.FREELANCE,
        timestamp = createTimestamp(5, 14, 0),
        note = "Milestone 2 completion",
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        isRecurring = false
      )
    )

    // Groceries
    sampleList.add(
      TransactionItem(
        title = "Whole Foods Organic Market",
        amount = 142.50,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.GROCERIES,
        timestamp = createTimestamp(6, 17, 45),
        note = "Weekly produce, dairy & pantry restock",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        isRecurring = false
      )
    )

    // Dining
    sampleList.add(
      TransactionItem(
        title = "Artisan Espresso & Brunch",
        amount = 34.20,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        timestamp = createTimestamp(8, 11, 15),
        note = "Weekend breakfast with friends",
        paymentMethod = PaymentMethod.UPI_WALLET,
        isRecurring = false
      )
    )

    // Utilities
    sampleList.add(
      TransactionItem(
        title = "High-speed Fiber Internet & Power",
        amount = 118.00,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.UTILITIES,
        timestamp = createTimestamp(10, 8, 30),
        note = "Monthly utility billing",
        paymentMethod = PaymentMethod.DEBIT_CARD,
        isRecurring = true
      )
    )

    // Transportation
    sampleList.add(
      TransactionItem(
        title = "Metro Pass & EV Charging",
        amount = 65.00,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.TRANSPORTATION,
        timestamp = createTimestamp(12, 16, 20),
        note = "Monthly transit reload",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        isRecurring = false
      )
    )

    // Shopping
    sampleList.add(
      TransactionItem(
        title = "Noise-Cancelling Earbuds",
        amount = 89.99,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.SHOPPING,
        timestamp = createTimestamp(15, 19, 10),
        note = "Sale discount replacement",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        isRecurring = false
      )
    )

    // Subscriptions
    sampleList.add(
      TransactionItem(
        title = "Cloud Storage & Music Streaming",
        amount = 24.99,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.SUBSCRIPTIONS,
        timestamp = createTimestamp(17, 9, 0),
        note = "Monthly digital services",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        isRecurring = true
      )
    )

    // Dining
    sampleList.add(
      TransactionItem(
        title = "Sushi Dinner Takeout",
        amount = 46.50,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        timestamp = createTimestamp(20, 20, 0),
        note = "Dinner after work",
        paymentMethod = PaymentMethod.UPI_WALLET,
        isRecurring = false
      )
    )

    // Dividends / Investment Income
    sampleList.add(
      TransactionItem(
        title = "Index Fund Quarterly Dividend",
        amount = 125.40,
        type = TransactionType.INCOME,
        category = TransactionCategory.INVESTMENTS,
        timestamp = createTimestamp(22, 10, 0),
        note = "Portfolio distribution",
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        isRecurring = false
      )
    )

    // Healthcare
    sampleList.add(
      TransactionItem(
        title = "Pharmacy & Vitamin Supplements",
        amount = 38.00,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.HEALTHCARE,
        timestamp = createTimestamp(24, 15, 30),
        note = "Monthly wellness supplies",
        paymentMethod = PaymentMethod.DEBIT_CARD,
        isRecurring = false
      )
    )

    // Entertainment
    sampleList.add(
      TransactionItem(
        title = "Cinema IMAX Tickets & Snacks",
        amount = 32.50,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.ENTERTAINMENT,
        timestamp = createTimestamp(26, 21, 15),
        note = "Sci-fi movie night",
        paymentMethod = PaymentMethod.UPI_WALLET,
        isRecurring = false
      )
    )

    // Today / Yesterday draft
    sampleList.add(
      TransactionItem(
        title = "Fresh Bakery & Matcha Latte",
        amount = 14.80,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        timestamp = createTimestamp(currentDay, 8, 45),
        note = "Morning fuel draft",
        paymentMethod = PaymentMethod.CASH,
        isRecurring = false
      )
    )

    val entities = sampleList.map { TransactionEntity.fromModel(it) }
    dao.insertAll(entities)
  }
}
