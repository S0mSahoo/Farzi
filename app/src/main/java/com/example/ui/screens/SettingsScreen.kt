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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.drive.GoogleDriveState
import com.example.ui.components.AppBrandLogo
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalBlue
import com.example.ui.theme.MinimalIndigo
import com.example.ui.viewmodel.FinanceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
  viewModel: FinanceViewModel,
  onOpenExportModal: () -> Unit
) {
  val context = LocalContext.current
  val userProfile by viewModel.userProfile.collectAsState()
  val googleDriveState by viewModel.googleDriveState.collectAsState()

  var showEditNameDialog by remember { mutableStateOf(false) }
  var showClearDataDialog by remember { mutableStateOf(false) }
  var showDisconnectDialog by remember { mutableStateOf(false) }
  var editedName by remember { mutableStateOf(userProfile.name) }

  // Google Sign-In Activity Result Launcher
  val driveSignInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
      try {
        val account = task.getResult(ApiException::class.java)
        if (account != null) {
          viewModel.onDriveSignInSuccess(account)
          Toast.makeText(context, "Connected to Google Drive as ${account.email}", Toast.LENGTH_SHORT).show()
        } else {
          viewModel.onDriveSignInFailure("Unable to get account from Google Sign-In.")
        }
      } catch (e: ApiException) {
        viewModel.onDriveSignInFailure("Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}")
      }
    } else {
      viewModel.resetDriveStateToConnected()
    }
  }

  // Dialog to Edit Name
  if (showEditNameDialog) {
    AlertDialog(
      onDismissRequest = { showEditNameDialog = false },
      title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = editedName,
          onValueChange = { editedName = it },
          placeholder = { Text("Enter your name") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (editedName.trim().isNotEmpty()) {
              viewModel.updateUserName(editedName.trim())
              showEditNameDialog = false
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditNameDialog = false }) {
          Text("Cancel")
        }
      },
      shape = RoundedCornerShape(20.dp)
    )
  }

  // Clear Data confirmation dialog
  if (showClearDataDialog) {
    ConfirmationDialog(
      title = "Clear All Financial Data?",
      message = "This will permanently erase all transactions, budgets, and recurring rules from your device.",
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

  // Disconnect Drive dialog
  if (showDisconnectDialog) {
    ConfirmationDialog(
      title = "Disconnect Google Drive?",
      message = "Your local records will remain intact. Cloud backups will stop until you connect again.",
      confirmButtonText = "Disconnect",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        viewModel.disconnectDrive {
          Toast.makeText(context, "Google Drive disconnected", Toast.LENGTH_SHORT).show()
        }
        showDisconnectDialog = false
      },
      onDismiss = { showDisconnectDialog = false }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("settings_screen"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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

    // 1. User Profile Section
    item {
      SettingsSection(title = "User Profile") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(vertical = 6.dp)) {
            // Display Name Row
            SettingsRow(
              icon = Icons.Default.Person,
              iconTint = MaterialTheme.colorScheme.primary,
              iconBg = MaterialTheme.colorScheme.primaryContainer,
              title = if (userProfile.name.isNotBlank()) userProfile.name else "Self",
              subtitle = "Display Name",
              action = {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                      editedName = userProfile.name
                      showEditNameDialog = true
                    }
                ) {
                  Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Default.Edit,
                      contentDescription = "Edit Name",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Currency Row
            SettingsRow(
              icon = Icons.Default.Language,
              iconTint = MaterialTheme.colorScheme.primary,
              iconBg = MaterialTheme.colorScheme.primaryContainer,
              title = "${userProfile.currencySymbol} (${userProfile.currencyCode})",
              subtitle = "Auto-detected from Device Locale",
              action = {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                  Text(
                    text = "System",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            )
          }
        }
      }
    }

    // 2. Cloud Storage Section (Real Google Drive)
    item {
      SettingsSection(title = "Cloud Storage") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            // Google Drive status & header row
            Row(
              modifier = Modifier.fillMaxWidth(),
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
                  color = when (googleDriveState) {
                    is GoogleDriveState.Connected, is GoogleDriveState.BackupSuccess -> IncomeGreen.copy(alpha = 0.15f)
                    is GoogleDriveState.BackingUp, is GoogleDriveState.Connecting -> MinimalIndigo.copy(alpha = 0.15f)
                    is GoogleDriveState.BackupFailed -> ExpenseRed.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  },
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    val icon = when (googleDriveState) {
                      is GoogleDriveState.Connected, is GoogleDriveState.BackupSuccess -> Icons.Default.CloudDone
                      is GoogleDriveState.BackingUp, is GoogleDriveState.Connecting -> Icons.Default.CloudSync
                      is GoogleDriveState.BackupFailed -> Icons.Default.ErrorOutline
                      else -> Icons.Default.CloudQueue
                    }
                    val tint = when (googleDriveState) {
                      is GoogleDriveState.Connected, is GoogleDriveState.BackupSuccess -> IncomeGreen
                      is GoogleDriveState.BackingUp, is GoogleDriveState.Connecting -> MinimalIndigo
                      is GoogleDriveState.BackupFailed -> ExpenseRed
                      else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                  }
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Google Drive Backup",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  val statusSubtitle = when (val state = googleDriveState) {
                    is GoogleDriveState.Connected -> "Connected: ${state.email}"
                    is GoogleDriveState.BackupSuccess -> "Connected: ${state.email}"
                    is GoogleDriveState.BackingUp -> "Uploading backup to Google Drive..."
                    is GoogleDriveState.Connecting -> "Authenticating with Google..."
                    is GoogleDriveState.BackupFailed -> "Backup failed • Retry available"
                    is GoogleDriveState.NotConnected -> "Optional private cloud backup"
                  }
                  Text(
                    text = statusSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              // Status chip or indicator
              when (googleDriveState) {
                is GoogleDriveState.BackingUp, is GoogleDriveState.Connecting -> {
                  CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                }
                is GoogleDriveState.Connected, is GoogleDriveState.BackupSuccess -> {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IncomeGreen.copy(alpha = 0.15f)
                  ) {
                    Text(
                      text = "Connected",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = IncomeGreen,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
                else -> {}
              }
            }

            // Backup Timestamp Info
            val lastBackupTs = when (val state = googleDriveState) {
              is GoogleDriveState.Connected -> state.lastBackupTimestampMillis
              is GoogleDriveState.BackupSuccess -> state.timestampMillis
              else -> viewModel.driveService.getLastBackupTimestamp()
            }

            if (googleDriveState !is GoogleDriveState.NotConnected && googleDriveState !is GoogleDriveState.Connecting) {
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Last Successful Backup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = if (lastBackupTs != null && lastBackupTs > 0L) {
                      val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                      sdf.format(Date(lastBackupTs))
                    } else "Never",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }

            // Error notice if failed
            if (googleDriveState is GoogleDriveState.BackupFailed) {
              val errMsg = (googleDriveState as GoogleDriveState.BackupFailed).errorMessage
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = ExpenseRed.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = errMsg,
                  style = MaterialTheme.typography.bodySmall,
                  color = ExpenseRed,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            when (googleDriveState) {
              is GoogleDriveState.NotConnected -> {
                Button(
                  onClick = {
                    viewModel.startDriveConnecting()
                    driveSignInLauncher.launch(viewModel.getDriveSignInIntent())
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                  Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Connect Google Drive", fontWeight = FontWeight.SemiBold)
                }
              }
              is GoogleDriveState.Connecting -> {
                OutlinedButton(
                  onClick = {},
                  enabled = false,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("Authenticating...")
                }
              }
              else -> {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Button(
                    onClick = {
                      viewModel.performDriveBackup(
                        onSuccessMessage = { msg ->
                          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onErrorMessage = { err ->
                          Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                      )
                    },
                    enabled = googleDriveState !is GoogleDriveState.BackingUp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    if (googleDriveState is GoogleDriveState.BackingUp) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Backing Up...")
                    } else {
                      Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Back Up Now", fontWeight = FontWeight.SemiBold)
                    }
                  }

                  OutlinedButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Data & Reports Section
    item {
      SettingsSection(title = "Data & Reports") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(vertical = 6.dp)) {
            // Export PDF Row
            SettingsRow(
              icon = Icons.Default.PictureAsPdf,
              iconTint = MaterialTheme.colorScheme.primary,
              iconBg = MaterialTheme.colorScheme.primaryContainer,
              title = "Export PDF Statement",
              subtitle = "Monthly, Yearly, Custom Range, or All-Time",
              onClick = { onOpenExportModal() },
              action = {
                Icon(
                  Icons.Default.ChevronRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
              }
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Erase All Data Row
            SettingsRow(
              icon = Icons.Default.DeleteForever,
              iconTint = ExpenseRed,
              iconBg = ExpenseRed.copy(alpha = 0.12f),
              title = "Erase All Data",
              titleColor = ExpenseRed,
              subtitle = "Permanently delete local database",
              onClick = { showClearDataDialog = true }
            )
          }
        }
      }
    }

    // 4. Application Information Card
    item {
      SettingsSection(title = "Application") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            AppBrandLogo(size = 44.dp)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Paisa",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Text(
              text = "Version 3.0.0",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "100% Offline-First • Zero Ads • Zero Telemetry\nYour financial data remains strictly under your control.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              lineHeight = 18.sp
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

/**
 * Standardized Section Heading with unified left margin and typography.
 */
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
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 4.dp)
    )
    content()
  }
}

/**
 * Standardized Settings Item Row adhering to a unified layout grid:
 * [Icon (40dp)] [Title / Subtitle] [Action]
 */
@Composable
private fun SettingsRow(
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  title: String,
  subtitle: String,
  titleColor: Color = MaterialTheme.colorScheme.onSurface,
  onClick: (() -> Unit)? = null,
  action: @Composable (() -> Unit)? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(
        if (onClick != null) Modifier.clickable { onClick() } else Modifier
      )
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = iconBg,
        modifier = Modifier.size(40.dp)
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

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = titleColor,
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
    }

    if (action != null) {
      Spacer(modifier = Modifier.width(8.dp))
      action()
    }
  }
}
