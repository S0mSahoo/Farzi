package com.example.pro.entitlement

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Development-only implementation of [ProEntitlementProvider].
 * Allows instant local toggling between FREE and PRO for testing during development.
 * Persists the toggle state in dedicated SharedPreferences.
 */
class DevelopmentProEntitlementProvider(context: Context) : ProEntitlementProvider {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _stateFlow = MutableStateFlow(readCurrentState())
    override val entitlementStateFlow: StateFlow<EntitlementState> = _stateFlow.asStateFlow()

    private fun readCurrentState(): EntitlementState {
        val isPro = prefs.getBoolean(KEY_PRO_ENABLED, false)
        return if (isPro) EntitlementState.PRO else EntitlementState.FREE
    }

    override fun getEntitlementState(): EntitlementState {
        return readCurrentState()
    }

    /**
     * Toggles the Pro entitlement state for local development / testing.
     */
    fun setProEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRO_ENABLED, enabled).apply()
        _stateFlow.value = if (enabled) EntitlementState.PRO else EntitlementState.FREE
    }

    companion object {
        private const val PREFS_NAME = "paisa_dev_entitlement_prefs"
        private const val KEY_PRO_ENABLED = "dev_pro_enabled"
    }
}
