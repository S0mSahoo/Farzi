package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppThemeMode
import com.example.ui.components.MonthSalaryDraftModal
import com.example.ui.components.QuickAddDraftSheet
import com.example.ui.components.SalaryBudgetModal
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
  viewModel: FinanceViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  var showMenu by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(34.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.EditNote,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "DailyDraft",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          // Theme Quick Selector
          IconButton(onClick = { viewModel.openThemeDialog() }) {
            val themeIcon = when (uiState.themeMode) {
              AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
              AppThemeMode.LIGHT -> Icons.Default.LightMode
              AppThemeMode.AMOLED_DARK -> Icons.Default.DarkMode
            }
            Icon(
              imageVector = themeIcon,
              contentDescription = "Theme: ${uiState.themeMode.displayName}",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          // Salary Setup Quick Button
          IconButton(onClick = { viewModel.openSalaryModal() }) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Salary & Budget Setup",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          // More Options Menu
          IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
          ) {
            DropdownMenuItem(
              text = {
                Text(
                  "Theme Settings",
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Medium
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Default.Palette,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              onClick = {
                showMenu = false
                viewModel.openThemeDialog()
              }
            )
            DropdownMenuItem(
              text = {
                Text(
                  "Draft Monthly Salary",
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Medium
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Default.Savings,
                  contentDescription = null,
                  tint = EmeraldPrimary
                )
              },
              onClick = {
                showMenu = false
                viewModel.autoDraftSalaryForMonth()
              }
            )
            DropdownMenuItem(
              text = {
                Text(
                  "Clean App Data",
                  color = ExpenseRed,
                  fontWeight = FontWeight.SemiBold
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Default.DeleteSweep,
                  contentDescription = null,
                  tint = ExpenseRed
                )
              },
              onClick = {
                showMenu = false
                viewModel.openClearDataDialog()
              }
            )
            DropdownMenuItem(
              text = {
                Text(
                  "Load Lightweight Starter",
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Normal
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Default.Refresh,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              onClick = {
                showMenu = false
                viewModel.resetToSampleData()
              }
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
      ) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 0.dp
        ) {
          NavigationBarItem(
            selected = uiState.currentTab == 0,
            onClick = { viewModel.setTab(0) },
            icon = {
              Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = "Daily Drafts"
              )
            },
            label = { Text("Daily Drafts", fontWeight = if (uiState.currentTab == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.onSurface,
              selectedTextColor = MaterialTheme.colorScheme.onSurface,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
          )

          NavigationBarItem(
            selected = uiState.currentTab == 1,
            onClick = { viewModel.setTab(1) },
            icon = {
              Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "Analytics"
              )
            },
            label = { Text("Analytics", fontWeight = if (uiState.currentTab == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.onSurface,
              selectedTextColor = MaterialTheme.colorScheme.onSurface,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
          )

          NavigationBarItem(
            selected = uiState.currentTab == 2,
            onClick = { viewModel.setTab(2) },
            icon = {
              Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = "Salary & Budget"
              )
            },
            label = { Text("Salary & Budget", fontWeight = if (uiState.currentTab == 2) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.onSurface,
              selectedTextColor = MaterialTheme.colorScheme.onSurface,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
          )
        }
      }
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.openAddDraftSheet() },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Draft",
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Draft",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = uiState.currentTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tab_transition"
      ) { tabIndex ->
        when (tabIndex) {
          0 -> DailyDraftScreen(
            uiState = uiState,
            onSearchQueryChange = viewModel::setSearchQuery,
            onTimeRangeSelected = viewModel::setTimeRange,
            onActiveModuleSelected = viewModel::setActiveModule,
            onPrevMonth = viewModel::prevMonth,
            onNextMonth = viewModel::nextMonth,
            onResetCurrentMonth = viewModel::resetToCurrentMonth,
            onTypeFilterSelected = viewModel::setTypeFilter,
            onCategoryFilterSelected = viewModel::setCategoryFilter,
            onItemClick = viewModel::openDetailDialog,
            onAddClick = { viewModel.openAddDraftSheet() },
            onOpenSalarySetup = { viewModel.openSalaryModal() },
            onOpenDraftMonthSalaryDialog = { viewModel.openMonthSalaryDraftDialog() }
          )
          1 -> AnalyticsScreen(
            uiState = uiState,
            onTimeRangeSelected = viewModel::setTimeRange
          )
          2 -> SalaryScreen(
            uiState = uiState,
            onOpenSalarySetup = { viewModel.openSalaryModal() },
            onPostSalaryDraftNow = { viewModel.autoDraftSalaryForMonth() },
            onItemClick = viewModel::openDetailDialog
          )
        }
      }
    }

    // Quick Add / Edit Bottom Sheet
    QuickAddDraftSheet(
      isOpen = uiState.isAddDraftSheetOpen,
      editingItem = uiState.editingTransaction,
      currencySymbol = uiState.salarySettings.currencySymbol,
      onDismiss = viewModel::closeAddDraftSheet,
      onSave = viewModel::saveTransaction
    )

    // Salary & Budget Settings Modal
    SalaryBudgetModal(
      isOpen = uiState.isSalaryModalOpen,
      currentSettings = uiState.salarySettings,
      onDismiss = viewModel::closeSalaryModal,
      onSave = viewModel::updateSalarySettings,
      onPostSalaryDraftNow = viewModel::autoDraftSalaryForMonth
    )

    // Custom Month Salary Draft Modal
    MonthSalaryDraftModal(
      isOpen = uiState.isMonthSalaryDraftDialogOpen,
      selectedYear = uiState.selectedYear,
      selectedMonth = uiState.selectedMonth,
      defaultSalaryAmount = uiState.salarySettings.salaryAmount,
      defaultPayDay = uiState.salarySettings.payDayOfMonth,
      currencySymbol = uiState.salarySettings.currencySymbol,
      onDismiss = viewModel::closeMonthSalaryDraftDialog,
      onDraftSalary = { year, month, amount, payDay, method, note ->
        viewModel.draftSalaryForSpecificMonth(
          year = year,
          month = month,
          amount = amount,
          payDay = payDay,
          paymentMethod = method,
          note = note
        )
      }
    )

    // Theme Selection Modal
    if (uiState.isThemeDialogOpen) {
      ThemeSelectionDialog(
        currentTheme = uiState.themeMode,
        onSelectTheme = { mode ->
          viewModel.setThemeMode(mode)
          viewModel.closeThemeDialog()
        },
        onDismiss = viewModel::closeThemeDialog
      )
    }

    // Clean App Data Confirmation Modal
    if (uiState.isClearDataDialogOpen) {
      AlertDialog(
        onDismissRequest = viewModel::closeClearDataDialog,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
          Surface(
            shape = CircleShape,
            color = ExpenseRed.copy(alpha = 0.12f),
            modifier = Modifier.size(52.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = ExpenseRed,
                modifier = Modifier.size(28.dp)
              )
            }
          }
        },
        title = {
          Text(
            text = "Clean App Data?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        text = {
          Text(
            text = "This will permanently remove all transaction drafts and logs from your device storage, leaving a clean lightweight app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        confirmButton = {
          Button(
            onClick = { viewModel.cleanAllData() },
            colors = ButtonDefaults.buttonColors(
              containerColor = ExpenseRed,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Wipe All Data", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = viewModel::closeClearDataDialog,
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
          }
        }
      )
    }

    // Transaction Details & Actions Modal
    if (uiState.selectedDetailItem != null) {
      TransactionDetailDialog(
        item = uiState.selectedDetailItem!!,
        currencySymbol = uiState.salarySettings.currencySymbol,
        onDismiss = viewModel::closeDetailDialog,
        onEdit = {
          val item = uiState.selectedDetailItem!!
          viewModel.openAddDraftSheet(item)
        },
        onDelete = {
          val item = uiState.selectedDetailItem!!
          viewModel.deleteTransaction(item)
        }
      )
    }
  }
}

@Composable
fun ThemeSelectionDialog(
  currentTheme: AppThemeMode,
  onSelectTheme: (AppThemeMode) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Palette,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Appearance & Theme",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Select your preferred visual style:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val themeOptions = listOf(
          Triple(
            AppThemeMode.SYSTEM,
            "Sync with System",
            "Automatically matches your device dark/light theme setting"
          ),
          Triple(
            AppThemeMode.AMOLED_DARK,
            "Pure AMOLED Dark",
            "True #000000 black canvas for OLED displays & maximum battery savings"
          ),
          Triple(
            AppThemeMode.LIGHT,
            "Clean Light",
            "Crisp high-contrast layout for well-lit environments"
          )
        )

        themeOptions.forEach { (mode, title, desc) ->
          val isSelected = currentTheme == mode
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSelectTheme(mode) },
            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            border = BorderStroke(
              1.dp,
              if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { onSelectTheme(mode) },
                colors = RadioButtonDefaults.colors(
                  selectedColor = MaterialTheme.colorScheme.primary,
                  unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = title,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = desc,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Text("Done", fontWeight = FontWeight.Bold)
      }
    }
  )
}
