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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.example.ui.util.AppNavigationType
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.util.calculateAdaptiveInfo
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
import com.example.ui.components.AddEditRecurringSheet
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.AppBrandLogo
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DateUtils
import com.example.ui.components.ExportDataModal
import com.example.ui.components.SetBudgetSheet
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.FinanceViewModel

sealed class AppDestination(val index: Int, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  object Dashboard : AppDestination(0, "Home", Icons.Default.Dashboard)
  object Transactions : AppDestination(1, "Transactions", Icons.Default.ReceiptLong)
  object Calendar : AppDestination(2, "Calendar", Icons.Default.CalendarMonth)
  object Budgets : AppDestination(3, "Budgets", Icons.Default.AccountBalanceWallet)
  object Recurring : AppDestination(4, "Recurring", Icons.Default.Repeat)
  object Categories : AppDestination(5, "Categories", Icons.Default.Category)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
  viewModel: FinanceViewModel
) {
  val context = LocalContext.current
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val dashboardCalendar by viewModel.dashboardCalendar.collectAsStateWithLifecycle()
  val budgetCalendar by viewModel.budgetCalendar.collectAsStateWithLifecycle()
  val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var isSettingsOpen by remember { mutableStateOf(false) }
  var isAnalyticsOpen by remember { mutableStateOf(false) }
  var isMoreOpen by remember { mutableStateOf(false) }
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

  val transactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val recurringSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val budgetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
      val adaptiveInfo = calculateAdaptiveInfo(maxWidth, maxHeight)

      CompositionLocalProvider(LocalAdaptiveInfo provides adaptiveInfo) {
        when (adaptiveInfo.navigationType) {
          AppNavigationType.PERMANENT_DRAWER -> {
            Row(modifier = Modifier.fillMaxSize()) {
              // Left Permanent Navigation Drawer (240dp)
              Surface(
                modifier = Modifier
                  .fillMaxHeight()
                  .width(240.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                      AppBrandLogo(size = 32.dp)
                      Text(
                        text = "Paisa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }

                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      val navItems = listOf(
                        AppDestination.Dashboard,
                        AppDestination.Transactions,
                        AppDestination.Calendar,
                        AppDestination.Budgets,
                        AppDestination.Recurring,
                        AppDestination.Categories
                      )

                      navItems.forEach { destination ->
                        val isSelected = !isSettingsOpen && !isAnalyticsOpen && selectedTab == destination.index
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
                          modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                              isSettingsOpen = false
                              isAnalyticsOpen = false
                              isMoreOpen = false
                              selectedTab = destination.index
                            }
                            .testTag("desktop_nav_${destination.label.lowercase()}")
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
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

                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Export & Portability Item
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color.Transparent,
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showExportModal = true }
                        .testTag("desktop_nav_export")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                      shape = RoundedCornerShape(12.dp),
                      color = if (isSettingsOpen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isSettingsOpen = !isSettingsOpen }
                        .testTag("desktop_nav_settings")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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

              // Main content area for Drawer
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
                          isAnalyticsOpen -> "Analytics"
                          else -> listOf("Home", "Transactions", "Calendar", "Budgets", "Recurring", "Categories")[selectedTab]
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                      )
                    },
                    navigationIcon = {
                      if (isSettingsOpen || isAnalyticsOpen) {
                        IconButton(onClick = { if (isSettingsOpen) isSettingsOpen = false else isAnalyticsOpen = false }) {
                          Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                      }
                    },
                    actions = {
                      if (!isSettingsOpen && !isAnalyticsOpen && !isMoreOpen) {
                        IconButton(
                          onClick = { isSettingsOpen = false; isAnalyticsOpen = true },
                          modifier = Modifier.testTag("analytics_open_button")
                        ) {
                          Icon(Icons.Default.TrendingUp, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                          onClick = { showExportModal = true },
                          modifier = Modifier.testTag("landscape_top_bar_export")
                        ) {
                          Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                          onClick = { isAnalyticsOpen = false; isSettingsOpen = true },
                          modifier = Modifier.testTag("landscape_top_bar_settings")
                        ) {
                          Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.background,
                      titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                  )
                },
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButton = {
                  if (!isSettingsOpen && !isAnalyticsOpen && (selectedTab == 0 || selectedTab == 1 || selectedTab == 2)) {
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
                      elevation = FloatingActionButtonDefaults.elevation(4.dp),
                      modifier = Modifier
                        .padding(20.dp)
                        .testTag("global_add_transaction_fab")
                    ) {
                      Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(26.dp))
                    }
                  }
                }
              ) { innerPadding ->
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                  contentAlignment = Alignment.TopCenter
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .widthIn(max = 1200.dp)
                  ) {
                    MainScreenContent(
                      selectedTab = selectedTab,
                      isSettingsOpen = isSettingsOpen,
                      isAnalyticsOpen = isAnalyticsOpen,
                      isMoreOpen = isMoreOpen,
                      onBackFromAnalytics = { isAnalyticsOpen = false },
                      onBackFromMore = { isMoreOpen = false },
                      onOpenMoreDestination = { destination ->
                        isMoreOpen = false
                        isAnalyticsOpen = false
                        isSettingsOpen = false
                        selectedTab = destination
                      },
                      onOpenMoreAnalytics = { isMoreOpen = false; isSettingsOpen = false; isAnalyticsOpen = true },
                      onOpenMoreSettings = { isMoreOpen = false; isAnalyticsOpen = false; isSettingsOpen = true },
                      onBackFromSettings = { isSettingsOpen = false },
                      viewModel = viewModel,
                      dashboardCalendar = dashboardCalendar,
                      budgetCalendar = budgetCalendar,
                      onOpenAddTransaction = { prefilledType ->
                        editingTransaction = null
                        prefilledDateTimestamp = null
                        prefilledTxType = prefilledType
                        showAddEditTransactionSheet = true
                      },
                      onOpenSetBudget = { key, label ->
                        targetBudgetMonthKey = key
                        targetBudgetMonthLabel = label
                        showSetBudgetSheet = true
                      },
                      onAddTransactionForDate = { timestamp ->
                        editingTransaction = null
                        prefilledDateTimestamp = timestamp
                        prefilledTxType = null
                        showAddEditTransactionSheet = true
                      },
                      onEditTransaction = { item ->
                        editingTransaction = item
                        showAddEditTransactionSheet = true
                      },
                      onOpenAddRecurringRule = {
                        editingRecurringRule = null
                        showAddEditRecurringSheet = true
                      },
                      onEditRecurringRule = { rule ->
                        editingRecurringRule = rule
                        showAddEditRecurringSheet = true
                      },
                      onOpenExportModal = { isMoreOpen = false; showExportModal = true }
                    )
                  }
                }
              }
            }
          }

          AppNavigationType.NAVIGATION_RAIL -> {
            Row(modifier = Modifier.fillMaxSize()) {
              // Space-efficient NavigationRail for landscape phones & medium tablets
              NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                  Spacer(modifier = Modifier.height(if (adaptiveInfo.isLandscapePhone) 4.dp else 12.dp))
                  AppBrandLogo(size = if (adaptiveInfo.isLandscapePhone) 26.dp else 32.dp)
                  Spacer(modifier = Modifier.height(if (adaptiveInfo.isLandscapePhone) 4.dp else 12.dp))
                }
              ) {
                Column(
                  modifier = Modifier.fillMaxHeight(),
                  verticalArrangement = Arrangement.SpaceBetween,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(if (adaptiveInfo.isLandscapePhone) 2.dp else 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    val navItems = listOf(
                      AppDestination.Dashboard,
                      AppDestination.Transactions,
                      AppDestination.Calendar,
                      AppDestination.Budgets,
                      AppDestination.Recurring,
                        AppDestination.Categories
                    )

                    navItems.forEach { destination ->
                      val isSelected = !isSettingsOpen && !isAnalyticsOpen && selectedTab == destination.index
                      NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                          isSettingsOpen = false
                          isAnalyticsOpen = false
                          isMoreOpen = false
                          selectedTab = destination.index
                        },
                        icon = {
                          Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(if (adaptiveInfo.isLandscapePhone) 20.dp else 22.dp)
                          )
                        },
                        label = if (!adaptiveInfo.isLandscapePhone) {
                          {
                            Text(
                              text = destination.label,
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                          }
                        } else null,
                        colors = NavigationRailItemDefaults.colors(
                          selectedIconColor = MaterialTheme.colorScheme.primary,
                          indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("desktop_nav_${destination.label.lowercase()}")
                      )
                    }
                  }

                  // Rail Bottom Actions
                  Column(
                    verticalArrangement = Arrangement.spacedBy(if (adaptiveInfo.isLandscapePhone) 2.dp else 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = if (adaptiveInfo.isLandscapePhone) 4.dp else 12.dp)
                  ) {
                    IconButton(
                      onClick = { showExportModal = true },
                      modifier = Modifier.testTag("desktop_nav_export")
                    ) {
                      Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                      onClick = { isAnalyticsOpen = false; isSettingsOpen = !isSettingsOpen },
                      modifier = Modifier.testTag("desktop_nav_settings")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (isSettingsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }

              // Content Scaffold for NavigationRail
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
                          isAnalyticsOpen -> "Analytics"
                          else -> listOf("Home", "Transactions", "Calendar", "Budgets", "Recurring", "Categories")[selectedTab]
                        },
                        style = if (adaptiveInfo.isLandscapePhone) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                      )
                    },
                    navigationIcon = {
                      if (isSettingsOpen || isAnalyticsOpen) {
                        IconButton(onClick = { if (isSettingsOpen) isSettingsOpen = false else isAnalyticsOpen = false }) {
                          Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                      }
                    },
                    actions = {
                      if (!isSettingsOpen && !isAnalyticsOpen) {
                        IconButton(
                          onClick = { isSettingsOpen = false; isAnalyticsOpen = true },
                          modifier = Modifier.testTag("analytics_open_button")
                        ) {
                          Icon(Icons.Default.TrendingUp, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                          onClick = { showExportModal = true },
                          modifier = Modifier.testTag("landscape_top_bar_export")
                        ) {
                          Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                          onClick = { isAnalyticsOpen = false; isSettingsOpen = true },
                          modifier = Modifier.testTag("landscape_top_bar_settings")
                        ) {
                          Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.background,
                      titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                  )
                },
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButton = {
                  if (!isSettingsOpen && !isAnalyticsOpen && (selectedTab == 0 || selectedTab == 1 || selectedTab == 2)) {
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
                      elevation = FloatingActionButtonDefaults.elevation(4.dp),
                      modifier = Modifier
                        .padding(if (adaptiveInfo.isLandscapePhone) 8.dp else 16.dp)
                        .testTag("global_add_transaction_fab")
                    ) {
                      Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(24.dp))
                    }
                  }
                }
              ) { innerPadding ->
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = if (adaptiveInfo.isLandscapePhone) 10.dp else 16.dp),
                  contentAlignment = Alignment.TopCenter
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .widthIn(max = 1100.dp)
                  ) {
                    MainScreenContent(
                      selectedTab = selectedTab,
                      isSettingsOpen = isSettingsOpen,
                      isAnalyticsOpen = isAnalyticsOpen,
                      isMoreOpen = isMoreOpen,
                      onBackFromAnalytics = { isAnalyticsOpen = false },
                      onBackFromMore = { isMoreOpen = false },
                      onOpenMoreDestination = { destination ->
                        isMoreOpen = false
                        isAnalyticsOpen = false
                        isSettingsOpen = false
                        selectedTab = destination
                      },
                      onOpenMoreAnalytics = { isMoreOpen = false; isSettingsOpen = false; isAnalyticsOpen = true },
                      onOpenMoreSettings = { isMoreOpen = false; isAnalyticsOpen = false; isSettingsOpen = true },
                      onBackFromSettings = { isSettingsOpen = false },
                      viewModel = viewModel,
                      dashboardCalendar = dashboardCalendar,
                      budgetCalendar = budgetCalendar,
                      onOpenAddTransaction = { prefilledType ->
                        editingTransaction = null
                        prefilledDateTimestamp = null
                        prefilledTxType = prefilledType
                        showAddEditTransactionSheet = true
                      },
                      onOpenSetBudget = { key, label ->
                        targetBudgetMonthKey = key
                        targetBudgetMonthLabel = label
                        showSetBudgetSheet = true
                      },
                      onAddTransactionForDate = { timestamp ->
                        editingTransaction = null
                        prefilledDateTimestamp = timestamp
                        prefilledTxType = null
                        showAddEditTransactionSheet = true
                      },
                      onEditTransaction = { item ->
                        editingTransaction = item
                        showAddEditTransactionSheet = true
                      },
                      onOpenAddRecurringRule = {
                        editingRecurringRule = null
                        showAddEditRecurringSheet = true
                      },
                      onEditRecurringRule = { rule ->
                        editingRecurringRule = rule
                        showAddEditRecurringSheet = true
                      },
                      onOpenExportModal = { isMoreOpen = false; showExportModal = true }
                    )
                  }
                }
              }
            }
          }

          AppNavigationType.BOTTOM_BAR -> {
            // Standard portrait phone scaffold
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
                        text = when { isSettingsOpen -> "Settings"; isAnalyticsOpen -> "Analytics"; isMoreOpen -> "More"; selectedTab == 1 -> "Transactions"; selectedTab == 2 -> "Calendar"; selectedTab == 3 -> "Budgets"; selectedTab == 4 -> "Recurring"; selectedTab == 5 -> "Categories"; else -> "Paisa" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  },
                  navigationIcon = {
                    if (isSettingsOpen || isAnalyticsOpen) {
                      IconButton(onClick = { if (isSettingsOpen) isSettingsOpen = false else isAnalyticsOpen = false }) {
                          Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                      }
                    }
                  },
                  actions = {
                    if (!isSettingsOpen && !isAnalyticsOpen) {
                      IconButton(
                          onClick = { isSettingsOpen = false; isAnalyticsOpen = true },
                          modifier = Modifier.testTag("analytics_open_button")
                        ) {
                          Icon(Icons.Default.TrendingUp, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                        onClick = { showExportModal = true },
                        modifier = Modifier.testTag("top_bar_export")
                      ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                      }
                      IconButton(
                        onClick = { isAnalyticsOpen = false; isSettingsOpen = true },
                        modifier = Modifier.testTag("top_bar_settings")
                      ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                      }
                      Spacer(modifier = Modifier.width(4.dp))
                    }
                  },
                  colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                  )
                )
              },
              bottomBar = {
                if (!isSettingsOpen && !isAnalyticsOpen) {
                  NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                  ) {
                    NavigationBarItem(
                      selected = !isMoreOpen && selectedTab == AppDestination.Dashboard.index,
                      onClick = {
                        isMoreOpen = false
                        isSettingsOpen = false
                        isAnalyticsOpen = false
                        selectedTab = AppDestination.Dashboard.index
                      },
                      icon = {
                        Icon(
                          imageVector = AppDestination.Dashboard.icon,
                          contentDescription = "Home",
                          modifier = Modifier.size(22.dp)
                        )
                      },
                      label = { Text("Home", maxLines = 1, softWrap = false) },
                      colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                      ),
                      modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                      selected = !isMoreOpen && selectedTab == AppDestination.Transactions.index,
                      onClick = {
                        isMoreOpen = false
                        isSettingsOpen = false
                        isAnalyticsOpen = false
                        selectedTab = AppDestination.Transactions.index
                      },
                      icon = {
                        Icon(
                          imageVector = AppDestination.Transactions.icon,
                          contentDescription = "Transactions",
                          modifier = Modifier.size(22.dp)
                        )
                      },
                      label = { Text("Transactions", maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) },
                      colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                      ),
                      modifier = Modifier.testTag("nav_transactions")
                    )
                    NavigationBarItem(
                      selected = false,
                      onClick = {
                        isMoreOpen = false
                        isSettingsOpen = false
                        isAnalyticsOpen = false
                        editingTransaction = null
                        prefilledDateTimestamp = null
                        prefilledTxType = null
                        showAddEditTransactionSheet = true
                      },
                      icon = {
                        Surface(
                          shape = CircleShape,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(40.dp)
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              imageVector = Icons.Default.Add,
                              contentDescription = "Add transaction",
                              tint = Color.White,
                              modifier = Modifier.size(23.dp)
                            )
                          }
                        }
                      },
                      label = { Text("Add", maxLines = 1, softWrap = false) },
                      colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                      ),
                      modifier = Modifier.testTag("nav_add_transaction")
                    )
                    NavigationBarItem(
                      selected = !isMoreOpen && selectedTab == AppDestination.Budgets.index,
                      onClick = {
                        isMoreOpen = false
                        isSettingsOpen = false
                        isAnalyticsOpen = false
                        selectedTab = AppDestination.Budgets.index
                      },
                      icon = {
                        Icon(
                          imageVector = AppDestination.Budgets.icon,
                          contentDescription = "Budgets",
                          modifier = Modifier.size(22.dp)
                        )
                      },
                      label = { Text("Budgets", maxLines = 1, softWrap = false) },
                      colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                      ),
                      modifier = Modifier.testTag("nav_budgets")
                    )
                    NavigationBarItem(
                      selected = isMoreOpen || selectedTab == AppDestination.Calendar.index || selectedTab == AppDestination.Recurring.index || selectedTab == AppDestination.Categories.index,
                      onClick = {
                        isMoreOpen = true
                        isSettingsOpen = false
                        isAnalyticsOpen = false
                      },
                      icon = {
                        Icon(
                          imageVector = Icons.Default.MoreHoriz,
                          contentDescription = "More",
                          modifier = Modifier.size(22.dp)
                        )
                      },
                      label = { Text("More", maxLines = 1, softWrap = false) },
                      colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                      ),
                      modifier = Modifier.testTag("nav_more")
                    )
                  }
                }
              },
              floatingActionButton = { }
            ) { innerPadding ->
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(innerPadding)
              ) {
                MainScreenContent(
                  selectedTab = selectedTab,
                  isSettingsOpen = isSettingsOpen,
                  isAnalyticsOpen = isAnalyticsOpen,
                  isMoreOpen = isMoreOpen,
                  onBackFromAnalytics = { isAnalyticsOpen = false },
                  onBackFromMore = { isMoreOpen = false },
                  onOpenMoreDestination = { destination ->
                    isMoreOpen = false
                    isAnalyticsOpen = false
                    isSettingsOpen = false
                    selectedTab = destination
                  },
                  onOpenMoreAnalytics = { isMoreOpen = false; isSettingsOpen = false; isAnalyticsOpen = true },
                  onOpenMoreSettings = { isMoreOpen = false; isAnalyticsOpen = false; isSettingsOpen = true },
                  onBackFromSettings = { isSettingsOpen = false },
                  viewModel = viewModel,
                  dashboardCalendar = dashboardCalendar,
                  budgetCalendar = budgetCalendar,
                  onOpenAddTransaction = { prefilledType ->
                    editingTransaction = null
                    prefilledDateTimestamp = null
                    prefilledTxType = prefilledType
                    showAddEditTransactionSheet = true
                  },
                  onOpenSetBudget = { key, label ->
                    targetBudgetMonthKey = key
                    targetBudgetMonthLabel = label
                    showSetBudgetSheet = true
                  },
                  onAddTransactionForDate = { timestamp ->
                    editingTransaction = null
                    prefilledDateTimestamp = timestamp
                    prefilledTxType = null
                    showAddEditTransactionSheet = true
                  },
                  onEditTransaction = { item ->
                    editingTransaction = item
                    showAddEditTransactionSheet = true
                  },
                  onOpenAddRecurringRule = {
                    editingRecurringRule = null
                    showAddEditRecurringSheet = true
                  },
                  onEditRecurringRule = { rule ->
                    editingRecurringRule = rule
                    showAddEditRecurringSheet = true
                  },
                  onOpenExportModal = { isMoreOpen = false; showExportModal = true }
                )
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
}

