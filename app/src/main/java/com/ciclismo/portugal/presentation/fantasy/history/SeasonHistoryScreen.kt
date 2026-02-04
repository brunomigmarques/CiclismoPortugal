package com.ciclismo.portugal.presentation.fantasy.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.SeasonSummary
import com.ciclismo.portugal.domain.model.TeamRaceHistoryItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonHistoryScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: SeasonHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableSeasons by viewModel.availableSeasons.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val seasonSummary by viewModel.seasonSummary.collectAsState()
    val raceHistory by viewModel.raceHistory.collectAsState()
    val selectedRaceDetail by viewModel.selectedRaceDetail.collectAsState()
    val isLoadingDetail by viewModel.isLoadingDetail.collectAsState()

    var showSeasonDropdown by remember { mutableStateOf(false) }

    // Race detail dialog
    selectedRaceDetail?.let { detail ->
        RaceDetailDialog(
            detail = detail,
            isLoading = isLoadingDetail,
            onDismiss = { viewModel.dismissRaceDetail() }
        )
    }

    LaunchedEffect(userId) {
        viewModel.loadHistory(userId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1565C0),
                            Color(0xFF1976D2)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Historico",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Resultados por temporada",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Season Selector
                Box {
                    OutlinedButton(
                        onClick = { showSeasonDropdown = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.5f)))
                        )
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$selectedSeason")
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = showSeasonDropdown,
                        onDismissRequest = { showSeasonDropdown = false }
                    ) {
                        availableSeasons.forEach { season ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Temporada $season")
                                        if (season == SeasonConfig.CURRENT_SEASON) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "(Atual)",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectSeason(season)
                                    showSeasonDropdown = false
                                },
                                leadingIcon = {
                                    if (season == selectedSeason) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Content
        when (uiState) {
            is SeasonHistoryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SeasonHistoryUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as SeasonHistoryUiState.Error).message,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is SeasonHistoryUiState.NoTeam -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.SportsScore,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sem equipa em ${(uiState as SeasonHistoryUiState.NoTeam).season}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nao participaste nesta temporada",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is SeasonHistoryUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Season Summary Card
                    item {
                        seasonSummary?.let { summary ->
                            SeasonSummaryCard(summary = summary)
                        }
                    }

                    // Race History Header
                    item {
                        Text(
                            text = "Resultados por Corrida",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Race History List
                    if (raceHistory.isEmpty()) {
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
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.DirectionsBike,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Ainda sem resultados",
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Os pontos aparecerao aqui apos cada corrida",
                                        fontSize = 12.sp,
                                        color = Color.Gray.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(raceHistory) { race ->
                            RaceHistoryItem(
                                race = race,
                                onClick = { viewModel.loadRaceDetails(race) }
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonSummaryCard(summary: SeasonSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1565C0).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Team name and season
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = summary.teamName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Temporada ${summary.season}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                // Total points badge
                Surface(
                    color = Color(0xFF1565C0),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${summary.totalPoints} pts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.EmojiEvents,
                    value = "${summary.racesParticipated}",
                    label = "Corridas",
                    color = Color(0xFFFFD700)
                )
                StatItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    value = "${summary.bestRacePoints}",
                    label = "Melhor",
                    color = Color(0xFF4CAF50)
                )
                summary.rank?.let { rank ->
                    StatItem(
                        icon = Icons.Default.Leaderboard,
                        value = "#$rank",
                        label = "Ranking",
                        color = Color(0xFF2196F3)
                    )
                }
            }

            // Best race info
            if (summary.bestRaceName != "-" && summary.racesParticipated > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Melhor resultado: ",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = summary.bestRaceName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RaceHistoryItem(
    race: TeamRaceHistoryItem,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Points indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = when {
                                race.pointsEarned >= 100 -> Color(0xFF4CAF50)
                                race.pointsEarned >= 50 -> Color(0xFF8BC34A)
                                race.pointsEarned >= 20 -> Color(0xFFFFC107)
                                race.pointsEarned > 0 -> Color(0xFFFF9800)
                                else -> Color.Gray
                            }.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${race.pointsEarned}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when {
                            race.pointsEarned >= 100 -> Color(0xFF4CAF50)
                            race.pointsEarned >= 50 -> Color(0xFF8BC34A)
                            race.pointsEarned >= 20 -> Color(0xFFFFC107)
                            race.pointsEarned > 0 -> Color(0xFFFF9800)
                            else -> Color.Gray
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = race.raceName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        // Grand Tour indicator
                        if (race.isLikelyGrandTour) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = "Grand Tour",
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (race.raceDate > 0) {
                            Text(
                                text = dateFormat.format(Date(race.raceDate)),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        if (race.isLikelyGrandTour) {
                            if (race.raceDate > 0) {
                                Text(
                                    text = " • ",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "Grand Tour",
                                fontSize = 11.sp,
                                color = Color(0xFFE91E63),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Arrow indicator for clickable
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Ver detalhes",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RaceDetailDialog(
    detail: RaceDetailState,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = detail.raceName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (detail.raceDate > 0) {
                    Text(
                        text = dateFormat.format(Date(detail.raceDate)),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Team and total points
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1565C0).copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = detail.teamName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${detail.totalPoints} pts",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1565C0)
                                    )
                                }

                                // Captain info
                                detail.captainName?.let { captain ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Capitao: $captain",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        if (detail.wasTripleCaptainActive) {
                                            Text(
                                                text = " (3x)",
                                                fontSize = 12.sp,
                                                color = Color(0xFF4CAF50),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Wildcards info
                                if (detail.wasBenchBoostActive) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.EventSeat,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Bench Boost ativo",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Show Grand Tour stages or cyclist breakdown
                    if (detail.isGrandTour && detail.stageResults.isNotEmpty()) {
                        // Grand Tour indicator
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Grand Tour - ${detail.stageResults.size} etapas",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE91E63)
                                )
                            }
                        }

                        // Stage results list
                        items(detail.stageResults) { stage ->
                            StageBreakdownItem(stage = stage)
                        }
                    } else if (detail.cyclistBreakdown.isNotEmpty()) {
                        // One-day race - show cyclist breakdown
                        item {
                            Text(
                                text = "Pontuacao por Ciclista",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Cyclists list
                        items(detail.cyclistBreakdown) { cyclist ->
                            CyclistBreakdownItem(cyclist = cyclist)
                        }
                    } else {
                        item {
                            Text(
                                text = "Detalhes da pontuacao nao disponiveis",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun CyclistBreakdownItem(cyclist: CyclistRaceDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (cyclist.isCaptain) Color(0xFFFFD700).copy(alpha = 0.1f)
                else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (cyclist.isCaptain) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Capitao",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Column {
                Text(
                    text = cyclist.name,
                    fontSize = 13.sp,
                    fontWeight = if (cyclist.isCaptain) FontWeight.Medium else FontWeight.Normal
                )
                if (cyclist.team.isNotEmpty()) {
                    Text(
                        text = cyclist.team,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Position or status (DNF, DNS, DSQ, DNP)
        val positionText = when {
            cyclist.position != null -> "#${cyclist.position}"
            cyclist.status.isNotBlank() -> cyclist.status.uppercase()
            else -> "DNP" // Fallback for old data without status field
        }
        val positionColor = when {
            cyclist.position != null -> Color.Gray
            cyclist.status.uppercase() in listOf("DNF", "DNS", "DSQ") -> Color(0xFFE57373) // Red for DNF/DNS/DSQ
            else -> Color(0xFF9E9E9E) // Gray for DNP
        }
        Text(
            text = positionText,
            fontSize = 11.sp,
            color = positionColor,
            modifier = Modifier.padding(end = 8.dp)
        )

        // Points
        Text(
            text = "${cyclist.points} pts",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = when {
                cyclist.points >= 20 -> Color(0xFF4CAF50)
                cyclist.points >= 10 -> Color(0xFF8BC34A)
                cyclist.points > 0 -> Color(0xFFFFC107)
                else -> Color.Gray
            }
        )
    }
}

@Composable
private fun StageBreakdownItem(stage: StageResultItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Stage number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = Color(0xFFE91E63).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stage.stageNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = stage.stageName.ifBlank { "Etapa ${stage.stageNumber}" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                // Show wildcards if active
                if (stage.wasTripleCaptainActive || stage.wasBenchBoostActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stage.wasTripleCaptainActive) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "3x ",
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        if (stage.wasBenchBoostActive) {
                            Icon(
                                Icons.Default.EventSeat,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "BB",
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }

        // Points
        Text(
            text = "${stage.pointsEarned} pts",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = when {
                stage.pointsEarned >= 50 -> Color(0xFF4CAF50)
                stage.pointsEarned >= 25 -> Color(0xFF8BC34A)
                stage.pointsEarned > 0 -> Color(0xFFFFC107)
                else -> Color.Gray
            }
        )
    }
}
