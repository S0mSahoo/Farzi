package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    private fun invokeProcessAgenticAction(rawText: String) {
        val processMethod = AiAdvisorViewModel::class.java.getDeclaredMethod("processAgenticAction", String::class.java)
        processMethod.isAccessible = true
        processMethod.invoke(aiViewModel, rawText)
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

        val rawAiResponse = """
            I can update that for you.
            ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "", "newAmount": 60.0}
        """.trimIndent()

        invokeProcessAgenticAction(rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull("Pending group should be created", pendingGroup)
        assertNotNull("Validation error must be present for blank target", pendingGroup?.validationError)
        assertTrue("Error should mention blank target", pendingGroup?.validationError?.contains("blank") == true)
    }

    @Test
    fun testAmbiguousQueryMatchesRejectedWithValidationError() {
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

        val rawAiResponse = """
            Deleting Coffee.
            ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "Coffee"}
        """.trimIndent()

        invokeProcessAgenticAction(rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull("Pending group should be created", pendingGroup)
        assertNotNull("Validation error must be present for ambiguous query", pendingGroup?.validationError)
        assertTrue("Error should mention ambiguous query", pendingGroup?.validationError?.contains("Ambiguous") == true)
    }

    @Test
    fun testInvalidAmountsRejectedWithValidationError() {
        val rawAiResponse = """
            Logging expense.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Lunch", "amount": -15.0, "type": "EXPENSE", "category": "FOOD_DINING", "paymentMethod": "CASH"}
        """.trimIndent()

        invokeProcessAgenticAction(rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingGroup)
        assertNotNull("Validation error must be present for negative amount", pendingGroup?.validationError)
        assertTrue("Error should mention amount", pendingGroup?.validationError?.contains("amount") == true)
    }

    @Test
    fun testInvalidDatesAndMonthOnlyDates() {
        // Malformed explicit date
        val rawInvalidDateResponse = """
            Logging expense with bad date.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Dinner", "amount": 25.0, "type": "EXPENSE", "category": "FOOD_DINING", "paymentMethod": "CASH", "date": "not-a-valid-date"}
        """.trimIndent()

        invokeProcessAgenticAction(rawInvalidDateResponse)
        val badDateGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(badDateGroup)
        assertNotNull("Validation error must be present for malformed date", badDateGroup?.validationError)
        assertTrue(badDateGroup?.validationError?.contains("Invalid date format") == true)

        aiViewModel.cancelPendingAction()

        // Impossible calendar date Feb 30
        val rawFeb30Response = """
            Logging expense for Feb 30.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Dinner", "amount": 25.0, "type": "EXPENSE", "category": "FOOD_DINING", "paymentMethod": "CASH", "date": "2026-02-30"}
        """.trimIndent()

        invokeProcessAgenticAction(rawFeb30Response)
        val feb30Group = aiViewModel.pendingActionGroup.value
        assertNotNull(feb30Group)
        assertNotNull("Validation error must be present for impossible calendar date", feb30Group?.validationError)
        assertTrue(feb30Group?.validationError?.contains("Invalid date format") == true)

        aiViewModel.cancelPendingAction()

        // Valid month-only date "2026-08" -> should parse to 2026-08-01 12:00:00
        val rawMonthOnlyResponse = """
            Logging expense for August 2026.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Rent Deposit", "amount": 500.0, "type": "EXPENSE", "category": "HOUSING", "paymentMethod": "NET_BANKING", "date": "2026-08"}
        """.trimIndent()

        invokeProcessAgenticAction(rawMonthOnlyResponse)
        val monthGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(monthGroup)
        assertNull("Month-only date should be accepted without validation error", monthGroup?.validationError)

        val logData = monthGroup?.actionItems?.first()?.data as ProposedActionData.LogTransactionData
        val cal = Calendar.getInstance().apply { timeInMillis = logData.timestamp }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, cal.get(Calendar.MONTH))
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testInvalidCategoryPaymentMethodAndInterval() {
        // Unknown Category
        val rawBadCategory = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Crypto", "amount": 100.0, "type": "EXPENSE", "category": "CRYPTOCURRENCY", "paymentMethod": "CASH"}
        """.trimIndent()
        invokeProcessAgenticAction(rawBadCategory)
        val badCatGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(badCatGroup?.validationError)
        assertTrue(badCatGroup?.validationError?.contains("Unknown category") == true)

        aiViewModel.cancelPendingAction()

        // Incompatible Category (SALARY for EXPENSE transaction)
        val rawIncompatibleCategory = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Incompatible", "amount": 100.0, "type": "EXPENSE", "category": "SALARY", "paymentMethod": "CASH"}
        """.trimIndent()
        invokeProcessAgenticAction(rawIncompatibleCategory)
        val incompGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(incompGroup?.validationError)
        assertTrue(incompGroup?.validationError?.contains("not valid for Expense") == true)

        aiViewModel.cancelPendingAction()

        // Unknown Payment Method
        val rawBadPm = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Gold", "amount": 100.0, "type": "EXPENSE", "category": "SHOPPING", "paymentMethod": "GOLD_COINS"}
        """.trimIndent()
        invokeProcessAgenticAction(rawBadPm)
        val badPmGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(badPmGroup?.validationError)
        assertTrue(badPmGroup?.validationError?.contains("Unknown payment method") == true)

        aiViewModel.cancelPendingAction()

        // Unknown Recurrence Interval
        val rawBadInterval = """
            ACTION_JSON: {"action": "LOG_RECURRING", "title": "Gym", "amount": 30.0, "type": "EXPENSE", "category": "FITNESS", "interval": "BIWEEKLY", "paymentMethod": "CREDIT_CARD"}
        """.trimIndent()
        invokeProcessAgenticAction(rawBadInterval)
        val badIntervalGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(badIntervalGroup?.validationError)
        assertTrue(badIntervalGroup?.validationError?.contains("Unknown recurrence interval") == true)
    }

    @Test
    fun testUnknownTransactionTypeAndMissingRequiredFields() {
        // Unknown transaction type
        val rawBadType = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Stock", "amount": 100.0, "type": "CRYPTO", "category": "INVESTMENTS", "paymentMethod": "CASH"}
        """.trimIndent()
        invokeProcessAgenticAction(rawBadType)
        val badTypeGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(badTypeGroup?.validationError)
        assertTrue(badTypeGroup?.validationError?.contains("Unknown transaction type") == true)

        aiViewModel.cancelPendingAction()

        // Blank title
        val rawBlankTitle = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "   ", "amount": 100.0, "type": "EXPENSE", "category": "GROCERIES", "paymentMethod": "CASH"}
        """.trimIndent()
        invokeProcessAgenticAction(rawBlankTitle)
        val blankTitleGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(blankTitleGroup?.validationError)
        assertTrue(blankTitleGroup?.validationError?.contains("title cannot be blank") == true)

        aiViewModel.cancelPendingAction()

        // Missing required category
        val rawMissingCat = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Lunch", "amount": 15.0, "type": "EXPENSE", "paymentMethod": "CASH"}
        """.trimIndent()
        invokeProcessAgenticAction(rawMissingCat)
        val missingCatGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(missingCatGroup?.validationError)
        assertTrue(missingCatGroup?.validationError?.contains("Category is required") == true)
    }

    @Test
    fun testTypeChangeIncompatibleWithCategoryOnUpdate() {
        financeViewModel.addTransaction(
            title = "Supermarket",
            amount = 80.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            timestamp = System.currentTimeMillis(),
            note = "",
            paymentMethod = PaymentMethod.CREDIT_CARD
        )
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "Supermarket" } }

        // Change type to INCOME without providing newCategory (Groceries is EXPENSE-only)
        val rawUpdateTypeResponse = """
            ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "Supermarket", "newType": "INCOME"}
        """.trimIndent()

        invokeProcessAgenticAction(rawUpdateTypeResponse)
        val group = aiViewModel.pendingActionGroup.value
        assertNotNull(group)
        assertNotNull("Incompatible category for new type must cause validation error", group?.validationError)
        assertTrue(group?.validationError?.contains("not valid for Income") == true)
    }

    @Test
    fun testNormalizedDateMatchingForUpdateAndDelete() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetDateStr = "2026-08-15"
        val targetTime = sdf.parse(targetDateStr)!!.time

        financeViewModel.addTransaction(
            title = "Target Date Tx",
            amount = 45.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FOOD_DINING,
            timestamp = targetTime,
            note = "Special lunch",
            paymentMethod = PaymentMethod.UPI
        )
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "Target Date Tx" } }

        // Query with slash format "2026/08/15" -> should match normalized ISO day
        val rawSlashQuery = """
            ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "Target Date Tx", "targetDate": "2026/08/15", "newAmount": 55.0}
        """.trimIndent()

        invokeProcessAgenticAction(rawSlashQuery)
        val slashGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(slashGroup)
        assertNull("Slash date '2026/08/15' should normalize and match successfully", slashGroup?.validationError)

        aiViewModel.cancelPendingAction()

        // Query with month name "August 2026" -> should match normalized month key
        val rawMonthNameQuery = """
            ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "Target Date Tx", "targetDate": "August 2026", "newAmount": 65.0}
        """.trimIndent()

        invokeProcessAgenticAction(rawMonthNameQuery)
        val monthGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(monthGroup)
        assertNull("Month name 'August 2026' should normalize and match successfully", monthGroup?.validationError)
    }

    @Test
    fun testMissingOptionalUpdateFieldsPreserveOriginalValues() {
        val origTime = System.currentTimeMillis() - 86400000L
        financeViewModel.addTransaction(
            title = "Original Title",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            timestamp = origTime,
            note = "Original Note",
            paymentMethod = PaymentMethod.CREDIT_CARD
        )
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "Original Title" } }

        // Update ONLY newAmount
        val rawUpdateResponse = """
            ACTION_JSON: {"action": "UPDATE_TRANSACTION", "targetQuery": "Original Title", "newAmount": 150.0}
        """.trimIndent()

        invokeProcessAgenticAction(rawUpdateResponse)
        val updateGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(updateGroup)
        assertNull(updateGroup?.validationError)

        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines { financeViewModel.allTransactions.value.any { it.amount == 150.0 } }

        val updatedTx = financeViewModel.allTransactions.value.first { it.amount == 150.0 }
        assertEquals("Original Title", updatedTx.title)
        assertEquals(TransactionCategory.GROCERIES, updatedTx.category)
        assertEquals(PaymentMethod.CREDIT_CARD, updatedTx.paymentMethod)
        assertEquals(origTime, updatedTx.timestamp)
        assertEquals("Original Note", updatedTx.note)
    }

    @Test
    fun testProfileCurrencyPersistence() {
        val rawProfileResponse = """
            ACTION_JSON: {"action": "UPDATE_PROFILE", "name": "Siddharth", "currencySymbol": "$"}
        """.trimIndent()

        invokeProcessAgenticAction(rawProfileResponse)
        val profileGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(profileGroup)
        assertNull(profileGroup?.validationError)

        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines { financeViewModel.userProfile.value.name == "Siddharth" }

        val updatedProfile = financeViewModel.userProfile.value
        assertEquals("Siddharth", updatedProfile.name)
        assertEquals("$", updatedProfile.currencySymbol)
    }

    @Test
    fun testNoMutationsOnValidationError() {
        val initialTxCount = financeViewModel.allTransactions.value.size

        val rawBadAction = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Bad Tx", "amount": -50.0, "type": "EXPENSE", "category": "FOOD_DINING", "paymentMethod": "CASH"}
        """.trimIndent()

        invokeProcessAgenticAction(rawBadAction)
        val group = aiViewModel.pendingActionGroup.value
        assertNotNull(group)
        assertNotNull(group?.validationError)

        // Attempting to confirm with a validation error must not mutate data
        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines()

        assertEquals(initialTxCount, financeViewModel.allTransactions.value.size)
    }

    @Test
    fun testDeleteAllRequiresExplicitConfirmation() {
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

        invokeProcessAgenticAction(rawAiResponse)

        val pendingGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingGroup)
        assertNull("Valid delete all should not have validation error if transactions exist", pendingGroup?.validationError)
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

        invokeProcessAgenticAction(rawAiResponse)

        val pendingGroupBefore = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingGroupBefore)
        assertNull(pendingGroupBefore?.validationError)

        aiViewModel.confirmAndExecutePendingAction()
        
        // Wait for processing to start AND finish
        flushCoroutines { aiViewModel.isProcessingAction.value }
        flushCoroutines { !aiViewModel.isProcessingAction.value }
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "August Salary" } }

        assertNull("Pending group should be cleared after execution", aiViewModel.pendingActionGroup.value)
        assertEquals("Database should have one more transaction", initialCount + 1, financeViewModel.allTransactions.value.size)
        val addedTx = financeViewModel.allTransactions.value.first { it.title == "August Salary" }
        assertEquals(3000.0, addedTx.amount, 0.01)
        assertEquals(TransactionType.INCOME, addedTx.type)
    }

    @Test
    fun testUnambiguousPaymentMethodParsing() {
        // Canonical name
        val rawCanonical = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Card Expense", "amount": 20.0, "type": "EXPENSE", "category": "SHOPPING", "paymentMethod": "CREDIT_CARD"}
        """.trimIndent()
        invokeProcessAgenticAction(rawCanonical)
        assertNull("Canonical enum name CREDIT_CARD must be accepted", aiViewModel.pendingActionGroup.value?.validationError)
        aiViewModel.cancelPendingAction()

        // Display name
        val rawDisplay = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Cash Expense", "amount": 20.0, "type": "EXPENSE", "category": "SHOPPING", "paymentMethod": "Cash"}
        """.trimIndent()
        invokeProcessAgenticAction(rawDisplay)
        assertNull("Display name Cash must be accepted", aiViewModel.pendingActionGroup.value?.validationError)
        aiViewModel.cancelPendingAction()

        // Explicit alias CC
        val rawAlias = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Alias Expense", "amount": 20.0, "type": "EXPENSE", "category": "SHOPPING", "paymentMethod": "CC"}
        """.trimIndent()
        invokeProcessAgenticAction(rawAlias)
        val aliasGroup = aiViewModel.pendingActionGroup.value
        assertNull("Explicit alias CC must be accepted", aliasGroup?.validationError)
        val data = aliasGroup?.actionItems?.first()?.data as ProposedActionData.LogTransactionData
        assertEquals(PaymentMethod.CREDIT_CARD, data.paymentMethod)
        aiViewModel.cancelPendingAction()

        // Ambiguous prefix "C" must be rejected
        val rawAmbiguous = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Ambiguous Expense", "amount": 20.0, "type": "EXPENSE", "category": "SHOPPING", "paymentMethod": "C"}
        """.trimIndent()
        invokeProcessAgenticAction(rawAmbiguous)
        val ambGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(ambGroup?.validationError)
        assertTrue("Ambiguous prefix C must be rejected", ambGroup?.validationError?.contains("Unknown payment method") == true)
        aiViewModel.cancelPendingAction()

        // Unknown value "BITCOIN" must be rejected
        val rawUnknown = """
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Crypto Expense", "amount": 20.0, "type": "EXPENSE", "category": "SHOPPING", "paymentMethod": "BITCOIN"}
        """.trimIndent()
        invokeProcessAgenticAction(rawUnknown)
        val unkGroup = aiViewModel.pendingActionGroup.value
        assertNotNull(unkGroup?.validationError)
        assertTrue("Unknown value BITCOIN must be rejected", unkGroup?.validationError?.contains("Unknown payment method") == true)
    }

    @Test
    fun testNormalizedDateMatchingForDeletionsActualRecords() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeAug10 = sdf.parse("2026-08-10")!!.time
        val timeAug15 = sdf.parse("2026-08-15")!!.time
        val timeSept1 = sdf.parse("2026-09-01")!!.time

        financeViewModel.addTransaction("Aug 10 Coffee", 10.0, TransactionType.EXPENSE, TransactionCategory.FOOD_DINING, timeAug10, "", PaymentMethod.CASH)
        financeViewModel.addTransaction("Aug 15 Groceries", 50.0, TransactionType.EXPENSE, TransactionCategory.GROCERIES, timeAug15, "", PaymentMethod.UPI)
        financeViewModel.addTransaction("Sept 1 Utilities", 100.0, TransactionType.EXPENSE, TransactionCategory.UTILITIES, timeSept1, "", PaymentMethod.NET_BANKING)

        flushCoroutines { financeViewModel.allTransactions.value.size >= 3 }

        // 1. Delete Aug 15 Groceries using slash format "2026/08/15"
        val rawDeleteSlashDay = """
            ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "Groceries", "targetDate": "2026/08/15"}
        """.trimIndent()

        invokeProcessAgenticAction(rawDeleteSlashDay)
        val pendingSlash = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingSlash)
        assertNull(pendingSlash?.validationError)

        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines { financeViewModel.allTransactions.value.none { it.title == "Aug 15 Groceries" } }

        val txsAfterDayDelete = financeViewModel.allTransactions.value
        assertTrue("Aug 15 Groceries must be deleted", txsAfterDayDelete.none { it.title == "Aug 15 Groceries" })
        assertTrue("Aug 10 Coffee must remain", txsAfterDayDelete.any { it.title == "Aug 10 Coffee" })
        assertTrue("Sept 1 Utilities must remain", txsAfterDayDelete.any { it.title == "Sept 1 Utilities" })

        // 2. Delete ALL August transactions using month-name target "August 2026"
        val rawDeleteAugMonth = """
            ACTION_JSON: {"action": "DELETE_TRANSACTION", "targetQuery": "ALL", "targetDate": "August 2026"}
        """.trimIndent()

        invokeProcessAgenticAction(rawDeleteAugMonth)
        val pendingAug = aiViewModel.pendingActionGroup.value
        assertNotNull(pendingAug)
        assertNull(pendingAug?.validationError)

        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines { financeViewModel.allTransactions.value.none { it.title == "Aug 10 Coffee" } }

        val txsAfterMonthDelete = financeViewModel.allTransactions.value
        assertTrue("Aug 10 Coffee must be deleted", txsAfterMonthDelete.none { it.title == "Aug 10 Coffee" })
        assertTrue("Sept 1 Utilities in different month MUST remain untouched", txsAfterMonthDelete.any { it.title == "Sept 1 Utilities" })
    }

    @Test
    fun testPartialBatchRetrySkipsCompletedActionsAndRetriesFailed() {
        // Pre-insert StaleTx into DB so Action 2 passes initial parse validation
        financeViewModel.addTransaction("StaleTx", 15.0, TransactionType.EXPENSE, TransactionCategory.SHOPPING, System.currentTimeMillis(), "", PaymentMethod.CASH)
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "StaleTx" } }
        val staleItem = financeViewModel.allTransactions.value.first { it.title == "StaleTx" }

        val rawBatchAction = """
            ACTION_JSON: [
                {"action": "LOG_TRANSACTION", "title": "Batch Tx 1", "amount": 20.0, "type": "EXPENSE", "category": "FOOD_DINING", "paymentMethod": "CASH"},
                {"action": "DELETE_TRANSACTION", "targetQuery": "StaleTx"}
            ]
        """.trimIndent()

        invokeProcessAgenticAction(rawBatchAction)
        val group = aiViewModel.pendingActionGroup.value
        assertNotNull(group)
        assertNull("Batch should be parsed without initial validation error", group?.validationError)
        assertEquals(2, group?.actionItems?.size)

        // Delete StaleTx directly to simulate a stale target at confirmation time
        financeViewModel.deleteTransaction(staleItem)
        flushCoroutines { financeViewModel.allTransactions.value.none { it.title == "StaleTx" } }

        // Confirm batch execution - Action 1 succeeds, Action 2 fails (stale target)
        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines { aiViewModel.pendingActionGroup.value?.actionItems?.get(0)?.status == ActionExecutionStatus.SUCCESS }
        flushCoroutines { financeViewModel.allTransactions.value.any { it.title == "Batch Tx 1" } }

        val groupAfterFail = aiViewModel.pendingActionGroup.value
        assertNotNull("Pending group should remain for partial batch retry", groupAfterFail)
        assertEquals(ActionExecutionStatus.SUCCESS, groupAfterFail?.actionItems?.get(0)?.status)
        assertEquals(ActionExecutionStatus.FAILED, groupAfterFail?.actionItems?.get(1)?.status)

        val tx1CountBeforeRetry = financeViewModel.allTransactions.value.count { it.title == "Batch Tx 1" }
        assertEquals(1, tx1CountBeforeRetry)

        // Retry batch - Action 1 MUST be skipped and NOT duplicated!
        aiViewModel.confirmAndExecutePendingAction()
        flushCoroutines { !aiViewModel.isProcessingAction.value }
        flushCoroutines()

        val tx1CountAfterRetry = financeViewModel.allTransactions.value.count { it.title == "Batch Tx 1" }
        assertEquals("Retrying MUST NOT duplicate successfully completed actions in batch", 1, tx1CountAfterRetry)
    }

    @Test
    fun testCancelPendingActionDismissesStateWithoutMutating() {
        val initialCount = financeViewModel.allTransactions.value.size

        val rawAiResponse = """
            Logging expense.
            ACTION_JSON: {"action": "LOG_TRANSACTION", "title": "Dinner", "amount": 25.0, "type": "EXPENSE", "category": "FOOD_DINING", "paymentMethod": "CASH"}
        """.trimIndent()

        invokeProcessAgenticAction(rawAiResponse)

        assertNotNull(aiViewModel.pendingActionGroup.value)

        aiViewModel.cancelPendingAction()

        assertNull(aiViewModel.pendingActionGroup.value)
        assertEquals("Database must remain untouched after cancellation", initialCount, financeViewModel.allTransactions.value.size)
    }
}


