package com.ciclismo.portugal.presentation.fantasy.team

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.TeamCyclist
import com.ciclismo.portugal.presentation.ads.BannerAdView
import com.ciclismo.portugal.presentation.fantasy.team.components.RaceExtrasSection
import com.ciclismo.portugal.presentation.theme.AppImages
import com.ciclismo.portugal.presentation.fantasy.market.CategoryClimber
import com.ciclismo.portugal.presentation.fantasy.market.CategoryHills
import com.ciclismo.portugal.presentation.fantasy.market.CategoryTT
import com.ciclismo.portugal.presentation.fantasy.market.CategorySprint
import com.ciclismo.portugal.presentation.fantasy.market.CategoryGC
import com.ciclismo.portugal.presentation.fantasy.market.CategoryOneday

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTeamScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMarket: () -> Unit,
    onRestartWizard: () -> Unit,
    onNavigateToAiAssistant: () -> Unit = {},
    viewModel: MyTeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCyclist by viewModel.selectedCyclist.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestartWizardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Edit Team Wizard Confirmation Dialog
    if (showRestartWizardDialog) {
        val successState = uiState as? MyTeamUiState.Success
        val team = successState?.team
        val hasUnlimitedTransfers = team?.hasUnlimitedTransfers == true
        val remainingFreeTransfers = team?.remainingFreeTransfers ?: 2

        AlertDialog(
            onDismissRequest = { showRestartWizardDialog = false },
            icon = {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF006600)
                )
            },
            title = { Text("Editar Equipa") },
            text = {
                Column {
                    Text(
                        "Abre o wizard com a tua equipa atual carregada. " +
                        "Podes alterar ciclistas categoria por categoria."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (hasUnlimitedTransfers) {
                        Text(
                            "✓ Wildcard/Free Hit ativo - Transferencias ilimitadas",
                            color = Color(0xFF4CAF50),
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            "• $remainingFreeTransfers transferencias gratis esta semana",
                            fontSize = 13.sp
                        )
                        Text(
                            "• 4 pontos de penalizacao por cada extra",
                            fontSize = 13.sp,
                            color = Color(0xFFF39C12)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartWizardDialog = false
                        onRestartWizard()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF006600)
                    )
                ) {
                    Text("Abrir Wizard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartWizardDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // FAB removed - now handled by global AiGlobalOverlay in NavGraph
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
                    .height(140.dp)
            ) {
                // Background cycling image
                AsyncImage(
                    model = AppImages.FANTASY_TEAM,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.7f)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Minha Equipa",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Restart wizard button (only when user has a team)
                        if (uiState is MyTeamUiState.Success) {
                            TextButton(onClick = { showRestartWizardDialog = true }) {
                                Text(
                                    text = "Refazer",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (uiState is MyTeamUiState.Success) {
                        val state = uiState as MyTeamUiState.Success
                        Spacer(modifier = Modifier.height(12.dp))
                        TeamStatsRow(
                            team = state.team,
                            teamValue = state.teamValue,
                            cyclistCount = state.cyclists.size,
                            activeCount = state.activeCount
                        )

                        // Active Wildcards Banner
                        if (state.team.isTripleCaptainActive || state.team.isBenchBoostActive) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ActiveWildcardsBanner(
                                isTripleCaptainActive = state.team.isTripleCaptainActive,
                                isBenchBoostActive = state.team.isBenchBoostActive,
                                onCancelTripleCaptain = { viewModel.cancelTripleCaptain() },
                                onCancelBenchBoost = { viewModel.cancelBenchBoost() }
                            )
                        }
                    }
                }
            }

            // Content
            when (uiState) {
                is MyTeamUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is MyTeamUiState.NotAuthenticated -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Faz login para ver a tua equipa")
                    }
                }

                is MyTeamUiState.NoTeam -> {
                    NoTeamContent(onNavigateToMarket = onNavigateToMarket)
                }

                is MyTeamUiState.Success -> {
                    val state = uiState as MyTeamUiState.Success
                    TeamContent(
                        state = state,
                        selectedCyclist = selectedCyclist,
                        onCyclistClick = { viewModel.selectCyclist(it) },
                        onToggleActive = { cyclistId, isActive -> viewModel.toggleActive(cyclistId, isActive) },
                        onSetCaptain = { cyclistId -> viewModel.setCaptain(cyclistId) },
                        onNavigateToMarket = onNavigateToMarket,
                        // Per-race wildcard callbacks
                        onActivateTripleCaptain = { viewModel.activateTripleCaptainForRace(it) },
                        onDeactivateTripleCaptain = { viewModel.cancelTripleCaptain() },
                        onActivateBenchBoost = { viewModel.activateBenchBoostForRace(it) },
                        onDeactivateBenchBoost = { viewModel.cancelBenchBoost() },
                        onActivateWildcard = { viewModel.activateWildcardForRace(it) },
                        onDeactivateWildcard = { viewModel.cancelWildcard() }
                    )
                }

                is MyTeamUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (uiState as MyTeamUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshTeam() }) {
                                Text("Tentar novamente")
                            }
                        }
                    }
                }
            }
        }

        // Bottom sheet for cyclist details
        if (selectedCyclist != null) {
            CyclistBottomSheet(
                cyclist = selectedCyclist!!,
                onDismiss = { viewModel.selectCyclist(null) },
                onSetCaptain = { viewModel.setCaptain(selectedCyclist!!.second.id) },
                onToggleActive = { viewModel.toggleActive(selectedCyclist!!.second.id, !selectedCyclist!!.first.isActive) },
                onRemove = { viewModel.removeCyclist(selectedCyclist!!.second.id) }
            )
        }
    }
}

