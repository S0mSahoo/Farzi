package com.example.pro.entitlement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of [ProEntitlementProvider].
 * Defaults strictly to [EntitlementState.FREE] without any backdoor or bypass.
 * When Google Play Billing is introduced, this or a dedicated billing provider will handle subscriptions.
 */
class ProductionProEntitlementProvider : ProEntitlementProvider {

    private val _stateFlow = MutableStateFlow(EntitlementState.FREE)
    override val entitlementStateFlow: StateFlow<EntitlementState> = _stateFlow.asStateFlow()

    override fun getEntitlementState(): EntitlementState {
        return EntitlementState.FREE
    }
}
