package com.ciclismo.portugal.presentation.fantasy.cyclist

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Login
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.presentation.ads.BannerAdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclistDetailScreen(
    onNavigateBack: () -> Unit,
    onSignIn: () -> Unit = {},
    viewModel: CyclistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cyclist by viewModel.cyclist.collectAsState()
    val myTeam by viewModel.myTeam.collectAsState()
    val isInMyTeam by viewModel.isInMyTeam.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Ciclista") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF006600),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is CyclistDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is CyclistDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (uiState as CyclistDetailUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is CyclistDetailUiState.Success -> {
                    cyclist?.let { cyclistData ->
                        CyclistDetailContent(
                            cyclist = cyclistData,
                            isInMyTeam = isInMyTeam,
                            remainingBudget = myTeam?.budget ?: 0.0,
                            hasTeam = myTeam != null,
                            isAuthenticated = isAuthenticated,
                            onBuy = { viewModel.buyCyclist() },
                            onSell = { viewModel.sellCyclist() },
                            onSignIn = onSignIn,
                            onOpenProfile = {
                                cyclistData.profileUrl?.let { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyclistDetailContent(
    cyclist: Cyclist,
    isInMyTeam: Boolean,
    remainingBudget: Double,
    hasTeam: Boolean,
    isAuthenticated: Boolean,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    onSignIn: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val categoryColor = getCategoryColor(cyclist.category)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF006600),
                            Color(0xFF3949AB),
                            Color(0xFF5C6BC0)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo - clickable to open ProCyclingStats profile
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .then(
                            if (cyclist.profileUrl != null) {
                                Modifier.clickable { onOpenProfile() }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (cyclist.photoUrl != null) {
                        AsyncImage(
                            model = cyclist.photoUrl,
                            contentDescription = cyclist.fullName,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter // Top-down crop
                        )
                    } else {
                        Text(
                            text = "${cyclist.firstName.firstOrNull() ?: ""}${cyclist.lastName.firstOrNull() ?: ""}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = cyclist.fullName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Team
                Text(
                    text = cyclist.teamName,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryColor.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = getCategoryName(cyclist.category),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price
                Text(
                    text = cyclist.displayPrice,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }

        // Stats section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Informacoes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // UCI Ranking
                    cyclist.uciRanking?.let { ranking ->
                        InfoRow(
                            icon = Icons.Default.EmojiEvents,
                            label = "Ranking UCI",
                            value = "#$ranking",
                            color = when {
                                ranking <= 10 -> Color(0xFFFFD700)
                                ranking <= 50 -> Color(0xFFC0C0C0)
                                ranking <= 100 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Speciality
                    cyclist.speciality?.let { speciality ->
                        InfoRow(
                            icon = Icons.AutoMirrored.Filled.DirectionsBike,
                            label = "Especialidade",
                            value = speciality
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Age
                    cyclist.age?.let { age ->
                        InfoRow(
                            icon = Icons.Default.Person,
                            label = "Idade",
                            value = "$age anos"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Nationality
                    if (cyclist.nationality.isNotEmpty()) {
                        InfoRow(
                            icon = Icons.Default.Flag,
                            label = "Nacionalidade",
                            value = cyclist.nationality
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Points
                    InfoRow(
                        icon = Icons.Default.Score,
                        label = "Pontos Fantasy",
                        value = "${cyclist.totalPoints} pts"
                    )
                }
            }

            // ProCyclingStats link
            if (cyclist.profileUrl != null) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3498DB)
                    )
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver perfil no ProCyclingStats")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buy/Sell button - different states for guests, authenticated without team, and authenticated with team
            if (!isAuthenticated) {
                // Guest user - prompt to sign in
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = Color(0xFF006600),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Entra para comprar ciclistas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF006600)
                        )
                        Text(
                            text = "Cria a tua equipa e compete no Fantasy Ciclismo!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onSignIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF006600)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registar Utilizador")
                        }
                    }
                }
            } else if (hasTeam) {
                // Authenticated user with team
                if (isInMyTeam) {
                    Button(
                        onClick = onSell,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vender (${cyclist.displayPrice})")
                    }
                } else {
                    val canAfford = remainingBudget >= cyclist.price

                    Button(
                        onClick = onBuy,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (canAfford) "Comprar (${cyclist.displayPrice})"
                            else "Orcamento insuficiente"
                        )
                    }

                    if (!canAfford) {
                        Text(
                            text = "Tens ${String.format("%.1f", remainingBudget)}M disponiveis",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Authenticated user without team
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF39C12).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF39C12)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cria uma equipa primeiro para comprar ciclistas",
                            fontSize = 14.sp,
                            color = Color(0xFFF39C12)
                        )
                    }
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
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

private fun getCategoryColor(category: CyclistCategory): Color {
    return when (category) {
        CyclistCategory.CLIMBER -> Color(0xFFE74C3C)
        CyclistCategory.HILLS -> Color(0xFF9B59B6)
        CyclistCategory.TT -> Color(0xFF3498DB)
        CyclistCategory.SPRINT -> Color(0xFF2ECC71)
        CyclistCategory.GC -> Color(0xFFF39C12)
        CyclistCategory.ONEDAY -> Color(0xFF1ABC9C)
    }
}

private fun getCategoryName(category: CyclistCategory): String {
    return when (category) {
        CyclistCategory.CLIMBER -> "Escalador"
        CyclistCategory.HILLS -> "Puncheur"
        CyclistCategory.TT -> "Contra-Relogio"
        CyclistCategory.SPRINT -> "Sprinter"
        CyclistCategory.GC -> "Geral"
        CyclistCategory.ONEDAY -> "Classicas"
    }
}
