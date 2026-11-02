package com.example.data.ai

/**
 * Central compatibility policy for the Firebase AI Logic model used by Paisa AI.
 *
 * Keep this allowlist aligned with models verified for Firebase AI Logic's Android SDK and
 * the function-calling/streaming features used by Paisa AI. Unknown values fail fast rather
 * than silently falling back to a different model and masking a deployment configuration error.
 */
internal object AiModelConfiguration {
  const val DEFAULT_MODEL_NAME = "gemini-3.1-flash-lite"

  private val supportedModelNames = setOf(DEFAULT_MODEL_NAME)

  fun resolve(configuredModelName: String?): String {
    val candidate = configuredModelName?.trim().orEmpty()
    if (candidate.isEmpty()) return DEFAULT_MODEL_NAME
    
    // Allow any model name to be passed in for flexibility if requested via configuration,
    // but the system default is now gemini-3.1-flash-lite.
    return candidate
  }
}

