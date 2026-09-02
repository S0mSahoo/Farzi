package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BudgetModel
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.AddEditRecurringSheet
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.AppBrandLogo
import com.example.ui.components.DateUtils
import com.example.ui.components.ExportDataModal
import com.example.ui.components.SetBudgetSheet
import com.example.ui.theme.MinimalEmerald
import com.example.ui.theme.MinimalIndigo
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

sealed class AppDestination(val index: Int, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  object Dashboard : AppDestination(0, "Home", Icons.Default.Dashboard)
  object Calendar : AppDestination(1, "Calendar", Icons.Default.CalendarMonth)
  object Transactions : AppDestination(2, "History", Icons.Default.ReceiptLong)
  object Budgets : AppDestination(3, "Budgets", Icons.Default.AccountBalanceWallet)
  object Recurring : AppDestination(4, "Recurring", Icons.Default.Repeat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
  viewModel: FinanceViewModel
) {
  val userProfile by viewModel.userProfile.collectAsState()
  val selectedCalendar by viewModel.selectedCalendar.collectAsState()
  val allBudgets by viewModel.allBudgets.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  var isSettingsOpen by remember { mutableStateOf(false) }
  var isSecureVaultOpen by remember { mutableStateOf(false) }

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  // Sheet states
  var showAddEditTransactionSheet by remember { mutableStateOf(false) }
  var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }
  var prefilledDateTimestamp by remember { mutableStateOf<Long?>(null) }
  var prefilledTxType by remember { mutableStateOf<TransactionType?>(null) }

  var showAddEditRecurringSheet by remember { mutableStateOf(false) }
  var editingRecurringRule by remember { mutableStateOf<RecurringRule?>(null) }

  var showSetBudgetSheet by remember { mutableStateOf(false) }
  var targetBudgetMonthKey by remember { mutableStateOf<String?>(null) }
  var targetBudgetMonthLabel by remember { mutableStateOf<String?>(null) }

  var showExportModal by remember { mutableStateOf(false) }

  val transactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val recurringSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val budgetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val activeBudgetMonthKey = targetBudgetMonthKey ?: DateUtils.getMonthKey(selectedCalendar)
  val activeBudgetMonthLabel = targetBudgetMonthLabel ?: DateUtils.getMonthLabel(selectedCalendar)
  val currentBudget = allBudgets.find { it.monthKey == activeBudgetMonthKey }

  // Sheets
  if (showAddEditTransactionSheet) {
    AddEditTransactionSheet(
      sheetState = transactionSheetState,
      initialTransaction = editingTransaction,
      prefilledTimestamp = prefilledDateTimestamp,
      prefilledType = prefilledTxType,
      currencySymbol = userProfile.currencySymbol,
      onDismiss = {
        showAddEditTransactionSheet = false
        editingTransaction = null
        prefilledDateTimestamp = null
        prefilledTxType = null
      },
      onSave = { title, amount, type, category, timestamp, note, paymentMethod, isRecurring ->
        if (editingTransaction != null) {
          viewModel.updateTransaction(
            editingTransaction!!.copy(
              title = title,
              amount = amount,
              type = type,
              category = category,
              timestamp = timestamp,
              note = note,
              paymentMethod = paymentMethod,
              isRecurring = isRecurring,
              updatedAt = System.currentTimeMillis()
            )
          )
        } else {
          viewModel.addTransaction(
            title = title,
            amount = amount,
            type = type,
            category = category,
            timestamp = timestamp,
            note = note,
            paymentMethod = paymentMethod,
            isRecurring = isRecurring
          )
        }
      }
    )
  }

  if (showAddEditRecurringSheet) {
    AddEditRecurringSheet(
      sheetState = recurringSheetState,
      initialRule = editingRecurringRule,
      currencySymbol = userProfile.currencySymbol,
      onDismiss = {
        showAddEditRecurringSheet = false
        editingRecurringRule = null
      },
      onSave = { title, amount, type, category, interval, startDate, endDate, paymentMethod, note ->
        if (editingRecurringRule != null) {
          viewModel.updateRecurringRule(
            editingRecurringRule!!.copy(
              title = title,
              amount = amount,
              type = type,
              category = category,
              interval = interval,
              startDate = startDate,
              endDate = endDate,
              paymentMethod = paymentMethod,
              note = note
            )
          )
        } else {
          viewModel.addRecurringRule(
            title = title,
            amount = amount,
            type = type,
            category = category,
            interval = interval,
            startDate = startDate,
            endDate = endDate,
            paymentMethod = paymentMethod,
            note = note
          )
        }
      }
    )
  }

  if (showSetBudgetSheet) {
    SetBudgetSheet(
      sheetState = budgetSheetState,
      monthKey = activeBudgetMonthKey,
      monthLabel = activeBudgetMonthLabel,
      currentBudget = currentBudget,
      currencySymbol = userProfile.currencySymbol,
      onDismiss = {
        showSetBudgetSheet = false
        targetBudgetMonthKey = null
        targetBudgetMonthLabel = null
      },
      onSave = { key, totalLimit, catLimits ->
        viewModel.saveMonthlyBudget(key, totalLimit, catLimits)
      }
    )
  }

  if (showExportModal) {
    ExportDataModal(
      sheetState = exportSheetState,
      viewModel = viewModel,
      onDismiss = { showExportModal = false }
    )
  }

  if (isSecureVaultOpen) {
    SecureVaultScreen(
      viewModel = viewModel,
      onBack = { isSecureVaultOpen = false }
    )
    return
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          // Header: Google Account Profile
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                if (!userProfile.photoUrl.isNullOrBlank()) {
                  AsyncImage(
                    model = userProfile.photoUrl,
                    contentDescription = "Google Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                  )
                } else {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = if (userProfile.name.isNotBlank()) userProfile.name.take(1).uppercase() else "G",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                  Text(
                    text = userProfile.email.ifBlank { "Google Account" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(12.dp))

          // Navigation Links
          val destinations = listOf(
            AppDestination.Dashboard,
            AppDestination.Calendar,
            AppDestination.Transactions,
            AppDestination.Budgets,
            AppDestination.Recurring
          )

          destinations.forEach { dest ->
            val isSelected = !isSettingsOpen && !isSecureVaultOpen && selectedTab == dest.index
            NavigationDrawerItem(
              icon = { Icon(dest.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
              label = { Text(dest.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
              selected = isSelected,
              onClick = {
                isSettingsOpen = false
                isSecureVaultOpen = false
                selectedTab = dest.index
                scope.launch { drawerState.close() }
              },
              shape = RoundedCornerShape(14.dp),
              colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.padding(vertical = 2.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(8.dp))

          // Private Vault Action
          NavigationDrawerItem(
            icon = { Icon(Icons.Rounded.Security, contentDescription = null, tint = MinimalIndigo, modifier = Modifier.size(20.dp)) },
            label = { Text("Private Vault", fontWeight = FontWeight.Medium) },
            selected = isSecureVaultOpen,
            onClick = {
              scope.launch { drawerState.close() }
              isSecureVaultOpen = true
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(vertical = 2.dp)
          )

          // Export & Portability Quick Action
          NavigationDrawerItem(
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
            label = { Text("Export & Portability", fontWeight = FontWeight.Medium) },
            selected = false,
            onClick = {
              scope.launch { drawerState.close() }
              showExportModal = true
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_export_pdf_button")
          )

          // Settings Action
          NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
            label = { Text("Settings", fontWeight = if (isSettingsOpen) FontWeight.Bold else FontWeight.Medium) },
            selected = isSettingsOpen,
            onClick = {
              isSettingsOpen = true
              isSecureVaultOpen = false
              scope.launch { drawerState.close() }
            },
            shape = RoundedCornerShape(14.dp),
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_settings_button")
          )

          Spacer(modifier = Modifier.weight(1f))

          // Bottom Info Badge
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              AppBrandLogo(size = 24.dp)
              Column {
                Text("Paisa v3.0.0", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("100% Offline & Hardware Encrypted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  ) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              AppBrandLogo(size = 26.dp)
              Text(
                text = if (isSettingsOpen) "Settings" else "Paisa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
              )
            }
          },
          navigationIcon = {
            if (isSettingsOpen) {
              IconButton(onClick = { isSettingsOpen = false }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Dashboard")
              }
            } else {
              IconButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier.testTag("profile_drawer_button")
              ) {
                Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu")
              }
            }
          },
          actions = {
            if (!isSettingsOpen) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { scope.launch { drawerState.open() } }
                  .testTag("header_profile_pill")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text(
                    text = if (userProfile.name.isNotBlank()) userProfile.name.split(" ").first() else "Menu",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
          )
        )
      },
      bottomBar = {
        if (!isSettingsOpen) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
          ) {
            val navItems = listOf(
              AppDestination.Dashboard,
              AppDestination.Calendar,
              AppDestination.Transactions,
              AppDestination.Budgets,
              AppDestination.Recurring
            )

            navItems.forEach { destination ->
              val isSelected = selectedTab == destination.index
              NavigationBarItem(
                selected = isSelected,
                onClick = { selectedTab = destination.index },
                icon = {
                  Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    modifier = Modifier.size(22.dp)
                  )
                },
                label = {
                  Text(
                    text = destination.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("nav_${destination.label.lowercase()}")
              )
            }
          }
        }
      },
      floatingActionButton = {
        if (!isSettingsOpen && (selectedTab == 0 || selectedTab == 1 || selectedTab == 2)) {
          FloatingActionButton(
            onClick = {
              editingTransaction = null
              prefilledDateTimestamp = null
              prefilledTxType = null
              showAddEditTransactionSheet = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier.testTag("global_add_transaction_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (isSettingsOpen) {
          SettingsScreen(
            viewModel = viewModel,
            onOpenExportModal = { showExportModal = true },
            onNavigateToVault = {
              isSettingsOpen = false
              isSecureVaultOpen = true
            }
          )
        } else {
          when (selectedTab) {
            0 -> DashboardScreen(
              viewModel = viewModel,
              onOpenAddTransaction = { prefilledType ->
                editingTransaction = null
                prefilledDateTimestamp = null
                prefilledTxType = prefilledType
                showAddEditTransactionSheet = true
              },
              onOpenSetBudget = {
                targetBudgetMonthKey = DateUtils.getMonthKey(selectedCalendar)
                targetBudgetMonthLabel = DateUtils.getMonthLabel(selectedCalendar)
                showSetBudgetSheet = true
              },
              onNavigateToRecurring = {
                selectedTab = 4
              }
            )
            1 -> CalendarScreen(
              viewModel = viewModel,
              onAddTransactionForDate = { timestamp ->
                editingTransaction = null
                prefilledDateTimestamp = timestamp
                prefilledTxType = null
                showAddEditTransactionSheet = true
              },
              onEditTransaction = { item ->
                editingTransaction = item
                showAddEditTransactionSheet = true
              }
            )
            2 -> TransactionsScreen(
              viewModel = viewModel,
              onEditTransaction = { item ->
                editingTransaction = item
                showAddEditTransactionSheet = true
              }
            )
            3 -> BudgetScreen(
              viewModel = viewModel,
              onOpenSetBudget = {
                targetBudgetMonthKey = DateUtils.getMonthKey(selectedCalendar)
                targetBudgetMonthLabel = DateUtils.getMonthLabel(selectedCalendar)
                showSetBudgetSheet = true
              }
            )
            4 -> RecurringScreen(
              viewModel = viewModel,
              onOpenAddRecurringRule = {
                editingRecurringRule = null
                showAddEditRecurringSheet = true
              },
              onEditRecurringRule = { rule ->
                editingRecurringRule = rule
                showAddEditRecurringSheet = true
              }
            )
          }
        }
      }
    }
  }
}
