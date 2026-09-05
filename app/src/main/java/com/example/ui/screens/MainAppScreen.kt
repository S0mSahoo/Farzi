package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.pro.entitlement.ProFeature
import com.example.ui.components.AddEditRecurringSheet
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.AppBrandLogo
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CopilotSheet
import com.example.ui.components.DateUtils
import com.example.ui.components.ExportDataModal
import com.example.ui.components.ForecastSheet
import com.example.ui.components.ProUpgradeSheet
import com.example.ui.components.SetBudgetSheet
import com.example.ui.components.WhatIfSheet
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.FinanceViewModel
import java.time.YearMonth

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
  val context = LocalContext.current
  val userProfile by viewModel.userProfile.collectAsState()
  val dashboardCalendar by viewModel.dashboardCalendar.collectAsState()
  val budgetCalendar by viewModel.budgetCalendar.collectAsState()
  val allBudgets by viewModel.allBudgets.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  var isSettingsOpen by remember { mutableStateOf(false) }
  var showProfileSheet by remember { mutableStateOf(false) }
  var showSignOutDialog by remember { mutableStateOf(false) }

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

  // Paisa Pro Sheet States
  var showProUpgradeSheet by remember { mutableStateOf(false) }
  var targetProFeature by remember { mutableStateOf<ProFeature?>(null) }
  var showForecastSheet by remember { mutableStateOf(false) }
  var showWhatIfSheet by remember { mutableStateOf(false) }
  var showCopilotSheet by remember { mutableStateOf(false) }

  val copilotMessages by viewModel.copilotMessages.collectAsState()
  val isCopilotLoading by viewModel.isCopilotLoading.collectAsState()

  val transactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val recurringSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val budgetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val proUpgradeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val forecastSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val whatIfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val copilotSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val activeBudgetMonthKey = targetBudgetMonthKey ?: DateUtils.getMonthKey(budgetCalendar)
  val activeBudgetMonthLabel = targetBudgetMonthLabel ?: DateUtils.getMonthLabel(budgetCalendar)
  val currentBudget = allBudgets.find { it.monthKey == activeBudgetMonthKey }

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

  // Paisa Pro Sheets
  if (showProUpgradeSheet) {
    ProUpgradeSheet(
      sheetState = proUpgradeSheetState,
      targetFeature = targetProFeature,
      onDismiss = {
        showProUpgradeSheet = false
        targetProFeature = null
      },
      onUnlockPro = {
        viewModel.setDevProEnabled(true)
        Toast.makeText(context, "Paisa Pro Activated!", Toast.LENGTH_SHORT).show()
        val featureToOpen = targetProFeature
        showProUpgradeSheet = false
        targetProFeature = null
        when (featureToOpen) {
          ProFeature.CASH_FLOW_FORECAST -> showForecastSheet = true
          ProFeature.WHAT_IF_SIMULATOR -> showWhatIfSheet = true
          ProFeature.AI_COPILOT -> showCopilotSheet = true
          else -> {}
        }
      }
    )
  }

  if (showForecastSheet) {
    val cal = dashboardCalendar
    val yearMonth = YearMonth.of(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
    val forecastResult = remember(cal, allBudgets) {
      viewModel.getCashFlowForecast(yearMonth)
    }
    ForecastSheet(
      forecast = forecastResult,
      currencySymbol = userProfile.currencySymbol,
      sheetState = forecastSheetState,
      onDismiss = { showForecastSheet = false }
    )
  }

  if (showWhatIfSheet) {
    WhatIfSheet(
      currencySymbol = userProfile.currencySymbol,
      sheetState = whatIfSheetState,
      onRunSimulation = { scenario ->
        val cal = dashboardCalendar
        val yearMonth = YearMonth.of(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
        viewModel.simulateWhatIfScenario(scenario, yearMonth)
      },
      onDismiss = { showWhatIfSheet = false }
    )
  }

  if (showCopilotSheet) {
    CopilotSheet(
      messages = copilotMessages,
      isLoading = isCopilotLoading,
      sheetState = copilotSheetState,
      onSendMessage = { question -> viewModel.askCopilot(question) },
      onClearChat = { viewModel.clearCopilotMessages() },
      onDismiss = { showCopilotSheet = false }
    )
  }

  if (showProfileSheet) {
    ModalBottomSheet(
      onDismissRequest = { showProfileSheet = false },
      sheetState = profileSheetState,
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Account Profile Header
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (!userProfile.photoUrl.isNullOrBlank()) {
              AsyncImage(
                model = userProfile.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp).clip(CircleShape)
              )
            } else {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(50.dp)) {
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



        // Export & Portability Item
        Surface(
          onClick = {
            showProfileSheet = false
            showExportModal = true
          },
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_export_button")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Export & Portability", fontWeight = FontWeight.Bold)
              Text("PDF reports & JSON backups", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        // Settings Item
        Surface(
          onClick = {
            showProfileSheet = false
            isSettingsOpen = true
          },
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_settings_button")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Settings & Preferences", fontWeight = FontWeight.Bold)
              Text("App lock, themes & currency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sign Out Button
        OutlinedButton(
          onClick = {
            showProfileSheet = false
            showSignOutDialog = true
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
          border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("profile_sign_out_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out of Account", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Screen blocker overlay for recents
  val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
  var isAppInBackground by remember { mutableStateOf(false) }
  androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      when (event) {
        androidx.lifecycle.Lifecycle.Event.ON_START -> isAppInBackground = false
        androidx.lifecycle.Lifecycle.Event.ON_STOP -> isAppInBackground = true
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktopLandscape = maxWidth > 480.dp

        if (isDesktopLandscape) {
          Row(modifier = Modifier.fillMaxSize()) {
            // Left Sidebar / NavigationRail
            Surface(
              modifier = Modifier
                .fillMaxHeight()
                .width(260.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 3.dp,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                  ) {
                    AppBrandLogo(size = 32.dp)
                    Text(
                      text = "Paisa",
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }

                  androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val navItems = listOf(
                      AppDestination.Dashboard,
                      AppDestination.Calendar,
                      AppDestination.Transactions,
                      AppDestination.Budgets,
                      AppDestination.Recurring
                    )

                    navItems.forEach { destination ->
                      val isSelected = selectedTab == destination.index
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(14.dp))
                          .clickable { selectedTab = destination.index }
                          .testTag("desktop_nav_${destination.label.lowercase()}")
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                          Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                          )
                          Text(
                            text = destination.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                          )
                        }
                      }
                    }
                  }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                  // Export & Portability Item
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { showExportModal = true }
                      .testTag("desktop_nav_export")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                      Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                      Text(
                        text = "Export & Reports",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }

                  // Settings Item
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSettingsOpen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { isSettingsOpen = !isSettingsOpen }
                      .testTag("desktop_nav_settings")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (isSettingsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                      )
                      Text(
                        text = "Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSettingsOpen) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSettingsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }

            // Right Content Area with TopAppBar & Action Buttons
            Scaffold(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
              topBar = {
                TopAppBar(
                  title = {
                    Text(
                      text = when {
                        isSettingsOpen -> "Settings"
                        else -> listOf("Dashboard", "Calendar", "Transactions", "Budgets", "Recurring")[selectedTab]
                      },
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold
                    )
                  },
                  navigationIcon = {
                    if (isSettingsOpen) {
                      IconButton(onClick = { isSettingsOpen = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                      }
                    }
                  },
                  actions = {
                    if (!isSettingsOpen) {
                      IconButton(
                        onClick = { showExportModal = true },
                        modifier = Modifier.testTag("landscape_top_bar_export")
                      ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                      }
                      IconButton(
                        onClick = { isSettingsOpen = true },
                        modifier = Modifier.testTag("landscape_top_bar_settings")
                      ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                      }
                      Spacer(modifier = Modifier.width(4.dp))
                    }
                  },
                  colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                  )
                )
              },
              containerColor = MaterialTheme.colorScheme.background,
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
                    modifier = Modifier
                      .padding(24.dp)
                      .testTag("global_add_transaction_fab")
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
                  .padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
                    .graphicsLayer(scaleX = 0.92f, scaleY = 0.92f)
                ) {
                  if (isSettingsOpen) {
                    BackHandler { isSettingsOpen = false }
                    SettingsScreen(
                      viewModel = viewModel,
                      onOpenExportModal = { showExportModal = true }
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
                          targetBudgetMonthKey = DateUtils.getMonthKey(dashboardCalendar)
                          targetBudgetMonthLabel = DateUtils.getMonthLabel(dashboardCalendar)
                          showSetBudgetSheet = true
                        },
                        onOpenCopilot = { showCopilotSheet = true },
                        onOpenForecast = { showForecastSheet = true },
                        onOpenWhatIf = { showWhatIfSheet = true },
                        onOpenProUpgrade = { feature ->
                          targetProFeature = feature
                          showProUpgradeSheet = true
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
                          targetBudgetMonthKey = DateUtils.getMonthKey(budgetCalendar)
                          targetBudgetMonthLabel = DateUtils.getMonthLabel(budgetCalendar)
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
        } else {
          // Portrait Mode Scaffold
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
                  }
                },
                actions = {
                  if (!isSettingsOpen) {
                    IconButton(
                      onClick = { showExportModal = true },
                      modifier = Modifier.testTag("top_bar_export")
                    ) {
                      Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                      onClick = { isSettingsOpen = true },
                      modifier = Modifier.testTag("top_bar_settings")
                    ) {
                      Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
                BackHandler { isSettingsOpen = false }
                SettingsScreen(
                  viewModel = viewModel,
                  onOpenExportModal = { showExportModal = true }
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
                      targetBudgetMonthKey = DateUtils.getMonthKey(dashboardCalendar)
                      targetBudgetMonthLabel = DateUtils.getMonthLabel(dashboardCalendar)
                      showSetBudgetSheet = true
                    },
                    onOpenCopilot = { showCopilotSheet = true },
                    onOpenForecast = { showForecastSheet = true },
                    onOpenWhatIf = { showWhatIfSheet = true },
                    onOpenProUpgrade = { feature ->
                      targetProFeature = feature
                      showProUpgradeSheet = true
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
                      targetBudgetMonthKey = DateUtils.getMonthKey(budgetCalendar)
                      targetBudgetMonthLabel = DateUtils.getMonthLabel(budgetCalendar)
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

      if (isAppInBackground) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = false) {}, // Block clicks
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppBrandLogo(size = 80.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "App Content Blocked",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
