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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.data.model.ThemeMode
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalBlue
import com.example.ui.theme.MinimalEmerald
import com.example.ui.theme.MinimalIndigo
import com.example.ui.theme.MinimalRose
import com.example.ui.viewmodel.FinanceViewModel
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
  onOpenExportModal: () -> Unit,
  onNavigateToVault: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by viewModel.userProfile.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
  val syncErrorMessage by viewModel.syncErrorMessage.collectAsState()
  val driveConsentIntent by viewModel.driveConsentIntent.collectAsState()
  val themeMode by viewModel.themeMode.collectAsState()
  val isAppLockConfigured by viewModel.isAppLockConfigured.collectAsState()
  val driveStorageBytes by viewModel.driveStorageUsageBytes.collectAsState()

  var isExportingJson by remember { mutableStateOf(false) }
  var isValidatingImport by remember { mutableStateOf(false) }

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
      message = "This will permanently erase all transactions, budgets, recurring rules, and secure notes from your device and cloud storage.",
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
        text = "Settings & Vault",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp
        ),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    // 1. Google Account Identity Card
    item {
      SettingsSection(title = "Google Account") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              if (!userProfile.photoUrl.isNullOrBlank()) {
                AsyncImage(
                  model = userProfile.photoUrl,
                  contentDescription = "Google Profile Picture",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                )
              } else {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(52.dp)
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

            OutlinedButton(
              onClick = { showSignOutDialog = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
              border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("google_sign_out_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign Out of Account", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
          }
        }
      }
    }

    // 2. Security & Biometrics (Requirements 35-38, 49-53)
    item {
      SettingsSection(title = "Security & Protection") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // Biometric App Lock Toggle
            SettingsRow(
              icon = Icons.Default.Fingerprint,
              iconTint = MinimalEmerald,
              iconBg = MinimalEmerald.copy(alpha = 0.15f),
              title = "Biometric App Lock",
              subtitle = "Require fingerprint / face to open Paisa",
              action = {
                Switch(
                  checked = isAppLockConfigured,
                  onCheckedChange = { enable ->
                    val act = context as? FragmentActivity
                    if (act != null) {
                      viewModel.setAppLockEnabled(act, enable)
                    }
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MinimalEmerald
                  )
                )
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // Private Notes Vault
            SettingsRow(
              icon = Icons.Rounded.Security,
              iconTint = MinimalIndigo,
              iconBg = MinimalIndigo.copy(alpha = 0.15f),
              title = "Private Notes Vault",
              subtitle = "AES-256 encrypted cards, bank accounts & PINs",
              action = {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MinimalIndigo.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToVault() }
                ) {
                  Text(
                    text = "Open Vault",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MinimalIndigo,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            )
          }
        }
      }
    }

    // 3. Theme & Appearance
    item {
      SettingsSection(title = "Appearance & Theme") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              Triple(ThemeMode.SYSTEM, "System", Icons.Default.BrightnessAuto),
              Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
              Triple(ThemeMode.DARK, "AMOLED Dark", Icons.Default.DarkMode)
            ).forEach { (mode, label, icon) ->
              val selected = themeMode == mode
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { viewModel.setThemeMode(mode) }
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. Google Drive Cloud Storage Card
    item {
      SettingsSection(title = "Cloud Synchronization") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                      CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MinimalBlue)
                    } else if (syncErrorMessage != null) {
                      Icon(Icons.Default.CloudOff, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(22.dp))
                    } else {
                      Icon(Icons.Default.CloudDone, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(22.dp))
                    }
                  }
                }

                Column {
                  Text(
                    text = "Google Drive Sync",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (isSyncing) "Syncing encrypted cloud records..." else if (syncErrorMessage != null) "Sync Error" else "Synced & Hardware Encrypted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSyncing) MinimalBlue else if (syncErrorMessage != null) ExpenseRed else IncomeGreen,
                    fontWeight = FontWeight.Medium
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSyncing) MinimalBlue.copy(alpha = 0.12f) else if (syncErrorMessage != null) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
              ) {
                Text(
                  text = if (isSyncing) "SYNCING" else if (syncErrorMessage != null) "OFFLINE" else "ACTIVE",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                  color = if (isSyncing) MinimalBlue else if (syncErrorMessage != null) ExpenseRed else IncomeGreen,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val lastSyncText = formatLastSync(lastSyncTimestamp)
            val storageText = if (driveStorageBytes > 0) "Storage: ${(driveStorageBytes / 1024.0).toInt().coerceAtLeast(1)} KB used" else "Storage: Active"
            Text(
              text = "Last synced: $lastSyncText • $storageText",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (syncErrorMessage != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(text = syncErrorMessage ?: "", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
            }

            Spacer(modifier = Modifier.height(16.dp))

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
              Icon(if (driveConsentIntent != null) Icons.Default.CloudSync else Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (driveConsentIntent != null) "Grant Drive Permission" else "Sync Now",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    }

    // 5. Data Management & Export Section
    item {
      SettingsSection(title = "Data & Reports") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

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
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(16.dp), color = MinimalBlue, strokeWidth = 2.dp)
                  } else {
                    Text(text = "JSON", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MinimalBlue, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                  }
                }
              }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // JSON Backup Import
            SettingsRow(
              icon = Icons.Default.CloudUpload,
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
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(16.dp), color = IncomeGreen, strokeWidth = 2.dp)
                  } else {
                    Text(text = "Import", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = IncomeGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                  }
                }
              }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

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
                  Text(text = "Erase", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ExpenseRed, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
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
          Text(text = "Paisa v3.0.0", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(2.dp))
          Text(text = "Hardware Encrypted • Offline & Private", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
          Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
      }

      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
