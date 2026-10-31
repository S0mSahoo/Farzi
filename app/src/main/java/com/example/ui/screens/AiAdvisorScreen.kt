package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.util.PaisaMotion
import com.example.ui.util.LocalAdaptiveInfo
import com.example.ui.viewmodel.ActionExecutionStatus
import com.example.ui.viewmodel.AiAdvisorViewModel
import com.example.ui.viewmodel.AiUiState
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.Source
import com.example.ui.viewmodel.PendingActionGroup
import com.example.util.parseMarkdownToAnnotatedString
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen(
    viewModel: AiAdvisorViewModel,
    onClose: () -> Unit,
    financialSummaryContext: String = ""
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingActionGroup by viewModel.pendingActionGroup.collectAsStateWithLifecycle()
    val isProcessingAction by viewModel.isProcessingAction.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val useTwoPane = LocalAdaptiveInfo.current.useTwoPaneLayout

    val streamingLength = (uiState as? AiUiState.Streaming)?.text?.length ?: 0

    // Auto-scroll to latest line continuously during message updates and streaming generation
    LaunchedEffect(messages.size, uiState, streamingLength, pendingActionGroup) {
        val totalCount = messages.size +
                (if (uiState is AiUiState.Loading || uiState is AiUiState.Streaming || uiState is AiUiState.Error) 1 else 0) +
                (if (pendingActionGroup != null) 1 else 0)
        if (totalCount > 0) {
            listState.scrollToItem(totalCount - 1)
        }
    }

    val isControlsDisabled = pendingActionGroup != null || isProcessingAction || uiState is AiUiState.Loading || uiState is AiUiState.Streaming

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Paisa AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Agentic Financial Assistant", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("ai_advisor_close")) {
                        Icon(Icons.Default.Close, contentDescription = "Close Paisa AI")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(if (useTwoPane) 2.1f else 1f)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                if (messages.isEmpty()) {
                    item {
                        ChatBubble(
                            message = ChatMessage(
                                role = "model",
                                text = "Hello! I'm Paisa AI, your agentic financial assistant. 🌟\n\nI can analyze your spending, compare periods, or help you log transactions and set budgets. How can I help you manage your finances today?"
                            )
                        )
                    }
                }

                itemsIndexed(messages, key = { index, msg -> "${msg.timestamp}_$index" }) { _, message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(PaisaMotion.EnterFadeDurationMillis)) + slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        ChatBubble(message = message)
                    }
                }

                pendingActionGroup?.let { group ->
                    item {
                        PendingActionPreviewCard(
                            group = group,
                            isProcessing = isProcessingAction,
                            onConfirm = { viewModel.confirmAndExecutePendingAction() },
                            onCancel = { viewModel.cancelPendingAction() }
                        )
                    }
                }

                if (uiState is AiUiState.Loading || uiState is AiUiState.Streaming) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("ai_typing_indicator")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ThinkingDotsAnimation()
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (uiState is AiUiState.Streaming) "Paisa AI is typing..." else "Paisa AI is thinking...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (uiState is AiUiState.Streaming) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = parseMarkdownToAnnotatedString((uiState as AiUiState.Streaming).text),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (uiState is AiUiState.Error) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_error_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = (uiState as AiUiState.Error).message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.retryLastMessage() },
                                        modifier = Modifier.testTag("ai_retry_button")
                                    ) {
                                        Text("Retry", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
            if (useTwoPane) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Try a question",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Suggestions update from your recorded transactions, budgets and recurring rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { viewModel.sendMessage(suggestion.prompt) },
                            enabled = !isControlsDisabled,
                            label = { Text(suggestion.label) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_suggestion_${suggestion.id}")
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "Paisa AI uses read-only financial tools for analysis. Any proposed transaction or budget change must be reviewed and explicitly confirmed by you.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Compact suggestions stay near the composer on phones; tablet/desktop uses the side panel.
                if (!useTwoPane) {
                                    // Suggestions are derived from the current local financial context.
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .padding(bottom = 6.dp)
                                                .testTag("ai_suggestion_chips")
                                        ) {
                                            items(suggestions, key = { it.id }) { suggestion ->
                                                AssistChip(
                                                    onClick = { viewModel.sendMessage(suggestion.prompt) },
                                                    enabled = !isControlsDisabled,
                                                    label = { Text(suggestion.label) }
                                                )
                                            }
                                        }
                }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Paisa AI to log, set budget, or advise...") },
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isControlsDisabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input"),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !isControlsDisabled,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (!isControlsDisabled && inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("ai_chat_send")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (!isControlsDisabled && inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingActionPreviewCard(
    group: PendingActionGroup,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val isDestructive = group.isDestructive || group.requiresExplicitDeleteAllConfirmation
    val hasFailedItems = group.actionItems.any { it.status == ActionExecutionStatus.FAILED }
    val hasCompletedItems = group.actionItems.any { it.status == ActionExecutionStatus.SUCCESS }

    val cardBg = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val border = BorderStroke(1.dp, if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_pending_action_preview")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDestructive) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (group.requiresExplicitDeleteAllConfirmation) "⚠️ Dedicated Delete All Confirmation"
                    else if (hasFailedItems) "⚠️ Action Execution Incomplete"
                    else if (isDestructive) "⚠️ Confirm Destructive Action"
                    else "📋 Review Proposed Financial Action",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive || hasFailedItems) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (group.requiresExplicitDeleteAllConfirmation) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "⚠️ EXPLICIT CONFIRMATION REQUIRED: This action will permanently delete all records matching the scope. This operation cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            group.actionItems.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (item.status) {
                        ActionExecutionStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ActionExecutionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        ActionExecutionStatus.PENDING -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("action_status_${item.status.name.lowercase()}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (item.status) {
                                    ActionExecutionStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                                    ActionExecutionStatus.FAILED -> MaterialTheme.colorScheme.error
                                    ActionExecutionStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = when (item.status) {
                                        ActionExecutionStatus.SUCCESS -> "✓ Completed"
                                        ActionExecutionStatus.FAILED -> "✗ Failed"
                                        ActionExecutionStatus.PENDING -> "⏳ Pending"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.status) {
                                        ActionExecutionStatus.SUCCESS -> MaterialTheme.colorScheme.onPrimary
                                        ActionExecutionStatus.FAILED -> MaterialTheme.colorScheme.onError
                                        ActionExecutionStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.detailSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.status == ActionExecutionStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "❌ Failure Reason: ${item.errorMessage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (group.validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_validation_error_card")
                ) {
                    Text(
                        text = "❌ Validation Error: ${group.validationError}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (hasFailedItems && hasCompletedItems) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ℹ️ Retrying will skip completed actions and re-run only failed ones.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isProcessing,
                    modifier = Modifier.testTag("ai_cancel_action")
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onConfirm,
                    enabled = group.validationError == null && !isProcessing,
                    colors = if (isDestructive && !hasFailedItems) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                    modifier = Modifier.testTag("ai_confirm_action")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Executing...")
                    } else {
                        val buttonText = if (hasFailedItems) {
                            "Retry Failed Action(s)"
                        } else if (isDestructive) {
                            "Confirm Deletion"
                        } else {
                            "Confirm & Execute"
                        }
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.82f else 0.88f)
                .testTag(if (isUser) "chat_bubble_user" else "chat_bubble_model")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = parseMarkdownToAnnotatedString(message.text),
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (message.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = if (isUser) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "Sources:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.sources.distinctBy { it.url }.forEach { source ->
                            SourceLink(source = source, isUserBubble = isUser)
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun SourceLink(source: Source, isUserBubble: Boolean) {
    val context = LocalContext.current
    Surface(
        onClick = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                context.startActivity(intent)
            } catch (e: Exception) {
                // Ignore malformed URLs
            }
        },
        shape = RoundedCornerShape(8.dp),
        color = if (isUserBubble) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, if (isUserBubble) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isUserBubble) Color.White else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = source.title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isUserBubble) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ThinkingDotsAnimation() {
    val transition = rememberInfiniteTransition(label = "dots_transition")
    val dot1Scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).graphicsLayer { scaleX = dot1Scale; scaleY = dot1Scale }.background(MaterialTheme.colorScheme.primary, CircleShape))
        Box(modifier = Modifier.size(7.dp).graphicsLayer { scaleX = dot2Scale; scaleY = dot2Scale }.background(MaterialTheme.colorScheme.primary, CircleShape))
        Box(modifier = Modifier.size(7.dp).graphicsLayer { scaleX = dot3Scale; scaleY = dot3Scale }.background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}
