package com.example

import com.example.util.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoManagerTest {
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
}
