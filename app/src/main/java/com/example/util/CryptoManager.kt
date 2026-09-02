package com.example.util

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val KEY_ALGORITHM = "AES"
  private const val TAG_LENGTH_BIT = 128
  private const val IV_LENGTH_BYTE = 12
  private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val ITERATIONS = 10000
  private const val KEY_LENGTH_BIT = 256

  // Deterministic domain salt for cloud key derivation per Google account
  private val DOMAIN_SALT = "PAISA_FINANCE_SECURE_CLOUD_SALT_v4".toByteArray(StandardCharsets.UTF_8)
  private val LOCAL_VAULT_SALT = "PAISA_LOCAL_SECURE_VAULT_SALT_v1".toByteArray(StandardCharsets.UTF_8)

  private val secureRandom = SecureRandom()

  /**
   * Derives a 256-bit AES SecretKey from an identifier (e.g. Google User ID or local master seed)
   */
  private fun deriveKey(identifier: String, salt: ByteArray): SecretKey {
    val cleanId = identifier.ifBlank { "paisa_default_secure_id" }
    val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
    val spec = PBEKeySpec(cleanId.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BIT)
    val secretKeyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(secretKeyBytes, KEY_ALGORITHM)
  }

  /**
   * Encrypts plaintext string using AES-256-GCM.
   * Returns a JSON envelope string: {"iv": "...", "ciphertext": "...", "version": 4}
   */
  fun encryptWithKey(plaintext: String, secretKey: SecretKey): String {
    val iv = ByteArray(IV_LENGTH_BYTE)
    secureRandom.nextBytes(iv)

    val cipher = Cipher.getInstance(ALGORITHM)
    val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

    val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
    val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
    val cipherBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)

    val json = JSONObject()
    json.put("v", 4)
    json.put("iv", ivBase64)
    json.put("ct", cipherBase64)
    return json.toString()
  }

  /**
   * Decrypts an AES-256-GCM encrypted envelope string.
   */
  fun decryptWithKey(encryptedJson: String, secretKey: SecretKey): String {
    val json = JSONObject(encryptedJson)
    val ivBase64 = json.getString("iv")
    val cipherBase64 = json.getString("ct")

    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
    val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)

    val cipher = Cipher.getInstance(ALGORITHM)
    val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

    val decryptedBytes = cipher.doFinal(cipherBytes)
    return String(decryptedBytes, StandardCharsets.UTF_8)
  }

  /**
   * Encrypts application cloud data for Google Drive backup using the user's account ID.
   */
  fun encryptCloudPayload(plaintextJson: String, userIdentifier: String): String {
    val key = deriveKey(userIdentifier, DOMAIN_SALT)
    val encryptedEnvelope = encryptWithKey(plaintextJson, key)
    val container = JSONObject()
    container.put("version", "4.0.0")
    container.put("isEncrypted", true)
    container.put("encryptedPayload", encryptedEnvelope)
    container.put("timestamp", System.currentTimeMillis())
    return container.toString()
  }

  /**
   * Decrypts Google Drive cloud backup payload.
   * Handles backward compatibility for unencrypted legacy backups (v3.0.0).
   */
  fun decryptCloudPayload(cloudJson: String, userIdentifier: String): String {
    val trimmed = cloudJson.trim()
    if (!trimmed.startsWith("{")) return trimmed

    val root = JSONObject(trimmed)
    if (root.optBoolean("isEncrypted", false)) {
      val encryptedEnvelope = root.getString("encryptedPayload")
      val key = deriveKey(userIdentifier, DOMAIN_SALT)
      return decryptWithKey(encryptedEnvelope, key)
    }
    // Legacy unencrypted backup
    return cloudJson
  }

  /**
   * Encrypts local private vault notes.
   */
  fun encryptLocalVaultData(plaintext: String): String {
    val key = deriveKey("paisa_vault_local_key", LOCAL_VAULT_SALT)
    return encryptWithKey(plaintext, key)
  }

  /**
   * Decrypts local private vault notes.
   */
  fun decryptLocalVaultData(encryptedString: String): String {
    return try {
      val key = deriveKey("paisa_vault_local_key", LOCAL_VAULT_SALT)
      decryptWithKey(encryptedString, key)
    } catch (e: Exception) {
      ""
    }
  }
}
