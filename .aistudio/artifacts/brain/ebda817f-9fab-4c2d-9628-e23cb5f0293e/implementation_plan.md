# Comprehensive UI Consistency, Interaction, and State Fix Plan

## Phase 1 — Finish & Verify System-Bar Behavior (`MainActivity.kt`)
- Verify status-bar and navigation-bar icon contrast logic using `WindowCompat.getInsetsController()` in `SideEffect`.
- Ensure light mode shows dark icons and dark/AMOLED mode shows light icons, updating dynamically on theme changes.

## Phase 2 — Fix Rectangular Ripple and Press-Highlight Defects (`AddEditRecurringSheet.kt`)
- Refactor recurrence interval selector buttons, category chips, and start-date selector to use shape-aware `Surface(onClick = ..., shape = ...)` instead of `modifier = Modifier.clickable(...)` without proper clipping. This guarantees shape-bound press ripples.

## Phase 3 — Complete Transitions Across Screens
- Add smooth, restrained transitions (`AnimatedContent` / `Crossfade`) for period toggles (`DashboardScreen`, `AnalyticsScreen`), tab switches (`RecurringScreen`), and type switches (`CategoriesScreen`).

## Phase 4 — Unify Card Depth (`SettingsScreen.kt`, `AnalyticsScreen.kt`)
- Replace direct zero-elevation Material 3 `Card` instances in `SettingsScreen.kt` and `AnalyticsScreen.kt` with `PremiumCard` to ensure consistent depth, borders, and visual hierarchy across Light and AMOLED Dark modes.

## Phase 5 — Correct Recurring Form State & Semantics (`AddEditRecurringSheet.kt`)
- Refactor `AddEditRecurringSheet` type switching to update category state directly in the user event handler rather than relying on `LaunchedEffect` during/after composition.
- Add proper accessibility selection semantics (`semantics { role = Role.RadioButton / Tab / ToggleButton }` or `selected = ...`) where needed.

## Phase 6 — Testing & Build Verification
- Run `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` to verify build integrity.
