package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SecurityManager(private val context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("paisa_security_prefs", Context.MODE_PRIVATE)

  companion object {
    private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
    private const val KEY_VAULT_PIN = "key_vault_pin"
    private const val KEY_THEME_MODE = "key_theme_mode"
  }

  fun isAppLockEnabled(): Boolean {
    return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
  }

  fun setAppLockEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
  }

  fun getThemeMode(): String {
    return prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
  }

  fun setThemeMode(mode: String) {
    prefs.edit().putString(KEY_THEME_MODE, mode).apply()
  }

  fun canAuthenticateWithBiometrics(): Boolean {
    val biometricManager = BiometricManager.from(context)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.BIOMETRIC_WEAK or
      BiometricManager.Authenticators.DEVICE_CREDENTIAL
    return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
  }

  /**
   * Prompts user for biometric authentication or device PIN/pattern/passcode.
   */
  suspend fun authenticate(
    activity: FragmentActivity,
    title: String = "Authenticate",
    subtitle: String = "Verify your identity to proceed"
  ): Boolean = suspendCancellableCoroutine { continuation ->
    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        if (continuation.isActive) {
          continuation.resume(true)
        }
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        if (continuation.isActive) {
          continuation.resume(false)
        }
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        // Single attempt failed, wait for retry or final error
      }
    }

    try {
      val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(
          BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

      val prompt = BiometricPrompt(activity, executor, callback)
      prompt.authenticate(promptInfo)

      continuation.invokeOnCancellation {
        prompt.cancelAuthentication()
      }
    } catch (e: Exception) {
      if (continuation.isActive) {
        continuation.resume(false)
      }
    }
  }
}
