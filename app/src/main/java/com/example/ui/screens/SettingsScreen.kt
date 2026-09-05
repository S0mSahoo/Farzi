package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.data.drive.GoogleDriveState
import com.example.data.model.ThemeMode
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalBlue
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BiometricAuthManager
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
  val userProfile by viewModel.userProfile.collectAsState()
  val isProUser by viewModel.isProUser.collectAsState()
  val googleDriveState by viewModel.googleDriveState.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
  val syncErrorMessage by viewModel.syncErrorMessage.collectAsState()
  val driveConsentIntent by viewModel.driveConsentIntent.collectAsState()
  val driveQuota by viewModel.driveStorageQuota.collectAsState()
  val themeMode by viewModel.themeMode.collectAsState()
  val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
  val universalCal by viewModel.selectedCalendar.collectAsState()

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
            shape = RoundedCornerShape(12.dp),
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
        Button(
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
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Merge & Restore")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            showImportConfirmDialog = false
            pendingBackupToImport = null
            importValidationSummary = null
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Cancel")
        }
      }
    )
  }

  // Sign Out confirmation dialog
  if (showSignOutDialog) {
    ConfirmationDialog(
      title = "Sign Out from Google Account?",
      message = "Signing out will end your session and clear local device cache. Your financial data is securely preserved on Google Drive and will reload when you sign in again.",
      confirmButtonText = "Sign Out",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        viewModel.signOut {
          Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }
        showSignOutDialog = false
      },
      onDismiss = { showSignOutDialog = false }
    )
  }

  // Clear Data confirmation dialog
  if (showClearDataDialog) {
    ConfirmationDialog(
      title = "Clear All Financial Data?",
      message = "This will permanently erase all transactions, budgets, and recurring rules from your device and cloud storage.",
      confirmButtonText = "Erase Everything",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        viewModel.clearAllData {
          Toast.makeText(context, "All records permanently erased", Toast.LENGTH_SHORT).show()
        }
        showClearDataDialog = false
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
            shape = RoundedCornerShape(12.dp),
            color = if (bulkDeleteOption == "MONTH") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, if (bulkDeleteOption == "MONTH") MaterialTheme.colorScheme.primary else Color.Transparent),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
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
            shape = RoundedCornerShape(12.dp),
            color = if (bulkDeleteOption == "OLDER_30") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, if (bulkDeleteOption == "OLDER_30") MaterialTheme.colorScheme.primary else Color.Transparent),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
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
        Button(
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
          shape = RoundedCornerShape(12.dp)
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
        OutlinedButton(
          onClick = { showBulkDeleteDialog = false },
          enabled = !isDeletingBulk,
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Cancel")
        }
      }
    )
  }


  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("settings_screen"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // Header
    item {
      Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
      )
    }

    // 1. Google Account Identity Card
    item {
      SettingsSection(title = "Google Account") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
            OutlinedButton(
              onClick = { showSignOutDialog = true },
              shape = RoundedCornerShape(12.dp),
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
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                shape = RoundedCornerShape(8.dp),
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
            Button(
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
              shape = RoundedCornerShape(12.dp),
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

    // 2.5 Paisa Pro Section
    item {
      SettingsSection(title = "Paisa Pro & Intelligence") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, if (isProUser) AccentAmber.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth().testTag("settings_pro_card")
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = AccentAmber.copy(alpha = 0.18f),
                  modifier = Modifier.size(42.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.Star,
                      contentDescription = null,
                      tint = AccentAmber,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                }
                Column {
                  Text(
                    text = "Paisa Pro Subscription",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (isProUser) "All Pro capabilities active" else "Free Plan active",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isProUser) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isProUser) AccentAmber.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
              ) {
                Text(
                  text = if (isProUser) "PRO" else "FREE",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isProUser) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Text(
              text = "Includes Cash-Flow Forecast, What-If Simulator, and local AI Financial Copilot.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                  text = "Enable Pro (Development Mode)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Toggle entitlement locally for testing & evaluation",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = isProUser,
                onCheckedChange = { enabled ->
                  viewModel.setDevProEnabled(enabled)
                  Toast.makeText(
                    context,
                    if (enabled) "Paisa Pro Enabled" else "Paisa Free Mode Enabled",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                modifier = Modifier.testTag("settings_pro_toggle")
              )
            }
          }
        }
      }
    }

    // 3. Currency & Preferences Section
    item {
      SettingsSection(title = "Regional & Display") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
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
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                      viewModel.setAppLockEnabled(targetState)
                    }
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                  )
                )
              }
            )
          }
        }
      }
    }


    // 4. Data Management & Export Section
    item {
      SettingsSection(title = "Data & Reports") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
                  shape = RoundedCornerShape(8.dp),
                  color = MinimalBlue.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
                  shape = RoundedCornerShape(8.dp),
                  color = IncomeGreen.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
                  shape = RoundedCornerShape(8.dp),
                  color = ExpenseRed.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
                  shape = RoundedCornerShape(8.dp),
                  color = ExpenseRed.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.weight(1f)
    ) {
      Surface(
        shape = CircleShape,
        color = iconBg,
        modifier = Modifier.size(38.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Column {
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
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    action()
  }
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
