package com.example.data.drive

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class GoogleDriveRecoveryEnvelopeTest {

  private val service: GoogleDriveBackupService by lazy {
    GoogleDriveBackupService(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun parseRawCloudEnvelopeRejectsBlankContent() {
    try {
      service.parseRawCloudEnvelope("  ")
      org.junit.Assert.fail("Blank cloud envelope must be rejected")
    } catch (e: IOException) {
      assertTrue(e.message.orEmpty().contains("empty"))
    }
  }

  @Test
  fun parseRawCloudEnvelopeRejectsMalformedJson() {
    try {
      service.parseRawCloudEnvelope("{not-json")
      org.junit.Assert.fail("Malformed cloud envelope must be rejected")
    } catch (e: IOException) {
      assertTrue(e.message.orEmpty().contains("Malformed cloud backup envelope"))
    }
  }

  @Test
  fun parseRawCloudEnvelopeRejectsEncryptedEnvelopeWithMissingFields() {
    val json = """
      {
        "version": "5.0.0",
        "encrypted": true,
        "exportTimestamp": 1,
        "saltBase64": "",
        "ivBase64": "aXY=",
        "ciphertextBase64": "Y2lwaGVydGV4dA==",
        "encryptionScheme": "recovery-passphrase-v1"
      }
    """.trimIndent()

    try {
      service.parseRawCloudEnvelope(json)
      org.junit.Assert.fail("Encrypted envelope missing salt must be rejected")
    } catch (e: IOException) {
      assertTrue(e.message.orEmpty().contains("missing required encryption fields"))
    }
  }

  @Test
  fun parseRawCloudEnvelopePreservesRecoverySchemeAndCipherFields() {
    val json = """
      {
        "version": "5.0.0",
        "encrypted": true,
        "exportTimestamp": 123,
        "saltBase64": "c2FsdA==",
        "ivBase64": "aXY=",
        "ciphertextBase64": "Y2lwaGVydGV4dA==",
        "encryptionScheme": "recovery-passphrase-v1"
      }
    """.trimIndent()

    val parsed = service.parseRawCloudEnvelope(json)

    assertEquals("recovery-passphrase-v1", parsed.encryptionScheme)
    assertEquals("c2FsdA==", parsed.saltBase64)
    assertEquals("aXY=", parsed.ivBase64)
    assertEquals("Y2lwaGVydGV4dA==", parsed.ciphertextBase64)
  }

  @Test
  fun parseRawCloudEnvelopeAllowsLegacyUnencryptedEnvelopeForCompatibility() {
    val json = """
      {
        "version": "3.0.0",
        "encrypted": false,
        "exportTimestamp": 123,
        "saltBase64": "",
        "ivBase64": "",
        "ciphertextBase64": ""
      }
    """.trimIndent()

    val parsed = service.parseRawCloudEnvelope(json)

    assertFalse(parsed.encrypted)
    assertEquals("google-id-v1", parsed.encryptionScheme)
  }
}
