package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExportPeriod
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MinimalBlue
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.JsonValidationResult
import com.example.util.PaisaJsonBackup
import com.example.util.ValidationSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataModal(
  sheetState: SheetState,
  viewModel: FinanceViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val selectedCal by viewModel.selectedCalendar.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: PDF Report, 1: JSON Portability
  var selectedPeriod by remember { mutableStateOf(ExportPeriod.CURRENT_MONTH) }
  var isExporting by remember { mutableStateOf(false) }
  var isValidatingImport by remember { mutableStateOf(false) }

  // Custom date range state
  var customStartDateMillis by remember { mutableStateOf(DateUtils.getStartOfMonth(selectedCal)) }
  var customEndDateMillis by remember { mutableStateOf(DateUtils.getEndOfMonth(selectedCal)) }

  var showStartDatePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }

  // Import Dialog Confirmation State
  var pendingBackupToImport by remember { mutableStateOf<PaisaJsonBackup?>(null) }
  var importValidationSummary by remember { mutableStateOf<ValidationSummary?>(null) }
  var showImportConfirmDialog by remember { mutableStateOf(false) }

  val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  // JSON File Picker Launcher
  val jsonPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
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

  if (showStartDatePicker) {
    AppDatePickerDialog(
      initialDateMillis = customStartDateMillis,
      onDateSelected = { customStartDateMillis = it },
      onDismiss = { showStartDatePicker = false }
    )
  }

  if (showEndDatePicker) {
    AppDatePickerDialog(
      initialDateMillis = customEndDateMillis,
      onDateSelected = { customEndDateMillis = it },
      onDismiss = { showEndDatePicker = false }
    )
  }

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
            text = "Valid backup identified. Your existing data will be safely preserved and merged idempotently without duplicates.",
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
                onDismiss()
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

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .navigationBarsPadding()
        .padding(bottom = 24.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Data Export & Portability",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Share statements or backup complete data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Tab selector: PDF vs JSON
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.clip(RoundedCornerShape(14.dp))
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("PDF Statement", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
            }
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("JSON Backup", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
            }
          }
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // ================= TAB 0: PDF REPORT EXPORT =================
      if (selectedTab == 0) {
        Text(
          text = "Select Time Period for Report",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Period Selection Options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val periods = listOf(
            Triple(ExportPeriod.CURRENT_MONTH, "Selected Month (${monthFormat.format(selectedCal.time)})", Icons.Default.CalendarMonth),
            Triple(ExportPeriod.SELECTED_YEAR, "Selected Year (${selectedCal.get(Calendar.YEAR)})", Icons.Default.CalendarToday),
            Triple(ExportPeriod.CUSTOM_RANGE, "Custom Date Range", Icons.Default.DateRange),
            Triple(ExportPeriod.ALL_TIME, "All Time Records (Lifetime)", Icons.Default.History)
          )

          periods.forEach { (period, label, icon) ->
            val isSelected = selectedPeriod == period

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { selectedPeriod = period }
                .testTag("export_period_${period.name}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                }

                if (isSelected) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Custom Date Range Selector Inputs
        AnimatedVisibility(
          visible = selectedPeriod == ExportPeriod.CUSTOM_RANGE,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Start Date
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { showStartDatePicker = true }
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("From Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(dateFormat.format(Date(customStartDateMillis)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
              }

              // End Date
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { showEndDatePicker = true }
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("To Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(dateFormat.format(Date(customEndDateMillis)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PDF Info Banner
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = IncomeGreen.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
            Text(
              text = "Generates a human-readable financial statement with executive summaries, spending charts, and itemized records.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export PDF Button
        Button(
          onClick = {
            isExporting = true
            viewModel.exportToPdf(
              context = context,
              period = selectedPeriod,
              customStart = if (selectedPeriod == ExportPeriod.CUSTOM_RANGE) customStartDateMillis else null,
              customEnd = if (selectedPeriod == ExportPeriod.CUSTOM_RANGE) customEndDateMillis else null,
              onSuccess = { msg ->
                isExporting = false
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onDismiss()
              },
              onError = { err ->
                isExporting = false
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
              }
            )
          },
          enabled = !isExporting,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("export_submit_button")
        ) {
          if (isExporting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Generate & Share PDF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // ================= TAB 1: JSON PORTABILITY (EXPORT & IMPORT) =================
      if (selectedTab == 1) {
        // Purpose Description Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MinimalBlue.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, MinimalBlue.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Code, contentDescription = null, tint = MinimalBlue, modifier = Modifier.size(20.dp))
              Text(
                text = "Full Machine-Readable Portability",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = "Export your entire financial dataset (transactions, monthly budget limits, and recurring automation rules) into a clean, portable JSON format. You can import this backup file back onto any device anytime.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Security, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
              Text(
                text = "Strictly private: Authentication tokens and secrets are never exported.",
                style = MaterialTheme.typography.labelSmall,
                color = IncomeGreen,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "Actions",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Export JSON Button
        Button(
          onClick = {
            isExporting = true
            viewModel.exportJson(
              context = context,
              onSuccess = { msg ->
                isExporting = false
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onDismiss()
              },
              onError = { err ->
                isExporting = false
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
              }
            )
          },
          enabled = !isExporting && !isValidatingImport,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("export_json_button")
        ) {
          if (isExporting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Export Complete JSON Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Import JSON Button
        OutlinedButton(
          onClick = {
            try {
              jsonPickerLauncher.launch("*/*")
            } catch (e: Exception) {
              Toast.makeText(context, "Cannot open file picker", Toast.LENGTH_SHORT).show()
            }
          },
          enabled = !isExporting && !isValidatingImport,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("import_json_button")
        ) {
          if (isValidatingImport) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Import & Merge JSON Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }
}
