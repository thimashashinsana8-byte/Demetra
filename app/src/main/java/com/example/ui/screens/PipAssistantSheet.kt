package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PipChatMessage
import com.example.model.PipSuggestion
import com.example.model.UserRole
import com.example.ui.components.PipMascot
import com.example.ui.components.PipMood
import com.example.ui.theme.*
import com.example.viewmodel.PipUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipAssistantSheet(
    role: UserRole,
    currentScreen: String,
    messages: List<PipChatMessage>,
    suggestions: List<PipSuggestion>,
    uiState: PipUiState,
    askHint: String,
    reducedMotion: Boolean,
    onDismiss: () -> Unit,
    onSendQuery: (String) -> Unit,
    onDismissError: () -> Unit,
    onQuickActionNavigate: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val lastMsg = messages.lastOrNull()
    val streamScrollBucket = (lastMsg?.text?.length ?: 0) / 40
    LaunchedEffect(lastMsg?.isStreaming, streamScrollBucket) {
        if (lastMsg?.isStreaming == true && messages.isNotEmpty() && !listState.isScrollInProgress) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WarmCream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = CreamCardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            // Pip Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SageTint),
                        contentAlignment = Alignment.Center
                    ) {
                        PipMascot(
                            size = 44.dp,
                            mood = if (uiState is PipUiState.Loading) PipMood.THINKING else PipMood.HAPPY,
                            reducedMotion = reducedMotion
                        )
                    }
                    Column {
                        Text(
                            text = "Pip • Demetra AI Guide",
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkText
                        )
                        Text(
                            text = "${role.title} • $currentScreen",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).testTag("close_pip_button")
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close Pip Assistant", tint = DarkText)
                }
            }

            // Contextual quick suggestion chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { item ->
                    SuggestionChip(
                        onClick = { onSendQuery(item.prompt) },
                        label = { Text(item.label) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SoftWhite,
                            labelColor = ForestGreen
                        ),
                        border = BorderStroke(1.dp, CreamCardBorder)
                    )
                }
            }

            // Error surface (transparent error propagation per Firebase AI Logic guidelines)
            if (uiState is PipUiState.Error) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = TerracottaTint,
                    border = BorderStroke(1.dp, ErrorTerracotta.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorTerracotta,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Dismiss error", tint = ErrorTerracotta)
                        }
                    }
                }
            }

            // Conversation list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id },
                    contentType = { if (it.isFromUser) "user_msg" else "pip_msg" }
                ) { msg ->
                    val align = if (msg.isFromUser) Alignment.End else Alignment.Start
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = align
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (msg.isFromUser) 18.dp else 4.dp,
                                bottomEnd = if (msg.isFromUser) 4.dp else 18.dp
                            ),
                            color = if (msg.isFromUser) ForestGreen else SoftWhite,
                            border = if (msg.isFromUser) null else BorderStroke(1.dp, CreamCardBorder),
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (msg.isStreaming && msg.text.isBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = ForestGreen
                                        )
                                        Text(
                                            text = "Pip is thinking...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedText
                                        )
                                    }
                                } else {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (msg.isFromUser) SoftWhite else DarkText
                                    )
                                }
                            }
                        }

                        if (!msg.isFromUser && msg.quickActionLabel != null && msg.quickActionRoute != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                onClick = {
                                    onQuickActionNavigate(msg.quickActionRoute)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(50),
                                color = GoldTint,
                                border = BorderStroke(1.dp, HarvestGold)
                            ) {
                                Text(
                                    text = msg.quickActionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepGreen,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Isolated input bar so keystrokes do not recompose the message list or Pip mascot
            PipInputBar(
                askHint = askHint,
                isLoading = uiState is PipUiState.Loading,
                onSendQuery = onSendQuery
            )
        }
    }
}

@Composable
private fun PipInputBar(
    askHint: String,
    isLoading: Boolean,
    onSendQuery: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text(askHint, maxLines = 1) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SoftWhite,
                unfocusedContainerColor = SoftWhite
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("pip_chat_input")
        )
        Surface(
            onClick = {
                if (inputText.isNotBlank() && !isLoading) {
                    onSendQuery(inputText)
                    inputText = ""
                }
            },
            shape = CircleShape,
            color = if (inputText.isNotBlank()) ForestGreen else SageGreen.copy(alpha = 0.5f),
            modifier = Modifier
                .size(50.dp)
                .testTag("pip_send_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send question to Pip",
                    tint = SoftWhite
                )
            }
        }
    }
}
