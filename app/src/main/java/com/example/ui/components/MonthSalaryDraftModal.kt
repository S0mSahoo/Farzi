package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthSalaryDraftModal(
  isOpen: Boolean,
  selectedYear: Int,
  selectedMonth: Int,
  defaultSalaryAmount: Double,
  defaultPayDay: Int,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onDraftSalary: (year: Int, month: Int, amount: Double, payDay: Int, method: PaymentMethod, note: String) -> Unit
) {
  if (!isOpen) return

  var year by remember(selectedYear) { mutableIntStateOf(selectedYear) }
  var month by remember(selectedMonth) { mutableIntStateOf(selectedMonth) }
  var amountText by remember(defaultSalaryAmount) {
    mutableStateOf(String.format(Locale.US, "%.0f", defaultSalaryAmount))
  }
  var payDay by remember(defaultPayDay) { mutableIntStateOf(defaultPayDay) }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
  var noteText by remember { mutableStateOf("") }

  val targetCal = Calendar.getInstance().apply {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month)
  }
  val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(targetCal.time)

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, IncomeGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Work,
              contentDescription = null,
              tint = IncomeGreen,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Draft Month Salary",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Log custom salary for $monthLabel",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Month Selector Bar inside Modal
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Target Month:",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    val cal = Calendar.getInstance().apply {
                      set(Calendar.YEAR, year)
                      set(Calendar.MONTH, month)
                      add(Calendar.MONTH, -1)
                    }
                    year = cal.get(Calendar.YEAR)
                    month = cal.get(Calendar.MONTH)
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.Transparent
              ) {
                Text("‹ Prev", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = monthLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    val cal = Calendar.getInstance().apply {
                      set(Calendar.YEAR, year)
                      set(Calendar.MONTH, month)
                      add(Calendar.MONTH, 1)
                    }
                    year = cal.get(Calendar.YEAR)
                    month = cal.get(Calendar.MONTH)
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.Transparent
              ) {
                Text("Next ›", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
              }
            }
          }
        }

        // Salary Amount Field
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Salary Amount for this Month",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          OutlinedTextField(
            value = amountText,
            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amountText = it },
            prefix = {
              Text(
                text = "$currencySymbol ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = IncomeGreen
              )
            },
            placeholder = { Text("e.g. 65000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = IncomeGreen,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )
        }

        // Quick Amount Presets
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(50000.0, 65000.0, 75000.0, 100000.0).forEach { preset ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { amountText = String.format(Locale.US, "%.0f", preset) }
                .padding(vertical = 6.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
              Text(
                text = "$currencySymbol${preset.toInt() / 1000}k",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }

        // Pay Day selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Pay Day of the Month",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(1, 5, 15, 25, 28, 30).forEach { day ->
              val isSelected = payDay == day
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { payDay = day }
                  .padding(vertical = 8.dp)
              ) {
                Text(
                  text = "${day}th",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          }
        }

        // Payment Method
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Receiving Account / Method",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              PaymentMethod.BANK_TRANSFER to "Bank / NEFT",
              PaymentMethod.UPI to "UPI",
              PaymentMethod.CASH to "Cash"
            ).forEach { (method, label) ->
              val isSelected = paymentMethod == method
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { paymentMethod = method }
                  .padding(vertical = 8.dp)
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          }
        }

        // Optional Note
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Note / Memo (Optional)") },
          placeholder = { Text("e.g. Regular monthly base + quarterly bonus") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull() ?: defaultSalaryAmount
          onDraftSalary(year, month, amt, payDay, paymentMethod, noteText)
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Save & Draft $monthLabel Salary",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Cancel")
      }
    }
  )
}
