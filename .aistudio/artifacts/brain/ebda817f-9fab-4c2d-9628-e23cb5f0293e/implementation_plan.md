# Implementation Plan: Paisa UI Consistency and Interaction Pass

## Phase 1: Core Shared Components Stabilization (Priority)
1. **System Bars Syncing:** Update `MainActivity.kt` to synchronize status/navigation bar icon colors with the resolved `ThemeMode` (Light/Dark/AMOLED).
2. **Button System & Ripple Clipping:**
   - Standardize all clickable controls across the app to ensure ripple/highlight is clipped to their actual shape (rounded/circular).
   - Audit `PremiumButton` wrappers in `PremiumFinanceUi.kt` and apply consistent clipping and interaction state handling.
   - Refactor custom clickable elements across the app to use these premium wrappers or adopt the same `Modifier` pattern (e.g., `.clip(shape).clickable(...)`).

## Phase 2: Repository-Wide UI/UX Audit & Refinement
1. **Repository Audit:** Systematically scan all files under `/app/src/main/java/com/example/` for:
   - Improperly shaped ripples/pressed highlights.
   - Bypassed premium cards (direct `Card` usage vs `PremiumCard`).
   - Abrupt state transitions (need for `AnimatedVisibility`, `Crossfade`, etc.).
   - State mutation during composition (specifically in `AddEditRecurringSheet.kt`).
   - Accessibility/semantics (ensure selectable controls expose proper state).
2. **Refinement Execution:** Apply fixes identified in the audit.
   - Update `AddEditRecurringSheet.kt` to move state mutation out of composition.
   - Apply `PremiumCard` styling consistently.
   - Add transition animations for UI state changes (Home dashboard toggle, Analytics periods, etc.).

## Phase 3: Verification
1. **Unit/Build Tests:** Run `gradle :app:testDebugUnitTest` and ensure compilation success.
2. **Manual Verification:**
   - **System Bars:** Toggle Light/Dark/AMOLED and confirm icon contrast.
   - **Ripples/Shapes:** Check rounded controls for clipped ripples.
   - **Transitions:** Validate smooth toggling in Dashboard, Analytics, Recurring, etc.
   - **Forms:** Verify category selection in `AddEditRecurringSheet.kt` for stability.
