package com.ciclismo.portugal.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.ciclismo.portugal.domain.model.LocalRanking
import com.ciclismo.portugal.domain.model.RankingPointsSystem
import com.ciclismo.portugal.domain.model.UserRaceType
import com.ciclismo.portugal.presentation.theme.AppImages
import com.ciclismo.portugal.presentation.theme.PortugueseGreen
import com.ciclismo.portugal.presentation.theme.PortugueseRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRankingsAdminScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRankingDetail: (String) -> Unit = {},
    viewModel: LocalRankingsAdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val rankings by viewModel.rankings.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<LocalRanking?>(null) }
    var raceTypeExpanded by remember { mutableStateOf(false) }
    var pointsSystemExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
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
                                Color(0xFFB71C1C).copy(alpha = 0.7f),
                                Color(0xFFD32F2F).copy(alpha = 0.85f)
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
                            text = "Rankings Locais",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Gerir rankings regionais",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = PortugueseGreen,
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Criar ranking",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${rankings.size} ranking(s) ativos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Content
        when (uiState) {
            is LocalRankingsAdminUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is LocalRankingsAdminUiState.Empty -> {
                EmptyRankingsContent(onCreateClick = { showCreateDialog = true })
            }

            is LocalRankingsAdminUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(rankings, key = { it.id }) { ranking ->
                        RankingCard(
                            ranking = ranking,
                            onClick = { onNavigateToRankingDetail(ranking.id) },
                            onToggleActive = { viewModel.toggleRankingActive(ranking) },
                            onDelete = { showDeleteDialog = ranking }
                        )
                    }
                }
            }

            is LocalRankingsAdminUiState.Error -> {
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
                            text = (uiState as LocalRankingsAdminUiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadRankings() }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }
        }
    }

    // Create Ranking Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Criar Ranking") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.rankingName.value,
                        onValueChange = { viewModel.rankingName.value = it },
                        label = { Text("Nome *") },
                        placeholder = { Text("Ex: Ranking Gravel Algarve") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = viewModel.rankingDescription.value,
                        onValueChange = { viewModel.rankingDescription.value = it },
                        label = { Text("Descricao") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // Race Type dropdown
                    ExposedDropdownMenuBox(
                        expanded = raceTypeExpanded,
                        onExpandedChange = { raceTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedRaceType.value?.displayName ?: "Todos os tipos",
                            onValueChange = { },
                            label = { Text("Tipo de Prova") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = raceTypeExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = raceTypeExpanded,
                            onDismissRequest = { raceTypeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos os tipos") },
                                onClick = {
                                    viewModel.selectedRaceType.value = null
                                    raceTypeExpanded = false
                                }
                            )
                            UserRaceType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        viewModel.selectedRaceType.value = type
                                        raceTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.selectedRegion.value,
                        onValueChange = { viewModel.selectedRegion.value = it },
                        label = { Text("Regiao") },
                        placeholder = { Text("Ex: Algarve") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Points System dropdown
                    ExposedDropdownMenuBox(
                        expanded = pointsSystemExpanded,
                        onExpandedChange = { pointsSystemExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedPointsSystem.value.displayName,
                            onValueChange = { },
                            label = { Text("Sistema de Pontos") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pointsSystemExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = pointsSystemExpanded,
                            onDismissRequest = { pointsSystemExpanded = false }
                        ) {
                            RankingPointsSystem.entries.forEach { system ->
                                DropdownMenuItem(
                                    text = { Text(system.displayName) },
                                    onClick = {
                                        viewModel.selectedPointsSystem.value = system
                                        pointsSystemExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Selected races
                    if (viewModel.selectedRaces.isNotEmpty()) {
                        Text(
                            "Provas selecionadas:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.selectedRaces) { race ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeRace(race.id) },
                                    label = { Text(race.name, maxLines = 1) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remover",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        "Nota: As provas podem ser adicionadas depois de criar o ranking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createRanking()
                        showCreateDialog = false
                    },
                    enabled = !isCreating && viewModel.rankingName.value.isNotBlank()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Criar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearForm()
                    showCreateDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { ranking ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Apagar Ranking") },
            text = {
                Text("Tens a certeza que queres apagar o ranking \"${ranking.name}\"? Esta acao nao pode ser desfeita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRanking(ranking.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PortugueseRed
                    )
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RankingCard(
    ranking: LocalRanking,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (ranking.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = if (ranking.isActive) PortugueseGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ranking.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!ranking.description.isNullOrBlank()) {
                        Text(
                            text = ranking.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!ranking.isActive) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Inativo", fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ranking.raceType?.let { type ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(type.displayName, fontSize = 10.sp) }
                    )
                }
                ranking.region?.let { region ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(region, fontSize = 10.sp) }
                    )
                }
                SuggestionChip(
                    onClick = { },
                    label = { Text("${ranking.selectedRaceIds.size} provas", fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Criado: ${dateFormat.format(Date(ranking.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            if (ranking.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (ranking.isActive) "Desativar" else "Ativar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Apagar",
                            tint = PortugueseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRankingsContent(onCreateClick: () -> Unit) {
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
            text = "Sem rankings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cria um ranking para acompanhar a performance dos ciclistas nas provas locais.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = PortugueseGreen
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Criar Ranking")
        }
    }
}

