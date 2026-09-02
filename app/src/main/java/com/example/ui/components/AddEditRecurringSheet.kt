package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.model.RecurrenceInterval
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringSheet(
  sheetState: SheetState,
  initialRule: RecurringRule? = null,
  currencySymbol: String = "₹",
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    amount: Double,
    type: TransactionType,
    category: TransactionCategory,
    interval: RecurrenceInterval,
    startDate: Long,
    endDate: Long?,
    paymentMethod: PaymentMethod,
    note: String
  ) -> Unit
) {
  var ruleType by remember {
    mutableStateOf(initialRule?.type ?: TransactionType.EXPENSE)
  }
  var amountText by remember {
    mutableStateOf(
      if (initialRule != null) {
        if (initialRule.amount % 1.0 == 0.0) initialRule.amount.toLong().toString()
        else initialRule.amount.toString()
      } else ""
    )
  }
  var titleText by remember { mutableStateOf(initialRule?.title ?: "") }
  var selectedCategory by remember {
    mutableStateOf(
      initialRule?.category ?: if (ruleType == TransactionType.INCOME) TransactionCategory.SALARY else TransactionCategory.SUBSCRIPTIONS
    )
  }
  var selectedInterval by remember {
    mutableStateOf(initialRule?.interval ?: RecurrenceInterval.MONTHLY)
  }
  var startDate by remember {
    mutableStateOf(initialRule?.startDate ?: System.currentTimeMillis())
  }
  var endDate by remember {
    mutableStateOf(initialRule?.endDate)
  }
  var noteText by remember { mutableStateOf(initialRule?.note ?: "") }
  var paymentMethod by remember {
    mutableStateOf(initialRule?.paymentMethod ?: PaymentMethod.UPI)
  }

  var showStartDatePicker by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val availableCategories = remember(ruleType) {
    TransactionCategory.values().filter { it.defaultType == ruleType }
  }

  if (selectedCategory.defaultType != ruleType) {
    selectedCategory = availableCategories.firstOrNull() ?: TransactionCategory.OTHER_EXPENSE
  }

  if (showStartDatePicker) {
    AppDatePickerDialog(
      initialDateMillis = startDate,
      onDateSelected = { startDate = it },
      onDismiss = { showStartDatePicker = false }
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (initialRule == null) "New Recurring Rule" else "Edit Recurring Rule",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Type Toggle (Expense / Income) with Sliding Pill Animation
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          .padding(4.dp)
      ) {
        val segmentWidth = maxWidth / 2
        val pillOffset by animateDpAsState(
          targetValue = if (ruleType == TransactionType.EXPENSE) 0.dp else segmentWidth,
          animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
          label = "pill_offset_recurring"
        )
        val pillColor by animateColorAsState(
          targetValue = if (ruleType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen,
          animationSpec = tween(durationMillis = 250),
          label = "pill_color_recurring"
        )

        // Sliding background pill
        Box(
          modifier = Modifier
            .offset(x = pillOffset)
            .width(segmentWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(pillColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(12.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) { ruleType = TransactionType.EXPENSE }
              .testTag("recurring_type_expense"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Recurring Expense",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = if (ruleType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(12.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) { ruleType = TransactionType.INCOME }
              .testTag("recurring_type_income"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Recurring Income",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = if (ruleType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Recurrence Interval Selector (Daily, Weekly, Monthly, Yearly)
      Text(
        text = "Recurrence Interval",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RecurrenceInterval.values().forEach { interval ->
          val isSelected = selectedInterval == interval
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .weight(1f)
              .clickable { selectedInterval = interval }
              .testTag("interval_${interval.name}")
          ) {
            Box(
              modifier = Modifier.padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = interval.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Amount Field
      Text(
        text = "Amount per occurrence",
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
        placeholder = { Text("0.00") },
        leadingIcon = {
          Text(
            text = currencySymbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (ruleType == TransactionType.INCOME) IncomeGreen else ExpenseRed,
            modifier = Modifier.padding(start = 12.dp)
          )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("recurring_amount_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Title
      Text(
        text = "Rule Title",
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
        placeholder = { Text(if (ruleType == TransactionType.INCOME) "e.g., Monthly Salary, Rental Dividend" else "e.g., Netflix Subscription, Apartment Rent") },
        leadingIcon = {
          Icon(Icons.Default.Title, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("recurring_title_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Category Selector
      Text(
        text = "Category",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        availableCategories.forEach { category ->
          val isCatSelected = selectedCategory == category
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isCatSelected) category.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.clickable { selectedCategory = category }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(category.icon, contentDescription = null, tint = if (isCatSelected) Color.White else category.color, modifier = Modifier.size(18.dp))
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

      Spacer(modifier = Modifier.height(16.dp))

      // Start Date
      Text(
        text = "Start Date",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showStartDatePicker = true }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          Text(
            text = DateUtils.getDisplayDate(startDate),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Notes
      Text(
        text = "Notes (Optional)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        placeholder = { Text("Add any auto-generation note") },
        leadingIcon = {
          Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        maxLines = 2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = ExpenseRed,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()
          if (amt == null || amt <= 0.0) {
            errorMessage = "Please enter a valid amount"
            return@Button
          }
          if (titleText.trim().isEmpty()) {
            errorMessage = "Please enter a rule title"
            return@Button
          }

          onSave(
            titleText.trim(),
            amt,
            ruleType,
            selectedCategory,
            selectedInterval,
            startDate,
            endDate,
            paymentMethod,
            noteText.trim()
          )
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (ruleType == TransactionType.INCOME) IncomeGreen else ExpenseRed,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_recurring_rule_button")
      ) {
        Text(
          text = if (initialRule == null) "Create Recurring Rule" else "Update Recurring Rule",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
