package com.ciclismo.portugal.presentation.ai.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ciclismo.portugal.domain.model.AiAction
import com.ciclismo.portugal.domain.model.AiSuggestionType
import com.ciclismo.portugal.presentation.ai.AiAssistantViewModel
import com.ciclismo.portugal.presentation.ai.AiAssistantUiState
import com.ciclismo.portugal.presentation.ai.AiOverlayViewModel
import com.ciclismo.portugal.presentation.ai.ChatMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Global AI overlay that appears on all screens.
 * Coordinates FAB, mini tips, expandable cards, and full chat.
 */
@Composable
fun AiGlobalOverlay(
    navController: NavController,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier,
    overlayViewModel: AiOverlayViewModel = hiltViewModel(),
    chatViewModel: AiAssistantViewModel = hiltViewModel()
) {
    // Attach NavController for automatic screen tracking
    LaunchedEffect(navController) {
        overlayViewModel.attachNavController(navController)
    }

    // Collect overlay states
    val isExpanded by overlayViewModel.isExpanded.collectAsState()
    val currentSuggestion by overlayViewModel.currentSuggestion.collectAsState()
    val showMiniTip by overlayViewModel.showMiniTip.collectAsState()
    val showExpandableCard by overlayViewModel.showExpandableCard.collectAsState()
    val hasUnreadSuggestion by overlayViewModel.hasUnreadSuggestion.collectAsState()

    // Collect chat states
    val messages by chatViewModel.messages.collectAsState()
    val quickSuggestions by chatViewModel.quickSuggestions.collectAsState()
    val pendingActions by chatViewModel.pendingActions.collectAsState()
    val chatUiState by chatViewModel.uiState.collectAsState()
    val isContextLoading by chatViewModel.isContextLoading.collectAsState()

    // Handle navigation events from overlay
    LaunchedEffect(Unit) {
        overlayViewModel.navigationEvent.collectLatest { route ->
            onNavigateTo(route)
        }
    }

    // Handle navigation events from chat
    LaunchedEffect(Unit) {
        chatViewModel.navigationEvent.collectLatest { route ->
            overlayViewModel.collapse()
            onNavigateTo(route)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Layer 1: Mini tip (above FAB)
        AnimatedVisibility(
            visible = showMiniTip && !isExpanded && currentSuggestion != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
        ) {
            currentSuggestion?.let { suggestion ->
                if (suggestion.type == AiSuggestionType.MINI_TIP ||
                    suggestion.type == AiSuggestionType.TUTORIAL) {
                    AiMiniTip(
                        suggestion = suggestion,
                        onExpand = { overlayViewModel.expandToChat() },
                        onDismiss = { overlayViewModel.dismissCurrentSuggestion() }
                    )
                }
            }
        }

        // Layer 2: Expandable card (above FAB)
        AnimatedVisibility(
            visible = showExpandableCard && !isExpanded && currentSuggestion != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
        ) {
            currentSuggestion?.let { suggestion ->
                if (suggestion.type == AiSuggestionType.EXPANDABLE_CARD) {
                    AiExpandableCard(
                        suggestion = suggestion,
                        onAskAssistant = { overlayViewModel.expandToChat() },
                        onDismiss = { overlayViewModel.dismissCurrentSuggestion() },
                        onQuickAction = if (suggestion.quickAction != null) {
                            { overlayViewModel.executeQuickAction(suggestion) }
                        } else null
                    )
                }
            }
        }

        // Layer 3: Full chat container
        AiAssistantContainer(
            isExpanded = isExpanded,
            onClose = { overlayViewModel.collapse() },
            content = {
                OverlayChatContent(
                    messages = messages,
                    quickSuggestions = quickSuggestions,
                    pendingActions = pendingActions,
                    isLoading = chatUiState is AiAssistantUiState.Loading,
                    isContextLoading = isContextLoading,
                    onSendMessage = { chatViewModel.sendMessage(it) },
                    onQuickSuggestion = { suggestion ->
                        when (suggestion) {
                            "Analisa a minha equipa" -> chatViewModel.analyzeTeam()
                            "Quem devo comprar?" -> chatViewModel.getTransferRecommendations()
                            else -> chatViewModel.sendMessage(suggestion)
                        }
                    },
                    onExecuteAction = { chatViewModel.executeAction(it) },
                    onDismissAction = { chatViewModel.dismissAction(it) }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )

        // Layer 4: FAB (always visible)
        AiFloatingButton(
            isExpanded = isExpanded,
            hasUnreadMessage = hasUnreadSuggestion && !isExpanded,
            onClick = { overlayViewModel.toggleExpanded() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

/**
 * Simplified chat content for the overlay.
 * Mirrors the functionality of AiChatContent in AiAssistantScreen.
 */
@Composable
private fun OverlayChatContent(
    messages: List<ChatMessage>,
    quickSuggestions: List<String>,
    pendingActions: List<AiAction>,
    isLoading: Boolean,
    isContextLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onQuickSuggestion: (String) -> Unit,
    onExecuteAction: (AiAction) -> Unit,
    onDismissAction: (AiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Context loading indicator
            if (isContextLoading && messages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "A carregar contexto do utilizador...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Welcome message if empty and not loading context
            if (messages.isEmpty() && !isContextLoading) {
                item {
                    OverlayWelcomeMessage(
                        quickSuggestions = quickSuggestions,
                        onSuggestionClick = onQuickSuggestion
                    )
                }
            }

            // Chat messages
            items(messages, key = { it.id }) { message ->
                OverlayChatBubble(
                    message = message,
                    onExecuteAction = onExecuteAction,
                    onDismissAction = onDismissAction
                )
            }

            // Loading indicator
            if (isLoading) {
                item {
                    OverlayLoadingBubble()
                }
            }
        }

        // Quick suggestions when chat has messages
        if (messages.isNotEmpty() && !isLoading) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickSuggestions.take(3)) { suggestion ->
                    SuggestionChip(
                        onClick = { onQuickSuggestion(suggestion) },
                        label = { Text(suggestion, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (isContextLoading) "A carregar..." else "Escreve uma mensagem...",
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                enabled = !isContextLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isContextLoading) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !isLoading && !isContextLoading,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun OverlayWelcomeMessage(
    quickSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFF6366F1)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ola! Como posso ajudar?",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Escolhe uma sugestao ou escreve a tua pergunta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick suggestions
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSuggestions.take(4).forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = { Text(suggestion, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun OverlayChatBubble(
    message: ChatMessage,
    onExecuteAction: ((AiAction) -> Unit)? = null,
    onDismissAction: ((AiAction) -> Unit)? = null
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) {
        Color(0xFF6366F1)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp
                )

                // CRITICAL FIX: Show action buttons if message has actions
                if (message.actions.isNotEmpty() && !message.isUser && onExecuteAction != null && onDismissAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Acoes sugeridas:",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.actions.forEach { action ->
                        OverlayActionButton(
                            action = action,
                            onExecute = { onExecuteAction(action) },
                            onDismiss = { onDismissAction(action) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact action button for overlay chat.
 * Uses distinctive colors to stand out from message text.
 */
@Composable
private fun OverlayActionButton(
    action: AiAction,
    onExecute: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Main action button with vibrant color
        Button(
            onClick = onExecute,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1), // Vibrant indigo
                contentColor = Color.White
            )
        ) {
            Text(
                text = action.title,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        // Dismiss button (X) with subtle styling
        OutlinedIconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Text("×", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OverlayLoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF6366F1).copy(alpha = 0.5f + (index * 0.15f))
                    )
                }
            }
        }
    }
}
