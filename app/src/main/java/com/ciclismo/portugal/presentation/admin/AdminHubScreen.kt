package com.ciclismo.portugal.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.presentation.admin.FakeTeamsState
import com.ciclismo.portugal.presentation.theme.AppImages
import com.ciclismo.portugal.presentation.theme.PortugueseGreen
import com.ciclismo.portugal.presentation.theme.PortugueseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProvasAdmin: () -> Unit,
    onNavigateToLocalRankings: () -> Unit,
    onNavigateToCyclistAdmin: () -> Unit,
    onNavigateToSeasonAdmin: () -> Unit,
    onNavigateToRaceResultsAdmin: () -> Unit,
    onNavigateToPriceReduction: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: AdminSyncViewModel = hiltViewModel()
) {
    // AI Quota state
    val aiUsageStats by viewModel.aiUsageStats.collectAsState()
    val aiQuotaResetState by viewModel.aiQuotaResetState.collectAsState()

    // Fake Teams state
    val fakeTeamsState by viewModel.fakeTeamsState.collectAsState()

    // Load AI stats on screen load
    LaunchedEffect(Unit) {
        viewModel.loadAiUsageStats()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with cycling image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            // Background cycling image
            AsyncImage(
                model = AppImages.ROAD_CYCLING,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay with admin red tint
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

            // Header content
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
                    Column {
                        Text(
                            text = "Administração",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Gestão de dados da aplicação",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Admin warning badge
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acesso Restrito",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Section: Provas
            Text(
                "Provas e Eventos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            AdminMenuCard(
                title = "Gestão de Provas",
                description = "Ocultar, mostrar ou apagar provas de ciclismo. Limpar e re-sincronizar base de dados local.",
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                iconTint = PortugueseRed,
                onClick = onNavigateToProvasAdmin
            )

            AdminMenuCard(
                title = "Rankings Locais",
                description = "Criar e gerir rankings de provas locais. Selecionar provas e sistema de pontos.",
                icon = Icons.Default.Leaderboard,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToLocalRankings
            )

            // Section: Fantasy
            Text(
                "Fantasy Cycling",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            AdminMenuCard(
                title = "Gestão de Ciclistas",
                description = "Importar CSV, adicionar fotos, validar ciclistas, web scraping de equipas.",
                icon = Icons.Default.People,
                iconTint = PortugueseGreen,
                onClick = onNavigateToCyclistAdmin
            )

            AdminMenuCard(
                title = "Gestão de Temporadas",
                description = "Iniciar nova temporada, mudar de temporada, estatísticas por época.",
                icon = Icons.Default.CalendarMonth,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = onNavigateToSeasonAdmin
            )

            AdminMenuCard(
                title = "Corridas e Resultados",
                description = "Importar corridas WorldTour, processar resultados de etapas, calcular pontos fantasy.",
                icon = Icons.Default.EmojiEvents,
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = onNavigateToRaceResultsAdmin
            )

            AdminMenuCard(
                title = "Redução de Preços",
                description = "Reduzir preços de ciclistas por categoria ou globalmente. Ideal para promoções.",
                icon = Icons.Default.TrendingDown,
                iconTint = Color(0xFFE53935),
                onClick = onNavigateToPriceReduction
            )

            AdminMenuCard(
                title = "Notificações",
                description = "Enviar notificações para utilizadores de ligas específicas sobre provas, descontos, etc.",
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF9C27B0),
                onClick = onNavigateToNotifications
            )

            // Section: Debug
            Text(
                "Debug / Desenvolvimento",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            // AI Quota Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color(0xFF9B59B6).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color(0xFF9B59B6),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "AI Quota",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Gestão de limite diário de pedidos AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Stats
                    aiUsageStats?.let { stats ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Pedidos hoje", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${stats.dailyCount} / ${stats.dailyLimit}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (stats.dailyCount >= stats.dailyLimit) Color(0xFFE74C3C) else Color(0xFF27AE60)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Restantes", style = MaterialTheme.typography.bodySmall)
                                    Text("${stats.remainingToday}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Reset button
                    when (aiQuotaResetState) {
                        is AiQuotaResetState.Idle -> {
                            Button(
                                onClick = { viewModel.resetAiQuota() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Repor Quota Diária")
                            }
                        }
                        is AiQuotaResetState.Resetting -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A repor quota...")
                            }
                        }
                        is AiQuotaResetState.Success -> {
                            Text("Quota reposta com sucesso!", color = Color(0xFF27AE60), fontWeight = FontWeight.Bold)
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(2000)
                                viewModel.resetAiQuotaState()
                                viewModel.loadAiUsageStats()
                            }
                        }
                        is AiQuotaResetState.Error -> {
                            Text((aiQuotaResetState as AiQuotaResetState.Error).message, color = Color(0xFFE74C3C))
                        }
                    }
                }
            }

            // Fake Teams Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color(0xFF7B1FA2).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF7B1FA2),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Fake Teams (Liga Portugal)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Adicionar/remover equipas de teste na liga global",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // State-based content
                    when (fakeTeamsState) {
                        is FakeTeamsState.Idle -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.addFakeTeamsToGlobalLeague(236) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Criar 236", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.removeFakeTeamsFromGlobalLeague() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remover", fontSize = 12.sp)
                                }
                            }
                        }
                        is FakeTeamsState.Processing -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text((fakeTeamsState as FakeTeamsState.Processing).message)
                            }
                        }
                        is FakeTeamsState.Success -> {
                            Text(
                                (fakeTeamsState as FakeTeamsState.Success).message,
                                color = Color(0xFF27AE60),
                                fontWeight = FontWeight.Bold
                            )
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(2000)
                                viewModel.resetFakeTeamsState()
                            }
                        }
                        is FakeTeamsState.Error -> {
                            Text(
                                (fakeTeamsState as FakeTeamsState.Error).message,
                                color = Color(0xFFE74C3C)
                            )
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(3000)
                                viewModel.resetFakeTeamsState()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Warning footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Atenção: Alterações nesta área afetam todos os utilizadores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMenuCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconTint.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
