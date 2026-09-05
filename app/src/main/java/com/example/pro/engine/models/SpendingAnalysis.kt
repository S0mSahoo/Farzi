package com.example.pro.engine.models

import com.example.data.model.TransactionCategory

data class SpendingAnalysis(
    val totalExpense: Double,
    val dailySpendingRate: Double,
    val categoryTrends: List<CategorySpendingTrend>,
    val highestCategory: TransactionCategory?,
    val highestCategorySpend: Double,
    val unusualSpendingDetected: Boolean,
    val unusualCategories: List<TransactionCategory>
)
