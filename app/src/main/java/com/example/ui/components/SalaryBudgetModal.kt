package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
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
import com.example.data.model.MonthlySalarySettings
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalaryBudgetModal(
  isOpen: Boolean,
  currentSettings: MonthlySalarySettings,
  onDismiss: () -> Unit,
  onSave: (salary: Double, payDay: Int, budget: Double, symbol: String) -> Unit,
  onPostSalaryDraftNow: () -> Unit
) {
  if (!isOpen) return

  var salaryText by remember(currentSettings) {
    mutableStateOf(String.format(Locale.US, "%.0f", currentSettings.salaryAmount))
  }
  var budgetText by remember(currentSettings) {
    mutableStateOf(String.format(Locale.US, "%.0f", currentSettings.monthlyBudgetGoal))
  }
  var selectedPayDay by remember(currentSettings) {
    mutableStateOf(currentSettings.payDayOfMonth)
  }
  var selectedSymbol by remember(currentSettings) {
    mutableStateOf(currentSettings.currencySymbol)
  }

  val currencySymbols = listOf("₹", "$", "€", "£", "¥", "₱", "AED", "CAD")

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(22.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Salary & Budget Setup",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(28.dp)
        ) {
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Set your regular monthly salary and spending limit to calculate safe daily burn rate and analytics.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Currency Symbol Picker
        Column {
          Text(
            text = "Currency Symbol",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            currencySymbols.forEach { sym ->
              val isSel = selectedSymbol == sym
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedSymbol = sym }
              ) {
                Text(
                  text = sym,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        // Monthly Salary Input
        Column {
          Text(
            text = "Monthly Base Salary ($selectedSymbol)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = salaryText,
            onValueChange = { salaryText = it },
            placeholder = { Text("e.g. 65000") },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = IncomeGreen)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

        // Monthly Budget Goal Input
        Column {
          Text(
            text = "Monthly Spending Budget Cap ($selectedSymbol)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = budgetText,
            onValueChange = { budgetText = it },
            placeholder = { Text("e.g. 35000") },
            leadingIcon = {
              Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentIndigo)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

        // Pay Day of Month (1st, 5th, 15th, 25th, 28th)
        Column {
          Text(
            text = "Salary Pay Day (Monthly)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(1, 5, 15, 25, 28).forEach { day ->
              val isSel = selectedPayDay == day
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSel) AccentIndigo else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedPayDay = day }
              ) {
                Text(
                  text = "${day}${when(day){ 1->"st"; 2->"nd"; 3->"rd"; else->"th"}}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                  ),
                  color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(vertical = 8.dp),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          }
        }

        // Quick Post Salary Draft Button
        OutlinedButton(
          onClick = {
            onPostSalaryDraftNow()
            onDismiss()
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = IncomeGreen),
          border = androidx.compose.foundation.BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Post Monthly Salary Draft Now",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val sal = salaryText.toDoubleOrNull() ?: currentSettings.salaryAmount
          val bud = budgetText.toDoubleOrNull() ?: currentSettings.monthlyBudgetGoal
          onSave(sal, selectedPayDay, bud, selectedSymbol)
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Save & Apply Settings", fontWeight = FontWeight.Bold)
      }
    }
  )
}
