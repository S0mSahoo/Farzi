package com.example.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class AiStreamSanitizerTest {
  @Test
  fun removesActionPayloadAfterCompleteMarker() {
    assertEquals(
      "Here is the proposed change.",
      AiStreamSanitizer.visibleText("Here is the proposed change. ACTION_JSON:{\"actions\":[]}")
    )
  }

  @Test
  fun withholdsPartialActionMarkerAtChunkBoundary() {
    assertEquals(
      "Here is the proposed change.",
      AiStreamSanitizer.visibleText("Here is the proposed change. ACTION_JS")
    )
    assertEquals(
      "Here is the proposed change.",
      AiStreamSanitizer.visibleText("Here is the proposed change. ACTION_JSON:")
    )
  }

  @Test
  fun preservesOrdinaryTextWhenNoMarkerIsPresent() {
    assertEquals(
      "Your expense total is ₹2,450.",
      AiStreamSanitizer.visibleText("Your expense total is ₹2,450.")
    )
  }

  @Test
  fun handlesMarkerBeginningAtStartOfResponse() {
    assertEquals("", AiStreamSanitizer.visibleText("ACTION_JSON:{\"actions\":[]}"))
    assertEquals("", AiStreamSanitizer.visibleText("ACTION_"))
  }
}
