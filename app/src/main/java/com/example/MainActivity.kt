package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ThemeMode
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.pro.entitlement.ProEntitlementManagerProvider

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ProEntitlementManagerProvider.initialize(this)
    enableEdgeToEdge()
    setContent {
      val viewModel: FinanceViewModel = viewModel()
      val userProfile by viewModel.userProfile.collectAsState()
      val themeMode by viewModel.themeMode.collectAsState()
      val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
      val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()

      val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      val lifecycleOwner = LocalLifecycleOwner.current
      val context = LocalContext.current
      DisposableEffect(lifecycleOwner) {
        // Global FLAG_SECURE removed to allow preview; sensitive screens will apply it locally.
        
        val observer = LifecycleEventObserver { _, event ->
          when (event) {
            Lifecycle.Event.ON_RESUME -> {
              viewModel.triggerAutoSyncOnResume()
              viewModel.refreshDriveStorageQuota()
              if (System.currentTimeMillis() - viewModel.lastStopTime > 60_000L) {
                viewModel.lockApp()
              }
            }
            Lifecycle.Event.ON_STOP -> {
              viewModel.lastStopTime = System.currentTimeMillis()
            }
            else -> {}
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
        }
      }

      MyApplicationTheme(darkTheme = isDark) {
        if (isAppLockEnabled && !isAppUnlocked) {
          AppLockScreen(
            onUnlockSuccess = { viewModel.unlockApp() }
          )
        } else {
          Crossfade(
            targetState = userProfile.hasCompletedOnboarding && userProfile.email.isNotBlank(),
            label = "onboarding_crossfade"
          ) { completed ->
            if (completed) {
              MainAppScreen(viewModel = viewModel)
            } else {
              OnboardingScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}

