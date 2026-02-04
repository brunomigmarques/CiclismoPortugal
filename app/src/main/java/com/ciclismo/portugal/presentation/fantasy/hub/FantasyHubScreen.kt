package com.ciclismo.portugal.presentation.fantasy.hub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.R
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.User
import com.ciclismo.portugal.presentation.ads.BannerAdView
import com.ciclismo.portugal.presentation.theme.AppColors
import com.ciclismo.portugal.presentation.theme.AppImages
import com.google.android.gms.auth.api.signin.GoogleSignInClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FantasyHubScreen(
    user: User?, // Nullable for guest mode
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onNavigateToMarket: () -> Unit = {},
    onNavigateToMyTeam: () -> Unit = {},
    onNavigateToCreateTeam: () -> Unit = {},
    onNavigateToLeagues: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToAdminSync: (() -> Unit)? = null,
    onNavigateToProfile: () -> Unit = {},
    googleSignInClient: GoogleSignInClient? = null,
    viewModel: FantasyHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val upcomingRaceReminder by viewModel.upcomingRaceReminder.collectAsState()
    val activeRacesWithStages by viewModel.activeRacesWithStages.collectAsState()
    val isGuest = user == null
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user?.id) {
        user?.id?.let { viewModel.loadUserData(it) }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Race reminder dialog
    upcomingRaceReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRaceReminder() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Corrida Amanhã!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = reminder.raceName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = reminder.message,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (reminder.raceCount > 1) {
                        Text(
                            text = "+${reminder.raceCount - 1} outras corridas",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissRaceReminder()
                        onNavigateToMyTeam()
                    }
                ) {
                    Text("Ver Equipa")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRaceReminder() }) {
                    Text("OK, entendi")
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
        // Header with cycling background image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Background image - cycling peloton
            AsyncImage(
                model = "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=1200&q=80",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Header content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 36.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Jogo das",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Apostas",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!isGuest && uiState.hasTeam && uiState.team != null) {
                            Text(
                                text = uiState.team!!.teamName,
                                fontSize = 12.sp,
                                color = AppColors.Yellow
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isGuest) {
                            // Login button for guests
                            TextButton(
                                onClick = onSignIn,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Entrar")
                            }
                        } else {
                            // User avatar (clickable to profile)
                            IconButton(onClick = onNavigateToProfile) {
                                if (user!!.photoUrl != null) {
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = "Perfil",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "Perfil",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Stats row with background for better visibility
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (isGuest) {
                            StatItemEnhanced(label = "Orçamento", value = "100M")
                            StatItemEnhanced(label = "Ciclistas", value = "15")
                            StatItemEnhanced(label = "Ativos", value = "8")
                        } else {
                            StatItemEnhanced(label = "Pontos", value = uiState.displayPoints)
                            StatItemEnhanced(label = "Ranking", value = uiState.displayRank)
                            StatItemEnhanced(label = "Orçamento", value = uiState.displayBudget)
                        }
                    }
                }
            }
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Guest mode - show game rules and info
            if (isGuest) {
                item {
                    Text(
                        text = "Como Jogar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    GameRulesCard()
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explorar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.ShoppingCart,
                        title = "Explorar Mercado",
                        description = "Vê os ciclistas disponíveis e os seus preços",
                        onClick = onNavigateToMarket
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.Leaderboard,
                        title = "Ver Ligas",
                        description = "Vê o ranking global e ligas existentes",
                        onClick = onNavigateToLeagues
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Sign in prompt
                    SignInPromptCard(onSignIn = onSignIn)
                }
            }
            // Authenticated user with team
            else if (uiState.hasTeam) {
                item {
                    Text(
                        text = "A Minha Equipa",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    // My Team card - highlighted
                    ActionCard(
                        icon = Icons.Default.Groups,
                        title = "Ver Equipa",
                        description = "Gere a tua equipa, define o capitão e faz transferências",
                        onClick = onNavigateToMyTeam,
                        highlighted = true
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.ShoppingCart,
                        title = "Mercado",
                        description = "Compra e vende ciclistas para melhorar a tua equipa",
                        onClick = onNavigateToMarket
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.Leaderboard,
                        title = "Ligas",
                        description = "Vê o teu ranking e compete com outros jogadores",
                        onClick = onNavigateToLeagues
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.History,
                        title = "Historico",
                        description = "Vê os teus resultados por temporada",
                        onClick = onNavigateToHistory
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.Info,
                        title = "Regras do Jogo",
                        description = "Aprende como funciona o Fantasy Ciclismo",
                        onClick = onNavigateToRules
                    )
                }
            } else {
                // Authenticated user without team yet - simplified onboarding
                item {
                    // Welcome card for new players
                    NewPlayerWelcomeCard(
                        userName = user?.displayName?.split(" ")?.firstOrNull() ?: "Ciclista",
                        onCreateTeam = onNavigateToCreateTeam
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Antes de Começar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.ShoppingCart,
                        title = "Explorar Mercado",
                        description = "Vê os ciclistas disponíveis e seus preços",
                        onClick = onNavigateToMarket
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.Info,
                        title = "Como Jogar",
                        description = "Aprende as regras do Fantasy Ciclismo",
                        onClick = onNavigateToRules
                    )
                }
            }

            // Active Races section
            if (activeRacesWithStages.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Corridas Ativas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Find first upcoming (not yet started) race index
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val firstUpcomingIndex = activeRacesWithStages.indexOfFirst { it.race.startDate > today }

                items(activeRacesWithStages.size) { index ->
                    ActiveRaceCard(
                        raceWithStageInfo = activeRacesWithStages[index],
                        isFirstUpcoming = index == firstUpcomingIndex
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Em Breve",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                ComingSoonCard(
                    icon = Icons.Default.Timer,
                    title = "Pontuação em Tempo Real",
                    description = "Acompanha os pontos durante as corridas"
                )
            }

            // Banner Ad
            item {
                BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            }
        }
    }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StatItemEnhanced(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val containerColor = if (highlighted) {
        Color(0xFF006600).copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val iconContainerColor = if (highlighted) {
        Color(0xFF006600).copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val iconTint = if (highlighted) {
        Color(0xFF006600)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlighted) Color(0xFF006600) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (highlighted) Color(0xFF006600) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}


@Composable
private fun ComingSoonCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Em Breve",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun GameRulesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fantasy Ciclismo Portugal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006600)
            )

            RuleItem(
                icon = Icons.Default.AttachMoney,
                title = "Orçamento: 100M",
                description = "Começa com 100M. Mínimo é 0M (sem saldo negativo)"
            )

            RuleItem(
                icon = Icons.Default.Groups,
                title = "15 Ciclistas",
                description = "8 ativos que pontuam + 7 suplentes"
            )

            RuleItem(
                icon = Icons.Default.Category,
                title = "Categorias Obrigatórias",
                description = "GC, Escaladores, Sprinters, Contra-Relógio, Punchers, Clássicas"
            )

            RuleItem(
                icon = Icons.Default.Star,
                title = "Capitão x2",
                description = "O teu capitão ganha pontos a dobrar"
            )

            RuleItem(
                icon = Icons.Default.SwapHoriz,
                title = "2 Transferências Grátis",
                description = "Por mês. Transferências extra custam -4 pontos"
            )

            RuleItem(
                icon = Icons.Default.Paid,
                title = "Ganhar Dinheiro",
                description = "50M/corrida, 20M/etapa - só top 50% da liga ganha (promove competição!)"
            )

            RuleItem(
                icon = Icons.Default.EmojiEvents,
                title = "Pontuação Real",
                description = "Baseada em resultados reais de corridas WorldTour"
            )
        }
    }
}

@Composable
private fun RuleItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF006600),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NewPlayerWelcomeCard(
    userName: String,
    onCreateTeam: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF006600).copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome emoji
            Text(
                text = "🚴",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Bem-vindo, $userName!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006600)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cria a tua equipa de ciclismo com 100M de orçamento e compete com outros jogadores!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Quick facts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickFact("15", "Ciclistas")
                QuickFact("100M", "Orçamento")
                QuickFact("8", "Ativos")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Create team button - prominent
            Button(
                onClick = onCreateTeam,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006600)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Criar Minha Equipa",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickFact(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF006600)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SignInPromptCard(onSignIn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF006600).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = Color(0xFF006600),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Pronto para jogar?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006600)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Entra com a tua conta Google para criar a tua equipa e competir com outros jogadores",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006600)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registar Utilizador")
            }
        }
    }
}

