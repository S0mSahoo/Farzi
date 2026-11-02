package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AiModelConfigurationTest {
  @Test
  fun blankBuildConfigurationUsesVerifiedDefaultModel() {
    assertEquals(AiModelConfiguration.DEFAULT_MODEL_NAME, AiModelConfiguration.resolve(null))
    assertEquals(AiModelConfiguration.DEFAULT_MODEL_NAME, AiModelConfiguration.resolve(""))
    assertEquals(AiModelConfiguration.DEFAULT_MODEL_NAME, AiModelConfiguration.resolve("   "))
  }

  @Test
  fun configuredVerifiedModelIsTrimmedAndPreserved() {
    assertEquals(
      AiModelConfiguration.DEFAULT_MODEL_NAME,
      AiModelConfiguration.resolve("  gemini-3.8-flash  ")
    )
  }

  @Test
  fun verifiedFlashLiteModelCanBeSelectedForLowerCostDeployments() {
    assertEquals(
      AiModelConfiguration.FAST_FALLBACK_MODEL_NAME,
      AiModelConfiguration.resolve(AiModelConfiguration.FAST_FALLBACK_MODEL_NAME)
    )
  }

  @Test
  fun unsupportedModelFailsFastInsteadOfSilentlyChangingModels() {
    assertThrows(IllegalArgumentException::class.java) {
      AiModelConfiguration.resolve("gemini-2.0-flash")
    }
  }
}
