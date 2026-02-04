package com.ciclismo.portugal.presentation.fantasy.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.rules.CyclistEligibility
import com.ciclismo.portugal.domain.rules.FantasyGameRules
import com.ciclismo.portugal.presentation.ads.BannerAdView
import com.ciclismo.portugal.presentation.fantasy.market.components.TransferReviewCard
import com.ciclismo.portugal.presentation.theme.AppImages

// Cores por categoria de ciclista (6 categorias)
val CategoryClimber = Color(0xFFE74C3C)   // Vermelho
val CategoryHills = Color(0xFF9B59B6)     // Roxo
val CategoryTT = Color(0xFF3498DB)        // Azul
val CategorySprint = Color(0xFF2ECC71)    // Verde
val CategoryGC = Color(0xFFF39C12)        // Laranja
val CategoryOneday = Color(0xFF1ABC9C)    // Turquesa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onNavigateBack: () -> Unit,
    onCyclistClick: (String) -> Unit,
    onSignIn: () -> Unit = {},
    viewModel: MarketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val teamInfo by viewModel.teamInfo.collectAsState()
    val effectiveTeamInfo by viewModel.effectiveTeamInfo.collectAsState()
    val message by viewModel.message.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val showTeamConfirmation by viewModel.showTeamConfirmation.collectAsState()
    val hasPendingChanges by viewModel.hasPendingChanges.collectAsState()
    val pendingAdditions by viewModel.pendingAdditions.collectAsState()
    val pendingRemovals by viewModel.pendingRemovals.collectAsState()
    val isConfirming by viewModel.isConfirming.collectAsState()
    val showOnlyAffordable by viewModel.showOnlyAffordable.collectAsState()

    // Transfer tracking state
    val transferCount by viewModel.transferCount.collectAsState()
    val transferPenalty by viewModel.transferPenalty.collectAsState()
    val remainingFreeTransfers by viewModel.remainingFreeTransfers.collectAsState()
    val hasUnlimitedTransfers by viewModel.hasUnlimitedTransfers.collectAsState()
    val showTransferPenaltyWarning by viewModel.showTransferPenaltyWarning.collectAsState()
    val showWildcardDialog by viewModel.showWildcardDialog.collectAsState()

    // Use effective team info for display (includes pending changes)
    val displayTeamInfo = effectiveTeamInfo ?: teamInfo

    // State for restart wizard confirmation dialog

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with cycling image - increased height and better organization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // Background cycling image
            AsyncImage(
                model = AppImages.ROAD_CYCLING,
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
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Header content - title and actions only
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
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
                            text = "Mercado",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = { viewModel.syncCyclists() }) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sincronizar",
                            tint = Color.White
                        )
                    }
                }

                // Team summary in header (compact version)
                displayTeamInfo?.let { info ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format("%.1f", info.team.budget)}M",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${info.teamSize}/${FantasyGameRules.TEAM_SIZE}",
                            color = if (info.teamSize >= FantasyGameRules.TEAM_SIZE) Color(0xFFFFCD00) else Color.White,
                            fontSize = 14.sp
                        )
                        // Transfer info
                        if (hasUnlimitedTransfers) {
                            Text(
                                text = "WILDCARD",
                                color = Color(0xFF9C27B0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "$remainingFreeTransfers gratis",
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp
                            )
                        }
                        if (hasPendingChanges) {
                            val penaltyText = if (transferPenalty > 0) " (-${transferPenalty}pts)" else ""
                            Text(
                                text = "+${pendingAdditions.size}/-${pendingRemovals.size}$penaltyText",
                                color = if (transferPenalty > 0) Color(0xFFE57373) else Color(0xFFF39C12),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Search bar - moved outside header for better visibility
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text("Pesquisar ciclistas...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Limpar",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        // Category filter chips with countdown (current/required)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Todos" chip showing total remaining - uses effective state
                val totalCurrent = displayTeamInfo?.teamSize ?: 0
                val totalRequired = FantasyGameRules.TEAM_SIZE
                CategoryFilterChipWithCountdown(
                    text = "Todos",
                    current = totalCurrent,
                    required = totalRequired,
                    selected = selectedCategory == null,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.onCategorySelected(null) }
                )

                CyclistCategory.entries.forEach { category ->
                    val count = displayTeamInfo?.categoryCount?.get(category) ?: 0
                    val required = FantasyGameRules.CATEGORY_REQUIREMENTS[category] ?: 0

                    CategoryFilterChipWithCountdown(
                        text = getCategoryName(category),
                        current = count,
                        required = required,
                        selected = selectedCategory == category,
                        color = getCategoryColor(category),
                        onClick = { viewModel.onCategorySelected(category) }
                    )
                }
            }

            // Euro toggle button - shows only affordable cyclists
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (showOnlyAffordable) Color(0xFFFFCD00) else Color.Gray.copy(alpha = 0.3f)
                    )
                    .clickable { viewModel.toggleShowOnlyAffordable() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "€",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (showOnlyAffordable) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Content
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.syncCyclists() },
            modifier = Modifier.fillMaxSize()
        ) {
            when (uiState) {
                is MarketUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is MarketUiState.Empty -> {
                    EmptyMarketState(onSync = { viewModel.syncCyclists() })
                }

                is MarketUiState.Success -> {
                    val allCyclists = (uiState as MarketUiState.Success).cyclists
                    val isTeamComplete = (displayTeamInfo?.teamSize ?: 0) >= FantasyGameRules.TEAM_SIZE

                    // Filter out cyclists from full categories (unless team is complete or category is selected)
                    val cyclists = allCyclists.filter { cyclistWithEligibility ->
                        val category = cyclistWithEligibility.cyclist.category
                        val count = displayTeamInfo?.categoryCount?.get(category) ?: 0
                        val required = FantasyGameRules.CATEGORY_REQUIREMENTS[category] ?: 0
                        val isCategoryFull = count >= required

                        // Show all if team complete, otherwise filter full categories
                        isTeamComplete || !isCategoryFull || cyclistWithEligibility.isInEffectiveTeam || selectedCategory == category
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Banner Ad
                        item {
                            BannerAdView(modifier = Modifier.padding(bottom = 8.dp))
                        }

                        item {
                            val remaining = FantasyGameRules.TEAM_SIZE - (displayTeamInfo?.teamSize ?: 0)
                            Text(
                                text = if (remaining > 0) "Faltam $remaining ciclistas" else "Equipa completa!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (remaining > 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color(0xFF4CAF50)
                            )
                        }
                        items(cyclists, key = { it.cyclist.id }) { cyclistWithEligibility ->
                            CyclistCardWithActions(
                                cyclistWithEligibility = cyclistWithEligibility,
                                isAuthenticated = isAuthenticated,
                                hasTeam = displayTeamInfo != null,
                                isTeamComplete = isTeamComplete,
                                onBuy = { viewModel.buyCyclist(cyclistWithEligibility.cyclist) },
                                onSell = { viewModel.sellCyclist(cyclistWithEligibility.cyclist) },
                                onSignIn = onSignIn,
                                onClick = { onCyclistClick(cyclistWithEligibility.cyclist.id) }
                            )
                        }
                    }
                }

                is MarketUiState.Error -> {
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
                                text = (uiState as MarketUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.syncCyclists() }) {
                                Text("Tentar novamente")
                            }
                        }
                    }
                }
            }
        }
    }

    // Inline transfer review card (replaces 3 separate dialogs)
    TransferReviewCard(
        pendingAdditions = pendingAdditions.size,
        pendingRemovals = pendingRemovals.size,
        transferCount = transferCount,
        freeTransfers = remainingFreeTransfers,
        transferPenalty = transferPenalty,
        hasUnlimitedTransfers = hasUnlimitedTransfers,
        hasWildcard = teamInfo?.team?.hasWildcard == true,
        isTeamComplete = (displayTeamInfo?.teamSize ?: 0) >= FantasyGameRules.TEAM_SIZE,
        isConfirming = isConfirming,
        onConfirm = { viewModel.checkAndConfirmTeam() },
        onUseWildcard = { viewModel.activateWildcardFromMarket() },
        onDiscard = { viewModel.discardChanges() },
        modifier = Modifier.align(Alignment.BottomCenter)
    )

    }
    }
}

