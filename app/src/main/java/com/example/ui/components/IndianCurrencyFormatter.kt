package com.example.ui.components

import com.example.data.model.CalendarDayData
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object IndianCurrencyFormatter {

  /**
   * Formats numbers according to Indian grouping (e.g. 1,00,000) or standard locale grouping
   */
  fun formatIndianNumber(amount: Double, includeDecimalsIfPresent: Boolean = true): String {
    val isNegative = amount < 0
    val absAmount = abs(amount)
    val locale = Locale.getDefault()
    val isIndianLocale = locale.country.equals("IN", ignoreCase = true) || locale.language.equals("hi", ignoreCase = true)

    if (!isIndianLocale) {
      val nf = NumberFormat.getNumberInstance(locale)
      if (includeDecimalsIfPresent && absAmount % 1.0 >= 0.005) {
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
      } else {
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
      }
      val res = nf.format(absAmount)
      return if (isNegative) "-$res" else res
    }

    val longPart = absAmount.toLong()
    val fractionPart = absAmount - longPart

    val longStr = longPart.toString()
    val formattedInt = if (longStr.length <= 3) {
      longStr
    } else {
      val lastThree = longStr.substring(longStr.length - 3)
      val remaining = longStr.substring(0, longStr.length - 3)
      val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
      "$grouped,$lastThree"
    }

    val finalStr = if (includeDecimalsIfPresent && fractionPart >= 0.005) {
      val dec = DecimalFormat(".00").format(fractionPart)
      "$formattedInt$dec"
    } else {
      formattedInt
    }

    return if (isNegative) "-$finalStr" else finalStr
  }

  fun formatWithSymbol(
    amount: Double,
    symbol: String = "₹",
    includeDecimalsIfPresent: Boolean = false,
    includeSign: Boolean = false
  ): String {
    val isNegative = amount < 0
    val isPositive = amount > 0
    val absVal = abs(amount)
    val formattedNum = formatIndianNumber(absVal, includeDecimalsIfPresent)

    return when {
      includeSign && isPositive -> "+$symbol$formattedNum"
      includeSign && isNegative -> "-$symbol$formattedNum"
      isNegative -> "-$symbol$formattedNum"
      else -> "$symbol$formattedNum"
    }
  }

  fun format(amount: Double, symbol: String = "₹"): String {
    return formatWithSymbol(amount, symbol)
  }

  fun formatCompact(amount: Double, symbol: String = "₹"): String {
    val isNegative = amount < 0
    val absVal = abs(amount)
    val locale = Locale.getDefault()
    val isIndianLocale = locale.country.equals("IN", ignoreCase = true) || locale.language.equals("hi", ignoreCase = true) || symbol == "₹"

    val str = if (isIndianLocale) {
      when {
        absVal >= 10_000_000 -> String.format(Locale.US, "%.2f Cr", absVal / 10_000_000.0)
        absVal >= 100_000 -> String.format(Locale.US, "%.2f L", absVal / 100_000.0)
        absVal >= 1_000 -> String.format(Locale.US, "%.1f k", absVal / 1_000.0)
        else -> formatIndianNumber(absVal, false)
      }
    } else {
      when {
        absVal >= 1_000_000_000 -> String.format(Locale.US, "%.2f B", absVal / 1_000_000_000.0)
        absVal >= 1_000_000 -> String.format(Locale.US, "%.2f M", absVal / 1_000_000.0)
        absVal >= 1_000 -> String.format(Locale.US, "%.1f k", absVal / 1_000.0)
        else -> formatIndianNumber(absVal, false)
      }
    }
    return if (isNegative) "-$symbol$str" else "$symbol$str"
  }
}

object DateUtils {
  private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
  private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
  private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
  private val dayOfWeekFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
  private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

  fun getMonthKey(cal: Calendar): String = monthKeyFormat.format(cal.time)
  fun getMonthKey(timestamp: Long): String = monthKeyFormat.format(Date(timestamp))

  fun getMonthLabel(cal: Calendar): String = monthFormat.format(cal.time)
  fun getMonthLabel(timestamp: Long): String = monthFormat.format(Date(timestamp))

