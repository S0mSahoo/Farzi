package com.example.ui.viewmodel

import com.example.data.model.BudgetModel
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiAdvisorPresentationTest {
  @Test
  fun greetingAndSuggestionsReflectRealFinancialContext() {
    val viewModel = AiAdvisorViewModel()
    val transaction = TransactionItem(
      id = 1L,
      title = "Grocery run",
      amount = 1250.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.GROCERIES,
      timestamp = System.currentTimeMillis(),
      note = "",
      paymentMethod = PaymentMethod.UPI
    )

    val method = AiAdvisorViewModel::class.java.getDeclaredMethod(
      "buildAssistantPresentation",
      List::class.java,
      List::class.java,
      List::class.java,
      String::class.java
    )
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val presentation = method.invoke(
      viewModel,
      listOf(transaction),
      emptyList<BudgetModel>(),
      emptyList<RecurringRule>(),
      "Som Sahoo"
    ) as Pair<String, List<ChatSuggestion>>

    assertTrue(
      "Greeting should use the supplied profile name",
      presentation.first.contains("Som")
    )
    assertTrue(
      "Greeting must remain contextual rather than claiming a fabricated snapshot",
      !presentation.first.contains("snapshot", ignoreCase = true)
    )
    assertTrue(
      "Suggestions should include the current month's top spending category",
      presentation.second.any { it.label == "Review Groceries" }
    )
    assertTrue(
      "Without a budget, suggest budget planning rather than showing invented amounts",
      presentation.second.any { it.id == "budget" && it.prompt.contains("monthly budget") }
    )
    assertTrue(
      "Suggestions must not contain hardcoded example transaction amounts",
      presentation.second.none { it.label.contains("₹45") || it.label.contains("₹3000") || it.label.contains("₹1500") }
    )
    assertTrue(
      "Suggestion count should remain compact",
      presentation.second.size in 1..4
    )
  }
}
