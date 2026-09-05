package com.example.pro.engine

import com.example.data.model.RecurringRule
import com.example.data.model.RecurrenceInterval
import com.example.data.model.TransactionType
import com.example.pro.engine.models.RecurringCommitmentSummary

class RecurringCommitmentAnalyzer {

    fun analyze(rules: List<RecurringRule>, totalMonthlyIncome: Double): RecurringCommitmentSummary {
        val activeRules = rules.filter { it.isActive }
        
        var totalMonthlyExpense = 0.0
        var totalMonthlyIncomeRecurring = 0.0
        var subscriptionCount = 0

        for (rule in activeRules) {
            val monthlyMultiplier = when (rule.interval) {
                RecurrenceInterval.DAILY -> 30.0
                RecurrenceInterval.WEEKLY -> 4.33
                RecurrenceInterval.MONTHLY -> 1.0
                RecurrenceInterval.YEARLY -> 1.0 / 12.0
            }
            val normalizedMonthlyAmount = rule.amount * monthlyMultiplier

            if (rule.type == TransactionType.EXPENSE) {
                totalMonthlyExpense += normalizedMonthlyAmount
                if (rule.category == com.example.data.model.TransactionCategory.SUBSCRIPTIONS) {
                    subscriptionCount++
                }
            } else if (rule.type == TransactionType.INCOME) {
                totalMonthlyIncomeRecurring += normalizedMonthlyAmount
            }
        }

        val ratio = FinancialCalculationUtils.safeRatio(totalMonthlyExpense, totalMonthlyIncome)

        return RecurringCommitmentSummary(
            totalMonthlyRecurringExpense = totalMonthlyExpense,
            totalMonthlyRecurringIncome = totalMonthlyIncomeRecurring,
            expenseToIncomeRatio = ratio,
            activeSubscriptionCount = subscriptionCount,
            upcomingObligationsCount = activeRules.size
        )
    }
}
