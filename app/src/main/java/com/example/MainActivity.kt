package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: FinanceViewModel = viewModel()
      val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
      val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()

      val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      val lifecycleOwner = LocalLifecycleOwner.current
      val context = LocalContext.current

      // Financial screens display balances and transaction details. Keep screenshots and the
      // Android Recents thumbnail disabled whenever the authenticated finance UI is visible.
      DisposableEffect(userProfile.hasCompletedOnboarding) {
        if (userProfile.hasCompletedOnboarding) {
          window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
      }

      DisposableEffect(lifecycleOwner) {
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