@Composable
private fun TeamInfoBar(
    budget: Double,
    teamSize: Int,
    categoryCount: Map<CyclistCategory, Int>,
    hasPendingChanges: Boolean = false,
    pendingAddCount: Int = 0,
    pendingRemoveCount: Int = 0,
    onDiscardChanges: () -> Unit = {}
) {
    Column {
        Surface(
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Budget
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${String.format("%.1f", budget)}M",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Orçamento",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )

                // Team size
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$teamSize/${FantasyGameRules.TEAM_SIZE}",
                        fontWeight = FontWeight.Bold,
                        color = if (teamSize >= FantasyGameRules.TEAM_SIZE) Color(0xFFFFCD00) else Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Equipa",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )

                // Missing cyclists count
                val missingCyclists = FantasyGameRules.TEAM_SIZE - teamSize
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (missingCyclists <= 0) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "$missingCyclists",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B),
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = if (missingCyclists <= 0) "Completo" else "Faltam",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Pending changes indicator
        if (hasPendingChanges) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFF39C12).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PendingActions,
                            contentDescription = null,
                            tint = Color(0xFFF39C12),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildString {
                                if (pendingAddCount > 0) append("+$pendingAddCount")
                                if (pendingAddCount > 0 && pendingRemoveCount > 0) append(" / ")
                                if (pendingRemoveCount > 0) append("-$pendingRemoveCount")
                                append(" pendentes")
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onDiscardChanges() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Descartar",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChipWithCountdown(
    text: String,
    current: Int,
    required: Int,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val isFull = current >= required
    val needsMore = current < required

    val statusColor = when {
        isFull -> Color(0xFF4CAF50)  // Green when complete
        else -> Color(0xFFFFCD00)     // Yellow when needs more
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text, fontSize = 12.sp)
                Text(
                    text = "$current/$required",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) color else statusColor
                )
                if (isFull) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CyclistCardWithActions(
    cyclistWithEligibility: CyclistWithEligibility,
    isAuthenticated: Boolean,
    hasTeam: Boolean,
    isTeamComplete: Boolean,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    onSignIn: () -> Unit,
    onClick: () -> Unit
) {
    val cyclist = cyclistWithEligibility.cyclist
    val eligibility = cyclistWithEligibility.eligibility
    val isEligible = cyclistWithEligibility.isEligible
    val isInEffectiveTeam = cyclistWithEligibility.isInEffectiveTeam
    val isPendingAdd = cyclistWithEligibility.isPendingAdd
    val isPendingRemove = cyclistWithEligibility.isPendingRemove

    val categoryColor = getCategoryColor(cyclist.category)

    // Fade non-team cyclists when team is complete
    val cardAlpha = if (isTeamComplete && !isInEffectiveTeam) 0.5f else 1f

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPendingAdd -> Color(0xFF4CAF50).copy(alpha = 0.12f) // Green tint for pending add
                isPendingRemove -> Color(0xFFE53935).copy(alpha = 0.08f) // Red tint for pending remove
                isInEffectiveTeam -> categoryColor.copy(alpha = 0.15f) // Category color tint like wizard
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        // Yellow/category border when selected (like wizard style)
        border = if (isInEffectiveTeam) {
            androidx.compose.foundation.BorderStroke(2.dp, categoryColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo - with top alignment crop
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (cyclist.photoUrl != null) {
                    AsyncImage(
                        model = cyclist.photoUrl,
                        contentDescription = cyclist.fullName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter // Top-down crop
                    )
                } else {
                    Text(
                        text = "${cyclist.firstName.firstOrNull() ?: ""}${cyclist.lastName.firstOrNull() ?: ""}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = categoryColor
                    )
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
                        text = cyclist.teamName.take(15),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Price
            Text(
                text = cyclist.displayPrice,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF006600),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Pending status badge
            if (isPendingAdd || isPendingRemove) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isPendingAdd) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = if (isPendingAdd) "+" else "-",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Buy/Sell button (smaller size: 24dp instead of 36dp)
            if (isInEffectiveTeam) {
                // Sell button (-)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .clickable { onSell() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Vender",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else if (!isAuthenticated) {
                // Sign in button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006600).copy(alpha = 0.2f))
                        .clickable { onSignIn() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Login,
                        contentDescription = "Entrar",
                        tint = Color(0xFF006600),
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else if (!hasTeam) {
                // Need to create team first
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF39C12).copy(alpha = 0.2f))
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Criar equipa",
                        tint = Color(0xFFF39C12),
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else if (isEligible) {
                // Buy button (+)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .clickable { onBuy() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Comprar",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                // Not eligible - show reason
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = getIneligibilityReason(eligibility),
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private fun getIneligibilityReason(eligibility: CyclistEligibility): String {
    return when (eligibility) {
        is CyclistEligibility.InsufficientBudget -> "Sem orçamento"
        is CyclistEligibility.TeamFull -> "Equipa cheia"
        is CyclistEligibility.TooManyFromSameTeam -> "Máx. equipa"
        is CyclistEligibility.CategoryFull -> "Máx. categoria"
        is CyclistEligibility.AlreadyInTeam -> "Na equipa"
        is CyclistEligibility.Eligible -> ""
    }
}

@Composable
private fun EmptyMarketState(onSync: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sem ciclistas disponíveis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sincroniza para carregar ciclistas do ProCyclingStats",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSync) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sincronizar Ciclistas")
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
