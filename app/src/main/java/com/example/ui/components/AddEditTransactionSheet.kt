package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
  sheetState: SheetState,
  initialTransaction: TransactionItem? = null,
  prefilledTimestamp: Long? = null,
  prefilledType: TransactionType? = null,
  currencySymbol: String = "₹",
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
  var transactionType by remember {
    mutableStateOf(initialTransaction?.type ?: prefilledType ?: TransactionType.EXPENSE)
  }
  var amountText by remember {
    mutableStateOf(
      if (initialTransaction != null) {
        if (initialTransaction.amount % 1.0 == 0.0) initialTransaction.amount.toLong().toString()
        else initialTransaction.amount.toString()
      } else ""
    )
  }
  var titleText by remember { mutableStateOf(initialTransaction?.title ?: "") }
  var selectedCategory by remember {
    mutableStateOf(
      initialTransaction?.category ?: if (transactionType == TransactionType.INCOME) TransactionCategory.SALARY else TransactionCategory.FOOD_DINING
    )
  }
  var selectedTimestamp by remember {
    mutableStateOf(initialTransaction?.timestamp ?: prefilledTimestamp ?: System.currentTimeMillis())
  }
  var noteText by remember { mutableStateOf(initialTransaction?.note ?: "") }
  var paymentMethod by remember {
    mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.UPI)
  }
  var isRecurring by remember {
    mutableStateOf(initialTransaction?.isRecurring ?: false)
  }

  var showDatePicker by remember { mutableStateOf(false) }
  var showPaymentMethodDropdown by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val availableCategories = remember(transactionType) {
    TransactionCategory.values().filter { it.defaultType == transactionType }
  }

  // Update selectedCategory if type changes and category belongs to other type
  if (selectedCategory.defaultType != transactionType) {
    selectedCategory = availableCategories.firstOrNull() ?: TransactionCategory.OTHER_EXPENSE
  }

  val activeColor by animateColorAsState(
    targetValue = if (transactionType == TransactionType.INCOME) IncomeGreen else ExpenseRed,
    animationSpec = spring(stiffness = Spring.StiffnessMedium),
    label = "active_color"
  )

  if (showDatePicker) {
    AppDatePickerDialog(
      initialDateMillis = selectedTimestamp,
      onDateSelected = { selectedTimestamp = it },
      onDismiss = { showDatePicker = false }
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
      // Header with stable height and close icon
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (initialTransaction == null) "New Record" else "Edit Record",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Smooth Sliding Income / Expense Toggle Segment with Stable Height
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          .padding(4.dp)
      ) {
        val segmentWidth = (maxWidth) / 2
        val slideOffset by animateFloatAsState(
          targetValue = if (transactionType == TransactionType.EXPENSE) 0f else 1f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "pill_slide"
        )

        // Sliding Background Pill
        Box(
          modifier = Modifier
            .offset(x = segmentWidth * slideOffset)
            .width(segmentWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(activeColor)
        )

        // Clickable Labels Row
        Row(modifier = Modifier.fillMaxWidth()) {
          // Expense
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(10.dp))
              .clickable { transactionType = TransactionType.EXPENSE }
              .testTag("type_toggle_expense"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Expense",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (transactionType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Income
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(10.dp))
              .clickable { transactionType = TransactionType.INCOME }
              .testTag("type_toggle_income"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Income",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (transactionType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Amount Input Field
      Text(
        text = "Amount",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = amountText,
        onValueChange = {
          if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            amountText = it
            errorMessage = null
          }
        },
        placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
        leadingIcon = {
          Text(
            text = currencySymbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = activeColor,
            modifier = Modifier.padding(start = 12.dp)
          )
        },
        textStyle = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          color = activeColor
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = activeColor,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("transaction_amount_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Title Input Field
      Text(
        text = "Title / Description",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = titleText,
        onValueChange = {
          titleText = it
          errorMessage = null
        },
        placeholder = { Text("Enter description") },
        leadingIcon = {
          Icon(Icons.Default.Title, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("transaction_title_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Category Selector (Stable Height)
      Text(
        text = "Category",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        availableCategories.forEach { category ->
          val isCatSelected = selectedCategory == category
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCatSelected) category.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .height(38.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedCategory = category }
              .testTag("category_chip_${category.name}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isCatSelected) Color.White else category.color,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Date and Payment Method Row (Stable Height)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Date Selector
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Date",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(14.dp))
              .clickable { showDatePicker = true }
              .testTag("select_date_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Text(
                text = DateUtils.getDisplayDate(selectedTimestamp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        // Payment Method Dropdown
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Payment Method",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showPaymentMethodDropdown = true }
                .testTag("select_payment_method_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                  text = paymentMethod.name.replace("_", " "),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            DropdownMenu(
              expanded = showPaymentMethodDropdown,
              onDismissRequest = { showPaymentMethodDropdown = false }
            ) {
              PaymentMethod.values().forEach { method ->
                DropdownMenuItem(
                  text = { Text(method.displayName) },
                  onClick = {
                    paymentMethod = method
                    showPaymentMethodDropdown = false
                  }
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Note Field
      Text(
        text = "Notes (Optional)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        placeholder = { Text("Add extra details or tags") },
        leadingIcon = {
          Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        maxLines = 2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Recurring Checkbox
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { isRecurring = !isRecurring }
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Checkbox(
          checked = isRecurring,
          onCheckedChange = { isRecurring = it }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Mark as recurring transaction",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // Error message if any
      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = ExpenseRed,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Save Button
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()
          if (amt == null || amt <= 0.0) {
            errorMessage = "Please enter a valid amount greater than 0"
            return@Button
          }
          if (titleText.trim().isEmpty()) {
            errorMessage = "Please enter a title or description"
            return@Button
          }

          onSave(
            titleText.trim(),
            amt,
            transactionType,
            selectedCategory,
            selectedTimestamp,
            noteText.trim(),
            paymentMethod,
            isRecurring
          )
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = activeColor,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_transaction_button")
      ) {
        Text(
          text = if (initialTransaction == null) "Save Transaction" else "Update Transaction",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
