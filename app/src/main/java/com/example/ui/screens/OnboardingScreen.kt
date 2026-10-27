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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppBrandLogo
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
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Brand row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AppBrandLogo(size = 46.dp)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(
            text = "Paisa",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Your personal finance companion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }
      }

      // Rounded hero panel with subtle native illustration rather than a static screenshot.
      Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          val glowPrimary = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
          val glowPrimaryInner = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
          val glowTertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
          val glowIncome = IncomeGreen.copy(alpha = 0.14f)
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(132.dp)
          ) {
            drawCircle(
              color = glowPrimary,
              radius = size.minDimension * 0.47f,
              center = Offset(size.width * 0.5f, size.height * 0.48f)
            )
            drawCircle(
              color = glowPrimaryInner,
              radius = size.minDimension * 0.31f,
              center = Offset(size.width * 0.5f, size.height * 0.48f)
            )
            drawCircle(
              color = glowTertiary,
              radius = size.minDimension * 0.14f,
              center = Offset(size.width * 0.72f, size.height * 0.28f)
            )
            drawCircle(
              color = glowIncome,
              radius = size.minDimension * 0.11f,
              center = Offset(size.width * 0.28f, size.height * 0.74f)
            )
          }
          Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(70.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
              )
            }
          }
          Text(
            text = "Your finances, all in one place",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          Text(
            text = "Track. Plan. Grow.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )
          Text(
            text = "Understand your money with a clear view of income, spending, budgets, and savings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }

      // Features use the real app capabilities; no additional permissions or services are implied.
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
          FeatureRow(
            icon = Icons.Default.TrendingUp,
            title = "See your money clearly",
            description = "Track recorded income, expenses, budgets, and monthly summaries."
          )
          FeatureRow(
            icon = Icons.Default.CloudDone,
            title = "Google Drive backup",
            description = "Keep your records in the private app storage area of your Google Drive."
          )
          FeatureRow(
            icon = Icons.Default.Lock,
            title = "Privacy and control",
            description = "Use your Google account to connect and manage your financial records."
          )
        }
      }

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = ExpenseRed.copy(alpha = 0.11f),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("google_sign_in_error")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = ExpenseRed,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = ExpenseRed,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

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
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 58.dp)
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
            text = statusMessage ?: "Signing in...",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Surface(
              shape = CircleShape,
              color = Color.White,
              modifier = Modifier.size(28.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "G",
                  fontWeight = FontWeight.Black,
                  fontSize = 16.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Continue with Google",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          }
        }
      }

      Text(
        text = "You can review backup, privacy, and security options in Settings after signing in.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp)
          .padding(bottom = 12.dp)
      )
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
