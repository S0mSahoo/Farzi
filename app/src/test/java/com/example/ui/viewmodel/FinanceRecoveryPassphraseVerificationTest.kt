package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.OccurrenceStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringOccurrence
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.util.CryptoManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceRecoveryPassphraseVerificationTest {

  private lateinit var app: Application
  private lateinit var viewModel: FinanceViewModel

  private fun flushCoroutines(condition: (() -> Boolean)? = null) {
    val deadline = System.currentTimeMillis() + 5000
    ShadowLooper.idleMainLooper()
    while (condition != null && !condition() && System.currentTimeMillis() < deadline) {
      ShadowLooper.idleMainLooper()
      Thread.sleep(20)
      ShadowLooper.idleMainLooper()
    }
    ShadowLooper.idleMainLooper()
  }

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext<Application>()
    viewModel = FinanceViewModel(app)
    flushCoroutines()
  }

  @Test
  fun testGuestModePermitsInitialPassphraseSetup() = runBlocking {
    val initialPassphrase = "guest recovery passphrase 2026"
    val result = viewModel.verifyAndSetRecoveryPassphrase(initialPassphrase)
    assertTrue("Guest mode with no cloud account permits passphrase setup", result.isSuccess)

    val stored = CryptoManager.getStoredCloudRecoveryPassphrase(app)
    assertEquals(initialPassphrase, stored)
  }

  @Test
  fun testShortPassphraseFailsValidationAndLeavesStoredValueIntact() = runBlocking {
    val initialPassphrase = "valid recovery passphrase 2026"
    viewModel.verifyAndSetRecoveryPassphrase(initialPassphrase)
    assertEquals(initialPassphrase, CryptoManager.getStoredCloudRecoveryPassphrase(app))

    val shortResult = viewModel.verifyAndSetRecoveryPassphrase("short")
    assertTrue("Passphrase under 12 characters must fail", shortResult.isFailure)
    assertEquals(
      "Existing stored passphrase must remain intact when short input is rejected",
      initialPassphrase,
      CryptoManager.getStoredCloudRecoveryPassphrase(app)
    )
  }

  @Test
  fun testAuthenticatedProfileWithMissingGoogleAccountFailsClosed() = runBlocking {
    val initialPassphrase = "authenticated original passphrase 2026"
    CryptoManager.replaceStoredCloudRecoveryPassphrase(app, initialPassphrase)
    assertEquals(initialPassphrase, CryptoManager.getStoredCloudRecoveryPassphrase(app))

    // Save authenticated profile in repository before ViewModel initialization
    val repo = com.example.data.repository.FinanceRepository(app)
    repo.saveUserProfile(
      UserProfile(
        name = "Alice User",
        email = "alice@example.com",
        googleId = "google_user_987654",
        hasCompletedOnboarding = true
      )
    )
    repo.setGuestDataMode(false)

    // Recreate viewModel with this authenticated profile
    val authViewModel = FinanceViewModel(app)
    flushCoroutines()

    // Now attempt to set a different passphrase when no Google account is signed in on device
    val candidate = "different passphrase attempting replacement 2026"
    val result = authViewModel.verifyAndSetRecoveryPassphrase(candidate)

    assertTrue("Verification must fail when Google account is missing for authenticated profile", result.isFailure)
    val errorMsg = result.exceptionOrNull()?.message ?: ""
    assertTrue(
      "Error message must indicate Google account is unavailable or required",
      errorMsg.contains("Google account is not currently signed in") || errorMsg.contains("unavailable")
    )

    // Stored passphrase must be preserved!
    assertEquals(
      "Original recovery passphrase must NOT be overwritten when account is missing",
      initialPassphrase,
      CryptoManager.getStoredCloudRecoveryPassphrase(app)
    )
  }

  @Test
  fun testRestoreRecurringOccurrenceSuspendCompletesLocalDatabaseMutation() = runBlocking {
    // 1. Add a recurring rule
    val ruleId = viewModel.addRecurringRuleSuspend(
      title = "Netflix",
      amount = 15.0,
      type = TransactionType.EXPENSE,
      category = TransactionCategory.ENTERTAINMENT,
      interval = RecurrenceInterval.MONTHLY,
      startDate = System.currentTimeMillis() - 86400000L,
      endDate = null,
      paymentMethod = PaymentMethod.CREDIT_CARD,
      note = "Monthly stream"
    )
    flushCoroutines { viewModel.allRecurringRules.value.any { it.id == ruleId } }
    val rule = viewModel.allRecurringRules.value.first { it.id == ruleId }

    val occurrence = RecurringOccurrence(
      rule = rule,
      occurrenceTimestamp = System.currentTimeMillis(),
      occurrenceDateKey = "2026-10-31",
      status = OccurrenceStatus.SCHEDULED,
      daysRelative = 0
    )

    // 2. Cancel the occurrence
    viewModel.cancelRecurringOccurrenceSuspend(occurrence)
    flushCoroutines()

    // 3. Restore the occurrence using the genuine suspend function
    viewModel.restoreRecurringOccurrenceSuspend(occurrence)
    flushCoroutines()

    // 4. Also verify compatibility wrapper does not throw
    viewModel.restoreRecurringOccurrence(occurrence)
    flushCoroutines()

    assertNotNull("Rule must still exist in recurring rules", viewModel.allRecurringRules.value.find { it.id == ruleId })
  }
}