@Composable
private fun TeamStatsRow(
    team: FantasyTeam,
    teamValue: Double,
    cyclistCount: Int,
    activeCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Pontos", value = team.totalPoints.toString())
        StatItem(label = "Orcamento", value = team.displayBudget)
        StatItem(label = "Valor", value = "${String.format("%.1f", teamValue)}M")
        StatItem(label = "Ativos", value = "$activeCount/8")
    }
}

@Composable
private fun ActiveWildcardsBanner(
    isTripleCaptainActive: Boolean,
    isBenchBoostActive: Boolean,
    onCancelTripleCaptain: () -> Unit,
    onCancelBenchBoost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFD700))
            .padding(12.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WILDCARDS ATIVOS",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Wildcard toggles
        if (isTripleCaptainActive) {
            WildcardToggleRow(
                name = "Triple Captain (3x)",
                isActive = true,
                onToggle = { onCancelTripleCaptain() }
            )
        }

        if (isBenchBoostActive) {
            if (isTripleCaptainActive) Spacer(modifier = Modifier.height(8.dp))
            WildcardToggleRow(
                name = "Bench Boost",
                isActive = true,
                onToggle = { onCancelBenchBoost() }
            )
        }
    }
}

@Composable
private fun WildcardToggleRow(
    name: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = Color.Black,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2ECC71),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun NoTeamContent(onNavigateToMarket: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ainda nao tens equipa",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cria a tua equipa e adiciona ciclistas do mercado",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateToMarket) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ir para o Mercado")
            }
        }
    }
}