@Composable
private fun ActiveRaceCard(
    raceWithStageInfo: RaceWithStageInfo,
    isFirstUpcoming: Boolean = false
) {
    val race = raceWithStageInfo.race
    val currentStage = raceWithStageInfo.currentStage
    val nextStage = raceWithStageInfo.nextStage

    // Determine if race has actually started
    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val hasStarted = race.startDate <= today
    val isActiveRace = hasStarted && !race.isFinished

    // Badge configuration based on race status
    val (badgeText, badgeColor, cardColor) = when {
        isActiveRace -> Triple("EM CURSO", Color(0xFFFF9800), Color(0xFFFF9800).copy(alpha = 0.1f))
        isFirstUpcoming -> Triple("PRÓXIMO", Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.1f))
        else -> Triple(race.displayDate.uppercase(), Color(0xFF2196F3), Color(0xFF2196F3).copy(alpha = 0.1f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Race header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = race.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = race.formattedDateRange,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Stage info for multi-stage races (only show for active races)
            if (raceWithStageInfo.isMultiStage && isActiveRace) {
                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                if (currentStage != null || nextStage != null) {
                    val currentStageNumber = currentStage?.stageNumber ?: (nextStage?.stageNumber?.minus(1) ?: 0)
                    val progress = currentStageNumber.toFloat() / raceWithStageInfo.totalStages.toFloat()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Etapa $currentStageNumber/${raceWithStageInfo.totalStages}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = badgeColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = badgeColor,
                            trackColor = badgeColor.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Current stage info
                if (currentStage != null) {
                    StageInfoRow(
                        label = "Hoje",
                        stage = currentStage,
                        isHighlighted = true,
                        highlightColor = badgeColor
                    )
                }

                // Next stage info
                if (nextStage != null && currentStage != nextStage) {
                    if (currentStage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    StageInfoRow(
                        label = "Proxima",
                        stage = nextStage,
                        isHighlighted = false,
                        highlightColor = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StageInfoRow(
    label: String,
    stage: com.ciclismo.portugal.domain.model.Stage,
    isHighlighted: Boolean,
    highlightColor: Color = Color(0xFFFF9800)
) {
    val backgroundColor = if (isHighlighted) {
        highlightColor.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stage emoji
        Text(
            text = stage.stageTypeEmoji,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) highlightColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Etapa ${stage.stageNumber} • ${stage.stageType.displayNamePt}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Route info
            if (stage.startLocation.isNotBlank() && stage.finishLocation.isNotBlank()) {
                Text(
                    text = "${stage.startLocation} → ${stage.finishLocation}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Distance
        if (stage.distanceDisplay.isNotBlank()) {
            Text(
                text = stage.distanceDisplay,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
