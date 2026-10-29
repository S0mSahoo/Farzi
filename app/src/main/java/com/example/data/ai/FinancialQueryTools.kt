package com.example.data.ai

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool

/**
 * Definition of read-only financial query tools for Paisa AI.
 * These tools provide structured, scoped read operations against the local database
 * without transferring complete unneeded database snapshots to the model.
 */
object FinancialQueryTools {

  val getFinancialSummaryDeclaration = FunctionDeclaration(
    name = "getFinancialSummary",
    description = "Retrieve the financial summary (total income, expenses, net savings, savings rate, budget usage, and insights) for a specified calendar month (format 'YYYY-MM') or year (format 'YYYY').",
    parameters = mapOf(
      "periodType" to Schema.enumeration(
        values = listOf("MONTHLY", "YEARLY"),
        description = "Whether to query a 'MONTHLY' or 'YEARLY' summary."
      ),
      "period" to Schema.string(
        description = "The target period: 'YYYY-MM' for monthly (e.g. '2026-08') or 'YYYY' for yearly (e.g. '2026')."
      )
    ),
    optionalParameters = emptyList()
  )

  val comparePeriodsDeclaration = FunctionDeclaration(
    name = "comparePeriods",
    description = "Compare financial totals (income, expenses, net savings, cash flow difference) between two distinct calendar months or years.",
    parameters = mapOf(
      "periodType" to Schema.enumeration(
        values = listOf("MONTHLY", "YEARLY"),
        description = "The period type to compare: 'MONTHLY' or 'YEARLY'."
      ),
      "period1" to Schema.string(
        description = "First period identifier: 'YYYY-MM' or 'YYYY' (e.g. '2026-07')."
      ),
      "period2" to Schema.string(
        description = "Second period identifier: 'YYYY-MM' or 'YYYY' (e.g. '2026-08')."
      )
    ),
    optionalParameters = emptyList()
  )

  val getCategorySpendingDeclaration = FunctionDeclaration(
    name = "getCategorySpending",
    description = "Retrieve spending broken down by category for a specific date range. Includes amounts, transaction counts, and percentages.",
    parameters = mapOf(
      "startDate" to Schema.string(
        description = "Start date in format 'YYYY-MM-DD' or 'YYYY-MM' (inclusive)."
      ),
      "endDate" to Schema.string(
        description = "End date in format 'YYYY-MM-DD' or 'YYYY-MM' (inclusive)."
      ),
      "category" to Schema.string(
        description = "Optional category name filter (e.g. 'GROCERIES', 'FOOD_DINING', 'SHOPPING'). If omitted, returns all categories.",
        nullable = true
      ),
      "type" to Schema.enumeration(
        values = listOf("EXPENSE", "INCOME"),
        description = "Transaction type to analyze (default is 'EXPENSE')."
      )
    ),
    optionalParameters = listOf("category", "type")
  )

  val getBudgetStatusDeclaration = FunctionDeclaration(
    name = "getBudgetStatus",
    description = "Retrieve the budget limit, recorded expenses, remaining amount, and utilization percentage for a given month.",
    parameters = mapOf(
      "monthKey" to Schema.string(
        description = "Month key in format 'YYYY-MM' (e.g. '2026-08')."
      )
    ),
    optionalParameters = emptyList()
  )

  val getRecurringCommitmentsDeclaration = FunctionDeclaration(
    name = "getRecurringCommitments",
    description = "Retrieve active recurring commitments, regular bills, and subscriptions, along with their intervals and estimated monthly commitment totals.",
    parameters = mapOf(
      "type" to Schema.enumeration(
        values = listOf("EXPENSE", "INCOME", "ALL"),
        description = "Filter by commitment type: 'EXPENSE', 'INCOME', or 'ALL' (default is 'ALL')."
      )
    ),
    optionalParameters = listOf("type")
  )

  val searchTransactionsDeclaration = FunctionDeclaration(
    name = "searchTransactions",
    description = "Search recorded transactions by keyword in title, date range, type, or category. Returns bounded results (max 25) without private notes to protect user privacy.",
    parameters = mapOf(
      "query" to Schema.string(
        description = "Optional search keyword to match in transaction titles (case-insensitive).",
        nullable = true
      ),
      "startDate" to Schema.string(
        description = "Optional start date filter in format 'YYYY-MM-DD' or 'YYYY-MM'.",
        nullable = true
      ),
      "endDate" to Schema.string(
        description = "Optional end date filter in format 'YYYY-MM-DD' or 'YYYY-MM'.",
        nullable = true
      ),
      "category" to Schema.string(
        description = "Optional category name filter (e.g. 'GROCERIES', 'TRANSPORTATION').",
        nullable = true
      ),
      "type" to Schema.enumeration(
        values = listOf("EXPENSE", "INCOME", "ALL"),
        description = "Optional transaction type filter ('EXPENSE', 'INCOME', or 'ALL')."
      ),
      "limit" to Schema.integer(
        description = "Maximum number of transactions to return (1-25, default 15). Hard capped at 25."
      )
    ),
    optionalParameters = listOf("query", "startDate", "endDate", "category", "type", "limit")
  )

  /**
   * Bundled tool declaration passed to Firebase AI GenerativeModel.
   */
  val allTools: List<Tool> = listOf(
    Tool.googleSearch(),
    Tool.functionDeclarations(
      listOf(
        getFinancialSummaryDeclaration,
        comparePeriodsDeclaration,
        getCategorySpendingDeclaration,
        getBudgetStatusDeclaration,
        getRecurringCommitmentsDeclaration,
        searchTransactionsDeclaration
      )
    )
  )
}
