package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.MinimalIndigo
import com.example.util.BiometricAuthManager

@Composable
fun AppLockScreen(
  onUnlockSuccess: () -> Unit
) {
  val context = LocalContext.current
  val activity = context as? FragmentActivity

  fun triggerBiometric() {
    if (activity != null && BiometricAuthManager.canAuthenticate(activity)) {
      BiometricAuthManager.authenticate(
        activity = activity,
        title = "Unlock Paisa",
        subtitle = "Confirm your fingerprint or screen lock to access your financial data",
        onSuccess = onUnlockSuccess,
        onError = { /* User can tap Unlock button to retry */ }
      )
    } else {
      // If biometrics not enrolled, unlock directly
      onUnlockSuccess()
    }
  }

  LaunchedEffect(Unit) {
    triggerBiometric()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("app_lock_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        shape = CircleShape,
        color = MinimalIndigo.copy(alpha = 0.12f),
        modifier = Modifier.size(96.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MinimalIndigo,
            modifier = Modifier.size(44.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = "Paisa is Locked",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Your financial records are protected by hardware encryption and biometric lock.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(36.dp))

      Button(
        onClick = { triggerBiometric() },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MinimalIndigo
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("app_lock_unlock_button")
      ) {
        Icon(
          imageVector = Icons.Default.Fingerprint,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = "Unlock with Biometrics",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
