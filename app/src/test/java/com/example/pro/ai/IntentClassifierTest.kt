package com.example.pro.ai

import org.junit.Assert.*
import org.junit.Test

class IntentClassifierTest {

    @Test
    fun `test intent classification`() {
        assertEquals(AiIntent.WHAT_IF_QUERY, IntentClassifier.classify("What if I spend 20000?"))
        assertEquals(AiIntent.SPENDING_ANALYSIS, IntentClassifier.classify("Where did my money go?"))
        assertEquals(AiIntent.FORECAST_QUERY, IntentClassifier.classify("How much can I spend this month?"))
        assertEquals(AiIntent.GENERAL_SUMMARY, IntentClassifier.classify("Hello"))
    }
}
