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
  fun configuredModelIsTrimmedAndPreserved() {
    val arbitraryModel = "my-custom-model"
    assertEquals(
      arbitraryModel,
      AiModelConfiguration.resolve("  $arbitraryModel  ")
    )
  }

  @Test
  fun anyNonEmptyModelCanBeSelected() {
    val model = "gemini-3.1-flash-lite"
    assertEquals(
      model,
      AiModelConfiguration.resolve(model)
    )
  }
}

