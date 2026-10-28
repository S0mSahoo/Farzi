package com.example.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberPaisaHaptics(): PaisaHaptics {
    val view = LocalView.current
    return remember(view) {
        PaisaHaptics(view)
    }
}

class PaisaHaptics(private val view: View) {
    fun performLightHaptic() {
        // Using CONTEXT_CLICK for a subtle, modern feel
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun performClickHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
