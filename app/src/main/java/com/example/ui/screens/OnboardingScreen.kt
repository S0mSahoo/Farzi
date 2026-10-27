package com.example.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppBrandLogo
import com.example.ui.components.PremiumCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
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
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 620.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
      ) {
        AppBrandLogo(size = 42.dp)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text("Paisa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          Text("A clearer relationship with money", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF163D2B),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.10f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.spacedBy(7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFD4EF75)))
              Text("PERSONAL FINANCE, MADE CLEAR", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f), fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            }
          }

          Text(
            "Know where\nyour money goes.",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp
          )
          Text(
            "Track your spending, plan each month, and see the bigger picture — without the clutter.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.76f)
          )

          Canvas(modifier = Modifier.fillMaxWidth().height(116.dp)) {
            val baseline = size.height * 0.88f
            val heights = listOf(0.28f, 0.48f, 0.36f, 0.65f, 0.55f, 0.82f, 0.72f, 0.96f)
            val gap = size.width * 0.022f
            val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
            drawLine(
              color = Color.White.copy(alpha = 0.16f),
              start = Offset(0f, baseline),
              end = Offset(size.width, baseline),
              strokeWidth = 1.dp.toPx()
            )
            heights.forEachIndexed { index, fraction ->
              val left = index * (barWidth + gap)
              val top = baseline - (baseline - 9.dp.toPx()) * fraction
              drawRoundRect(
                color = if (index == heights.lastIndex) Color(0xFFD4EF75) else Color.White.copy(alpha = 0.16f + index * 0.055f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, baseline - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OnboardingPill("Track")
            OnboardingPill("Plan")
            OnboardingPill("Grow")
          }
        }
      }

      PremiumCard {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(17.dp)
        ) {
          Text("Built for real life", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          FeatureRow(
            icon = Icons.Default.TrendingUp,
            title = "See your money clearly",
            description = "Follow recorded income, expenses, budgets and monthly patterns."
          )
          FeatureRow(
            icon = Icons.Default.CloudDone,
            title = "Your Google Drive backup",
            description = "Keep your financial records available through your connected Google account."
          )
          FeatureRow(
            icon = Icons.Default.Lock,
            title = "Privacy and control",
            description = "Choose how you connect and manage your financial records."
          )
        }
      }

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = ExpenseRed.copy(alpha = 0.10f),
          modifier = Modifier.fillMaxWidth().testTag("google_sign_in_error")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
            Text(
              errorMessage.orEmpty(),
              modifier = Modifier.weight(1f),
              style = MaterialTheme.typography.bodySmall,
              color = ExpenseRed,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
          modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("google_sign_in_button")
        ) {
          if (isSigningIn) {
            CircularProgressIndicator(modifier = Modifier.size(21.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            Spacer(Modifier.width(11.dp))
            Text(statusMessage ?: "Connecting...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
          } else {
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(27.dp)) {
              Box(contentAlignment = Alignment.Center) {
                Text("G", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
              }
            }
            Spacer(Modifier.width(10.dp))
            Text("Continue with Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }

        androidx.compose.material3.OutlinedButton(
          onClick = { viewModel.loginAsGuest() },
          enabled = !isSigningIn,
          shape = RoundedCornerShape(18.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
          modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("guest_login_button")
        ) {
          Text("Continue as Guest", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
      }

      Text(
        "Guest records stay on this device. Connect Google Drive from an account session to use online backup.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
      )
      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun OnboardingPill(label: String) {
  Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.11f)) {
    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
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
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp,
        maxLines = 3,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
    }
  }
}
