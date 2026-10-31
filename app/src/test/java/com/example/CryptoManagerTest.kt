package com.example

import com.example.util.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.Arrays
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoManagerTest {
  @Test
  fun pbkdf2HmacSha256MatchesPlatformDerivationForAsciiAndUnicode() {
    val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    listOf("legacy-google-user@example.com", "Paisa 🔐 recovery phrase").forEach { password ->
      val spec = PBEKeySpec(password.toCharArray(), salt, 10_000, 256)
      val expected = try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
          .generateSecret(spec).encoded
      } finally {
        spec.clearPassword()
      }
      val actual = CryptoManager.deriveKey(password, salt).encoded
      assertArrayEquals("PBKDF2 output must stay compatible for '$password'", expected, actual)
      Arrays.fill(expected, 0)
      Arrays.fill(actual, 0)
    }
  }

  @Test
  fun recoveryPassphraseEncryptionRoundTrips() {
    val original = """{"transactions":[{"title":"Rent","amount":12000}]}"""
    val passphrase = "correct horse battery staple 2026"

    val encrypted = CryptoManager.encryptCloudPayloadWithPassphrase(original, passphrase)
    val restored = CryptoManager.decryptCloudPayloadWithPassphrase(encrypted, passphrase)

    assertEquals(original, restored)
  }

  @Test
  fun wrongRecoveryPassphraseCannotDecryptPayload() {
    val encrypted = CryptoManager.encryptCloudPayloadWithPassphrase(
      plaintext = """{"private":"financial data"}""",
      passphrase = "correct horse battery staple 2026"
    )
    var decryptionFailed = false

    try {
      CryptoManager.decryptCloudPayloadWithPassphrase(
        encrypted,
        "an entirely different passphrase"
      )
    } catch (_: Exception) {
      decryptionFailed = true
    }

    assertTrue("AES-GCM must reject an incorrect recovery passphrase", decryptionFailed)
  }

  @Test
  fun recoveryPassphraseRequiresAtLeastTwelveCharacters() {
    var encryptionRejected = false
    try {
      CryptoManager.encryptCloudPayloadWithPassphrase("{}", "short")
    } catch (_: IllegalArgumentException) {
      encryptionRejected = true
    }

    assertTrue("Short recovery passphrases must be rejected", encryptionRejected)
  }

  @Test
  fun testVerifyPassphraseAgainstBundle() {
    val original = """{"profile":{"name":"Test"}}"""
    val passphrase = "correct horse battery staple 2026"
    val bundle = CryptoManager.encryptCloudPayloadWithPassphrase(original, passphrase)

    assertTrue("Correct passphrase must verify successfully", CryptoManager.verifyPassphraseAgainstBundle(bundle, passphrase))
    org.junit.Assert.assertFalse("Incorrect passphrase must fail verification", CryptoManager.verifyPassphraseAgainstBundle(bundle, "wrong passphrase 1234"))
  }

  @Test
  fun testPassphraseVerificationAgainstExistingBundle() {
    val originalPassphrase = "first valid recovery passphrase 2026"
    val originalData = """{"test":"payload"}"""
    val bundle = CryptoManager.encryptCloudPayloadWithPassphrase(originalData, originalPassphrase)

    val invalidCandidate = "invalid candidate passphrase 9999"
    org.junit.Assert.assertFalse("Invalid candidate must fail verification against existing cloud bundle", CryptoManager.verifyPassphraseAgainstBundle(bundle, invalidCandidate))
    assertTrue("Valid candidate must pass verification against existing cloud bundle", CryptoManager.verifyPassphraseAgainstBundle(bundle, originalPassphrase))
  }

  @Test
  fun testPassphraseValidationCases() {
    val passphrase = "valid passphrase 123456"
    val data = """{"data":"ok"}"""
    val bundle = CryptoManager.encryptCloudPayloadWithPassphrase(data, passphrase)

    // Case A & E: Null bundle or legacy backup does not block setting new passphrase
    assertTrue("Valid passphrase decrypts bundle", CryptoManager.verifyPassphraseAgainstBundle(bundle, passphrase))
    org.junit.Assert.assertFalse("Incorrect passphrase fails decryption", CryptoManager.verifyPassphraseAgainstBundle(bundle, "wrong passphrase 654321"))
  }

  @Test
  fun testStoredRecoveryPassphraseEncryptionAndDecryptionRoundTrip() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val testPassphrase = "my secure recovery passphrase 2026"

    CryptoManager.replaceStoredCloudRecoveryPassphrase(context, testPassphrase)
    val restored = CryptoManager.getStoredCloudRecoveryPassphrase(context)

    assertEquals("Restored passphrase must match original", testPassphrase, restored)

    val prefs = context.getSharedPreferences("paisa_cloud_recovery", android.content.Context.MODE_PRIVATE)
    val ivText = prefs.getString("encrypted_recovery_iv", null)
    org.junit.Assert.assertNotNull("Provider-generated IV must be stored", ivText)
    val ivBytes = android.util.Base64.decode(ivText, android.util.Base64.NO_WRAP)
    assertEquals("Generated IV must be standard GCM 12 bytes", 12, ivBytes.size)
  }

  @Test
  fun testStoreCloudRecoveryPassphrasePreventsAccidentalOverwrite() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val initialPassphrase = "initial recovery passphrase 2026"

    CryptoManager.replaceStoredCloudRecoveryPassphrase(context, initialPassphrase)
    assertEquals(initialPassphrase, CryptoManager.getStoredCloudRecoveryPassphrase(context))

    try {
      CryptoManager.storeCloudRecoveryPassphrase(context, "different passphrase attempting overwrite")
      org.junit.Assert.fail("storeCloudRecoveryPassphrase must not silently overwrite an existing stored passphrase")
    } catch (e: IllegalStateException) {
      assertTrue("Exception must explain already stored passphrase", e.message?.contains("already stored") == true)
    }

    assertEquals("Original passphrase must remain untouched", initialPassphrase, CryptoManager.getStoredCloudRecoveryPassphrase(context))
  }

  @Test
  fun testFailedReplacementDoesNotCorruptExistingPassphrase() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val initialPassphrase = "initial recovery passphrase 2026"

    CryptoManager.replaceStoredCloudRecoveryPassphrase(context, initialPassphrase)
    assertEquals(initialPassphrase, CryptoManager.getStoredCloudRecoveryPassphrase(context))

    try {
      CryptoManager.replaceStoredCloudRecoveryPassphrase(context, "short")
      org.junit.Assert.fail("Short passphrase must throw IllegalArgumentException")
    } catch (_: IllegalArgumentException) {}

    assertEquals("Original passphrase must remain intact after validation failure", initialPassphrase, CryptoManager.getStoredCloudRecoveryPassphrase(context))
  }
}
