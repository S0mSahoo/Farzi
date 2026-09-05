package com.example.pro.ai

object IntentClassifier {
    fun classify(question: String): AiIntent {
        val q = question.lowercase().trim()
        return when {
            q.contains("what if") || q.contains("what happens if") || q.contains("if i spend") || q.contains("if i buy") -> AiIntent.WHAT_IF_QUERY
            q.contains("forecast") || q.contains("how much can i spend") || q.contains("rest of the month") || q.contains("projected") || q.contains("can i afford") -> AiIntent.FORECAST_QUERY
            q.contains("compare") || q.contains("vs") || q.contains("last month") -> AiIntent.COMPARISON_QUERY
            q.contains("recurring") || q.contains("subscription") || q.contains("commitments") || q.contains("fixed expense") -> AiIntent.RECURRING_ANALYSIS
            q.contains("saving") || q.contains("saved") || q.contains("save") -> AiIntent.SAVINGS_ANALYSIS
            q.contains("budget") || q.contains("limit") -> AiIntent.BUDGET_ANALYSIS
            q.contains("food") || q.contains("grocery") || q.contains("groceries") || q.contains("shopping") || q.contains("entertainment") || q.contains("category") || q.contains("categories") -> AiIntent.CATEGORY_ANALYSIS
            q.contains("where did my money go") || q.contains("why did i spend") || q.contains("spend") || q.contains("spent") || q.contains("expense") -> AiIntent.SPENDING_ANALYSIS
            q.contains("transaction") || q.contains("history") || q.contains("show me") -> AiIntent.TRANSACTION_LOOKUP
            else -> AiIntent.GENERAL_SUMMARY
        }
    }
}
