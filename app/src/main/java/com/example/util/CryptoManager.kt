package com.example.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedBundle(
  val saltBase64: String,
  val ivBase64: String,
  val ciphertextBase64: String
)

object CryptoManager {
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 12
  private const val PBKDF2_ITERATIONS = 10000
  private const val KEY_LENGTH = 256
  private const val SALT_LENGTH = 16

  private val secureRandom = SecureRandom()

  /**
   * Derives an AES-256 key deterministically from a user identifier (e.g. Google ID)
   * and a cryptographic salt using PBKDF2. This allows cross-device synchronization:
   * any device logged into the same authorized Google account will derive the exact same key.
   */
  fun deriveKey(userIdentifier: String, salt: ByteArray): SecretKey {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(userIdentifier.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
    val tmp = factory.generateSecret(spec)
    return SecretKeySpec(tmp.encoded, "AES")
  }

  /**
   * Encrypts plaintext string using AES-256-GCM with a user-derived key.
   */
  fun encryptCloudPayload(plaintext: String, userIdentifier: String): EncryptedBundle {
    val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
    val key = deriveKey(userIdentifier, salt)

    val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
    val cipher = Cipher.getInstance(ALGORITHM)
    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, spec)

    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

    return EncryptedBundle(
      saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
      ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
      ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    )
  }

  /**
   * Decrypts an EncryptedBundle back into plaintext.
   */
  fun decryptCloudPayload(bundle: EncryptedBundle, userIdentifier: String): String {
    val salt = Base64.decode(bundle.saltBase64, Base64.NO_WRAP)
    val iv = Base64.decode(bundle.ivBase64, Base64.NO_WRAP)
    val ciphertext = Base64.decode(bundle.ciphertextBase64, Base64.NO_WRAP)

    val key = deriveKey(userIdentifier, salt)
    val cipher = Cipher.getInstance(ALGORITHM)
    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.DECRYPT_MODE, key, spec)

    val decryptedBytes = cipher.doFinal(ciphertext)
    return String(decryptedBytes, Charsets.UTF_8)
  }

  /**
   * Encrypts a local string (e.g. Secure Note) using a device-local secret master key.
   */
  fun encryptLocal(plaintext: String, localMasterKey: String): Pair<String, String> {
    val salt = "PAISA_LOCAL_SALT_V1".toByteArray(Charsets.UTF_8)
    val key = deriveKey(localMasterKey, salt)

    val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
    val cipher = Cipher.getInstance(ALGORITHM)
    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, spec)

    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return Pair(
      Base64.encodeToString(ciphertext, Base64.NO_WRAP),
      Base64.encodeToString(iv, Base64.NO_WRAP)
    )
  }

  /**
   * Decrypts a local string.
   */
  fun decryptLocal(ciphertextBase64: String, ivBase64: String, localMasterKey: String): String {
    val salt = "PAISA_LOCAL_SALT_V1".toByteArray(Charsets.UTF_8)
    val key = deriveKey(localMasterKey, salt)

    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
    val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

    val cipher = Cipher.getInstance(ALGORITHM)
    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.DECRYPT_MODE, key, spec)

    val decryptedBytes = cipher.doFinal(ciphertext)
    return String(decryptedBytes, Charsets.UTF_8)
  }
}
