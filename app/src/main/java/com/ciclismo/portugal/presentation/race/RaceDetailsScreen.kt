package com.ciclismo.portugal.presentation.race

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.StageType
import com.ciclismo.portugal.presentation.ads.BannerAdView
import com.ciclismo.portugal.presentation.theme.AppColors
import com.ciclismo.portugal.presentation.theme.AppImages
import com.ciclismo.portugal.presentation.theme.AppStyle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailsScreen(
    raceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToFantasy: () -> Unit,
    viewModel: RaceDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val raceResults by viewModel.raceResults.collectAsState()
    val isLoadingResults by viewModel.isLoadingResults.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val isLoadingStages by viewModel.isLoadingStages.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    // Background cycling image
                    AsyncImage(
                        model = AppImages.WORLDTOUR_HEADER,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Green gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF006600).copy(alpha = 0.7f),
                                        Color(0xFF004400).copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text("WorldTour", color = Color.White)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is RaceDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RaceDetailsUiState.Success -> {
                RaceDetailsContent(
                    race = state.race,
                    results = raceResults,
                    isLoadingResults = isLoadingResults,
                    stages = stages,
                    isLoadingStages = isLoadingStages,
                    onNavigateToFantasy = onNavigateToFantasy,
                    onExportToCalendar = { race ->
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = Uri.parse("content://com.android.calendar/events")
                            putExtra("title", race.name)
                            putExtra("eventLocation", race.country)
                            putExtra("description", "Corrida WorldTour - ${race.category}\n\nTipo: ${getRaceTypeLabel(race.type)}\nEtapas: ${race.stages}")
                            putExtra("beginTime", race.startDate)
                            putExtra("endTime", race.endDate ?: (race.startDate + (6 * 60 * 60 * 1000)))
                            putExtra("allDay", false)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Não foi possível abrir o calendário",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onShare = { race ->
                        val shareText = buildString {
                            append("🚴 ${race.name}\n")
                            append("📅 ${race.formattedDateRange}\n")
                            append("📍 ${race.country}\n")
                            append("🏆 ${race.category}\n")
                            append("\nConfira no Ciclismo Portugal!")
                        }
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partilhar corrida"))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is RaceDetailsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onNavigateBack) {
                            Text("Voltar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RaceDetailsContent(
    race: Race,
    results: List<RaceResultWithCyclist>,
    isLoadingResults: Boolean,
    stages: List<Stage>,
    isLoadingStages: Boolean,
    onNavigateToFantasy: () -> Unit,
    onExportToCalendar: (Race) -> Unit,
    onShare: (Race) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Fantasy tag header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFD700)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF006600),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Jogo das Apostas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006600)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Race name
            Text(
                text = race.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Race type chip
            AssistChip(
                onClick = {},
                label = { Text(getRaceTypeLabel(race.type)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = getRaceTypeColor(race.type)
                )
            )

            HorizontalDivider()

            // Active status - shown first for all race types
            if (race.isActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔴 Corrida em curso!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                HorizontalDivider()
            }

            // For multi-stage races: Show STAGES FIRST, then DETAILS
            if (race.stages > 1) {
                // Stages section FIRST for multi-stage races
                // Stages header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Etapas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = AppColors.Green.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${race.stages} etapas",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.Green
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingStages) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AppColors.Green
                        )
                    }
                } else if (stages.isNotEmpty()) {
                    // Stages list
                    stages.forEach { stage ->
                        StageRow(
                            stage = stage,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    // No stages defined yet
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(AppStyle.CardCornerRadius)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Percurso das etapas ainda não disponível",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Details section (shown LAST for multi-stage races)
            Text(
                text = "Detalhes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Date
            DetailRow(
                icon = "📅",
                label = "Data",
                value = race.formattedDateRange
            )

            // Country
            DetailRow(
                icon = "📍",
                label = "País",
                value = race.country
            )

            // Category
            DetailRow(
                icon = "🏆",
                label = "Categoria",
                value = race.category
            )

            // Stages count (for multi-stage races)
            if (race.stages > 1) {
                DetailRow(
                    icon = "🚴",
                    label = "Etapas",
                    value = "${race.stages} etapas"
                )
            }

            // Race type
            DetailRow(
                icon = "🎯",
                label = "Tipo",
                value = getRaceTypeLabel(race.type)
            )

            // Results section
            if (isLoadingResults) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF006600)
                    )
                }
            } else if (results.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Results header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resultados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Color(0xFF006600).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${results.size} ciclistas",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF006600)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Team Standings (aggregated by team)
                val teamStandings = results
                    .filter { it.cyclistTeam.isNotBlank() }
                    .groupBy { it.cyclistTeam }
                    .map { (team, cyclists) ->
                        team to cyclists.sumOf { it.result.totalPoints }
                    }
                    .sortedByDescending { it.second }

                if (teamStandings.isNotEmpty()) {
                    Text(
                        text = "Pontuação por Equipa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    teamStandings.take(10).forEachIndexed { index, (team, points) ->
                        TeamStandingRow(
                            position = index + 1,
                            teamName = team,
                            totalPoints = points,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Resultados Individuais",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Results list
                results.forEach { resultWithCyclist ->
                    RaceResultRow(
                        resultWithCyclist = resultWithCyclist,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fantasy info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF006600).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Jogo das Apostas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006600)
                    )
                    Text(
                        text = "Esta corrida faz parte do jogo Fantasy. Monte a sua equipa e ganhe pontos com os resultados reais!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Jogo das Apostas Button (Primary)
                Button(
                    onClick = onNavigateToFantasy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF006600)
                    )
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Jogo das Apostas")
                }

                // Export to External Calendar Button
                OutlinedButton(
                    onClick = { onExportToCalendar(race) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF006600)
                    )
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar para Google/Outlook")
                }

                // Share Button
                OutlinedButton(
                    onClick = { onShare(race) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF006600)
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partilhar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner Ad
            BannerAdView(modifier = Modifier.padding(vertical = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getRaceTypeLabel(type: RaceType): String {
    return when (type) {
        RaceType.GRAND_TOUR -> "Grande Volta"
        RaceType.ONE_DAY -> "Clássica"
        RaceType.STAGE_RACE -> "Volta por Etapas"
    }
}

@Composable
private fun getRaceTypeColor(type: RaceType): Color {
    return when (type) {
        RaceType.GRAND_TOUR -> Color(0xFFFFD700).copy(alpha = 0.3f)  // Gold
        RaceType.ONE_DAY -> Color(0xFF006600).copy(alpha = 0.3f)    // Portuguese Green
        RaceType.STAGE_RACE -> Color(0xFF43A047).copy(alpha = 0.3f) // Green
    }
}

@Composable
private fun RaceResultRow(
    resultWithCyclist: RaceResultWithCyclist,
    modifier: Modifier = Modifier
) {
    val position = resultWithCyclist.result.position ?: 0
    val positionColor = when (position) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val positionTextColor = if (position in 1..3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Position badge
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = positionColor
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = positionTextColor
                    )
                }
            }

            // Cyclist photo
            if (!resultWithCyclist.cyclistPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = resultWithCyclist.cyclistPhotoUrl,
                    contentDescription = resultWithCyclist.cyclistName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder with initials
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFF006600).copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = resultWithCyclist.cyclistName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006600)
                        )
                    }
                }
            }

            // Cyclist info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = resultWithCyclist.cyclistName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = resultWithCyclist.cyclistTeam.ifBlank { "Equipa desconhecida" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (resultWithCyclist.cyclistTeam.isNotBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Points and jersey indicators
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Points - always show
                val totalPoints = resultWithCyclist.result.totalPoints
                Surface(
                    color = if (totalPoints > 0) Color(0xFF006600).copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$totalPoints pts",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPoints > 0) Color(0xFF006600)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Jersey indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (resultWithCyclist.result.isGcLeader) {
                        Text("🟡", style = MaterialTheme.typography.labelSmall)
                    }
                    if (resultWithCyclist.result.isMountainsLeader) {
                        Text("🔴", style = MaterialTheme.typography.labelSmall)
                    }
                    if (resultWithCyclist.result.isPointsLeader) {
                        Text("🟢", style = MaterialTheme.typography.labelSmall)
                    }
                    if (resultWithCyclist.result.isYoungLeader) {
                        Text("⚪", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StageRow(
    stage: Stage,
    modifier: Modifier = Modifier
) {
    val stageTypeColor = when (stage.stageType) {
        StageType.MOUNTAIN -> Color(0xFF8B4513) // Brown for mountains
        StageType.ITT, StageType.PROLOGUE -> Color(0xFF1565C0) // Blue for time trials
        StageType.TTT -> Color(0xFF1565C0)
        StageType.HILLY -> Color(0xFF43A047) // Green for hilly
        StageType.FLAT -> Color(0xFF757575) // Gray for flat
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(AppStyle.CardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stage number badge
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = stageTypeColor.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stage.stageNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = stageTypeColor
                        )
                    }
                }
            }

            // Stage info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stage.stageTypeEmoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stage.stageType.displayNamePt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Route: start → finish
                if (stage.startLocation.isNotBlank() || stage.finishLocation.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (stage.startLocation.isNotBlank()) {
                                append(stage.startLocation)
                            }
                            if (stage.startLocation.isNotBlank() && stage.finishLocation.isNotBlank()) {
                                append(" → ")
                            }
                            if (stage.finishLocation.isNotBlank()) {
                                append(stage.finishLocation)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Distance and date
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Distance
                if (stage.distance != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stage.distanceDisplay,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Date
                if (stage.dateString.isNotBlank()) {
                    Text(
                        text = stage.dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Rest day indicator
                if (stage.isRestDayAfter) {
                    Text(
                        text = "🛏️ Descanso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Processed indicator
                if (stage.isProcessed) {
                    Text(
                        text = "✓ Processada",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Green
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamStandingRow(
    position: Int,
    teamName: String,
    totalPoints: Int,
    modifier: Modifier = Modifier
) {
    val positionColor = when (position) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val positionTextColor = if (position in 1..3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Position badge
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = positionColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = positionTextColor
                    )
                }
            }

            // Team name
            Text(
                text = teamName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Total points
            Surface(
                color = Color(0xFF006600).copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "$totalPoints pts",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006600)
                )
            }
        }
    }
}