@Composable
private fun MainScreenContent(
  selectedTab: Int,
  isSettingsOpen: Boolean,
  isAnalyticsOpen: Boolean,
  isMoreOpen: Boolean,
  onBackFromAnalytics: () -> Unit,
  onBackFromMore: () -> Unit,
  onOpenMoreDestination: (Int) -> Unit,
  onOpenMoreAnalytics: () -> Unit,
  onOpenMoreSettings: () -> Unit,
  onBackFromSettings: () -> Unit,
  viewModel: FinanceViewModel,
  dashboardCalendar: java.util.Calendar,
  budgetCalendar: java.util.Calendar,
  onOpenAddTransaction: (prefilledType: TransactionType?) -> Unit,
  onOpenSetBudget: (monthKey: String, monthLabel: String) -> Unit,
  onAddTransactionForDate: (timestamp: Long) -> Unit,
  onEditTransaction: (item: TransactionItem) -> Unit,
  onOpenAddRecurringRule: () -> Unit,
  onEditRecurringRule: (rule: RecurringRule) -> Unit,
  onOpenExportModal: () -> Unit
) {
  if (isSettingsOpen) {
    BackHandler { onBackFromSettings() }
    SettingsScreen(
      viewModel = viewModel,
      onOpenExportModal = onOpenExportModal
    )
  } else if (isAnalyticsOpen) {
    BackHandler { onBackFromAnalytics() }
    AnalyticsScreen(viewModel = viewModel)
  } else if (isMoreOpen) {
    BackHandler { onBackFromMore() }
    MoreScreen(
      onOpenCalendar = { onOpenMoreDestination(2) },
      onOpenRecurring = { onOpenMoreDestination(4) },
      onOpenAnalytics = onOpenMoreAnalytics,
      onOpenCategories = { onOpenMoreDestination(5) },
      onOpenExport = onOpenExportModal,
      onOpenSettings = onOpenMoreSettings
    )
  } else {
    when (selectedTab) {
      0 -> DashboardScreen(
        viewModel = viewModel,
        onOpenAddTransaction = onOpenAddTransaction,
        onOpenSetBudget = {
          onOpenSetBudget(
            DateUtils.getMonthKey(dashboardCalendar),
            DateUtils.getMonthLabel(dashboardCalendar)
          )
        }
      )
      1 -> TransactionsScreen(
        viewModel = viewModel,
        onEditTransaction = onEditTransaction
      )
      2 -> CalendarScreen(
        viewModel = viewModel,
        onAddTransactionForDate = onAddTransactionForDate,
        onEditTransaction = onEditTransaction
      )
      3 -> BudgetScreen(
        viewModel = viewModel,
        onOpenSetBudget = {
          onOpenSetBudget(
            DateUtils.getMonthKey(budgetCalendar),
            DateUtils.getMonthLabel(budgetCalendar)
          )
        }
      )
      4 -> RecurringScreen(
        viewModel = viewModel,
        onOpenAddRecurringRule = onOpenAddRecurringRule,
        onEditRecurringRule = onEditRecurringRule
      )
      5 -> CategoriesScreen(viewModel = viewModel)
    }
  }
}
