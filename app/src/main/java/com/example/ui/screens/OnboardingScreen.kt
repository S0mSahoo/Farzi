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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.getValue
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
  viewModel: FinanceViewModel
) {
  var isSigningIn by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { 8 })
  val pagerScope = rememberCoroutineScope()
  val currentPage = pagerState.currentPage

  @Suppress("DEPRECATION")
  val googleSignInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val intent = result.data
    val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
    try {
      val account = task.getResult(ApiException::class.java)
      if (account != null && !account.email.isNullOrBlank()) {
        Log.i("GoogleSignIn", "Google Sign-In successful")
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
            "v${com.example.BuildConfig.VERSION_NAME}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      // 2. Eight swipeable feature pages. Sign-in and Guest actions remain below and fixed.
      Column(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxWidth().heightIn(min = 270.dp, max = 390.dp).testTag("onboarding_pager"),
          key = { it }
        ) { page ->
          Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
          ) {
            when (page) {
              0 -> TrackHeroContent()
              1 -> OnboardingFeatureHero(
                eyebrow = "TRANSACTIONS",
                title = "Every entry,\nclearly organized.",
                description = "Record income and expenses with categories, notes, payment methods and dates.",
                highlights = listOf("Fast income and expense entry", "Search, filter and edit history", "A clear record of where money goes")
              )
              2 -> BudgetHeroContent()
              3 -> OnboardingFeatureHero(
                eyebrow = "CALENDAR",
                title = "See your money\nday by day.",
                description = "Explore each date, spot spending patterns and add transactions directly from your calendar.",
                highlights = listOf("Swipe between months", "See daily income and expenses", "Open the transactions for any date")
              )
              4 -> OnboardingFeatureHero(
                eyebrow = "RECURRING BILLS",
                title = "Stay ahead of\nregular payments.",
                description = "Keep subscriptions and recurring bills visible without automatically recording payments you have not made.",
                highlights = listOf("Daily, weekly, monthly and yearly rules", "Upcoming and overdue schedules", "Mark a payment paid only when it happens")
              )
              5 -> OnboardingFeatureHero(
                eyebrow = "ANALYTICS",
                title = "Understand the\npattern behind spending.",
                description = "Review monthly and yearly cash flow, category mix, budget use and meaningful changes over time.",
                highlights = listOf("Monthly and annual summaries", "Income versus expense trends", "Category insights grounded in recorded data")
              )
              6 -> OnboardingFeatureHero(
                eyebrow = "PAISA AI",
                title = "Ask questions\nabout your finances.",
                description = "Use Paisa AI to explore your recorded data, ask for practical guidance and propose changes for your confirmation.",
                highlights = listOf("Read-only financial queries", "Current external information when needed", "Review and confirm every proposed change")
              )
              else -> SyncHeroContent()
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          repeat(8) { index ->
            val selected = index == currentPage
            Box(
              modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(if (selected) 20.dp else 7.dp, 7.dp)
                .clip(CircleShape)
                .background(
                  if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                .clickable { pagerScope.launch { pagerState.animateScrollToPage(index) } }
                .testTag("onboarding_page_dot_$index")
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { pagerScope.launch { pagerState.animateScrollToPage((currentPage - 1).coerceAtLeast(0)) } },
            enabled = currentPage > 0 && !isSigningIn,
            modifier = Modifier.testTag("onboarding_back_button")
          ) {
            Text("Back")
          }
          Text(
            "Page ${currentPage + 1} of 8",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("onboarding_page_count")
          )
          Button(
            onClick = { pagerScope.launch { pagerState.animateScrollToPage((currentPage + 1).coerceAtMost(7)) } },
            enabled = currentPage < 7 && !isSigningIn,
            modifier = Modifier.testTag("onboarding_next_button")
          ) {
            Text("Next")
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
            "Guest data stays local on device. Optional encrypted Google Drive sync; Paisa AI processes queries via Google Gemini API.",
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
private fun OnboardingFeatureHero(
  eyebrow: String,
  title: String,
  description: String,
  highlights: List<String>
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      eyebrow,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.3.sp
    )
    Text(
      title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.ExtraBold,
      lineHeight = 30.sp
    )
    Text(
      description,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = 18.sp
    )
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
      ) {
        highlights.forEach { highlight ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              highlight,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
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
      "Encrypted Cloud Sync.\nYour data, your control.",
      style = MaterialTheme.typography.titleLarge,
      fontSize = 21.sp,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.ExtraBold,
      lineHeight = 27.sp
    )
    Text(
      "Your financial records back up directly into your personal Google Drive account with end-to-end passphrase encryption.",
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
        BulletCheckRow(label = "Encrypted Google Drive sync with recovery passphrase")
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
