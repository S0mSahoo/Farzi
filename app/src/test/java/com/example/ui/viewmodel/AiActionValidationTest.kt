package com.example.ui.viewmodel

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiActionValidationTest {

    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var aiViewModel: AiAdvisorViewModel

    private fun flushCoroutines(condition: (() -> Boolean)? = null) {
        val deadline = System.currentTimeMillis() + 6000
        ShadowLooper.idleMainLooper()
        while (condition != null && !condition() && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(25)
            ShadowLooper.idleMainLooper()
        }
        ShadowLooper.idleMainLooper()
    }

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        financeViewModel = FinanceViewModel(app)
        aiViewModel = AiAdvisorViewModel()
        aiViewModel.bindFinanceViewModel(financeViewModel)
        flushCoroutines()
    }

    @Test
    fun testBlankTargetRejectedWithValidationError() {
        val initialCount = financeViewModel.allTransactions.value.size
        // Add a dummy transaction
        financeViewModel.addTransaction(
            title = "Groceries",
            amount = 50.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            timestamp = System.currentTimeMillis(),
            note = "",
            paymentMethod = PaymentMethod.UPI
        )
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "Groceries" } }

        // Model attempts to send UPDATE_TRANSACTION with blank targetQuery
        val rawAiResponse = """
            I can update that for you.
            ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "", "newAmount": 60.0}
        """.trimIndent()

        val processMethod = AiAdvisorViewModel::class.java.getDeclaredMethod("processAgenticAction", String::class.java)
        processMethod.isAccessible = true
        processMethod.invoke(aiViewModel, rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull("Pending group should be created", pendingGroup)
        assertNotNull("Validation error must be present for blank target", pendingGroup?.validationError)
        assertTrue("Error should mention blank target", pendingGroup?.validationError?.contains("blank") == true)
    }

    @Test
    fun testAmbiguousQueryMatchesRejectedWithValidationError() {
        val initialCount = financeViewModel.allTransactions.value.size
        // Add two transactions with matching title "Coffee"
        financeViewModel.addTransaction(
            title = "Starbucks Coffee",
            amount = 5.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FOOD_DINING,
            timestamp = System.currentTimeMillis(),
            note = "",
            paymentMethod = PaymentMethod.UPI
        )
        financeViewModel.addTransaction(
            title = "Local Cafe Coffee",
            amount = 4.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FOOD_DINING,
            timestamp = System.currentTimeMillis(),
            note = "",
            paymentMethod = PaymentMethod.CASH
        )
        flushCoroutines {
            financeViewModel.allTransactions.value.count { it.title.contains("Coffee") } >= 2
        }

        // Model attempts to delete "Coffee" without specifying unique target
        val rawAiResponse = """
            Deleting Coffee.
            ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "Coffee"}
        """.trimIndent()

        val processMethod = AiAdvisorViewModel::class.java.getDeclaredMethod("processAgenticAction", String::class.java)
        processMethod.isAccessible = true
        processMethod.invoke(aiViewModel, rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull("Pending group should be created", pendingGroup)
        assertNotNull("Validation error must be present for ambiguous query: ${pendingGroup?.validationError}", pendingGroup?.validationError)
        assertTrue("Error should mention ambiguous query: ${pendingGroup?.validationError}", pendingGroup?.validationError?.contains("Ambiguous") == true)
    }

    @Test
    fun testInvalidAmountsRejectedWithValidationError() {
        val rawAiResponse = """
            Logging expense.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Lunch", "amount": -15.0}
        """.trimIndent()

        val processMethod = AiAdvisorViewModel::class.java.getDeclaredMethod("processAgenticAction", String::class.java)
        processMethod.isAccessible = true
        processMethod.invoke(aiViewModel, rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingGroup)
        assertNotNull("Validation error must be present for negative amount", pendingGroup?.validationError)
        assertTrue("Error should mention amount", pendingGroup?.validationError?.contains("amount") == true)
    }

    @Test
    fun testDeleteAllRequiresExplicitConfirmation() {
        val initialCount = financeViewModel.allTransactions.value.size
        financeViewModel.addTransaction(
            title = "Test TX 1",
            amount = 10.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.OTHER_EXPENSE,
            timestamp = System.currentTimeMillis(),
            note = "",
            paymentMethod = PaymentMethod.UPI
        )
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "Test TX 1" } }

        val rawAiResponse = """
            Deleting all transactions.
            ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "ALL"}
        """.trimIndent()

        val processMethod = AiAdvisorViewModel::class.java.getDeclaredMethod("processAgenticAction", String::class.java)
        processMethod.isAccessible = true
        processMethod.invoke(aiViewModel, rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingGroup)
        assertNull("Valid delete all should not have validation error if transactions exist: ${pendingGroup?.validationError}", pendingGroup?.validationError)
        assertTrue("isDestructive must be true", pendingGroup?.isDestructive == true)
        assertTrue("requiresExplicitDeleteAllConfirmation must be true", pendingGroup?.requiresExplicitDeleteAllConfirmation == true)
    }

    @Test
    fun testConfirmAndExecutePendingActionMutatesState() {
        val initialCount = financeViewModel.allTransactions.value.size

        val rawAiResponse = """
            Logging salary.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "August Salary", "amount": 3000.0, "type": "INCOME", "category": "SALARY", "paymentMethod": "NET_BANKING"}
        """.trimIndent()

        val processMethod = AiAdvisorViewModel::class.java.getDeclaredMethod("processAgenticAction", String::class.java)
        processMethod.isAccessible = true
        processMethod.invoke(aiViewModel, rawAiResponse)

        val pendingGroupBefore = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingGroupBefore)
        assertNull(pendingGroupBefore?.validationError)

        // Explicitly confirm action
        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "August Salary" } }

        // After confirmation, pending group should be cleared and 1 transaction added
        assertNull("Pending group should be cleared after execution", aiViewModel.pendingActionGroup.value)
        assertEquals(initialCount + 1, financeViewModel.allTransactions.value.size)
        val addedTx = financeViewModel.allTransactions.value.first { it.title == "August Salary" }
        assertEquals(3000.0, addedTx.amount, 0.01)
        assertEquals(TransactionType.INCOME, addedTx.type)
    }

    @Test
    fun testCancelPendingActionDismissesStateWithoutMutating() {
        val initialCount = financeViewModel.allTransactions.value.size

        val rawAiResponse = """
            Logging expense.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Dinner", "amount": 25.0}
        """.trimIndent()

        val processMethod = AiAdvisorViewModel::class.java.getDeclaredMethod("processAgenticAction", String::class.java)
        processMethod.isAccessible = true
        processMethod.invoke(aiViewModel, rawAiResponse)

        assertNotNull(aiViewModel.pendingActionGroup.value)

        // User cancels pending action
        aiViewModel.cancelPendingAction()

        assertNull(aiViewModel.pendingActionGroup.value)
        assertEquals("Database must remain untouched after cancellation", initialCount, financeViewModel.allTransactions.value.size)
    }
}
