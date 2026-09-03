package com.example.ui.components

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

  fun getFriendlyRelativeDate(timestamp: Long): String {
    val now = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val target = Calendar.getInstance().apply {
      timeInMillis = timestamp
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val diffDays = ((target.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    return when {
      diffDays == 0 -> "Due Today"
      diffDays == 1 -> "Due Tomorrow"
      diffDays == -1 -> "Overdue by 1 day"
      diffDays < -1 -> "Overdue by ${-diffDays} days"
      diffDays in 2..7 -> "Due in $diffDays days"
      else -> "Due on ${displayDateFormat.format(Date(timestamp))}"
    }
  }

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
}
