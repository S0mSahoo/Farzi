package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: FinanceViewModel = viewModel()
      val userProfile by viewModel.userProfile.collectAsState()

      MyApplicationTheme {
        Crossfade(
          targetState = userProfile.hasCompletedOnboarding,
          label = "onboarding_crossfade"
        ) { completed ->
          if (completed) {
            MainAppScreen(viewModel = viewModel)
          } else {
            OnboardingScreen(
              onComplete = { name ->
                viewModel.completeOnboarding(name)
              }
            )
          }
        }
      }
    }
  }
}
