package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Note
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecureNote
import com.example.data.model.SecureNoteType
import com.example.ui.theme.MinimalEmerald
import com.example.ui.theme.MinimalIndigo
import com.example.ui.theme.MinimalRose
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureVaultScreen(
  viewModel: FinanceViewModel,
  onBack: () -> Unit
) {
  val notes by viewModel.allSecureNotes.collectAsState()
  var editingNote by remember { mutableStateOf<SecureNote?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Private Vault",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "AES-256 Hardware Encrypted",
              style = MaterialTheme.typography.labelSmall,
              color = MinimalEmerald
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { isAddingNew = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add Note")
      }
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { paddingValues ->
    if (notes.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(MinimalIndigo.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Security,
              contentDescription = null,
              tint = MinimalIndigo,
              modifier = Modifier.size(36.dp)
            )
          }
          Spacer(modifier = Modifier.height(20.dp))
          Text(
            text = "Your Vault is Empty",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Store encrypted bank details, account credentials, and private financial notes securely.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { isAddingNew = true },
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create First Note")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(4.dp))
        }

        items(notes, key = { it.id }) { note ->
          SecureNoteCard(
            note = note,
            onEdit = { editingNote = note },
            onDelete = { viewModel.deleteSecureNote(note.id) },
            onCopy = { text, label ->
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText(label, text)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
            }
          )
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }

    if (isAddingNew || editingNote != null) {
      AddEditSecureNoteSheet(
        existingNote = editingNote,
        onDismiss = {
          isAddingNew = false
          editingNote = null
        },
        onSave = { note ->
          viewModel.saveSecureNote(note) {
            isAddingNew = false
            editingNote = null
          }
        }
      )
    }
  }
}

@Composable
fun SecureNoteCard(
  note: SecureNote,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onCopy: (String, String) -> Unit
) {
  var isRevealed by remember { mutableStateOf(false) }

  val icon = when (note.type) {
    SecureNoteType.CREDIT_DEBIT_CARD -> Icons.Rounded.CreditCard
    SecureNoteType.BANK_ACCOUNT -> Icons.Rounded.AccountBalance
    SecureNoteType.CREDENTIAL -> Icons.Rounded.Key
    SecureNoteType.GENERIC -> Icons.Rounded.Note
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          }
          Column {
            Text(
              text = note.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = note.type.displayName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row {
          IconButton(onClick = { isRevealed = !isRevealed }, modifier = Modifier.size(36.dp)) {
            Icon(
              imageVector = if (isRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
              contentDescription = if (isRevealed) "Hide" else "Show",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
          IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MinimalRose, modifier = Modifier.size(18.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Content or Structured details
      if (note.accountNumber != null) {
        DetailRow(
          label = "Account Number",
          value = note.accountNumber,
          isSecret = !isRevealed,
          onCopy = { onCopy(note.accountNumber, "Account Number") }
        )
      }

      if (note.ifscCode != null) {
        DetailRow(
          label = "IFSC Code",
          value = note.ifscCode,
          isSecret = false,
          onCopy = { onCopy(note.ifscCode, "IFSC Code") }
        )
      }

      if (note.maskedNumber != null) {
        DetailRow(
          label = "Card Number",
          value = note.maskedNumber,
          isSecret = !isRevealed,
          onCopy = { onCopy(note.maskedNumber, "Card Number") }
        )
      }

      if (note.content.isNotBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
        ) {
          Text(
            text = if (isRevealed) note.content else "••••••••••••••••••••",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
fun DetailRow(
  label: String,
  value: String,
  isSecret: Boolean,
  onCopy: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        text = if (isSecret) "•••• •••• •••• ${value.takeLast(4).ifBlank { "••••" }}" else value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
      Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSecureNoteSheet(
  existingNote: SecureNote?,
  onDismiss: () -> Unit,
  onSave: (SecureNote) -> Unit
) {
  var title by remember { mutableStateOf(existingNote?.title ?: "") }
  var type by remember { mutableStateOf(existingNote?.type ?: SecureNoteType.GENERIC) }
  var content by remember { mutableStateOf(existingNote?.content ?: "") }
  var accountNumber by remember { mutableStateOf(existingNote?.accountNumber ?: "") }
  var ifscCode by remember { mutableStateOf(existingNote?.ifscCode ?: "") }
  var cardNumber by remember { mutableStateOf(existingNote?.maskedNumber ?: "") }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
    ) {
      Text(
        text = if (existingNote != null) "Edit Secure Note" else "New Secure Note",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Type Selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SecureNoteType.values().forEach { t ->
          val selected = t == type
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .clickable { type = t }
          ) {
            Text(
              text = when (t) {
                SecureNoteType.GENERIC -> "Note"
                SecureNoteType.CREDIT_DEBIT_CARD -> "Card"
                SecureNoteType.BANK_ACCOUNT -> "Bank"
                SecureNoteType.CREDENTIAL -> "PIN"
              },
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
              ),
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title (e.g. HDFC Salary Account)") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (type == SecureNoteType.BANK_ACCOUNT) {
        OutlinedTextField(
          value = accountNumber,
          onValueChange = { accountNumber = it },
          label = { Text("Account Number") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = ifscCode,
          onValueChange = { ifscCode = it.uppercase() },
          label = { Text("IFSC Code") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (type == SecureNoteType.CREDIT_DEBIT_CARD) {
        OutlinedTextField(
          value = cardNumber,
          onValueChange = { cardNumber = it },
          label = { Text("Card Number / Expiry") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        label = { Text("Encrypted Notes / Credentials") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          if (title.isNotBlank()) {
            val note = SecureNote(
              id = existingNote?.id ?: 0,
              title = title.trim(),
              type = type,
              content = content.trim(),
              accountNumber = accountNumber.trim().takeIf { it.isNotBlank() },
              ifscCode = ifscCode.trim().takeIf { it.isNotBlank() },
              maskedNumber = cardNumber.trim().takeIf { it.isNotBlank() }
            )
            onSave(note)
          }
        },
        enabled = title.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text("Save to Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      }
    }
  }
}
