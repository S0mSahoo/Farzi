package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.CategorySpending
import com.example.data.model.TransactionCategory

@Composable
fun BudgetProgressBar(
    budgetLimit: Double,
    budgetUsed: Double,
    currencySymbol: String
) {
    // Placeholder stub
}

@Composable
fun CategorySpendingBreakdown(
    categories: List<CategorySpending>,
    currencySymbol: String,
    onCategoryClick: (TransactionCategory) -> Unit
) {
    // Placeholder stub
}

@Composable
fun IncomeExpenseComparisonBar(
    income: Double,
    expense: Double,
    currencySymbol: String
) {
    // Placeholder stub
}
