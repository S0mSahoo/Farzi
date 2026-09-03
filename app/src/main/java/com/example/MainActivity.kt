package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
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

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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
        // Apply FLAG_SECURE for the entire app to protect recents
        val window = (context as? FragmentActivity)?.window
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
        val observer = LifecycleEventObserver { _, event ->
          when (event) {
            Lifecycle.Event.ON_RESUME -> {
              viewModel.triggerAutoSyncOnResume()
              viewModel.refreshDriveStorageQuota()
            }
            Lifecycle.Event.ON_STOP -> {
              // Only lock if device is actually locked? Or just keep it simpler?
              // The user wants it to wait for screen lock/unlock. 
              // For now, let's keep it simple as requested, but maybe not lock on stop.
            }
            else -> {}
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
          // Keep FLAG_SECURE active
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

