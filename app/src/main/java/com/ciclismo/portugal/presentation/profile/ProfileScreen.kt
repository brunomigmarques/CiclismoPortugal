package com.ciclismo.portugal.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.*
import com.ciclismo.portugal.presentation.theme.*

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToAdmin: (() -> Unit)? = null,
    onStravaLogin: (String) -> Unit = {},
    stravaAuthCode: String? = null,
    onStravaAuthCodeConsumed: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToResults: () -> Unit = {},
    onNavigateToRankings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val stravaState by viewModel.stravaState.collectAsState()

    // Get app version from package info
    val context = LocalContext.current
    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    // Handle Strava OAuth callback
    LaunchedEffect(stravaAuthCode) {
        if (stravaAuthCode != null) {
            viewModel.handleStravaCallback(stravaAuthCode)
            onStravaAuthCodeConsumed()
        }
    }

    var showCyclingTypeDialog by remember { mutableStateOf(false) }
    var showExperienceDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showGoalsDialog by remember { mutableStateOf(false) }

    // Dialogs
    if (showCyclingTypeDialog) {
        PreferenceDialog(
            title = "Tipo de Ciclismo",
            options = CyclingType.entries,
            selected = preferences.cyclingType,
            displayName = { "${it.emoji} ${it.displayName}" },
            onSelect = {
                viewModel.setCyclingType(it)
                showCyclingTypeDialog = false
            },
            onDismiss = { showCyclingTypeDialog = false }
        )
    }

    if (showExperienceDialog) {
        PreferenceDialog(
            title = "Nível de Experiência",
            options = ExperienceLevel.entries,
            selected = preferences.experienceLevel,
            displayName = { it.displayName },
            onSelect = {
                viewModel.setExperienceLevel(it)
                showExperienceDialog = false
            },
            onDismiss = { showExperienceDialog = false }
        )
    }

    if (showRegionDialog) {
        PreferenceDialog(
            title = "Região Favorita",
            options = Region.entries,
            selected = preferences.favoriteRegion,
            displayName = { it.displayName },
            onSelect = {
                viewModel.setFavoriteRegion(it)
                showRegionDialog = false
            },
            onDismiss = { showRegionDialog = false }
        )
    }

    if (showGoalsDialog) {
        GoalsDialog(
            selectedGoals = preferences.goals,
            onToggle = { viewModel.toggleGoal(it) },
            onDismiss = { showGoalsDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with background image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            // Background image
            AsyncImage(
                model = AppImages.PROFILE_HEADER,
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
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Header content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 40.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                }

                Column {
                    Text(
                        text = "Meu",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Perfil",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Account Section
            item {
                UserAccountCard(
                    uiState = uiState,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut
                )
            }

            // Preferences Section
            item {
                Text(
                    text = "Preferências",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                PreferenceCard(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    title = "Tipo de Ciclismo",
                    value = "${preferences.cyclingType.emoji} ${preferences.cyclingType.displayName}",
                    onClick = { showCyclingTypeDialog = true }
                )
            }

            item {
                PreferenceCard(
                    icon = Icons.Default.Speed,
                    title = "Nível de Experiência",
                    value = preferences.experienceLevel.displayName,
                    onClick = { showExperienceDialog = true }
                )
            }

            item {
                PreferenceCard(
                    icon = Icons.Default.LocationOn,
                    title = "Região Favorita",
                    value = preferences.favoriteRegion.displayName,
                    onClick = { showRegionDialog = true }
                )
            }

            item {
                PreferenceCard(
                    icon = Icons.Default.Flag,
                    title = "Objetivos",
                    value = if (preferences.goals.isEmpty()) {
                        "Nenhum selecionado"
                    } else {
                        preferences.goals.joinToString(" ") { it.emoji }
                    },
                    onClick = { showGoalsDialog = true }
                )
            }

            // Connected Services Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Serviços Ligados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                StravaConnectionCard(
                    stravaState = stravaState,
                    onConnect = { onStravaLogin(viewModel.getStravaAuthUrl()) },
                    onDisconnect = { viewModel.disconnectStrava() }
                )
            }

            // Event History Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Minhas Provas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHistory() },
                    shape = RoundedCornerShape(AppStyle.CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = AppColors.Green,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Histórico de Provas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Ver provas onde participaste",
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

            // My Race Results Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToResults() },
                    shape = RoundedCornerShape(AppStyle.CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = AppColors.Gold,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Meus Resultados",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Regista e consulta os teus resultados",
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

            // Local Rankings Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRankings() },
                    shape = RoundedCornerShape(AppStyle.CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = PortugueseGreen,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rankings Locais",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Consulta as classificacoes regionais",
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

            // Notifications Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notificações",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                NotificationSettingsCard(
                    settings = preferences.notificationSettings,
                    onToggleNewProvas = { viewModel.toggleNewProvasNotification(it) },
                    onToggleProvaReminders = { viewModel.toggleProvaReminders(it) },
                    onToggleRoad = { viewModel.toggleRoadNotifications(it) },
                    onToggleBTT = { viewModel.toggleBTTNotifications(it) },
                    onToggleGravel = { viewModel.toggleGravelNotifications(it) },
                    onToggleOnlyFavoriteRegion = { viewModel.toggleOnlyFavoriteRegion(it) },
                    onToggleFantasyPoints = { viewModel.toggleFantasyPointsNotification(it) },
                    onToggleFantasyRanking = { viewModel.toggleFantasyRankingNotification(it) },
                    onToggleFantasyRaces = { viewModel.toggleFantasyRaceNotification(it) },
                    onToggleDailyTip = { viewModel.toggleDailyTipNotification(it) }
                )
            }

            // Admin Section (only visible for admins)
            val currentState = uiState
            if (currentState is ProfileUiState.LoggedIn && currentState.user.isAdmin && onNavigateToAdmin != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Administração",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAdmin() },
                        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.Red.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = AppColors.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Painel de Administração",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Gerir ciclistas, corridas e dados",
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
            }

            // Privacy & Data Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Privacidade e Dados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                PrivacyCard(
                    onPrivacyPolicy = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://brunomigmarques.github.io/CiclismoPortugal/privacy-policy.html")
                        )
                        context.startActivity(intent)
                    },
                    onRequestDataDeletion = {
                        val userEmail = (uiState as? ProfileUiState.LoggedIn)?.user?.email ?: ""
                        val subject = "Pedido de Eliminacao de Conta - Ciclismo Portugal"
                        val body = """
                            Solicito a eliminacao da minha conta e todos os dados associados na app Ciclismo Portugal.

                            Email da conta: $userEmail

                            Por favor confirmem quando os dados forem eliminados.

                            Obrigado.
                        """.trimIndent()

                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:")
                            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("app.cyclingai@gmail.com"))
                            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                            putExtra(android.content.Intent.EXTRA_TEXT, body)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // App Info Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sobre a App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyle.CardCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Versão",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = appVersion)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Desenvolvido por Fabwind ©",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.Green
                            )
                        }
                    }
                }
            }

            // Footer spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun UserAccountCard(
    uiState: ProfileUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = AppStyle.CardElevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        when (uiState) {
            is ProfileUiState.Anonymous -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Anonymous avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Utilizador Anónimo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Inicia sessão para jogar Fantasy Cycling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.height(AppStyle.ButtonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Green),
                        shape = RoundedCornerShape(AppStyle.CardCornerRadius)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Sessão", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            is ProfileUiState.LoggedIn -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User photo
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (uiState.user.photoUrl != null) {
                            AsyncImage(
                                model = uiState.user.photoUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.user.displayName ?: "Ciclista",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.user.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.user.isAdmin) {
                            Surface(
                                color = AppColors.Red.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.Red,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Terminar sessão",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Green)
                }
            }
        }
    }
}

