package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.model.SecureNoteItem
import com.example.data.model.SecureNoteType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureNotesScreen(
  viewModel: FinanceViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val secureNotes by viewModel.allSecureNotes.collectAsState()
  val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()

  var selectedNoteForDetail by remember { mutableStateOf<SecureNoteItem?>(null) }
  var isCreatingNewNote by remember { mutableStateOf(false) }
  var noteToDelete by remember { mutableStateOf<SecureNoteItem?>(null) }

  // Apply FLAG_SECURE protection to block screenshots, screen recording, and recent-app previews
  DisposableEffect(Unit) {
    val window = (context as? FragmentActivity)?.window ?: (context as? Activity)?.window
    window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    onDispose {
      // Do not clear FLAG_SECURE here so it remains active for the app
    }
  }

  fun authenticateUser(onSuccess: () -> Unit) {
    val activity = context as? FragmentActivity
    if (activity == null) {
      viewModel.unlockVault()
      onSuccess()
      return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        viewModel.unlockVault()
        onSuccess()
      }
      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Toast.makeText(context, "Authentication failed: $errString", Toast.LENGTH_SHORT).show()
      }
    })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Unlock Secure Vault")
      .setSubtitle("Confirm biometrics to view secure notes and files")
      .setNegativeButtonText("Cancel")
      .build()

    biometricPrompt.authenticate(promptInfo)
  }

  LaunchedEffect(Unit) {
    viewModel.lockVault() // Ensure it's locked whenever we enter this screen
    authenticateUser {}
  }

  // Full-page Detail / Editor Screen
  if (selectedNoteForDetail != null || isCreatingNewNote) {
    SecureNoteDetailScreen(
      initialNote = selectedNoteForDetail,
      onBack = {
        selectedNoteForDetail = null
        isCreatingNewNote = false
      },
      onSave = { note ->
        if (selectedNoteForDetail != null) {
          viewModel.updateSecureNote(note)
        } else {
          viewModel.addSecureNote(note)
        }
        selectedNoteForDetail = null
        isCreatingNewNote = false
      }
    )
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Secure Vault", fontWeight = FontWeight.Bold)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("secure_notes_back_button")) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (!isVaultUnlocked) {
            Button(
              onClick = { authenticateUser {} },
              modifier = Modifier.padding(end = 12.dp),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Unlock")
            }
          }
        }
      )
    },
    floatingActionButton = {
      if (isVaultUnlocked) {
        FloatingActionButton(
          onClick = {
            isCreatingNewNote = true
          },
          containerColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("add_secure_note_fab")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.White)
        }
      }
    }
  ) { padding ->
    if (!isVaultUnlocked) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Vault is Locked",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Authenticate with biometrics to access your private notes and uploaded files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
          Button(
            onClick = { authenticateUser {} },
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Unlock Vault")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (secureNotes.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 64.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  Icons.Default.Shield,
                  contentDescription = null,
                  modifier = Modifier.size(48.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text("Your Vault is Empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Tap + to add notes or upload private files.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
              }
            }
          }
        } else {
          items(secureNotes, key = { it.id }) { note ->
            GoogleKeepNoteCard(
              note = note,
              onClick = {
                selectedNoteForDetail = note
              },
              onDelete = {
                noteToDelete = note
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun GoogleKeepNoteCard(
  note: SecureNoteItem,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
  val dateStr = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (note.type == SecureNoteType.FILE) Icons.Default.AttachFile else Icons.Default.Note,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = note.title.ifBlank { "Untitled Note" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Delete Item",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
          )
        }
      }

      if (note.type == SecureNoteType.FILE && note.fileName.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Text(
              text = note.fileName,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      if (note.notes.isNotBlank()) {
        Text(
          text = note.notes,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = note.type.displayName,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = dateStr,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureNoteDetailScreen(
  initialNote: SecureNoteItem?,
  onBack: () -> Unit,
  onSave: (SecureNoteItem) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf(initialNote?.title ?: "") }
  var notes by remember { mutableStateOf(initialNote?.notes ?: "") }
  val type = SecureNoteType.NOTE

  // Save function to handle back action
  fun saveNote() {
    if (title.isNotBlank()) {
      onSave(
        SecureNoteItem(
          id = initialNote?.id ?: 0L,
          title = title,
          type = type,
          notes = notes,
          fileName = "",
          fileUri = "",
          updatedAt = System.currentTimeMillis()
        )
      )
    }
  }

  // Ensure FLAG_SECURE protection
  DisposableEffect(Unit) {
    val window = (context as? FragmentActivity)?.window ?: (context as? Activity)?.window
    window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    onDispose { /* Keep secure */ }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(text = if (initialNote == null) "New Note" else "Edit Note", fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
          IconButton(onClick = {
            saveNote()
            onBack()
          }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { saveNote() },
            enabled = title.isNotBlank()
          ) {
            Icon(Icons.Default.Save, contentDescription = "Save Note", tint = MaterialTheme.colorScheme.primary)
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      // Title
      androidx.compose.material3.TextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth()
      )

      // Body
      androidx.compose.material3.TextField(
        value = notes,
        onValueChange = { notes = it },
        placeholder = { Text("Note body", style = MaterialTheme.typography.bodyLarge) },
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
