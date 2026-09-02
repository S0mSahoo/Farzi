package com.example.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppBrandLogo
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.FinanceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

@Composable
fun OnboardingScreen(
  viewModel: FinanceViewModel
) {
  var isSigningIn by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val googleSignInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val intent = result.data
    val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
    try {
      val account = task.getResult(ApiException::class.java)
      if (account != null && !account.email.isNullOrBlank()) {
        Log.i("GoogleSignIn", "Google Sign-In successful for email: ${account.email}")
        statusMessage = "Connecting with ${account.displayName ?: account.email}..."
        viewModel.onGoogleSignInSuccess(account) { success, error ->
          isSigningIn = false
          if (!success) {
            errorMessage = error ?: "Could not sync cloud data. You can retry anytime in Settings."
          }
        }
      } else {
        isSigningIn = false
        errorMessage = "Could not retrieve Google account details. Please try again."
      }
    } catch (e: ApiException) {
      isSigningIn = false
      statusMessage = null
      Log.e("GoogleSignIn", "Google Sign-In ApiException: statusCode=${e.statusCode}, message=${e.localizedMessage}", e)
      when (e.statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED, 12501 -> {
          // Clean cancellation by user - do not display an error message
          errorMessage = null
        }
        GoogleSignInStatusCodes.NETWORK_ERROR, 7 -> {
          errorMessage = "Network connection error. Please check your internet connection."
        }
        GoogleSignInStatusCodes.DEVELOPER_ERROR, 10 -> {
          errorMessage = "Google Play Services authentication error (Code 10). Please try again."
        }
        12500 -> {
          errorMessage = "Google Sign-In failed on this device (Code 12500). Please check your Google Account settings."
        }
        else -> {
          errorMessage = "Google Sign-In could not be completed (${e.statusCode}). Please try again."
        }
      }
    } catch (e: Exception) {
      isSigningIn = false
      statusMessage = null
      Log.e("GoogleSignIn", "Unexpected sign-in error", e)
      errorMessage = e.localizedMessage ?: "An unexpected error occurred during Google sign-in."
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Abstract Minimal Brand Logo
      AppBrandLogo(size = 80.dp)

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Paisa",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Personal finance management with seamless Google Drive cloud sync.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Cloud & Security Benefits Surface
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          FeatureRow(
            icon = Icons.Default.CloudDone,
            title = "Google Drive Cloud Storage",
            description = "Your financial records are saved to your private Google Drive app storage."
          )
          FeatureRow(
            icon = Icons.Default.Sync,
            title = "Cross-Device Synchronization",
            description = "Access and update your income, expenses, and budgets seamlessly anywhere."
          )
          FeatureRow(
            icon = Icons.Default.Lock,
            title = "Private & Secure",
            description = "Zero manual onboarding. Your profile is automatically linked to your Google Account."
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = ExpenseRed.copy(alpha = 0.1f),
          border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = ExpenseRed,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
          )
        }
      }

      // Sign In with Google Button
      Button(
        onClick = {
          if (!isSigningIn) {
            isSigningIn = true
            errorMessage = null
            statusMessage = "Opening Google Sign-In..."
            googleSignInLauncher.launch(viewModel.getDriveSignInIntent())
          }
        },
        enabled = !isSigningIn,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("google_sign_in_button")
      ) {
        if (isSigningIn) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.5.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = statusMessage ?: "Signing In...",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Surface(
              shape = CircleShape,
              color = Color.White,
              modifier = Modifier.size(24.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "G",
                  fontWeight = FontWeight.Black,
                  fontSize = 14.sp,
                  color = Color(0xFF4285F4)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
      modifier = Modifier.size(36.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
      )
    }
  }
}