@Composable
private fun PreferenceCard(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.Green,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
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

@Composable
private fun <T> PreferenceDialog(
    title: String,
    options: List<T>,
    selected: T,
    displayName: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Green)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayName(option))
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
private fun GoalsDialog(
    selectedGoals: Set<UserGoal>,
    onToggle: (UserGoal) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Objetivos") },
        text = {
            LazyColumn {
                items(UserGoal.entries) { goal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(goal) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = goal in selectedGoals,
                            onCheckedChange = { onToggle(goal) },
                            colors = CheckboxDefaults.colors(checkedColor = AppColors.Green)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${goal.emoji} ${goal.displayName}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Concluído")
            }
        }
    )
}

@Composable
private fun NotificationSettingsCard(
    settings: NotificationSettings,
    onToggleNewProvas: (Boolean) -> Unit,
    onToggleProvaReminders: (Boolean) -> Unit,
    onToggleRoad: (Boolean) -> Unit,
    onToggleBTT: (Boolean) -> Unit,
    onToggleGravel: (Boolean) -> Unit,
    onToggleOnlyFavoriteRegion: (Boolean) -> Unit,
    onToggleFantasyPoints: (Boolean) -> Unit,
    onToggleFantasyRanking: (Boolean) -> Unit,
    onToggleFantasyRaces: (Boolean) -> Unit,
    onToggleDailyTip: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Events Section
            Text(
                text = "Provas e Eventos",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Green,
                fontWeight = FontWeight.SemiBold
            )

            NotificationToggle(
                title = "Novas provas",
                subtitle = "Avisar quando há novas provas",
                checked = settings.newProvasEnabled,
                onCheckedChange = onToggleNewProvas
            )

            NotificationToggle(
                title = "Lembretes de provas",
                subtitle = "Lembrar provas no calendário",
                checked = settings.provaRemindersEnabled,
                onCheckedChange = onToggleProvaReminders
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Filter by type
            Text(
                text = "Tipos de prova",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Green,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = settings.notifyRoadEvents,
                    onClick = { onToggleRoad(!settings.notifyRoadEvents) },
                    label = { Text("Estrada") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = settings.notifyBTTEvents,
                    onClick = { onToggleBTT(!settings.notifyBTTEvents) },
                    label = { Text("BTT") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = settings.notifyGravelEvents,
                    onClick = { onToggleGravel(!settings.notifyGravelEvents) },
                    label = { Text("Gravel") },
                    modifier = Modifier.weight(1f)
                )
            }

            NotificationToggle(
                title = "Só região favorita",
                subtitle = "Notificar apenas provas na tua região",
                checked = settings.notifyOnlyFavoriteRegion,
                onCheckedChange = onToggleOnlyFavoriteRegion
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Fantasy Section
            Text(
                text = "Fantasy Cycling",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Green,
                fontWeight = FontWeight.SemiBold
            )

            NotificationToggle(
                title = "Pontos ganhos",
                subtitle = "Avisar quando ganhas pontos",
                checked = settings.fantasyPointsEnabled,
                onCheckedChange = onToggleFantasyPoints
            )

            NotificationToggle(
                title = "Mudanças de ranking",
                subtitle = "Avisar quando sobes ou desces na liga",
                checked = settings.fantasyRankingEnabled,
                onCheckedChange = onToggleFantasyRanking
            )

            NotificationToggle(
                title = "Início de corridas",
                subtitle = "Lembrar antes das corridas começarem",
                checked = settings.fantasyRaceRemindersEnabled,
                onCheckedChange = onToggleFantasyRaces
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Daily Tip
            Text(
                text = "Conteúdo",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Green,
                fontWeight = FontWeight.SemiBold
            )

            NotificationToggle(
                title = "Dica do dia",
                subtitle = "Receber dica de ciclismo diária",
                checked = settings.dailyTipEnabled,
                onCheckedChange = onToggleDailyTip
            )
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Green
            )
        )
    }
}

// Strava brand color
private val StravaOrange = Color(0xFFFC4C02)

@Composable
private fun StravaConnectionCard(
    stravaState: StravaConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Strava icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StravaOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            when (stravaState) {
                is StravaConnectionState.Disconnected -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strava",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Conecta para ver as tuas atividades",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = StravaOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Ligar", fontWeight = FontWeight.SemiBold)
                    }
                }

                is StravaConnectionState.Connecting -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strava",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "A conectar...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = StravaOrange,
                        strokeWidth = 2.dp
                    )
                }

                is StravaConnectionState.Connected -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strava",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.Green,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stravaState.athlete.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Green
                            )
                        }
                    }
                    TextButton(
                        onClick = onDisconnect,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            "Desligar",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }

                is StravaConnectionState.Error -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strava",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Erro: ${stravaState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = StravaOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Tentar Novamente", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyCard(
    onPrivacyPolicy: () -> Unit,
    onRequestDataDeletion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Privacy Policy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPrivacyPolicy() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Policy,
                    contentDescription = null,
                    tint = AppColors.Green,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Politica de Privacidade",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Ver como os teus dados sao tratados",
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

            HorizontalDivider()

            // Request Data Deletion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestDataDeletion() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Eliminar Conta e Dados",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Solicitar eliminacao de todos os teus dados",
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
}