  fun getDayKey(timestamp: Long): String = dayKeyFormat.format(Date(timestamp))
  fun getDayKey(cal: Calendar): String = dayKeyFormat.format(cal.time)

  fun getDisplayDate(timestamp: Long): String = displayDateFormat.format(Date(timestamp))
  fun getDayOfWeekLabel(timestamp: Long): String = dayOfWeekFormat.format(Date(timestamp))
  fun getFormattedTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

  fun getStartOfMonth(cal: Calendar): Long {
    val c = cal.clone() as Calendar
    c.set(Calendar.DAY_OF_MONTH, 1)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
  }

  fun getEndOfMonth(cal: Calendar): Long {
    val c = cal.clone() as Calendar
    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 999)
    return c.timeInMillis
  }

  fun getStartOfYear(year: Int): Long {
    val c = Calendar.getInstance()
    c.set(year, Calendar.JANUARY, 1, 0, 0, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
  }

  fun getEndOfYear(year: Int): Long {
    val c = Calendar.getInstance()
    c.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
    c.set(Calendar.MILLISECOND, 999)
    return c.timeInMillis
  }

  fun getStartOfDay(timestamp: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = timestamp
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
  }

  fun getEndOfDay(timestamp: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = timestamp
    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 999)
    return c.timeInMillis
  }

  fun buildMonthCalendarDays(cal: Calendar, transactions: List<TransactionItem>): List<CalendarDayData> {
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH)

    val workCal = Calendar.getInstance().apply {
      set(Calendar.YEAR, currentYear)
      set(Calendar.MONTH, currentMonth)
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val daysInMonth = workCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = workCal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday...
    val leadingEmptyDays = (firstDayOfWeek - Calendar.SUNDAY)

    val todayCal = Calendar.getInstance()
    val isCurrentYearAndMonth = todayCal.get(Calendar.YEAR) == currentYear && todayCal.get(Calendar.MONTH) == currentMonth
    val todayDay = if (isCurrentYearAndMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1

    val days = mutableListOf<CalendarDayData>()

    // Leading days from previous month
    if (leadingEmptyDays > 0) {
      val prevCal = (workCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
      val prevMax = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
      for (i in (prevMax - leadingEmptyDays + 1)..prevMax) {
        val dayMillis = (prevCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, i) }.timeInMillis
        days.add(
          CalendarDayData(
            dayOfMonth = i,
            dateKey = getDayKey(dayMillis),
            epochMillis = dayMillis,
            isCurrentMonth = false,
            isToday = false,
            hasIncome = false,
            hasExpense = false,
            totalIncome = 0.0,
            totalExpense = 0.0,
            transactions = emptyList()
          )
        )
      }
    }

    // Days in current month
    for (day in 1..daysInMonth) {
      val dayCal = (workCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
      val startOfDay = getStartOfDay(dayCal.timeInMillis)
      val endOfDay = getEndOfDay(dayCal.timeInMillis)

      val dayTxs = transactions.filter { it.timestamp in startOfDay..endOfDay }
      val dayIncome = dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
      val dayExpense = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

      days.add(
        CalendarDayData(
          dayOfMonth = day,
          dateKey = getDayKey(dayCal.timeInMillis),
          epochMillis = dayCal.timeInMillis,
          isCurrentMonth = true,
          isToday = (day == todayDay),
          hasIncome = dayIncome > 0,
          hasExpense = dayExpense > 0,
          totalIncome = dayIncome,
          totalExpense = dayExpense,
          transactions = dayTxs
        )
      )
    }

    // Trailing days to complete the 7-column grid
    val totalCells = days.size
    val remainingCells = if (totalCells % 7 == 0) 0 else 7 - (totalCells % 7)
    if (remainingCells > 0) {
      val nextCal = (workCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
      for (day in 1..remainingCells) {
        val dayMillis = (nextCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
        days.add(
          CalendarDayData(
            dayOfMonth = day,
            dateKey = getDayKey(dayMillis),
            epochMillis = dayMillis,
            isCurrentMonth = false,
            isToday = false,
            hasIncome = false,
            hasExpense = false,
            totalIncome = 0.0,
            totalExpense = 0.0,
            transactions = emptyList()
          )
        )
      }
    }

    return days
  }
}
