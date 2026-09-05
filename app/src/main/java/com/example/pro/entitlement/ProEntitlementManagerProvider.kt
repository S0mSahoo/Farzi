package com.example.pro.entitlement

import android.content.Context
import com.example.BuildConfig

/**
 * Global provider / holder for accessing the singleton [ProEntitlementManager].
 * Enforces strict build-variant separation:
 * - Debug builds: uses [DevelopmentProEntitlementProvider] for easy local testing.
 * - Release builds: strictly uses [ProductionProEntitlementProvider] (always FREE until Billing is integrated).
 */
object ProEntitlementManagerProvider {

    @Volatile
    private var instance: ProEntitlementManager? = null

    /**
     * Initializes the manager with the build-appropriate provider.
     */
    fun initialize(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    val provider: ProEntitlementProvider = if (BuildConfig.DEBUG) {
                        DevelopmentProEntitlementProvider(context.applicationContext)
                    } else {
                        ProductionProEntitlementProvider()
                    }
                    instance = ProEntitlementManager(provider)
                }
            }
        }
    }

    /**
     * Retrieves the initialized [ProEntitlementManager].
     */
    fun get(): ProEntitlementManager {
        return instance ?: throw IllegalStateException(
            "ProEntitlementManager has not been initialized. Ensure initialize() is called in Application or Activity."
        )
    }

    /**
     * Allows custom injection for unit tests or mocking.
     */
    fun setForTesting(manager: ProEntitlementManager?) {
        instance = manager
    }
}
