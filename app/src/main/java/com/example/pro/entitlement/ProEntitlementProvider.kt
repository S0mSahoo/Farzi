package com.example.pro.entitlement

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface abstraction for determining entitlement state.
 * Allows decoupling between Development, Production (Free), and future Google Play Billing.
 */
interface ProEntitlementProvider {
    /**
     * Synchronous snapshot of current entitlement status.
     */
    fun getEntitlementState(): EntitlementState

    /**
     * Reactive state flow observing entitlement changes.
     */
    val entitlementStateFlow: StateFlow<EntitlementState>
}
