package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.BudgetModel
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetSheet(
  sheetState: SheetState,
  monthKey: String,
  monthLabel: String,
  currentBudget: BudgetModel?,
  currencySymbol: String = "₹",
  onDismiss: () -> Unit,
  onSave: (monthKey: String, totalBudget: Double, categoryBudgets: Map<TransactionCategory, Double>) -> Unit
) {
  var totalBudgetText by remember {
    mutableStateOf(
      if (currentBudget != null && currentBudget.totalBudget > 0) {
        if (currentBudget.totalBudget % 1.0 == 0.0) currentBudget.totalBudget.toLong().toString()
        else currentBudget.totalBudget.toString()
      } else ""
    )
  }

  val categoryBudgetsState = remember {
    mutableStateMapOf<TransactionCategory, String>().apply {
      currentBudget?.categoryBudgets?.forEach { (cat, amount) ->
        val str = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
        put(cat, str)
      }
    }
  }

  var showAddCategoryMenu by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val expenseCategories = remember {
    TransactionCategory.values().filter { it.defaultType == TransactionType.EXPENSE }
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
        Column {
          Text(
            text = "Monthly Budget",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = monthLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Total Monthly Spending Limit
      Text(
        text = "Total Monthly Spending Limit",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = totalBudgetText,
        onValueChange = {
          if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            totalBudgetText = it
            errorMessage = null
          }
        },
        placeholder = { Text("e.g., 50000") },
        leadingIcon = {
          Text(
            text = currencySymbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
          )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("total_budget_input")
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Category Specific Budgets Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Category Limits (Optional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Track limits on specific categories",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box {
          OutlinedButton(
            onClick = { showAddCategoryMenu = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_category_budget_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add")
          }

          DropdownMenu(
            expanded = showAddCategoryMenu,
            onDismissRequest = { showAddCategoryMenu = false }
          ) {
            expenseCategories
              .filter { !categoryBudgetsState.containsKey(it) }
              .forEach { cat ->
                DropdownMenuItem(
                  text = {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(18.dp))
                      Text(cat.displayName)
                    }
                  },
                  onClick = {
                    categoryBudgetsState[cat] = ""
                    showAddCategoryMenu = false
                  }
                )
              }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (categoryBudgetsState.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No category limits configured. Tap '+ Add' above to set limits for Food, Shopping, Transport, etc.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          categoryBudgetsState.keys.toList().forEach { category ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = category.color.copy(alpha = 0.15f),
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(20.dp))
                  }
                }

                Text(
                  text = category.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                  value = categoryBudgetsState[category] ?: "",
                  onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                      categoryBudgetsState[category] = it
                    }
                  },
                  placeholder = { Text("0") },
                  leadingIcon = { Text(currencySymbol, style = MaterialTheme.typography.bodyMedium) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.width(110.dp)
                )

                IconButton(
                  onClick = { categoryBudgetsState.remove(category) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = errorMessage ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = ExpenseRed,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          val totalAmt = totalBudgetText.toDoubleOrNull() ?: 0.0
          if (totalAmt <= 0.0 && categoryBudgetsState.isEmpty()) {
            errorMessage = "Please enter a monthly budget limit greater than 0"
            return@Button
          }

          val catMap = mutableMapOf<TransactionCategory, Double>()
          categoryBudgetsState.forEach { (cat, str) ->
            val num = str.toDoubleOrNull()
            if (num != null && num > 0.0) {
              catMap[cat] = num
            }
          }

          onSave(monthKey, totalAmt, catMap)
          onDismiss()
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_budget_button")
      ) {
        Text(
          text = "Save Budget",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
