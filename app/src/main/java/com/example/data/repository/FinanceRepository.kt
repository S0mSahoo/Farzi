package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.model.AppThemeMode
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

  companion object {
    private const val KEY_APP_INITIALIZED = "has_app_been_initialized_v1"
  }

  suspend fun insertTransaction(item: TransactionItem): Long = withContext(Dispatchers.IO) {
    markAppInitialized()
    dao.insertTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun updateTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    markAppInitialized()
    dao.updateTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun deleteTransaction(item: TransactionItem) = withContext(Dispatchers.IO) {
    markAppInitialized()
    dao.deleteTransaction(TransactionEntity.fromModel(item))
  }

  suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
    markAppInitialized()
    dao.deleteById(id)
  }

  suspend fun clearAll() = withContext(Dispatchers.IO) {
    markAppInitialized()
    dao.clearAll()
  }

  private fun markAppInitialized() {
    prefs.edit().putBoolean(KEY_APP_INITIALIZED, true).apply()
  }

  fun getThemeMode(): AppThemeMode {
    val modeStr = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
    return try {
      AppThemeMode.valueOf(modeStr)
    } catch (e: Exception) {
      AppThemeMode.SYSTEM
    }
  }

  fun saveThemeMode(mode: AppThemeMode) {
    prefs.edit().putString("theme_mode", mode.name).apply()
  }

  fun getSalarySettings(): MonthlySalarySettings {
    val salary = prefs.getFloat("salary_amount", 65000.0f).toDouble()
    val payDay = prefs.getInt("pay_day", 1)
    val budget = prefs.getFloat("monthly_budget", 35000.0f).toDouble()
    val symbol = prefs.getString("currency_symbol", "₹") ?: "₹"
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
    val isInitialized = prefs.getBoolean(KEY_APP_INITIALIZED, false)
    if (isInitialized) {
      // User has already initialized the app before (or cleared all drafts). Never auto-seed!
      return@withContext
    }

    val count = dao.getCount()
    if (count == 0) {
      seedInitialDrafts()
    }
    markAppInitialized()
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

    val sampleList = listOf(
      // 1. Base Salary
      TransactionItem(
        title = "Monthly Base Salary",
        amount = 65000.0,
        type = TransactionType.SALARY,
        category = TransactionCategory.SALARY,
        timestamp = createTimestamp(1, 9, 0),
        note = "Direct bank deposit from payroll",
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        isRecurring = true
      ),
      // 2. Rent
      TransactionItem(
        title = "Apartment Rent",
        amount = 14000.0,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.HOUSING,
        timestamp = createTimestamp(2, 10, 30),
        note = "Monthly lease payment via IMPS",
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        isRecurring = true
      ),
      // 3. Groceries
      TransactionItem(
        title = "Supermarket & Produce",
        amount = 2850.0,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.GROCERIES,
        timestamp = createTimestamp(currentDay.coerceAtLeast(3), 17, 30),
        note = "Weekly groceries & pantry essentials",
        paymentMethod = PaymentMethod.UPI,
        isRecurring = false
      ),
      // 4. Dining / Swiggy
      TransactionItem(
        title = "Swiggy Dinner Order",
        amount = 460.0,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        timestamp = createTimestamp(currentDay.coerceAtLeast(2), 20, 15),
        note = "Biryani & snacks with team",
        paymentMethod = PaymentMethod.UPI,
        isRecurring = false
      ),
      // 5. Chai / Daily Fuel
      TransactionItem(
        title = "Chai & Breakfast Snacks",
        amount = 120.0,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        timestamp = createTimestamp(currentDay, 8, 30),
        note = "Morning tea & snack",
        paymentMethod = PaymentMethod.UPI,
        isRecurring = false
      ),
      // 6. Bills & Utilities
      TransactionItem(
        title = "Electricity & WiFi Bill",
        amount = 1750.0,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.UTILITIES,
        timestamp = createTimestamp(5, 14, 0),
        note = "Broadband fiber & power bill",
        paymentMethod = PaymentMethod.UPI,
        isRecurring = true
      )
    )

    val entities = sampleList.map { TransactionEntity.fromModel(it) }
    dao.insertAll(entities)
  }
}
