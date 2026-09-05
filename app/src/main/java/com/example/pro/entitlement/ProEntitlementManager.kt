package com.example.pro.entitlement

import kotlinx.coroutines.flow.StateFlow

/**
 * Centralized gateway for querying Paisa Pro entitlement across the entire application.
 * All UI composables and viewmodels check access via [hasAccess] rather than hardcoding logic.
 *
 * @param provider The underlying [ProEntitlementProvider] strategy.
 */
class ProEntitlementManager(
    private val provider: ProEntitlementProvider
) {

    /**
     * Reactive state flow of current entitlement state.
     */
    val entitlementStateFlow: StateFlow<EntitlementState> = provider.entitlementStateFlow

    /**
     * Checks if the user currently has access to a specific [ProFeature].
     *
     * In the baseline entitlement model, having [EntitlementState.PRO] grants access to all Pro features.
     */
    fun hasAccess(feature: ProFeature): Boolean {
        return when (provider.getEntitlementState()) {
            EntitlementState.PRO -> true
            EntitlementState.FREE -> false
        }
    }

    /**
     * Retrieves the current snapshot [EntitlementState].
     */
    fun getEntitlementState(): EntitlementState {
        return provider.getEntitlementState()
    }

    /**
     * Returns the underlying provider for debug operations if applicable.
     */
    fun getProvider(): ProEntitlementProvider = provider
}
