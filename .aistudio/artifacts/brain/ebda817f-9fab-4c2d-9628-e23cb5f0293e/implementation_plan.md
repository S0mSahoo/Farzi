# Implementation Plan: Phase 6 - Adding Google Search Grounding to Paisa AI

## 1. Objective
Enable selective Google Search grounding for the Gemini advisor to answer questions requiring external, up-to-date information, while strictly preserving privacy for local financial data queries.

## 2. Implementation Steps

### Model & Tool Configuration
- **Modify `AiAdvisorViewModel.kt`**: Update `Firebase.ai.generativeModel` initialization to include `com.google.firebase.ai.type.Tool.googleSearch()` in the `tools` list alongside the existing financial query tools.

### Handling Grounding Metadata
- **Update Inference Loop**: Modify the `AiAdvisorViewModel` to inspect the response for grounding metadata (citations).
- **Metadata Extraction**: Parse `response.candidates[0].groundingMetadata` (or equivalent SDK structure) to extract `groundingChunks` (source title, URI).
- **Display UI**: Ensure extracted sources are passed to the chat UI and rendered as clear, clickable links.

### Privacy Safeguards
- **Selective Grounding**: Ensure the instruction prompt explicitly differentiates between financial query tools and external search.
- **Data Protection**: Explicitly instruct the model to prefer financial tools for user data and Google Search only for general external knowledge. Verify that local financial data is NOT shared with Google Search.

### Testing & Validation
- **Unit Testing**: Add unit tests in a new test class (e.g., `GroundingMetadataTest.kt`) to simulate `groundingMetadata` responses, including:
  - Valid source mapping (title/URL).
  - Missing metadata scenarios.
  - Duplicate/malformed sources.
- **Manual Verification**: Verify in the debug emulator that:
  - Grounded queries return sources.
  - Personal finance queries do not trigger external searches.

## 3. Assumptions & Risks
- **SDK Capability**: Assumes `Tool.googleSearch()` is available in the currently installed Firebase AI SDK version.
- **UI Constraints**: Requires a safe way to present external web links within the Android chat UI.
