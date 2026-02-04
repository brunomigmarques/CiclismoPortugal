package com.ciclismo.portugal.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.presentation.theme.AppImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationAdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationAdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val leagues by viewModel.leagues.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var notificationTitle by remember { mutableStateOf("") }
    var notificationBody by remember { mutableStateOf("") }
    var selectedLeagueIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var notificationType by remember { mutableStateOf(NotificationType.GENERAL) }
    var selectAll by remember { mutableStateOf(false) }

    // Confirmation dialog
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(selectAll) {
        if (selectAll) {
            selectedLeagueIds = leagues.map { it.id }.toSet()
        } else if (selectedLeagueIds.size == leagues.size && leagues.isNotEmpty()) {
            selectedLeagueIds = emptySet()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with cycling image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Background cycling image
                AsyncImage(
                    model = AppImages.ROAD_CYCLING,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay with purple tint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF7B1FA2).copy(alpha = 0.7f),
                                    Color(0xFF9C27B0).copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Header content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Notificações",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Enviar notificações para ligas",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notification type selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Tipo de Notificação",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NotificationType.entries.forEach { type ->
                                    FilterChip(
                                        selected = notificationType == type,
                                        onClick = { notificationType = type },
                                        label = { Text(type.displayName) },
                                        leadingIcon = {
                                            Icon(
                                                type.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = type.color.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Notification content
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Conteúdo da Notificação",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = notificationTitle,
                                onValueChange = { notificationTitle = it },
                                label = { Text("Título") },
                                placeholder = { Text("Ex: Nova prova disponível!") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Title, contentDescription = null)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = notificationBody,
                                onValueChange = { notificationBody = it },
                                label = { Text("Mensagem") },
                                placeholder = { Text("Ex: Inscreve-te na Volta a Portugal!") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                leadingIcon = {
                                    Icon(Icons.Default.Message, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                // League selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Ligas Alvo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectAll,
                                        onCheckedChange = { selectAll = it }
                                    )
                                    Text("Todas", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Seleciona as ligas que receberão a notificação",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Loading state
                when (uiState) {
                    is NotificationAdminUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is NotificationAdminUiState.Success -> {
                        if (leagues.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.GroupOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Nenhuma liga encontrada",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        } else {
                            items(leagues) { league ->
                                LeagueSelectionCard(
                                    league = league,
                                    isSelected = selectedLeagueIds.contains(league.id),
                                    onToggle = {
                                        selectedLeagueIds = if (selectedLeagueIds.contains(league.id)) {
                                            selectedLeagueIds - league.id
                                        } else {
                                            selectedLeagueIds + league.id
                                        }
                                        // Update selectAll state
                                        selectAll = selectedLeagueIds.size == leagues.size
                                    }
                                )
                            }
                        }
                    }

                    is NotificationAdminUiState.Error -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        (uiState as NotificationAdminUiState.Error).message,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Send button
                item {
                    val canSend = notificationTitle.isNotBlank() &&
                            notificationBody.isNotBlank() &&
                            selectedLeagueIds.isNotEmpty()

                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSend,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Notificação")
                    }

                    if (!canSend) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when {
                                notificationTitle.isBlank() -> "Adiciona um título"
                                notificationBody.isBlank() -> "Adiciona uma mensagem"
                                selectedLeagueIds.isEmpty() -> "Seleciona pelo menos uma liga"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Warning
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "As notificações serão enviadas imediatamente para todos os utilizadores das ligas selecionadas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Confirmation dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text("Confirmar Envio") },
                text = {
                    Column {
                        Text("Título: $notificationTitle")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Mensagem: $notificationBody")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ligas: ${selectedLeagueIds.size} selecionadas")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Tens a certeza que queres enviar esta notificação?",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.sendNotification(
                                title = notificationTitle,
                                body = notificationBody,
                                leagueIds = selectedLeagueIds.toList(),
                                type = notificationType
                            )
                            // Clear form
                            notificationTitle = ""
                            notificationBody = ""
                            selectedLeagueIds = emptySet()
                            selectAll = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Text("Enviar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun LeagueSelectionCard(
    league: LeagueInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF9C27B0).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF9C27B0))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    league.name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${league.memberCount} membros",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (league.isGlobal) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.2f)
                ) {
                    Text(
                        "Global",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

enum class NotificationType(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
) {
    GENERAL("Geral", Icons.Default.Notifications, Color(0xFF9C27B0)),
    PROVA("Provas", Icons.Default.EmojiEvents, Color(0xFF2196F3)),
    DESCONTO("Descontos", Icons.Default.LocalOffer, Color(0xFF4CAF50)),
    NEWS("Notícias", Icons.Default.Article, Color(0xFFFF9800))
}

data class LeagueInfo(
    val id: String,
    val name: String,
    val memberCount: Int,
    val isGlobal: Boolean = false
)

sealed class NotificationAdminUiState {
    object Loading : NotificationAdminUiState()
    object Success : NotificationAdminUiState()
    data class Error(val message: String) : NotificationAdminUiState()
}
