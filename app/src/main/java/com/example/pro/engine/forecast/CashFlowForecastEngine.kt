package com.example.pro.engine.forecast

import com.example.data.local.PaidRecurringOccurrenceEntity
import com.example.data.model.BudgetModel
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.pro.engine.FinancialCalculationUtils
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CashFlowForecastEngine {

    fun calculatePeriod(yearMonth: YearMonth, referenceDate: LocalDate = LocalDate.now()): ForecastPeriod {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val totalDays = yearMonth.lengthOfMonth()

        val today = if (referenceDate.year == yearMonth.year && referenceDate.month == yearMonth.month) {
            referenceDate.coerceIn(startDate, endDate)
        } else if (referenceDate.isBefore(startDate)) {
            startDate // If forecasting a future month
        } else {
            endDate // If forecasting a past month
        }

        val elapsedDays = if (referenceDate.isBefore(startDate)) {
            0
        } else if (referenceDate.isAfter(endDate)) {
            totalDays
        } else {
            today.dayOfMonth
        }

        val remainingDays = (totalDays - elapsedDays).coerceAtLeast(0)

        return ForecastPeriod(
            yearMonth = yearMonth,
            startDate = startDate,
            endDate = endDate,
            today = today,
            totalDaysInPeriod = totalDays,
            elapsedDays = elapsedDays,
            remainingDays = remainingDays
        )
    }

    private fun getNextOccurrence(currentDateMillis: Long, interval: RecurrenceInterval): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentDateMillis
        when (interval) {
            RecurrenceInterval.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            RecurrenceInterval.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
            RecurrenceInterval.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceInterval.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun generateForecast(
        allTransactions: List<TransactionItem>,
        recurringRules: List<RecurringRule>,
        paidOccurrences: List<PaidRecurringOccurrenceEntity>,
        activeBudget: BudgetModel?,
        yearMonth: YearMonth,
        referenceDate: LocalDate = LocalDate.now()
    ): CashFlowForecastResult {
        val period = calculatePeriod(yearMonth, referenceDate)

        // 1. Starting position (derived cumulative net balance across all recorded history)
        val totalHistInc = allTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalHistExp = allTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val startingPosition = totalHistInc - totalHistExp

        // 2. Actuals (transactions in this period up to today)
        val periodTransactions = allTransactions.filter {
            FinancialCalculationUtils.isTimestampInYearMonth(it.timestamp, yearMonth)
        }
        val actualTransactionsToDate = periodTransactions.filter {
            val date = FinancialCalculationUtils.millisToLocalDate(it.timestamp)
            !date.isAfter(period.today)
        }

        val actualIncome = actualTransactionsToDate.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val actualExpenses = actualTransactionsToDate.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val actualNet = actualIncome - actualExpenses

        // 3. Known future items (recurring rules scheduled for the remaining days of the period, not yet paid)
        var knownFutureIncome = 0.0
        var knownFutureExpenses = 0.0
        val lineItems = mutableListOf<ForecastLineItem>()

        // Add actual line items
        if (actualIncome > 0.0) {
            lineItems.add(ForecastLineItem("Actual Income to Date", actualIncome, ForecastCategoryType.ACTUAL, "Recorded income up to ${period.today}"))
        }
        if (actualExpenses > 0.0) {
            lineItems.add(ForecastLineItem("Actual Expenses to Date", actualExpenses, ForecastCategoryType.ACTUAL, "Recorded expenses up to ${period.today}"))
        }

        val activeRules = recurringRules.filter { it.isActive }
        val paidSet = paidOccurrences.filter { !it.isCancelled }.map { "${it.ruleId}_${it.occurrenceDateKey}" }.toSet()

        val endOfPeriodMillis = period.endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayMillis = period.today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        for (rule in activeRules) {
            var current = rule.startDate
            val maxLimit = endOfPeriodMillis

            while (current <= maxLimit && (rule.endDate == null || current <= rule.endDate)) {
                if (current > todayMillis && FinancialCalculationUtils.isTimestampInYearMonth(current, yearMonth)) {
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(current))
                    val occurrenceKey = "${rule.id}_$dateKey"
                    val isPaidOrCancelled = paidSet.contains(occurrenceKey) || paidOccurrences.any { it.ruleId == rule.id && it.occurrenceDateKey == dateKey && it.isCancelled }

                    if (!isPaidOrCancelled) {
                        if (rule.type == TransactionType.INCOME) {
                            knownFutureIncome += rule.amount
                            lineItems.add(ForecastLineItem("Known Income: ${rule.title}", rule.amount, ForecastCategoryType.KNOWN_FUTURE, "Scheduled on $dateKey"))
                        } else {
                            knownFutureExpenses += rule.amount
                            lineItems.add(ForecastLineItem("Known Expense: ${rule.title}", rule.amount, ForecastCategoryType.KNOWN_FUTURE, "Scheduled on $dateKey"))
                        }
                    }
                }
                current = getNextOccurrence(current, rule.interval)
            }
        }

        // 4. Estimated remaining discretionary spending
        val historicalMonthlySpends = allTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { YearMonth.from(FinancialCalculationUtils.millisToLocalDate(it.timestamp)) }
            .map { it.value.sumOf { tx -> tx.amount } }

        val baselineMonthlySpend = if (historicalMonthlySpends.isNotEmpty()) {
            historicalMonthlySpends.average()
        } else {
            actualExpenses * (period.totalDaysInPeriod.toDouble() / period.elapsedDays.coerceAtLeast(1).toDouble())
        }

        val dailyRunRate = if (period.elapsedDays > 0) {
            (actualExpenses / period.elapsedDays.toDouble())
        } else {
            baselineMonthlySpend / period.totalDaysInPeriod.toDouble()
        }

        val estimatedRemainingExpenses = (dailyRunRate * period.remainingDays).coerceAtLeast(0.0)
        if (estimatedRemainingExpenses > 0.0) {
            lineItems.add(ForecastLineItem("Estimated Discretionary Remaining", estimatedRemainingExpenses, ForecastCategoryType.ESTIMATED_REMAINING, "Projected for ${period.remainingDays} remaining days based on daily run-rate"))
        }

        val estimatedRemainingIncome = 0.0

        // 5. Totals & Projections
        val projectedTotalIncome = actualIncome + knownFutureIncome + estimatedRemainingIncome
        val projectedTotalExpenses = actualExpenses + knownFutureExpenses + estimatedRemainingExpenses
        val projectedEndPeriodPosition = startingPosition + (projectedTotalIncome - projectedTotalExpenses)
        val projectedSavings = projectedTotalIncome - projectedTotalExpenses
        val projectedSavingsRate = FinancialCalculationUtils.safeRatio(projectedSavings, projectedTotalIncome)

        // 6. Confidence calculation
        val historyMonthsCount = historicalMonthlySpends.size
        val confidence = when {
            historyMonthsCount >= 3 && period.elapsedDays >= 10 -> ForecastConfidence.HIGH
            historyMonthsCount >= 1 && period.elapsedDays >= 5 -> ForecastConfidence.MEDIUM
            historyMonthsCount >= 1 -> ForecastConfidence.LOW
            else -> ForecastConfidence.INSUFFICIENT_DATA
        }
        val confidenceReason = when (confidence) {
            ForecastConfidence.HIGH -> "Based on $historyMonthsCount months of transaction history and stable period progression."
            ForecastConfidence.MEDIUM -> "Based on limited history ($historyMonthsCount months) or early period progression."
            ForecastConfidence.LOW -> "Based on sparse transaction records; estimates carry higher uncertainty."
            ForecastConfidence.INSUFFICIENT_DATA -> "Insufficient historical transaction data to generate a reliable forecast."
        }

        // 7. Warnings generation
        val warnings = mutableListOf<String>()
        if (projectedEndPeriodPosition < 0.0) {
            warnings.add("Projected end-of-period position is negative (₹${String.format(Locale.getDefault(), "%.2f", projectedEndPeriodPosition)}).")
        }
        if (activeBudget != null && activeBudget.totalBudget > 0.0) {
            val projectedBudgetUsage = projectedTotalExpenses / activeBudget.totalBudget
            if (projectedBudgetUsage > 1.0) {
                warnings.add("Projected total expenses (₹${String.format(Locale.getDefault(), "%.2f", projectedTotalExpenses)}) exceed active budget limit (₹${String.format(Locale.getDefault(), "%.2f", activeBudget.totalBudget)}).")
            }
        }
        if (knownFutureExpenses > actualIncome + knownFutureIncome) {
            warnings.add("Known future recurring expenses exceed projected total income for the remainder of the period.")
        }

        return CashFlowForecastResult(
            periodKey = yearMonth.toString(),
            period = period,
            startingPosition = startingPosition,
            actualIncome = actualIncome,
            actualExpenses = actualExpenses,
            actualNet = actualNet,
            knownFutureIncome = knownFutureIncome,
            knownFutureExpenses = knownFutureExpenses,
            estimatedRemainingExpenses = estimatedRemainingExpenses,
            estimatedRemainingIncome = estimatedRemainingIncome,
            projectedTotalIncome = projectedTotalIncome,
            projectedTotalExpenses = projectedTotalExpenses,
            projectedEndPeriodPosition = projectedEndPeriodPosition,
            projectedSavings = projectedSavings,
            projectedSavingsRate = projectedSavingsRate,
            confidence = confidence,
            confidenceReason = confidenceReason,
            lineItems = lineItems,
            warnings = warnings
        )
    }
}
