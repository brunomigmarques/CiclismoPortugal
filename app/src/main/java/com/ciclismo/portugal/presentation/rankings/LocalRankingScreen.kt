package com.ciclismo.portugal.presentation.rankings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.LocalRanking
import com.ciclismo.portugal.domain.model.LocalRankingEntry
import com.ciclismo.portugal.presentation.theme.AppColors
import com.ciclismo.portugal.presentation.theme.AppImages
import com.ciclismo.portugal.presentation.theme.PortugueseGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRankingScreen(
    rankingId: String,
    onNavigateBack: () -> Unit,
    isAdmin: Boolean = false,
    viewModel: LocalRankingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ranking by viewModel.ranking.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val message by viewModel.message.collectAsState()
    val currentUserIndex by viewModel.currentUserIndex.collectAsState()
    val currentUserEntry by viewModel.currentUserEntry.collectAsState()

    var showAddFakeDialog by remember { mutableStateOf(false) }
    var showRemoveFakeDialog by remember { mutableStateOf(false) }
    var fakeCount by remember { mutableStateOf("236") }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(rankingId) {
        viewModel.loadRanking(rankingId)
    }

    // Auto-scroll to user's position when data loads
    LaunchedEffect(currentUserIndex, uiState) {
        if (uiState is LocalRankingUiState.Success && currentUserIndex != null) {
            // Small delay to ensure list is rendered
            kotlinx.coroutines.delay(300)
            listState.animateScrollToItem(currentUserIndex!!)
        }
    }

    // Show message as snackbar
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Add fake entries dialog
    if (showAddFakeDialog) {
        AlertDialog(
            onDismissRequest = { showAddFakeDialog = false },
            title = { Text("Criar Participantes de Teste") },
            text = {
                Column {
                    Text("Quantos participantes deseja criar?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fakeCount,
                        onValueChange = { fakeCount = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantidade") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddFakeDialog = false
                        val count = fakeCount.toIntOrNull() ?: 236
                        viewModel.addFakeEntries(count)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFakeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Remove fake entries dialog
    if (showRemoveFakeDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFakeDialog = false },
            title = { Text("Remover Participantes de Teste") },
            text = { Text("Tem a certeza que deseja remover todos os participantes de teste deste ranking?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveFakeDialog = false
                        viewModel.removeFakeEntries()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveFakeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            AsyncImage(
                model = AppImages.ROAD_CYCLING,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PortugueseGreen.copy(alpha = 0.7f),
                                PortugueseGreen.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ranking?.name ?: "Ranking",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        ranking?.description?.let { desc ->
                            Text(
                                text = desc,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Admin buttons
                    if (isAdmin) {
                        IconButton(
                            onClick = { showAddFakeDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color(0xFF9C27B0).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Criar Teste",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showRemoveFakeDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color.Red.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remover Teste",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Ranking info chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ranking?.raceType?.let { type ->
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    ranking?.region?.let { region ->
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = region,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${entries.size}",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Content
        when (uiState) {
            is LocalRankingUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is LocalRankingUiState.Empty -> {
                EmptyRankingContent()
            }

            is LocalRankingUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Podium (top 3) - special display
                    if (entries.size >= 3) {
                        item {
                            PodiumSection(
                                first = entries[0],
                                second = entries[1],
                                third = entries[2],
                                currentUserId = currentUserEntry?.id
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Rest of entries
                    val startIndex = if (entries.size >= 3) 3 else 0
                    items(entries.drop(startIndex), key = { it.id }) { entry ->
                        RankingEntryCard(
                            entry = entry,
                            isCurrentUser = entry.id == currentUserEntry?.id
                        )
                    }
                }
            }

            is LocalRankingUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as LocalRankingUiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadRanking(rankingId) }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun PodiumSection(
    first: LocalRankingEntry,
    second: LocalRankingEntry,
    third: LocalRankingEntry,
    currentUserId: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Podio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd place
                PodiumItem(
                    entry = second,
                    position = 2,
                    height = 80.dp,
                    color = Color(0xFFC0C0C0), // Silver
                    isCurrentUser = second.id == currentUserId
                )

                // 1st place
                PodiumItem(
                    entry = first,
                    position = 1,
                    height = 100.dp,
                    color = AppColors.Gold,
                    isCurrentUser = first.id == currentUserId
                )

                // 3rd place
                PodiumItem(
                    entry = third,
                    position = 3,
                    height = 60.dp,
                    color = Color(0xFFCD7F32), // Bronze
                    isCurrentUser = third.id == currentUserId
                )
            }
        }
    }
}

@Composable
private fun PodiumItem(
    entry: LocalRankingEntry,
    position: Int,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    isCurrentUser: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .then(
                if (isCurrentUser) {
                    Modifier
                        .background(
                            color = PortugueseGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = PortugueseGreen,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                } else Modifier
            )
    ) {
        // "Tu" label for current user
        if (isCurrentUser) {
            Text(
                text = "Tu",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PortugueseGreen
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // User photo or placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
                .then(
                    if (isCurrentUser) Modifier.border(2.dp, PortugueseGreen, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (entry.userPhotoUrl != null) {
                AsyncImage(
                    model = entry.userPhotoUrl,
                    contentDescription = entry.userName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = entry.userName.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = entry.userName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = if (isCurrentUser) PortugueseGreen else MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "${entry.totalPoints} pts",
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrentUser) PortugueseGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Podium stand
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .background(
                    color = color,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$position",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun RankingEntryCard(
    entry: LocalRankingEntry,
    isCurrentUser: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrentUser) {
                    Modifier.border(
                        width = 2.dp,
                        color = PortugueseGreen,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                PortugueseGreen.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isCurrentUser) PortugueseGreen else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entry.position}",
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Country flag + User photo
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (isCurrentUser) Modifier.border(2.dp, PortugueseGreen, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.userPhotoUrl != null) {
                        AsyncImage(
                            model = entry.userPhotoUrl,
                            contentDescription = entry.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = entry.userName.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Country flag badge
                Text(
                    text = entry.countryFlag,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name, team and region
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrentUser) PortugueseGreen else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Tu)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PortugueseGreen
                        )
                    }
                }
                // Team name
                entry.teamName?.let { team ->
                    Text(
                        text = team,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Region and stats
                Row {
                    entry.region?.let { region ->
                        Text(
                            text = "$region • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${entry.racesParticipated} provas | ${entry.wins}V | ${entry.podiums}P",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Points and position change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.totalPoints}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PortugueseGreen
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.positionChange != 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (entry.positionChange > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (entry.positionChange > 0) PortugueseGreen else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = entry.positionChangeDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.positionChange > 0) PortugueseGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRankingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Leaderboard,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sem participantes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Este ranking ainda nao tem participantes. Participa em provas para subires no ranking!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

sealed class LocalRankingUiState {
    data object Loading : LocalRankingUiState()
    data object Empty : LocalRankingUiState()
    data object Success : LocalRankingUiState()
    data class Error(val message: String) : LocalRankingUiState()
}
