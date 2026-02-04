package com.ciclismo.portugal.presentation.ai

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.ciclismo.portugal.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ciclismo.portugal.domain.model.AiAction
import com.ciclismo.portugal.domain.model.AiActionType
import com.ciclismo.portugal.domain.model.ActionPriority
import com.ciclismo.portugal.presentation.ai.components.AiAssistantContainer
import com.ciclismo.portugal.presentation.ai.components.AiFloatingButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Full-screen AI Assistant accessible from Fantasy Hub.
 * Provides a dedicated chat experience for cycling fantasy game.
 * Now with agentic capabilities - can suggest and execute actions.
 */
@Composable
fun AiAssistantFullScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    viewModel: AiAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val quickSuggestions by viewModel.quickSuggestions.collectAsState()
    val pendingActions by viewModel.pendingActions.collectAsState()

    // Handle navigation events from action execution
    LaunchedEffect(Unit) {
        viewModel.setCurrentScreen("ai_full")
        viewModel.navigationEvent.collectLatest { route ->
            onNavigateTo(route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with background image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .paint(
                    painter = painterResource(id = R.drawable.header_ai_background),
                    contentScale = ContentScale.Crop
                )
        ) {
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Back button row
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Title section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    // AI Robot icon with glow effect
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DS Assistant",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "O teu assistente de ciclismo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Chat content with action support
        AiChatContent(
            messages = messages,
            quickSuggestions = quickSuggestions,
            pendingActions = pendingActions,
            isLoading = uiState is AiAssistantUiState.Loading,
            onSendMessage = { viewModel.sendMessage(it) },
            onQuickSuggestion = { suggestion ->
                when (suggestion) {
                    "Analisa a minha equipa" -> viewModel.analyzeTeam()
                    "Quem devo comprar?" -> viewModel.getTransferRecommendations()
                    else -> viewModel.sendMessage(suggestion)
                }
            },
            onExecuteAction = { viewModel.executeAction(it) },
            onDismissAction = { viewModel.dismissAction(it) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Composable que inclui o botao flutuante e o chat expandido.
 * Pode ser usado como overlay em paginas especificas se necessario.
 * Now with agentic capabilities.
 */
@Composable
fun AiAssistantOverlay(
    currentScreen: String,
    onNavigateTo: (String) -> Unit = {},
    viewModel: AiAssistantViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isExpanded by viewModel.isExpanded.collectAsState()
    val quickSuggestions by viewModel.quickSuggestions.collectAsState()
    val pendingActions by viewModel.pendingActions.collectAsState()

    LaunchedEffect(currentScreen) {
        viewModel.setCurrentScreen(currentScreen)
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { route ->
            onNavigateTo(route)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Expanded chat container
        AiAssistantContainer(
            isExpanded = isExpanded,
            onClose = { viewModel.collapse() },
            content = {
                AiChatContent(
                    messages = messages,
                    quickSuggestions = quickSuggestions,
                    pendingActions = pendingActions,
                    isLoading = uiState is AiAssistantUiState.Loading,
                    onSendMessage = { viewModel.sendMessage(it) },
                    onQuickSuggestion = { suggestion ->
                        when (suggestion) {
                            "Analisa a minha equipa" -> viewModel.analyzeTeam()
                            "Quem devo comprar?" -> viewModel.getTransferRecommendations()
                            else -> viewModel.sendMessage(suggestion)
                        }
                    },
                    onExecuteAction = { viewModel.executeAction(it) },
                    onDismissAction = { viewModel.dismissAction(it) }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp)
        )

        // Floating button
        AiFloatingButton(
            isExpanded = isExpanded,
            hasUnreadMessage = messages.isNotEmpty() && messages.last().isUser.not() && !isExpanded,
            onClick = { viewModel.toggleExpanded() },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AiChatContent(
    messages: List<ChatMessage>,
    quickSuggestions: List<String>,
    pendingActions: List<AiAction> = emptyList(),
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onQuickSuggestion: (String) -> Unit,
    onExecuteAction: (AiAction) -> Unit = {},
    onDismissAction: (AiAction) -> Unit = {},
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
        modifier = modifier
            .fillMaxWidth()
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
            // Welcome message if empty
            if (messages.isEmpty()) {
                item {
                    WelcomeMessage(
                        quickSuggestions = quickSuggestions,
                        onSuggestionClick = onQuickSuggestion
                    )
                }
            }

            // Chat messages
            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onExecuteAction = onExecuteAction,
                    onDismissAction = onDismissAction
                )
            }

            // Loading indicator
            if (isLoading) {
                item {
                    LoadingBubble()
                }
            }

            // Pending actions section
            if (pendingActions.isNotEmpty() && !isLoading) {
                item {
                    PendingActionsCard(
                        actions = pendingActions,
                        onExecute = onExecuteAction,
                        onDismiss = onDismissAction
                    )
                }
            }
        }

        // Quick suggestions (when chat is active)
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escreve a tua pergunta...") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                enabled = inputText.isNotBlank() && !isLoading,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar"
                )
            }
        }
    }
}

@Composable
private fun WelcomeMessage(
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Ola! Sou o DS Assistant.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "O teu assistente de ciclismo e Fantasy!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sugestoes rapidas:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            quickSuggestions.forEach { suggestion ->
                SuggestionButton(
                    text = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF6366F1)
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onExecuteAction: (AiAction) -> Unit = {},
    onDismissAction: (AiAction) -> Unit = {}
) {
    val backgroundColor = when {
        message.isError -> Color(0xFFEF4444).copy(alpha = 0.1f)
        message.isSystemMessage -> Color(0xFF10B981).copy(alpha = 0.1f)
        message.isUser -> Color(0xFF6366F1)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        message.isError -> Color(0xFFEF4444)
        message.isSystemMessage -> Color(0xFF10B981)
        message.isUser -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Show inline action buttons if message has actions
                    if (message.actions.isNotEmpty() && !message.isUser) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Acoes sugeridas:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        message.actions.forEach { action ->
                            ActionButton(
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
}

/**
 * Compact action button for inline display in chat messages.
 */
@Composable
private fun ActionButton(
    action: AiAction,
    onExecute: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionColor = getActionColor(action.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Action icon
        Icon(
            imageVector = getActionIcon(action.type),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = actionColor
        )

        // Action title
        Text(
            text = action.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Execute button
        TextButton(
            onClick = onExecute,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = "Executar",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = actionColor
            )
        }
    }
}

/**
 * Card showing all pending actions that can be executed.
 */
@Composable
private fun PendingActionsCard(
    actions: List<AiAction>,
    onExecute: (AiAction) -> Unit,
    onDismiss: (AiAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF3C7) // Amber/yellow background
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Acoes pendentes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            actions.forEach { action ->
                PendingActionItem(
                    action = action,
                    onExecute = { onExecute(action) },
                    onDismiss = { onDismiss(action) }
                )
                if (action != actions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PendingActionItem(
    action: AiAction,
    onExecute: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionColor = getActionColor(action.type)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getActionIcon(action.type),
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (action.description.isNotBlank()) {
                        Text(
                            text = action.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Ignorar", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExecute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionColor
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aprovar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Get icon for action type.
 * Supports all action types for full app-wide tutoring.
 */
private fun getActionIcon(type: AiActionType) = when (type) {
    // Fantasy actions
    AiActionType.BUY_CYCLIST -> Icons.Default.AddCircle
    AiActionType.SELL_CYCLIST -> Icons.Default.RemoveCircle
    AiActionType.SET_CAPTAIN -> Icons.Default.Star
    AiActionType.ACTIVATE_CYCLIST -> Icons.Default.PersonAdd
    AiActionType.DEACTIVATE_CYCLIST -> Icons.Default.PersonRemove
    AiActionType.USE_TRIPLE_CAPTAIN -> Icons.Default.Bolt
    AiActionType.USE_WILDCARD -> Icons.Default.AutoAwesome

    // Navigation
    AiActionType.NAVIGATE_TO -> Icons.Default.OpenInNew
    AiActionType.VIEW_CYCLIST -> Icons.Default.Person
    AiActionType.VIEW_RACE -> Icons.Default.EmojiEvents

    // App-wide navigation
    AiActionType.VIEW_PROVA -> Icons.Default.DirectionsBike
    AiActionType.VIEW_NEWS -> Icons.Default.Newspaper
    AiActionType.VIEW_RANKINGS -> Icons.Default.Leaderboard
    AiActionType.VIEW_PROFILE -> Icons.Default.AccountCircle
    AiActionType.VIEW_VIDEO -> Icons.Default.PlayCircle

    // User results
    AiActionType.VIEW_MY_RESULTS -> Icons.Default.History
    AiActionType.ADD_RESULT -> Icons.Default.Add

    // Information
    AiActionType.SHOW_STANDINGS -> Icons.Default.Leaderboard
    AiActionType.SHOW_CALENDAR -> Icons.Default.CalendarMonth
    AiActionType.SHOW_HELP -> Icons.Default.Help
    AiActionType.SHOW_TUTORIAL -> Icons.Default.School

    // Settings
    AiActionType.ENABLE_NOTIFICATIONS -> Icons.Default.Notifications
    AiActionType.SET_REMINDER -> Icons.Default.Alarm

    // Premium
    AiActionType.VIEW_PREMIUM -> Icons.Default.Diamond
}

/**
 * Get color for action type.
 * Supports all action types for full app-wide tutoring.
 */
private fun getActionColor(type: AiActionType) = when (type) {
    // Fantasy actions - distinctive colors
    AiActionType.BUY_CYCLIST -> Color(0xFF10B981) // Green
    AiActionType.SELL_CYCLIST -> Color(0xFFEF4444) // Red
    AiActionType.SET_CAPTAIN -> Color(0xFFF59E0B) // Amber
    AiActionType.ACTIVATE_CYCLIST -> Color(0xFF3B82F6) // Blue
    AiActionType.DEACTIVATE_CYCLIST -> Color(0xFF6B7280) // Gray
    AiActionType.USE_TRIPLE_CAPTAIN -> Color(0xFFFFD700) // Gold
    AiActionType.USE_WILDCARD -> Color(0xFF8B5CF6) // Purple

    // Navigation - Indigo theme
    AiActionType.NAVIGATE_TO -> Color(0xFF6366F1)
    AiActionType.VIEW_CYCLIST -> Color(0xFF6366F1)
    AiActionType.VIEW_RACE -> Color(0xFF6366F1)

    // App-wide navigation - Teal theme
    AiActionType.VIEW_PROVA -> Color(0xFF14B8A6)
    AiActionType.VIEW_NEWS -> Color(0xFF0EA5E9)
    AiActionType.VIEW_RANKINGS -> Color(0xFFF59E0B)
    AiActionType.VIEW_PROFILE -> Color(0xFF8B5CF6)
    AiActionType.VIEW_VIDEO -> Color(0xFFEC4899)

    // User results - Green theme
    AiActionType.VIEW_MY_RESULTS -> Color(0xFF22C55E)
    AiActionType.ADD_RESULT -> Color(0xFF10B981)

    // Information - Blue theme
    AiActionType.SHOW_STANDINGS -> Color(0xFF6366F1)
    AiActionType.SHOW_CALENDAR -> Color(0xFF6366F1)
    AiActionType.SHOW_HELP -> Color(0xFF3B82F6)
    AiActionType.SHOW_TUTORIAL -> Color(0xFF0EA5E9)

    // Settings - Gray theme
    AiActionType.ENABLE_NOTIFICATIONS -> Color(0xFF6366F1)
    AiActionType.SET_REMINDER -> Color(0xFF6366F1)

    // Premium - Gold theme
    AiActionType.VIEW_PREMIUM -> Color(0xFFFFD700)
}

@Composable
private fun LoadingBubble() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    val delay = index * 100
                    TypingDot(delay = delay)
                }
            }
        }
    }
}

@Composable
private fun TypingDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = delay
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFF6366F1).copy(alpha = alpha))
    )
}