@Composable
private fun TeamContent(
    state: MyTeamUiState.Success,
    selectedCyclist: Pair<TeamCyclist, Cyclist>?,
    onCyclistClick: (Pair<TeamCyclist, Cyclist>) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onSetCaptain: (String) -> Unit,
    onNavigateToMarket: () -> Unit,
    // Per-race wildcard callbacks
    onActivateTripleCaptain: (String) -> Unit,
    onDeactivateTripleCaptain: () -> Unit,
    onActivateBenchBoost: (String) -> Unit,
    onDeactivateBenchBoost: () -> Unit,
    onActivateWildcard: (String) -> Unit,
    onDeactivateWildcard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Banner Ad
        item {
            BannerAdView(modifier = Modifier.padding(bottom = 8.dp))
        }

        // Team name and next race info
        item {
            Column {
                Text(
                    text = state.team.teamName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Race Extras Section (Wildcards per-race activation) - compact mode for cleaner UI
                RaceExtrasSection(
                    team = state.team,
                    nextRace = state.nextRace,
                    onActivateTripleCaptain = onActivateTripleCaptain,
                    onDeactivateTripleCaptain = onDeactivateTripleCaptain,
                    onActivateBenchBoost = onActivateBenchBoost,
                    onDeactivateBenchBoost = onDeactivateBenchBoost,
                    onActivateWildcard = onActivateWildcard,
                    onDeactivateWildcard = onDeactivateWildcard,
                    modifier = Modifier.padding(bottom = 8.dp),
                    compactMode = true
                )

                // Next race info (shows stage for multi-stage races)
                if (state.nextRace != null) {
                    NextRaceCard(
                        race = state.nextRace!!,
                        stage = state.nextStage
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Active section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ativos (${state.activeCount}/8)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2ECC71)
                )
                if (state.captain != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "C: ${state.captain!!.second.lastName}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (state.activeCyclists.isEmpty()) {
            item {
                EmptySlotCard(
                    text = "Nenhum ciclista ativo",
                    onClick = onNavigateToMarket
                )
            }
        } else {
            items(state.activeCyclists, key = { it.second.id }) { (teamCyclist, cyclist) ->
                TeamCyclistCard(
                    teamCyclist = teamCyclist,
                    cyclist = cyclist,
                    activeCount = state.activeCount,
                    onClick = { onCyclistClick(teamCyclist to cyclist) },
                    onToggleActive = { onToggleActive(cyclist.id, !teamCyclist.isActive) },
                    onSetCaptain = { onSetCaptain(cyclist.id) }
                )
            }
        }

        // Bench section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Suplentes (${state.benchCyclists.size}/7)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (state.benchCyclists.isEmpty() && state.cyclists.size < 15) {
            item {
                EmptySlotCard(
                    text = "Adicionar ciclista",
                    onClick = onNavigateToMarket
                )
            }
        } else {
            items(state.benchCyclists, key = { it.second.id }) { (teamCyclist, cyclist) ->
                TeamCyclistCard(
                    teamCyclist = teamCyclist,
                    cyclist = cyclist,
                    activeCount = state.activeCount,
                    onClick = { onCyclistClick(teamCyclist to cyclist) },
                    onToggleActive = { onToggleActive(cyclist.id, !teamCyclist.isActive) },
                    onSetCaptain = { onSetCaptain(cyclist.id) }
                )
            }
        }

        // Add more button if team not full
        if (state.cyclists.size < 15) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onNavigateToMarket,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Ciclista (${state.cyclists.size}/15)")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamCyclistCard(
    teamCyclist: TeamCyclist,
    cyclist: Cyclist,
    activeCount: Int,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    onSetCaptain: () -> Unit
) {
    val categoryColor = getCategoryColor(cyclist.category)
    val canActivate = !teamCyclist.isActive && activeCount < 8
    val canDeactivate = teamCyclist.isActive && !teamCyclist.isCaptain

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (teamCyclist.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Captain badge or photo
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cyclist.photoUrl != null) {
                        AsyncImage(
                            model = cyclist.photoUrl,
                            contentDescription = cyclist.fullName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter
                        )
                    } else {
                        Text(
                            text = "${cyclist.firstName.firstOrNull() ?: ""}${cyclist.lastName.firstOrNull() ?: ""}",
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }
                }

                if (teamCyclist.isCaptain) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700))
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cyclist.fullName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = getCategoryName(cyclist.category),
                            fontSize = 10.sp,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = cyclist.teamName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Price
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = cyclist.displayPrice,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF006600)
                )
                if (cyclist.totalPoints > 0) {
                    Text(
                        text = "${cyclist.totalPoints} pts",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Captain button (star) - only show if active
                if (teamCyclist.isActive) {
                    IconButton(
                        onClick = onSetCaptain,
                        enabled = !teamCyclist.isCaptain,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (teamCyclist.isCaptain) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = if (teamCyclist.isCaptain) "Capitao" else "Definir como Capitao",
                            tint = if (teamCyclist.isCaptain) {
                                Color(0xFFFFD700)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Toggle active/inactive button
                IconButton(
                    onClick = onToggleActive,
                    enabled = canActivate || canDeactivate,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (teamCyclist.isActive) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        contentDescription = if (teamCyclist.isActive) "Mover para suplentes" else "Ativar",
                        tint = if (teamCyclist.isActive) {
                            if (canDeactivate) Color(0xFFE74C3C) else Color.Gray.copy(alpha = 0.3f)
                        } else {
                            if (canActivate) Color(0xFF2ECC71) else Color.Gray.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptySlotCard(
    text: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CyclistBottomSheet(
    cyclist: Pair<TeamCyclist, Cyclist>,
    onDismiss: () -> Unit,
    onSetCaptain: () -> Unit,
    onToggleActive: () -> Unit,
    onRemove: () -> Unit
) {
    val (teamCyclist, cyclistData) = cyclist

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(cyclistData.category).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cyclistData.photoUrl != null) {
                        AsyncImage(
                            model = cyclistData.photoUrl,
                            contentDescription = cyclistData.fullName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter // Top-down crop
                        )
                    } else {
                        Text(
                            text = "${cyclistData.firstName.firstOrNull() ?: ""}${cyclistData.lastName.firstOrNull() ?: ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = getCategoryColor(cyclistData.category)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cyclistData.fullName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = getCategoryColor(cyclistData.category).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = getCategoryName(cyclistData.category),
                                fontSize = 11.sp,
                                color = getCategoryColor(cyclistData.category),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = cyclistData.teamName,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = cyclistData.displayPrice,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006600)
                    )
                    val priceDiff = cyclistData.price - teamCyclist.purchasePrice
                    if (priceDiff != 0.0) {
                        Text(
                            text = "${if (priceDiff > 0) "+" else ""}${String.format("%.1f", priceDiff)}M",
                            fontSize = 12.sp,
                            color = if (priceDiff > 0) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Set captain
                OutlinedButton(
                    onClick = onSetCaptain,
                    modifier = Modifier.weight(1f),
                    enabled = !teamCyclist.isCaptain && teamCyclist.isActive
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (teamCyclist.isCaptain) "Capitao" else "Capitao")
                }

                // Toggle active
                OutlinedButton(
                    onClick = onToggleActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (teamCyclist.isActive) Icons.Default.EventBusy else Icons.Default.EventAvailable,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (teamCyclist.isActive) "Suplente" else "Ativar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remove button
            Button(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remover da Equipa")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextRaceCard(
    race: Race,
    stage: Stage? = null
) {
    val context = LocalContext.current

    // Determine display based on whether we have a stage
    val hasStage = stage != null && race.type != RaceType.ONE_DAY
    val headerText = if (hasStage) "Proxima Etapa" else "Proxima Corrida"
    val mainText = if (hasStage) {
        "${race.name} - ${stage!!.displayName} ${stage.stageTypeEmoji}"
    } else {
        race.name
    }
    val subText = if (hasStage && stage!!.startLocation.isNotBlank()) {
        "${stage.startLocation} → ${stage.finishLocation}"
    } else null

    Card(
        onClick = {
            race.profileUrl?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3949AB).copy(alpha = 0.1f)
        ),
        enabled = race.profileUrl != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFF3949AB),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = mainText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subText != null) {
                    Text(
                        text = subText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = race.displayDate,
                    fontSize = 12.sp,
                    color = Color(0xFF3949AB),
                    fontWeight = FontWeight.Medium
                )
                if (hasStage && stage!!.distanceDisplay.isNotBlank()) {
                    Text(
                        text = stage.distanceDisplay,
                        fontSize = 10.sp,
                        color = Color(0xFF3949AB).copy(alpha = 0.7f)
                    )
                }
                if (race.profileUrl != null) {
                    @Suppress("DEPRECATION")
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Abrir",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF3949AB).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun getCategoryColor(category: CyclistCategory): Color {
    return when (category) {
        CyclistCategory.CLIMBER -> CategoryClimber
        CyclistCategory.HILLS -> CategoryHills
        CyclistCategory.TT -> CategoryTT
        CyclistCategory.SPRINT -> CategorySprint
        CyclistCategory.GC -> CategoryGC
        CyclistCategory.ONEDAY -> CategoryOneday
    }
}

private fun getCategoryName(category: CyclistCategory): String {
    return when (category) {
        CyclistCategory.CLIMBER -> "Climber"
        CyclistCategory.HILLS -> "Puncher"
        CyclistCategory.TT -> "TT"
        CyclistCategory.SPRINT -> "Sprint"
        CyclistCategory.GC -> "GC"
        CyclistCategory.ONEDAY -> "Clássicas"
    }
}
