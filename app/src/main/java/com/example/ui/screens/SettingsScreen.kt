package com.example.ui.screens

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.data.drive.GoogleDriveState
import com.example.data.model.ThemeMode
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumOutlinedButton
import com.example.ui.components.PremiumPageHeader
import com.example.ui.components.PremiumTextButton
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalBlue
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BiometricAuthManager
import com.example.util.CryptoManager
import com.example.util.JsonValidationResult
import com.example.util.PaisaJsonBackup
import com.example.util.ValidationSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
  viewModel: FinanceViewModel,
  onOpenExportModal: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val googleDriveState by viewModel.googleDriveState.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
  val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
  val syncErrorMessage by viewModel.syncErrorMessage.collectAsStateWithLifecycle()
  val driveConsentIntent by viewModel.driveConsentIntent.collectAsStateWithLifecycle()
  val driveQuota by viewModel.driveStorageQuota.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
  val universalCal by viewModel.selectedCalendar.collectAsStateWithLifecycle()
  val isGuestSession = userProfile.email.equals("guest@paisa.local", ignoreCase = true) &&
    userProfile.googleId.isNullOrBlank()
  var isRecoveryPassphraseConfigured by remember {
    mutableStateOf(runCatching { CryptoManager.getStoredCloudRecoveryPassphrase(context) != null }.getOrDefault(false))
  }
  var showRecoveryPassphraseDialog by remember { mutableStateOf(false) }
  var recoveryPassphraseInput by remember { mutableStateOf("") }
  var recoveryPassphraseConfirm by remember { mutableStateOf("") }
  var recoveryPassphraseError by remember { mutableStateOf<String?>(null) }

  var notificationsAllowed by remember { mutableStateOf(hasNotificationAccess(context)) }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    notificationsAllowed = hasNotificationAccess(context)
    Toast.makeText(
      context,
      if (notificationsAllowed) "Payment reminders are enabled"
      else if (granted) "Notifications are disabled in Android Settings"
      else "Permission not granted. You can enable notifications in Android Settings.",
      Toast.LENGTH_LONG
    ).show()
  }
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = hasNotificationAccess(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  var isExportingJson by remember { mutableStateOf(false) }
  var isValidatingImport by remember { mutableStateOf(false) }
  var showBulkDeleteDialog by remember { mutableStateOf(false) }
  var isDeletingBulk by remember { mutableStateOf(false) }
  var bulkDeleteOption by remember { mutableStateOf("MONTH") } // "MONTH" or "OLDER_30"


  // Import Dialog Confirmation State
  var pendingBackupToImport by remember { mutableStateOf<PaisaJsonBackup?>(null) }
  var importValidationSummary by remember { mutableStateOf<ValidationSummary?>(null) }
  var showImportConfirmDialog by remember { mutableStateOf(false) }

  val jsonPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: android.net.Uri? ->
    if (uri != null) {
      isValidatingImport = true
      coroutineScope.launch {
        when (val result = viewModel.validateImportFile(context, uri)) {
          is JsonValidationResult.Success -> {
            isValidatingImport = false
            pendingBackupToImport = result.backup
            importValidationSummary = result.summary
            showImportConfirmDialog = true
          }
          is JsonValidationResult.Error -> {
            isValidatingImport = false
            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  val consentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { _ ->
    viewModel.clearConsentIntent()
    viewModel.syncNow(
      onSuccessMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
      onErrorMessage = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    )
  }

  var showSignOutDialog by remember { mutableStateOf(false) }
  var showClearDataDialog by remember { mutableStateOf(false) }

  // Import Confirmation Dialog
  if (showImportConfirmDialog && importValidationSummary != null && pendingBackupToImport != null) {
    val summary = importValidationSummary!!
    val backup = pendingBackupToImport!!

    AlertDialog(
      onDismissRequest = {
        showImportConfirmDialog = false
        pendingBackupToImport = null
        importValidationSummary = null
      },
      icon = {
        Icon(
          Icons.Default.CloudUpload,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = "Import Paisa Backup?",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Valid backup identified. Existing data will be preserved and merged safely without duplicates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "• Transactions: ${summary.transactionCount} records",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "• Budget Plans: ${summary.budgetCount} months",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "• Recurring Rules: ${summary.recurringCount} rules",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
              )
              if (summary.accountName.isNotBlank()) {
                Text(
                  text = "• Export Source: ${summary.accountName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      },
      confirmButton = {
        PremiumButton(
          onClick = {
            showImportConfirmDialog = false
            viewModel.confirmAndApplyImport(
              backup = backup,
              onSuccess = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                pendingBackupToImport = null
                importValidationSummary = null
              },
              onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                pendingBackupToImport = null
                importValidationSummary = null
              }
            )
          },
          shape = RoundedCornerShape(18.dp)
        ) {
          Text("Merge & Restore")
        }
      },
      dismissButton = {
        PremiumOutlinedButton(
          onClick = {
            showImportConfirmDialog = false
            pendingBackupToImport = null
            importValidationSummary = null
          },
          shape = RoundedCornerShape(18.dp)
        ) {
          Text("Cancel")
        }
      }
    )
  }

  var isVerifyingPassphrase by remember { mutableStateOf(false) }

  if (showRecoveryPassphraseDialog) {
    AlertDialog(
      onDismissRequest = {
        if (!isVerifyingPassphrase) {
          showRecoveryPassphraseDialog = false
          recoveryPassphraseInput = ""
          recoveryPassphraseConfirm = ""
          recoveryPassphraseError = null
        }
      },
      title = {
        Text(
          if (isRecoveryPassphraseConfigured) "Enter Recovery Passphrase" else "Protect Cloud Backups",
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Create a recovery passphrase of at least 12 characters, or enter the exact same passphrase used on another device. It is stored locally using Android Keystore and is never uploaded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            "This changes only the passphrase saved on this device; it does not change the cloud backup. Enter the exact passphrase used to encrypt that backup. If it is lost, new-format backups cannot be decrypted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = recoveryPassphraseInput,
            onValueChange = {
              if (!isVerifyingPassphrase) {
                recoveryPassphraseInput = it
                recoveryPassphraseError = null
              }
            },
            enabled = !isVerifyingPassphrase,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Recovery passphrase") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
          )
          OutlinedTextField(
            value = recoveryPassphraseConfirm,
            onValueChange = {
              if (!isVerifyingPassphrase) {
                recoveryPassphraseConfirm = it
                recoveryPassphraseError = null
              }
            },
            enabled = !isVerifyingPassphrase,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm passphrase") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
          )
          if (recoveryPassphraseError != null) {
            Text(
              text = recoveryPassphraseError.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      confirmButton = {
        PremiumTextButton(
          enabled = !isVerifyingPassphrase,
          onClick = {
            when {
              recoveryPassphraseInput.length < 12 -> {
                recoveryPassphraseError = "Use at least 12 characters."
              }
              recoveryPassphraseInput != recoveryPassphraseConfirm -> {
                recoveryPassphraseError = "The passphrases do not match."
              }
              else -> {
                isVerifyingPassphrase = true
                recoveryPassphraseError = null
                coroutineScope.launch {
                  val result = viewModel.verifyAndSetRecoveryPassphrase(recoveryPassphraseInput)
                  isVerifyingPassphrase = false
                  result.fold(
                    onSuccess = {
                      isRecoveryPassphraseConfigured = true
                      showRecoveryPassphraseDialog = false
                      recoveryPassphraseInput = ""
                      recoveryPassphraseConfirm = ""
                      recoveryPassphraseError = null
                      Toast.makeText(
                        context,
                        "Recovery passphrase saved securely on this device. Tap Sync Now to sync or migrate your Drive backup.",
                        Toast.LENGTH_LONG
                      ).show()
                    },
                    onFailure = { error ->
                      recoveryPassphraseError = error.localizedMessage ?: "Verification failed."
                    }
                  )
                }
              }
            }
          }
        ) {
          if (isVerifyingPassphrase) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Text("Save Passphrase")
          }
        }
      },
      dismissButton = {
        PremiumTextButton(
          enabled = !isVerifyingPassphrase,
          onClick = {
            showRecoveryPassphraseDialog = false
            recoveryPassphraseInput = ""
            recoveryPassphraseConfirm = ""
            recoveryPassphraseError = null
          }
        ) { Text("Cancel") }
      }
    )
  }

  // Sign Out confirmation dialog
  if (showSignOutDialog) {
    ConfirmationDialog(
      title = if (isGuestSession) "Leave Guest Mode?" else "Sign Out from Google Account?",
      message = if (isGuestSession) {
        "Your guest financial records will remain on this device and will not be uploaded to Google Drive."
      } else {
        "Paisa will verify and save your latest backup before clearing the local cache. If verification or upload fails, your local records will be kept and sign-out will be stopped."
      },
      confirmButtonText = if (isGuestSession) "Leave Guest Mode" else "Sign Out",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        viewModel.signOut { success ->
          if (success) {
            Toast.makeText(
              context,
              if (isGuestSession) "Guest mode closed. Local records were kept." else "Signed out successfully",
              Toast.LENGTH_SHORT
            ).show()
            showSignOutDialog = false
          } else {
            Toast.makeText(
              context,
              syncErrorMessage ?: "Sign-out failed. Local records were kept.",
              Toast.LENGTH_LONG
            ).show()
          }
        }
      },
      onDismiss = { showSignOutDialog = false }
    )
  }

  // Clear Data confirmation dialog
  if (showClearDataDialog) {
    ConfirmationDialog(
      title = "Clear All Financial Data?",
      message = if (isGuestSession) {
        "This will permanently erase this device's guest records."
      } else {
        "This will erase transactions, budgets, and recurring rules from your device and Google Drive. If the cloud erase fails, local records will be restored."
      },
      confirmButtonText = "Erase Everything",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        viewModel.clearAllData { success ->
          if (success) {
            Toast.makeText(
              context,
              if (isGuestSession) "Guest records erased from this device" else "Local and cloud records erased",
              Toast.LENGTH_SHORT
            ).show()
            showClearDataDialog = false
          } else {
            Toast.makeText(
              context,
              syncErrorMessage ?: "Could not erase all records. Existing data was kept.",
              Toast.LENGTH_LONG
            ).show()
          }
        }
      },
      onDismiss = { showClearDataDialog = false }
    )
  }

  // Bulk Delete Transactions Dialog
  if (showBulkDeleteDialog) {
    val monthLabel = DateUtils.getMonthLabel(universalCal)
    AlertDialog(
      onDismissRequest = { if (!isDeletingBulk) showBulkDeleteDialog = false },
      icon = {
        Icon(
          Icons.Default.AutoDelete,
          contentDescription = null,
          tint = ExpenseRed,
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = "Bulk Delete Transactions",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Select transactions to permanently delete. This action cannot be undone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (bulkDeleteOption == "MONTH") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, if (bulkDeleteOption == "MONTH") MaterialTheme.colorScheme.primary else Color.Transparent),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .clickable { bulkDeleteOption = "MONTH" }
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "Delete Selected Month ($monthLabel)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (bulkDeleteOption == "MONTH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (bulkDeleteOption == "OLDER_30") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, if (bulkDeleteOption == "OLDER_30") MaterialTheme.colorScheme.primary else Color.Transparent),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .clickable { bulkDeleteOption = "OLDER_30" }
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "Delete Transactions Older than 30 Days",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (bulkDeleteOption == "OLDER_30") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      },
      confirmButton = {
        PremiumButton(
          onClick = {
            isDeletingBulk = true
            if (bulkDeleteOption == "MONTH") {
              viewModel.deleteTransactionsForMonth(universalCal) { count ->
                isDeletingBulk = false
                showBulkDeleteDialog = false
                Toast.makeText(context, "Deleted $count transactions for $monthLabel", Toast.LENGTH_SHORT).show()
              }
            } else {
              val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86_400_000L)
              val oldTransactions = viewModel.allTransactions.value.filter { it.timestamp < thirtyDaysAgo }
              val ids = oldTransactions.map { it.id }
              if (ids.isEmpty()) {
                isDeletingBulk = false
                showBulkDeleteDialog = false
                Toast.makeText(context, "No transactions older than 30 days found", Toast.LENGTH_SHORT).show()
              } else {
                viewModel.deleteTransactionsByIds(ids) { count ->
                  isDeletingBulk = false
                  showBulkDeleteDialog = false
                  Toast.makeText(context, "Deleted $count historical transactions", Toast.LENGTH_SHORT).show()
                }
              }
            }
          },
          enabled = !isDeletingBulk,
          colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
          shape = RoundedCornerShape(18.dp)
        ) {
          if (isDeletingBulk) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = Color.White,
              strokeWidth = 2.dp
            )
          } else {
            Text("Confirm Delete", fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        PremiumOutlinedButton(
          onClick = { showBulkDeleteDialog = false },
          enabled = !isDeletingBulk,
          shape = RoundedCornerShape(18.dp)
        ) {
          Text("Cancel")
        }
      }
    )
  }


  LazyColumn(
    modifier = Modifier
      .widthIn(max = 920.dp)
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("settings_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // Header
    item {
      PremiumPageHeader(
        title = "Settings",
        subtitle = "Your account, data and privacy preferences."
      )
    }

    // 1. Google Account Identity Card
    item {
      SettingsSection(title = "Google Account") {
        com.example.ui.components.PremiumCard(
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              // Profile Photo / Avatar
              if (!userProfile.photoUrl.isNullOrBlank()) {
                AsyncImage(
                  model = userProfile.photoUrl,
                  contentDescription = "Google Profile Picture",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                )
              } else {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(54.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = userProfile.name.take(1).uppercase().ifBlank { "U" },
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = userProfile.name.ifBlank { "Google User" },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = userProfile.email.ifBlank { "Connected via Google Sign-In" },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Out Button
            PremiumOutlinedButton(
              onClick = { showSignOutDialog = true },
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ExpenseRed
              ),
              border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("google_sign_out_button")
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Sign Out of Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }
    }

    // 2. Google Drive Primary Cloud Storage Card
    item {
      SettingsSection(title = "Cloud Synchronization") {
        com.example.ui.components.PremiumCard(
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = if (isSyncing) MinimalBlue.copy(alpha = 0.15f) else if (syncErrorMessage != null) ExpenseRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f),
                  modifier = Modifier.size(42.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    if (isSyncing) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MinimalBlue
                      )
                    } else if (syncErrorMessage != null) {
                      Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(22.dp)
                      )
                    } else {
                      Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(22.dp)
                      )
                    }
                  }
                }

                Column {
                  Text(
                    text = "Google Drive Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (isSyncing) "Syncing cloud records..." else if (syncErrorMessage != null) "Sync Error" else "Synced & Secure",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSyncing) MinimalBlue else if (syncErrorMessage != null) ExpenseRed else IncomeGreen,
                    fontWeight = FontWeight.Medium
                  )
                }
              }

              // Status Pill
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSyncing) MinimalBlue.copy(alpha = 0.12f) else if (syncErrorMessage != null) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
              ) {
                Text(
                  text = if (isSyncing) "SYNCING" else if (syncErrorMessage != null) "OFFLINE" else "ACTIVE",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Black,
                  color = if (isSyncing) MinimalBlue else if (syncErrorMessage != null) ExpenseRed else IncomeGreen,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Last Synced details
            val lastSyncText = formatLastSync(lastSyncTimestamp)
            Text(
              text = "Last synced: $lastSyncText",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = if (isRecoveryPassphraseConfigured) IncomeGreen.copy(alpha = 0.13f) else AccentAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    if (isRecoveryPassphraseConfigured) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isRecoveryPassphraseConfigured) IncomeGreen else AccentAmber,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "Backup recovery passphrase",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (isRecoveryPassphraseConfigured) {
                    "Configured on this device. Use the same passphrase on other devices."
                  } else {
                    "Required for new encrypted cloud backups and cross-device restore."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              PremiumOutlinedButton(
                onClick = { showRecoveryPassphraseDialog = true }
              ) {
                Text(if (isRecoveryPassphraseConfigured) "Enter / change" else "Set up")
              }
            }

            // Google Drive Storage Quota Display
            if (driveQuota != null) {
              val quota = driveQuota!!
              Spacer(modifier = Modifier.height(12.dp))
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "Drive Storage",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Text(
                  text = "${quota.formattedAvailable} free of ${quota.formattedTotal}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { quota.percentUsed },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = if (quota.percentUsed > 0.9f) ExpenseRed else MinimalBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
              )
            }

            if (syncErrorMessage != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = syncErrorMessage ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = ExpenseRed
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sync / Grant Permission Button
            PremiumButton(
              onClick = {
                val currentConsent = driveConsentIntent
                if (currentConsent != null) {
                  try {
                    consentLauncher.launch(currentConsent)
                  } catch (e: Exception) {
                    consentLauncher.launch(viewModel.getDriveSignInIntent())
                  }
                } else {
                  viewModel.syncNow(
                    onSuccessMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                    onErrorMessage = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                  )
                }
              },
              enabled = !isSyncing,
              shape = RoundedCornerShape(18.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (driveConsentIntent != null) MinimalBlue else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("sync_now_button")
            ) {
              Icon(
                if (driveConsentIntent != null) Icons.Default.CloudSync else Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (driveConsentIntent != null) "Grant Drive Permission" else "Sync Now",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 3. Currency & Preferences Section
    item {
      SettingsSection(title = "Regional & Display") {
        com.example.ui.components.PremiumCard(
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingsRow(
              icon = Icons.Default.Language,
              iconTint = MaterialTheme.colorScheme.primary,
              iconBg = MaterialTheme.colorScheme.primaryContainer,
              title = "${userProfile.currencySymbol} (${userProfile.currencyCode})",
              subtitle = "Device Locale Currency",
              action = {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                  Text(
                    text = "System Default",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Theme Mode Selector
            SettingsRow(
              icon = when (themeMode) {
                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                ThemeMode.LIGHT -> Icons.Default.LightMode
                ThemeMode.DARK -> Icons.Default.DarkMode
              },
              iconTint = MaterialTheme.colorScheme.primary,
              iconBg = MaterialTheme.colorScheme.primaryContainer,
              title = "Theme Mode",
              subtitle = themeMode.displayName,
              action = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  ThemeMode.values().forEach { mode ->
                    val isSelected = themeMode == mode
                    Surface(
                      shape = RoundedCornerShape(16.dp),
                      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                      modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.setThemeMode(mode) }
                    ) {
                      Text(
                        text = when (mode) {
                          ThemeMode.SYSTEM -> "Auto"
                          ThemeMode.LIGHT -> "Light"
                          ThemeMode.DARK -> "AMOLED"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                      )
                    }
                  }
                }
              }
            )
          }
        }
      }
    }

    // Security & Privacy Section
    item {
      SettingsSection(title = "Security & Privacy") {
        com.example.ui.components.PremiumCard(
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingsRow(
              icon = Icons.Default.Fingerprint,
              iconTint = MinimalBlue,
              iconBg = MinimalBlue.copy(alpha = 0.15f),
              title = "Biometric App Lock",
              subtitle = "Protect sensitive financial data with biometric scan or PIN",
              action = {
                Switch(
                  checked = isAppLockEnabled,
                  onCheckedChange = { targetState ->
                    val activity = context as? FragmentActivity
                    if (activity != null && BiometricAuthManager.canAuthenticate(activity)) {
                      BiometricAuthManager.authenticate(
                        activity = activity,
                        title = if (targetState) "Enable App Lock" else "Disable App Lock",
                        subtitle = "Confirm your identity to update security settings",
                        onSuccess = {
                          viewModel.setAppLockEnabled(targetState)
                          Toast.makeText(
                            context,
                            if (targetState) "App Lock enabled" else "App Lock disabled",
                            Toast.LENGTH_SHORT
                          ).show()
                        },
                        onError = {
                          Toast.makeText(context, "Authentication required to change lock", Toast.LENGTH_SHORT).show()
                        }
                      )
                    } else {
                      // Fail closed: enabling or disabling App Lock requires device authentication.
                      Toast.makeText(
                        context,
                        "Set up a device screen lock or biometric authentication in Android Settings before changing App Lock.",
                        Toast.LENGTH_LONG
                      ).show()
                      try {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                      } catch (_: Exception) {
                        // Keep the setting unchanged if security settings cannot be opened.
                      }
                    }
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                  )
                )
              }
            )
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
            SettingsRow(
              icon = Icons.Default.Notifications,
              iconTint = MaterialTheme.colorScheme.primary,
              iconBg = MaterialTheme.colorScheme.primaryContainer,
              title = "Payment Reminders",
              subtitle = if (notificationsAllowed) {
                "Notifications are enabled for scheduled bills"
              } else {
                "Enable notifications to receive payment due reminders"
              },
              action = {
                PremiumOutlinedButton(
                  onClick = {
                    val runtimePermissionMissing =
                      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                          context,
                          Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    val permissionPrefs = context.getSharedPreferences(
                      "paisa_permissions",
                      Context.MODE_PRIVATE
                    )
                    val alreadyAsked = permissionPrefs.getBoolean("notification_permission_requested", false)
                    if (runtimePermissionMissing && !alreadyAsked) {
                      permissionPrefs.edit().putBoolean("notification_permission_requested", true).apply()
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                      try {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                            Settings.EXTRA_APP_PACKAGE,
                            context.packageName
                          )
                        } else {
                          Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                          )
                        }
                        context.startActivity(intent)
                      } catch (_: Exception) {
                        Toast.makeText(
                          context,
                          "Open Paisa's notification settings in Android Settings.",
                          Toast.LENGTH_LONG
                        ).show()
                      }
                    }
                  },
                  shape = RoundedCornerShape(14.dp)
                ) {
                  Text(if (notificationsAllowed) "Manage" else "Enable")
                }
              }
            )
          }
        }
      }
    }


    // 4. Data Management & Export Section
    item {
      SettingsSection(title = "Data & Reports") {
        com.example.ui.components.PremiumCard(
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // PDF Export
            SettingsRow(
              icon = Icons.Default.PictureAsPdf,
              iconTint = MaterialTheme.colorScheme.primary,
              iconBg = MaterialTheme.colorScheme.primaryContainer,
              title = "Export PDF Statement",
              subtitle = "Generate human-readable financial statements",
              action = {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenExportModal() }
                ) {
                  Text(
                    text = "PDF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // JSON Backup Export
            SettingsRow(
              icon = Icons.Default.Code,
              iconTint = MinimalBlue,
              iconBg = MinimalBlue.copy(alpha = 0.15f),
              title = "Export JSON Backup",
              subtitle = "Complete machine-readable application data portability",
              action = {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MinimalBlue.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                      isExportingJson = true
                      viewModel.exportJson(
                        context = context,
                        onSuccess = { msg ->
                          isExportingJson = false
                          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                          isExportingJson = false
                          Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                      )
                    }
                ) {
                  if (isExportingJson) {
                    CircularProgressIndicator(
                      modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .size(16.dp),
                      color = MinimalBlue,
                      strokeWidth = 2.dp
                    )
                  } else {
                    Text(
                      text = "JSON",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MinimalBlue,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // JSON Backup Import
            SettingsRow(
              icon = Icons.Default.FileUpload,
              iconTint = IncomeGreen,
              iconBg = IncomeGreen.copy(alpha = 0.15f),
              title = "Import JSON Backup",
              subtitle = "Validate and merge Paisa data files safely",
              action = {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = IncomeGreen.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                      try {
                        jsonPickerLauncher.launch("*/*")
                      } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open file picker", Toast.LENGTH_SHORT).show()
                      }
                    }
                ) {
                  if (isValidatingImport) {
                    CircularProgressIndicator(
                      modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .size(16.dp),
                      color = IncomeGreen,
                      strokeWidth = 2.dp
                    )
                  } else {
                    Text(
                      text = "Import",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = IncomeGreen,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Bulk Delete Transactions
            SettingsRow(
              icon = Icons.Default.AutoDelete,
              iconTint = ExpenseRed,
              iconBg = ExpenseRed.copy(alpha = 0.15f),
              title = "Bulk Delete Transactions",
              subtitle = "Batch clear transactions by month or historical range",
              action = {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = ExpenseRed.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showBulkDeleteDialog = true }
                ) {
                  Text(
                    text = "Bulk Delete",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Clear Records
            SettingsRow(
              icon = Icons.Default.DeleteForever,
              iconTint = ExpenseRed,
              iconBg = ExpenseRed.copy(alpha = 0.15f),
              title = "Clear All Records",
              subtitle = "Permanently erase local and cloud financial records",
              action = {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = ExpenseRed.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showClearDataDialog = true }
                ) {
                  Text(
                    text = "Erase",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            )
          }
        }
      }
    }

    // App Version Footer
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Paisa v3.0.0",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Connected with Google Drive Cloud Storage",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsSection(
  title: String,
  content: @Composable () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 4.dp)
    )
    content()
  }
}

@Composable
private fun SettingsRow(
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  title: String,
  subtitle: String,
  action: @Composable () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = iconBg,
      modifier = Modifier.size(44.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(21.dp)
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }

    action()
  }
}

private fun hasNotificationAccess(context: Context): Boolean {
  val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
  return runtimePermissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun formatLastSync(timestamp: Long?): String {
  if (timestamp == null || timestamp <= 0L) return "Never"
  val diff = System.currentTimeMillis() - timestamp
  return when {
    diff < 60_000L -> "Just now"
    diff < 3600_000L -> "${diff / 60_000L} min ago"
    diff < 86400_000L -> "${diff / 3600_000L} hours ago"
    else -> SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
  }
}
