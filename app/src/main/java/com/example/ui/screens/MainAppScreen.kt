package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
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
import com.example.ui.components.QuickAddDraftSheet
import com.example.ui.components.SalaryBudgetModal
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.theme.EmeraldPrimary
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
          // Salary Setup Quick Button
          IconButton(onClick = { viewModel.openSalaryModal() }) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Salary & Budget Setup",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // More Options Menu
          IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Draft Monthly Salary") },
              leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldPrimary) },
              onClick = {
                showMenu = false
                viewModel.autoDraftSalaryForMonth()
              }
            )
            DropdownMenuItem(
              text = { Text("Reset Sample Data") },
              leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
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
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
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
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
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
            onTypeFilterSelected = viewModel::setTypeFilter,
            onCategoryFilterSelected = viewModel::setCategoryFilter,
            onItemClick = viewModel::openDetailDialog,
            onAddClick = { viewModel.openAddDraftSheet() },
            onOpenSalarySetup = { viewModel.openSalaryModal() }
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
