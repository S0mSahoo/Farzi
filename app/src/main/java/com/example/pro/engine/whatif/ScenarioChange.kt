package com.example.pro.engine.whatif

import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import java.time.LocalDate

sealed class ScenarioChange {
    data class OneTimeExpense(
        val amount: Double,
        val date: LocalDate,
        val category: TransactionCategory = TransactionCategory.OTHER_EXPENSE
    ) : ScenarioChange()

    data class RecurringExpenseChange(
        val ruleId: Long,
        val newAmount: Double,
        val effectiveDate: LocalDate
    ) : ScenarioChange()

    data class RecurringExpenseRemoval(
        val ruleId: Long,
        val effectiveDate: LocalDate
    ) : ScenarioChange()

    data class IncomeChange(
        val isPercentage: Boolean,
        val amountOrPercentage: Double,
        val effectiveDate: LocalDate
    ) : ScenarioChange()

    data class SavingsAdjustment(
        val monthlyTarget: Double
    ) : ScenarioChange()
}
