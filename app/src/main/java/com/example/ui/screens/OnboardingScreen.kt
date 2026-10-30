package com.example.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppBrandLogo
import com.example.ui.theme.ExpenseRed
import com.example.ui.util.PaisaMotion
import com.example.ui.viewmodel.FinanceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
  viewModel: FinanceViewModel
) {
  var isSigningIn by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var currentPage by remember { mutableIntStateOf(0) }

  // Auto advance pager every 4.2 seconds
  LaunchedEffect(currentPage, isSigningIn) {
    if (!isSigningIn) {
      delay(4200)
      currentPage = (currentPage + 1) % 3
    }
  }

  @Suppress("DEPRECATION")
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
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. Top Brand Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          AppBrandLogo(size = 38.dp)
          Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
              "Paisa",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              "Smart Personal Finance",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
          modifier = Modifier.clip(CircleShape)
        ) {
          Text(
            "v2.5 Premium",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      // 2. Main Content Area Blended Seamlessly into Background (No Outer Box)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Floating Tab Switcher
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            OnboardingTabButton(
              title = "Track",
              icon = Icons.AutoMirrored.Filled.TrendingUp,
              isSelected = currentPage == 0,
              onClick = { currentPage = 0 },
              modifier = Modifier.weight(1f)
            )
            OnboardingTabButton(
              title = "Budgets",
              icon = Icons.Default.Savings,
              isSelected = currentPage == 1,
              onClick = { currentPage = 1 },
              modifier = Modifier.weight(1f)
            )
            OnboardingTabButton(
              title = "Private Sync",
              icon = Icons.Default.Shield,
              isSelected = currentPage == 2,
              onClick = { currentPage = 2 },
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Dynamic Animated Content Blended Directly on Background
        AnimatedContent(
          targetState = currentPage,
          transitionSpec = {
            if (targetState > initialState) {
              (slideInHorizontally(animationSpec = tween(PaisaMotion.EnterFadeDurationMillis)) { width -> width } + fadeIn(animationSpec = tween(PaisaMotion.EnterFadeDurationMillis))).togetherWith(
                slideOutHorizontally(animationSpec = tween(PaisaMotion.ExitFadeDurationMillis)) { width -> -width } + fadeOut(animationSpec = tween(PaisaMotion.ExitFadeDurationMillis))
              )
            } else {
              (slideInHorizontally(animationSpec = tween(PaisaMotion.EnterFadeDurationMillis)) { width -> -width } + fadeIn(animationSpec = tween(PaisaMotion.EnterFadeDurationMillis))).togetherWith(
                slideOutHorizontally(animationSpec = tween(PaisaMotion.ExitFadeDurationMillis)) { width -> width } + fadeOut(animationSpec = tween(PaisaMotion.ExitFadeDurationMillis))
              )
            }
          },
          label = "onboarding_blended_hero"
        ) { page ->
          when (page) {
            0 -> TrackHeroContent()
            1 -> BudgetHeroContent()
            else -> SyncHeroContent()
          }
        }

        // Page Indicator Dots
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          repeat(3) { index ->
            val isSelected = index == currentPage
            Box(
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(if (isSelected) 22.dp else 7.dp, 7.dp)
                .clip(CircleShape)
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                .clickable { currentPage = index }
            )
          }
        }
      }

      // 3. Action Buttons & Security Footer Deck
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = ExpenseRed.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth().testTag("google_sign_in_error")
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = ExpenseRed,
                modifier = Modifier.size(16.dp)
              )
              Text(
                errorMessage.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = ExpenseRed,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Google Sign-In Button
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
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("google_sign_in_button")
        ) {
          if (isSigningIn) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
              statusMessage ?: "Connecting...",
              style = MaterialTheme.typography.titleSmall,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          } else {
            Surface(
              shape = CircleShape,
              color = Color.White,
              modifier = Modifier.size(24.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  "G",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Black,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
            Spacer(Modifier.width(10.dp))
            Text(
              "Continue with Google",
              style = MaterialTheme.typography.titleSmall,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Guest Mode Button
        OutlinedButton(
          onClick = { viewModel.loginAsGuest() },
          enabled = !isSigningIn,
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("guest_login_button")
        ) {
          Text(
            "Explore as Guest",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        // Security Notice
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp)
          )
          Spacer(Modifier.width(4.dp))
          Text(
            "100% Private. Guest data stays local; Google Drive sync is encrypted.",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun OnboardingTabButton(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val activeColor = MaterialTheme.colorScheme.primary
  val activeContent = MaterialTheme.colorScheme.onPrimary
  val inactiveContent = MaterialTheme.colorScheme.onSurfaceVariant

  val bgColor by androidx.compose.animation.animateColorAsState(
    targetValue = if (isSelected) activeColor else Color.Transparent,
    animationSpec = tween(PaisaMotion.EnterFadeDurationMillis),
    label = "tab_bg_color"
  )
  val contentColor by androidx.compose.animation.animateColorAsState(
    targetValue = if (isSelected) activeContent else inactiveContent,
    animationSpec = tween(PaisaMotion.EnterFadeDurationMillis),
    label = "tab_content_color"
  )

  Box(
    modifier = modifier
      .clip(CircleShape)
      .background(bgColor)
      .clickable(onClick = onClick)
      .padding(vertical = 7.dp, horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(13.dp)
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun TrackHeroContent() {
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      "Master your spending\nwith complete clarity.",
      style = MaterialTheme.typography.titleLarge,
      fontSize = 21.sp,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.ExtraBold,
      lineHeight = 27.sp
    )
    Text(
      "Categorize income and expenses effortlessly with instant visual breakdowns.",
      style = MaterialTheme.typography.bodySmall,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = 17.sp
    )

    // Sleek Blended Glass Preview Card
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "Monthly Overview",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            "₹48,250",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
          val baseline = size.height * 0.88f
          val heights = listOf(0.35f, 0.55f, 0.40f, 0.75f, 0.60f, 0.90f, 0.80f, 1.0f)
          val gap = size.width * 0.025f
          val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
          heights.forEachIndexed { index, fraction ->
            val left = index * (barWidth + gap)
            val top = baseline - (baseline - 4.dp.toPx()) * fraction
            val primaryPx = Color(0xFF8B5CF6)
            val barColor = if (index == heights.lastIndex) primaryPx else primaryPx.copy(alpha = 0.20f + index * 0.08f)
            drawRoundRect(
              color = barColor,
              topLeft = Offset(left, top),
              size = Size(barWidth, baseline - top),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BudgetHeroContent() {
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      "Smart budgets that\nkeep you on track.",
      style = MaterialTheme.typography.titleLarge,
      fontSize = 21.sp,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.ExtraBold,
      lineHeight = 27.sp
    )
    Text(
      "Set custom category limits, recurring payments, and monitor savings goals.",
      style = MaterialTheme.typography.bodySmall,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = 17.sp
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
            Text("Monthly Budget", style = MaterialTheme.typography.labelSmall, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
          }
          Text("65% Used", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
        }

        // Progress Bar
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(0.65f)
              .height(8.dp)
              .clip(CircleShape)
              .background(
                Brush.horizontalGradient(
                  colors = listOf(Color(0xFF34D399), Color(0xFF10B981))
                )
              )
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Spent: ₹32,500", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("Limit: ₹50,000", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun SyncHeroContent() {
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      "Your private cloud.\nZero third-party servers.",
      style = MaterialTheme.typography.titleLarge,
      fontSize = 21.sp,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.ExtraBold,
      lineHeight = 27.sp
    )
    Text(
      "Your financial records back up directly into your personal Google Drive account.",
      style = MaterialTheme.typography.bodySmall,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = 17.sp
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        BulletCheckRow(label = "Encrypted Google Drive cloud sync")
        BulletCheckRow(label = "Instant restore across your Android devices")
        BulletCheckRow(label = "Full offline support in Guest mode")
      }
    }
  }
}

@Composable
private fun BulletCheckRow(label: String) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(15.dp)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
