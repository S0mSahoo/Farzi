package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.viewmodel.AiAdvisorViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.util.AppNavigationType
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.util.calculateAdaptiveInfo
import com.example.ui.util.PaisaMotion
import com.example.ui.viewmodel.FinanceViewModel

sealed class AppDestination(val index: Int, val label: String, val icon: ImageVector) {
  object Dashboard : AppDestination(0, "Home", Icons.Default.Dashboard)
  object Transactions : AppDestination(1, "Transactions", Icons.AutoMirrored.Filled.ReceiptLong)
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
  var isAiAdvisorOpen by remember { mutableStateOf(false) }
  val aiViewModel: AiAdvisorViewModel = viewModel()
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
  val isGuestSession = userProfile.email.equals("guest@paisa.local", ignoreCase = true) &&
    userProfile.googleId.isNullOrBlank()

  if (showSignOutDialog) {
    ConfirmationDialog(
      title = if (isGuestSession) "Leave Guest Mode?" else "Sign Out from Google Account?",
      message = if (isGuestSession) {
        "Your guest financial records will remain on this device and will not be uploaded to Google Drive."
      } else {
        "Paisa will verify and save your latest backup before clearing the local cache."
      },
      confirmButtonText = if (isGuestSession) "Leave Guest Mode" else "Sign Out",
      confirmButtonColor = ExpenseRed,
      onConfirm = {
        viewModel.signOut { success ->
          if (success) {
            Toast.makeText(
              context,
              if (isGuestSession) "Guest mode closed." else "Signed out successfully",
              Toast.LENGTH_SHORT
            ).show()
            showSignOutDialog = false
          } else {
            Toast.makeText(
              context,
              viewModel.syncErrorMessage.value ?: "Sign-out failed.",
              Toast.LENGTH_LONG
            ).show()
          }
        }
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
        // Profile Header
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
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
                modifier = Modifier.size(52.dp).clip(CircleShape)
              )
            } else {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp)) {
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
                text = userProfile.name.ifBlank { "Guest User" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = userProfile.email.ifBlank { "Local Guest Session" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Export Item
        Surface(
          onClick = {
            showProfileSheet = false
            showExportModal = true
          },
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth().testTag("profile_export_button")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Export Reports & Backup", fontWeight = FontWeight.Bold)
              Text("PDF statements & JSON portability", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth().testTag("profile_settings_button")
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
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_sign_out_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isGuestSession) "Leave Guest Mode" else "Sign Out of Account", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  var isAppInBackground by remember { mutableStateOf(false) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> isAppInBackground = false
        Lifecycle.Event.ON_STOP -> isAppInBackground = true
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val primaryDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.Transactions,
    AppDestination.Calendar,
    AppDestination.Budgets,
    AppDestination.Recurring
  )

  Box(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val adaptiveInfo = calculateAdaptiveInfo(maxWidth, maxHeight)

      CompositionLocalProvider(LocalAdaptiveInfo provides adaptiveInfo) {
        when (adaptiveInfo.navigationType) {
          AppNavigationType.PERMANENT_DRAWER -> {
            Row(modifier = Modifier.fillMaxSize()) {
              Surface(
                modifier = Modifier.fillMaxHeight().width(240.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
              ) {
                Column(
                  modifier = Modifier.fillMaxSize().padding(16.dp),
                  verticalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                      AppBrandLogo(size = 32.dp)
                      Text("Paisa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      listOf(
                        AppDestination.Dashboard,
                        AppDestination.Transactions,
                        AppDestination.Calendar,
                        AppDestination.Budgets,
                        AppDestination.Recurring,
                        AppDestination.Categories
                      ).forEach { destination ->
                        val isSelected = !isSettingsOpen && selectedTab == destination.index
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent,
                          modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                              isSettingsOpen = false
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = if (isSettingsOpen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent,
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
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (isSettingsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text("Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSettingsOpen) FontWeight.Bold else FontWeight.Medium, color = if (isSettingsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                      }
                    }
                  }
                }
              }

              Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                topBar = {
                  TopAppBar(
                    title = {
                      Text(
                        text = when {
                          isSettingsOpen -> "Settings"
                          selectedTab == 0 -> "Dashboard & Analytics"
                          selectedTab == 1 -> "Transactions"
                          selectedTab == 2 -> "Calendar"
                          selectedTab == 3 -> "Monthly Budgets"
                          selectedTab == 4 -> "Recurring & Bills"
                          else -> "Categories"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                      )
                    },
                    navigationIcon = {
                      if (isSettingsOpen) {
                        IconButton(onClick = { isSettingsOpen = false }) {
                          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                      }
                    },
                    actions = {
                      if (!isSettingsOpen) {
                        IconButton(onClick = { isAiAdvisorOpen = true }, modifier = Modifier.testTag("landscape_top_bar_ai_advisor")) {
                          Icon(Icons.Default.AutoAwesome, contentDescription = "Paisa AI Advisor", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showExportModal = true }, modifier = Modifier.testTag("landscape_top_bar_export")) {
                          Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isSettingsOpen = true }, modifier = Modifier.testTag("landscape_top_bar_settings")) {
                          Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                      }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                  )
                },
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButton = {
                  if (!isSettingsOpen && selectedTab in listOf(0, 1, 2, 3, 4)) {
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
                      modifier = Modifier.padding(20.dp).testTag("global_add_transaction_fab")
                    ) {
                      Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(26.dp))
                    }
                  }
                }
              ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
                  Box(modifier = Modifier.fillMaxSize().widthIn(max = 1200.dp)) {
                    MainScreenContent(
                      selectedTab = selectedTab,
                      isSettingsOpen = isSettingsOpen,
                      onNavigateTab = { tab ->
                        isSettingsOpen = false
                        selectedTab = tab
                      },
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
                      onOpenExportModal = { showExportModal = true }
                    )
                  }
                }
              }
            }
          }

          AppNavigationType.NAVIGATION_RAIL -> {
            Row(modifier = Modifier.fillMaxSize()) {
              NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                  Spacer(modifier = Modifier.height(8.dp))
                  AppBrandLogo(size = 28.dp)
                  Spacer(modifier = Modifier.height(8.dp))
                }
              ) {
                Column(
                  modifier = Modifier.fillMaxHeight(),
                  verticalArrangement = Arrangement.SpaceBetween,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    primaryDestinations.forEach { destination ->
                      val isSelected = !isSettingsOpen && selectedTab == destination.index
                      NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                          isSettingsOpen = false
                          selectedTab = destination.index
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.size(22.dp)) },
                        label = { Text(destination.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationRailItemDefaults.colors(
                          selectedIconColor = MaterialTheme.colorScheme.primary,
                          indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.testTag("desktop_nav_${destination.label.lowercase()}")
                      )
                    }
                  }

                  Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 8.dp)
                  ) {
                    IconButton(onClick = { showExportModal = true }, modifier = Modifier.testTag("desktop_nav_export")) {
                      Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { isSettingsOpen = !isSettingsOpen }, modifier = Modifier.testTag("desktop_nav_settings")) {
                      Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (isSettingsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                  }
                }
              }

              Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                topBar = {
                  TopAppBar(
                    title = {
                      Text(
                        text = when {
                          isSettingsOpen -> "Settings"
                          selectedTab == 0 -> "Home"
                          selectedTab == 1 -> "Transactions"
                          selectedTab == 2 -> "Calendar"
                          selectedTab == 3 -> "Budgets"
                          selectedTab == 4 -> "Recurring"
                          else -> "Categories"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                      )
                    },
                    navigationIcon = {
                      if (isSettingsOpen) {
                        IconButton(onClick = { isSettingsOpen = false }) {
                          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                      }
                    },
                    actions = {
                      if (!isSettingsOpen) {
                        IconButton(onClick = { isAiAdvisorOpen = true }, modifier = Modifier.testTag("rail_top_bar_ai_advisor")) {
                          Icon(Icons.Default.AutoAwesome, contentDescription = "Paisa AI Advisor", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showExportModal = true }, modifier = Modifier.testTag("landscape_top_bar_export")) {
                          Icon(Icons.Default.PictureAsPdf, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isSettingsOpen = true }, modifier = Modifier.testTag("landscape_top_bar_settings")) {
                          Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                      }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                  )
                },
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButton = {
                  if (!isSettingsOpen && selectedTab in listOf(0, 1, 2, 3, 4)) {
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
                      modifier = Modifier.padding(12.dp).testTag("global_add_transaction_fab")
                    ) {
                      Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(24.dp))
                    }
                  }
                }
              ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp), contentAlignment = Alignment.TopCenter) {
                  Box(modifier = Modifier.fillMaxSize().widthIn(max = 1100.dp)) {
                    MainScreenContent(
                      selectedTab = selectedTab,
                      isSettingsOpen = isSettingsOpen,
                      onNavigateTab = { tab ->
                        isSettingsOpen = false
                        selectedTab = tab
                      },
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
                      onOpenExportModal = { showExportModal = true }
                    )
                  }
                }
              }
            }
          }

          AppNavigationType.BOTTOM_BAR -> {
            Scaffold(
              topBar = {
                TopAppBar(
                  title = {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      AppBrandLogo(size = 28.dp)
                      Text(
                        text = when {
                          isSettingsOpen -> "Settings"
                          selectedTab == 0 -> "Paisa"
                          selectedTab == 1 -> "Transactions"
                          selectedTab == 2 -> "Calendar"
                          selectedTab == 3 -> "Budgets"
                          selectedTab == 4 -> "Recurring Dues"
                          else -> "Categories"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  },
                  navigationIcon = {
                    if (isSettingsOpen || selectedTab == 5) {
                      IconButton(
                        onClick = {
                          if (isSettingsOpen) {
                            isSettingsOpen = false
                          } else {
                            selectedTab = 0
                          }
                        }
                      ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                      }
                    }
                  },
                  actions = {
                    if (!isSettingsOpen) {
                      IconButton(
                        onClick = { isAiAdvisorOpen = true },
                        modifier = Modifier.testTag("top_bar_ai_advisor")
                      ) {
                        Icon(
                          imageVector = Icons.Default.AutoAwesome,
                          contentDescription = "Paisa AI Advisor",
                          tint = MaterialTheme.colorScheme.primary
                        )
                      }
                      IconButton(
                        onClick = { showProfileSheet = true },
                        modifier = Modifier.testTag("top_bar_profile")
                      ) {
                        if (!userProfile.photoUrl.isNullOrBlank()) {
                          AsyncImage(
                            model = userProfile.photoUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                          )
                        } else {
                          Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                          ) {
                            Box(contentAlignment = Alignment.Center) {
                              Text(
                                text = userProfile.name.trim().firstOrNull()?.uppercase() ?: "G",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                              )
                            }
                          }
                        }
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
                if (!isSettingsOpen) {
                  NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    tonalElevation = 8.dp
                  ) {
                    primaryDestinations.forEach { destination ->
                      val isSelected = selectedTab == destination.index
                      NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                          isSettingsOpen = false
                          selectedTab = destination.index
                        },
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
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        },
                        colors = NavigationBarItemDefaults.colors(
                          selectedIconColor = MaterialTheme.colorScheme.primary,
                          selectedTextColor = MaterialTheme.colorScheme.primary,
                          indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_${destination.label.lowercase()}")
                      )
                    }
                  }
                }
              },
              floatingActionButton = {
                if (!isSettingsOpen && selectedTab in listOf(0, 1, 2, 3, 4)) {
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
                      .padding(bottom = 8.dp)
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
              ) {
                MainScreenContent(
                  selectedTab = selectedTab,
                  isSettingsOpen = isSettingsOpen,
                  onNavigateTab = { tab ->
                    isSettingsOpen = false
                    selectedTab = tab
                  },
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
                  onOpenExportModal = { showExportModal = true }
                )
              }
            }
          }
        }
      }
    }

    if (isAiAdvisorOpen) {
      BackHandler { isAiAdvisorOpen = false }
      aiViewModel.bindFinanceViewModel(viewModel)
      AiAdvisorScreen(
        viewModel = aiViewModel,
        onClose = { isAiAdvisorOpen = false },
        financialSummaryContext = "User currency: ${userProfile.currencySymbol}, User: ${userProfile.name}"
      )
    }
  }
}



@Composable
private fun MainScreenContent(
  selectedTab: Int,
  isSettingsOpen: Boolean,
  onNavigateTab: (Int) -> Unit,
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
  AnimatedContent(
    targetState = isSettingsOpen,
    transitionSpec = { PaisaMotion.DefaultContentTransform },
    label = "settings_navigation_transition"
  ) { settingsOpen ->
    if (settingsOpen) {
      BackHandler { onBackFromSettings() }
      SettingsScreen(
        viewModel = viewModel,
        onOpenExportModal = onOpenExportModal
      )
    } else {
      AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { PaisaMotion.DefaultContentTransform },
        label = "paisa_primary_navigation"
      ) { tab ->
        when (tab) {
          0 -> DashboardScreen(
            viewModel = viewModel,
            onOpenAddTransaction = onOpenAddTransaction,
            onEditTransaction = onEditTransaction,
            onOpenSetBudget = {
              onOpenSetBudget(
                DateUtils.getMonthKey(dashboardCalendar),
                DateUtils.getMonthLabel(dashboardCalendar)
              )
            },
            onOpenAllTransactions = { onNavigateTab(1) }
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
          else -> DashboardScreen(
            viewModel = viewModel,
            onOpenAddTransaction = onOpenAddTransaction,
            onEditTransaction = onEditTransaction,
            onOpenSetBudget = {
              onOpenSetBudget(
                DateUtils.getMonthKey(dashboardCalendar),
                DateUtils.getMonthLabel(dashboardCalendar)
              )
            },
            onOpenAllTransactions = { onNavigateTab(1) }
          )
        }
      }
    }
  }
}
