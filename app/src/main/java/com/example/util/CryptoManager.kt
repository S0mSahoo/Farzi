package com.example.util

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

data class EncryptedBundle(
  val saltBase64: String,
  val ivBase64: String,
  val ciphertextBase64: String
)

object CryptoManager {
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 12
  private const val PBKDF2_ITERATIONS = 10_000
  private const val RECOVERY_PBKDF2_ITERATIONS = 210_000
  private const val KEY_LENGTH = 256
  private const val SALT_LENGTH = 16

  private const val RECOVERY_PREFS = "paisa_cloud_recovery"
  private const val RECOVERY_IV_KEY = "encrypted_recovery_iv"
  private const val RECOVERY_VALUE_KEY = "encrypted_recovery_value"
  private const val RECOVERY_WRAP_KEY_ALIAS = "paisa_cloud_recovery_wrap_key_v1"

  private val secureRandom = SecureRandom()

  /**
   * Legacy key derivation retained exclusively to decrypt backups created by older app versions.
   * A Google account identifier is not a secret and must not protect newly written backups.
   */
  fun deriveKey(userIdentifier: String, salt: ByteArray): SecretKey {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(userIdentifier.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
    return try {
      val tmp = factory.generateSecret(spec)
      SecretKeySpec(tmp.encoded, "AES")
    } finally {
      spec.clearPassword()
    }
  }

  private fun deriveRecoveryKey(passphrase: String, salt: ByteArray): SecretKey {
    require(passphrase.length >= 12) { "Recovery passphrase must contain at least 12 characters." }
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, RECOVERY_PBKDF2_ITERATIONS, KEY_LENGTH)
    return try {
      val tmp = factory.generateSecret(spec)
      SecretKeySpec(tmp.encoded, "AES")
    } finally {
      spec.clearPassword()
    }
  }

  /** Encrypts a new cloud payload using a user-controlled cross-device recovery passphrase. */
  fun encryptCloudPayloadWithPassphrase(plaintext: String, passphrase: String): EncryptedBundle {
    val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
    val key = deriveRecoveryKey(passphrase, salt)
    val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return EncryptedBundle(
      saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
      ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
      ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    )
  }

  /** Decrypts a recovery-passphrase encrypted cloud payload. */
  fun decryptCloudPayloadWithPassphrase(bundle: EncryptedBundle, passphrase: String): String {
    val salt = Base64.decode(bundle.saltBase64, Base64.NO_WRAP)
    val iv = Base64.decode(bundle.ivBase64, Base64.NO_WRAP)
    val ciphertext = Base64.decode(bundle.ciphertextBase64, Base64.NO_WRAP)
    require(salt.size == SALT_LENGTH) { "Invalid backup encryption salt." }
    require(iv.size == GCM_IV_LENGTH) { "Invalid backup encryption IV." }
    val key = deriveRecoveryKey(passphrase, salt)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }

  /**
   * Stores the recovery passphrase encrypted with an AES key held by Android Keystore.
   * It is intentionally not included in Android backup/restore; another device must have the
   * user re-enter the same passphrase to access the cross-device cloud backup.
   */
  fun storeCloudRecoveryPassphrase(context: Context, passphrase: String) {
    require(passphrase.length >= 12) { "Use a recovery passphrase with at least 12 characters." }
    val current = getStoredCloudRecoveryPassphrase(context)
    if (current != null) {
      if (current == passphrase) return
      throw IllegalStateException(
        "A recovery passphrase is already stored on this device. It cannot be replaced without migrating the existing cloud backup."
      )
    }

    val key = getOrCreateRecoveryWrapKey()
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encrypted = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))
    context.getSharedPreferences(RECOVERY_PREFS, Context.MODE_PRIVATE).edit()
      .putString(RECOVERY_IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
      .putString(RECOVERY_VALUE_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
      .commit()
      .also { saved ->
        check(saved) { "Could not securely save the recovery passphrase on this device." }
      }
  }

  /**
   * Explicitly replaces the passphrase saved on this device. This does not change the cloud
   * encryption key or rewrite the cloud backup; it only lets a user correct a passphrase entry
   * or re-enter the same cross-device recovery passphrase on this device.
   */
  fun replaceStoredCloudRecoveryPassphrase(context: Context, passphrase: String) {
    require(passphrase.length >= 12) { "Recovery passphrase must contain at least 12 characters." }
    val key = getOrCreateRecoveryWrapKey()
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encrypted = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))
    val saved = context.getSharedPreferences(RECOVERY_PREFS, Context.MODE_PRIVATE).edit()
      .putString(RECOVERY_IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
      .putString(RECOVERY_VALUE_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
      .commit()
    check(saved) { "Could not securely save the recovery passphrase on this device." }
  }

  /** Returns the locally Keystore-wrapped passphrase, or null if none has been configured. */
  fun getStoredCloudRecoveryPassphrase(context: Context): String? {
    val prefs = context.getSharedPreferences(RECOVERY_PREFS, Context.MODE_PRIVATE)
    val ivText = prefs.getString(RECOVERY_IV_KEY, null)
    val encryptedText = prefs.getString(RECOVERY_VALUE_KEY, null)
    if (ivText == null && encryptedText == null) return null
    check(!ivText.isNullOrBlank() && !encryptedText.isNullOrBlank()) {
      "The locally stored recovery passphrase is incomplete. Re-enter the recovery passphrase in Settings."
    }

    val key = getOrCreateRecoveryWrapKey()
    val cipher = Cipher.getInstance(ALGORITHM)
    val iv = Base64.decode(ivText, Base64.NO_WRAP)
    val encrypted = Base64.decode(encryptedText, Base64.NO_WRAP)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return String(cipher.doFinal(encrypted), Charsets.UTF_8)
  }

  private fun getOrCreateRecoveryWrapKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (keyStore.getKey(RECOVERY_WRAP_KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        RECOVERY_WRAP_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return generator.generateKey()
  }

  /**
   * Legacy cloud decrypt method. Kept for backups made before recovery-passphrase encryption
   * was introduced; new cloud writes must use encryptCloudPayloadWithPassphrase.
   */
  fun decryptCloudPayload(bundle: EncryptedBundle, userIdentifier: String): String {
    val salt = Base64.decode(bundle.saltBase64, Base64.NO_WRAP)
    val iv = Base64.decode(bundle.ivBase64, Base64.NO_WRAP)
    val ciphertext = Base64.decode(bundle.ciphertextBase64, Base64.NO_WRAP)
    val key = deriveKey(userIdentifier, salt)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }

  /**
   * Encrypts a local string (e.g. Secure Note) using the existing local key derivation.
   * Kept compatible with ciphertext generated by prior versions.
   */
  fun encryptLocal(plaintext: String, localMasterKey: String): Pair<String, String> {
    val salt = "PAISA_LOCAL_SALT_V1".toByteArray(Charsets.UTF_8)
    val key = deriveKey(localMasterKey, salt)
    val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return Pair(
      Base64.encodeToString(ciphertext, Base64.NO_WRAP),
      Base64.encodeToString(iv, Base64.NO_WRAP)
    )
  }

  /** Decrypts a local string with the existing local key derivation. */
  fun decryptLocal(ciphertextBase64: String, ivBase64: String, localMasterKey: String): String {
    val salt = "PAISA_LOCAL_SALT_V1".toByteArray(Charsets.UTF_8)
    val key = deriveKey(localMasterKey, salt)
    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
    val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }
}
