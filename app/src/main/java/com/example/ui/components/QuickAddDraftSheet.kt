package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddDraftSheet(
  isOpen: Boolean,
  editingItem: TransactionItem?,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    timestamp: Long,
    note: String,
    paymentMethod: PaymentMethod,
    isRecurring: Boolean
  ) -> Unit
) {
  if (!isOpen) return

  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedType by remember(editingItem) {
    mutableStateOf(editingItem?.type ?: TransactionType.EXPENSE)
  }
  var amountText by remember(editingItem) {
    mutableStateOf(if (editingItem != null) String.format(Locale.US, "%.2f", editingItem.amount) else "")
  }
  var titleText by remember(editingItem) {
    mutableStateOf(editingItem?.title ?: "")
  }
  var selectedCategory by remember(editingItem, selectedType) {
    mutableStateOf(
      editingItem?.category ?: when (selectedType) {
        TransactionType.EXPENSE -> TransactionCategory.FOOD_DINING
        TransactionType.INCOME -> TransactionCategory.FREELANCE
        TransactionType.SALARY -> TransactionCategory.SALARY
      }
    )
  }
  var selectedPaymentMethod by remember(editingItem) {
    mutableStateOf(editingItem?.paymentMethod ?: PaymentMethod.UPI)
  }
  var selectedTimestamp by remember(editingItem) {
    mutableStateOf(editingItem?.timestamp ?: System.currentTimeMillis())
  }
  var noteText by remember(editingItem) {
    mutableStateOf(editingItem?.note ?: "")
  }
  var isRecurring by remember(editingItem) {
    mutableStateOf(editingItem?.isRecurring ?: false)
  }
  var showDatePicker by remember { mutableStateOf(false) }

  if (showDatePicker) {
    val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    val datePickerDialog = DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val newCal = Calendar.getInstance().apply {
          set(Calendar.YEAR, year)
          set(Calendar.MONTH, month)
          set(Calendar.DAY_OF_MONTH, dayOfMonth)
          val nowCal = Calendar.getInstance()
          set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
          set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE))
          set(Calendar.SECOND, 0)
        }
        selectedTimestamp = newCal.timeInMillis
        showDatePicker = false
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.setOnDismissListener {
      showDatePicker = false
    }
    DisposableEffect(showDatePicker) {
      datePickerDialog.show()
      onDispose {
        datePickerDialog.dismiss()
      }
    }
  }

  // Filter categories based on transaction type
  val availableCategories = remember(selectedType) {
    when (selectedType) {
      TransactionType.EXPENSE -> TransactionCategory.entries.filter { it.defaultType == TransactionType.EXPENSE }
      TransactionType.INCOME -> TransactionCategory.entries.filter { it.defaultType == TransactionType.INCOME }
      TransactionType.SALARY -> listOf(TransactionCategory.SALARY, TransactionCategory.BONUS, TransactionCategory.BUSINESS)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .size(width = 38.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (editingItem != null) "Edit Draft Entry" else "New Finance Draft",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Type Selector Tabs (Expense / Income / Salary)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          TransactionType.entries.forEach { type ->
            val isSelected = selectedType == type
            val typeColor = when (type) {
              TransactionType.EXPENSE -> ExpenseRed
              TransactionType.INCOME -> IncomeGreen
              TransactionType.SALARY -> AccentIndigo
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) typeColor else Color.Transparent,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  selectedType = type
                  // Auto-switch category default if not matching
                  if (type == TransactionType.SALARY) {
                    selectedCategory = TransactionCategory.SALARY
                  } else if (selectedCategory.defaultType != type && type != TransactionType.SALARY) {
                    selectedCategory = if (type == TransactionType.EXPENSE) TransactionCategory.FOOD_DINING else TransactionCategory.FREELANCE
                  }
                }
            ) {
              Box(
                modifier = Modifier.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = type.displayName,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // Large Amount Input
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Amount",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            // Allow only numbers and max 1 dot
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
              amountText = input
            }
          },
          placeholder = {
            Text(
              "0.00",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
              )
            )
          },
          leadingIcon = {
            Text(
              text = currencySymbol,
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = when (selectedType) {
                  TransactionType.EXPENSE -> ExpenseRed
                  TransactionType.INCOME -> IncomeGreen
                  TransactionType.SALARY -> AccentIndigo
                }
              ),
              modifier = Modifier.padding(start = 8.dp)
            )
          },
          textStyle = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          ),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
          ),
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier.fillMaxWidth()
        )

        // Quick bump chips (+50, +100, +500, +1000)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val bumps = listOf(50, 100, 500, 1000)
          bumps.forEach { bump ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  val current = amountText.toDoubleOrNull() ?: 0.0
                  amountText = String.format(Locale.US, "%.2f", current + bump)
                }
            ) {
              Text(
                text = "+$bump",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp)
              )
            }
          }
        }
      }

      // Title / Description
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Title / Merchant",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = titleText,
          onValueChange = { titleText = it },
          placeholder = { Text("e.g. Swiggy dinner, Chai & snack, Rent, Metro") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Category Grid
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Category",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          availableCategories.forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) category.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = androidx.compose.foundation.BorderStroke(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) category.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { selectedCategory = category }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = category.icon,
                  contentDescription = category.displayName,
                  tint = if (isSelected) category.color else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = category.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (isSelected) category.color else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      // Date Selection (Calendar & Quick Chips)
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Draft Date",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Pick past/future dates",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }

        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val isToday = currentCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) && currentCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
        val isYesterday = currentCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) && currentCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChipItem(
            label = "Today",
            isSelected = isToday,
            onClick = { selectedTimestamp = System.currentTimeMillis() },
            modifier = Modifier.weight(1f)
          )
          FilterChipItem(
            label = "Yesterday",
            isSelected = isYesterday,
            onClick = {
              val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
              selectedTimestamp = cal.timeInMillis
            },
            modifier = Modifier.weight(1f)
          )
          FilterChipItem(
            label = "Calendar 📅",
            isSelected = !isToday && !isYesterday,
            icon = Icons.Default.CalendarToday,
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1.2f)
          )
        }

        // Selected Date Card / Calendar Banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDatePicker = true }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = "Pick Date",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(selectedTimestamp)),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                val diffDays = ((todayCal.timeInMillis - selectedTimestamp) / (1000 * 60 * 60 * 24)).toInt()
                val relativeDesc = when {
                  isToday -> "Today's draft"
                  isYesterday -> "Yesterday's draft"
                  diffDays > 0 -> "$diffDays days ago • Past draft"
                  else -> "Future scheduled draft"
                }
                Text(
                  text = relativeDesc,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              modifier = Modifier.clickable { showDatePicker = true }
            ) {
              Text(
                text = "Change",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }
        }
      }

      // Payment Method Chips
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Payment Method",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PaymentMethod.entries.forEach { method ->
            val isSelected = selectedPaymentMethod == method
            FilterChipItem(
              label = method.displayName,
              isSelected = isSelected,
              onClick = { selectedPaymentMethod = method }
            )
          }
        }
      }

      // Notes / Tags
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Notes / Tag (Optional)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          placeholder = { Text("Add memo, receipt details, or tags...") },
          minLines = 2,
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Recurring Toggle Switch
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Autorenew,
              contentDescription = "Recurring",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Recurring Monthly Draft",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Track automatically as monthly recurring stream",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isRecurring,
            onCheckedChange = { isRecurring = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = MaterialTheme.colorScheme.primary
            )
          )
        }
      }

      // Submit Button
      val canSave = (amountText.toDoubleOrNull() ?: 0.0) > 0.0

      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull() ?: 0.0
          if (amt > 0.0) {
            onSave(
              titleText,
              amt,
              selectedType,
              selectedCategory,
              selectedTimestamp,
              noteText,
              selectedPaymentMethod,
              isRecurring
            )
          }
        },
        enabled = canSave,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
      ) {
        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (editingItem != null) "Update Draft" else "Save Daily Draft",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
