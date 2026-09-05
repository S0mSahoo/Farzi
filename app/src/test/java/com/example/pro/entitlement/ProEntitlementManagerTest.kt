package com.example.pro.entitlement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProEntitlementManagerTest {

    // Test Fake Provider that allows dynamic control
    private class FakeProEntitlementProvider(
        initialState: EntitlementState = EntitlementState.FREE
    ) : ProEntitlementProvider {
        private val _stateFlow = MutableStateFlow(initialState)
        override val entitlementStateFlow: StateFlow<EntitlementState> = _stateFlow.asStateFlow()

        override fun getEntitlementState(): EntitlementState = _stateFlow.value

        fun setState(state: EntitlementState) {
            _stateFlow.value = state
        }
    }

    @Test
    fun `default FREE state returns false for hasAccess across all Pro features`() {
        val fakeProvider = FakeProEntitlementProvider(EntitlementState.FREE)
        val manager = ProEntitlementManager(fakeProvider)

        assertEquals(EntitlementState.FREE, manager.getEntitlementState())

        // Verify all defined ProFeature enums return false
        ProFeature.entries.forEach { feature ->
            assertFalse(
                "Expected feature $feature to be inaccessible in FREE state",
                manager.hasAccess(feature)
            )
        }
    }

    @Test
    fun `PRO state returns true for hasAccess across all Pro features`() {
        val fakeProvider = FakeProEntitlementProvider(EntitlementState.PRO)
        val manager = ProEntitlementManager(fakeProvider)

        assertEquals(EntitlementState.PRO, manager.getEntitlementState())

        // Verify all defined ProFeature enums return true
        ProFeature.entries.forEach { feature ->
            assertTrue(
                "Expected feature $feature to be accessible in PRO state",
                manager.hasAccess(feature)
            )
        }
    }

    @Test
    fun `switching entitlement dynamically updates hasAccess for each feature`() {
        val fakeProvider = FakeProEntitlementProvider(EntitlementState.FREE)
        val manager = ProEntitlementManager(fakeProvider)

        // Initial FREE check
        assertFalse(manager.hasAccess(ProFeature.AI_COPILOT))
        assertFalse(manager.hasAccess(ProFeature.CASH_FLOW_FORECAST))
        assertFalse(manager.hasAccess(ProFeature.WHAT_IF_SIMULATOR))
        assertFalse(manager.hasAccess(ProFeature.FINANCIAL_GOALS))
        assertFalse(manager.hasAccess(ProFeature.SMART_INSIGHTS))
        assertFalse(manager.hasAccess(ProFeature.INTELLIGENT_ALERTS))
        assertFalse(manager.hasAccess(ProFeature.MONTHLY_REVIEW))
        assertFalse(manager.hasAccess(ProFeature.NET_WORTH))

        // Upgrade to PRO
        fakeProvider.setState(EntitlementState.PRO)
        assertEquals(EntitlementState.PRO, manager.getEntitlementState())
        assertTrue(manager.hasAccess(ProFeature.AI_COPILOT))
        assertTrue(manager.hasAccess(ProFeature.CASH_FLOW_FORECAST))
        assertTrue(manager.hasAccess(ProFeature.WHAT_IF_SIMULATOR))
        assertTrue(manager.hasAccess(ProFeature.FINANCIAL_GOALS))
        assertTrue(manager.hasAccess(ProFeature.SMART_INSIGHTS))
        assertTrue(manager.hasAccess(ProFeature.INTELLIGENT_ALERTS))
        assertTrue(manager.hasAccess(ProFeature.MONTHLY_REVIEW))
        assertTrue(manager.hasAccess(ProFeature.NET_WORTH))

        // Downgrade back to FREE
        fakeProvider.setState(EntitlementState.FREE)
        assertEquals(EntitlementState.FREE, manager.getEntitlementState())
        assertFalse(manager.hasAccess(ProFeature.AI_COPILOT))
        assertFalse(manager.hasAccess(ProFeature.CASH_FLOW_FORECAST))
    }

    @Test
    fun `ProductionProEntitlementProvider strictly returns FREE`() {
        val prodProvider = ProductionProEntitlementProvider()
        val manager = ProEntitlementManager(prodProvider)

        assertEquals(EntitlementState.FREE, prodProvider.getEntitlementState())
        assertEquals(EntitlementState.FREE, prodProvider.entitlementStateFlow.value)
        assertEquals(EntitlementState.FREE, manager.getEntitlementState())

        ProFeature.entries.forEach { feature ->
            assertFalse(
                "Production provider must strictly deny access to $feature",
                manager.hasAccess(feature)
            )
        }
    }
}
