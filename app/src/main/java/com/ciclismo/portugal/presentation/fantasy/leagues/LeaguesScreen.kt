package com.ciclismo.portugal.presentation.fantasy.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.snapshotFlow
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
import com.ciclismo.portugal.domain.model.League
import com.ciclismo.portugal.domain.model.LeagueMember
import com.ciclismo.portugal.domain.model.LeagueType
import com.ciclismo.portugal.presentation.theme.AppImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguesScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaguesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val myLeagues by viewModel.myLeagues.collectAsState()
    val availableLeagues by viewModel.availableLeagues.collectAsState()
    val selectedLeague by viewModel.selectedLeague.collectAsState()
    val leagueMembers by viewModel.leagueMembers.collectAsState()
    val message by viewModel.message.collectAsState()

    // Pagination state
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreMembers by viewModel.hasMoreMembers.collectAsState()
    val totalMemberCount by viewModel.totalMemberCount.collectAsState()
    val userPosition by viewModel.userPosition.collectAsState()
    val isLargeLeague by viewModel.isLargeLeague.collectAsState()

    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Show snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedLeague == null) {
                Column {
                    // Join with code FAB
                    SmallFloatingActionButton(
                        onClick = { showJoinDialog = true },
                        containerColor = Color(0xFF3949AB),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "Entrar com codigo",
                            tint = Color.White
                        )
                    }

                    // Create league FAB
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        containerColor = Color(0xFF006600)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Criar liga",
                            tint = Color.White
                        )
                    }
                }
            }
        }
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
                    .height(120.dp)
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
                            onClick = {
                                if (selectedLeague != null) {
                                    viewModel.clearSelectedLeague()
                                } else {
                                    onNavigateBack()
                                }
                            },
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
                        Text(
                            text = if (selectedLeague != null) selectedLeague!!.name else "Ligas",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Subtitle
                    Text(
                        text = if (selectedLeague != null) "Ranking e estatísticas" else "Compete com outros jogadores",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Content
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
            when {
                uiState is LeaguesUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState is LeaguesUiState.Error -> {
                    Text(
                        text = (uiState as LeaguesUiState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                selectedLeague != null -> {
                    LeagueDetailContent(
                        league = selectedLeague!!,
                        members = leagueMembers,
                        userPosition = userPosition,
                        totalMemberCount = totalMemberCount,
                        isLargeLeague = isLargeLeague,
                        isLoadingMore = isLoadingMore,
                        hasMoreMembers = hasMoreMembers,
                        onLoadMore = { viewModel.loadMoreMembers() },
                        onLeaveLeague = { viewModel.leaveLeague(selectedLeague!!.id) },
                        onDeleteLeague = { viewModel.deleteLeague(selectedLeague!!.id) }
                    )
                }

                else -> {
                    LeaguesListContent(
                        myLeagues = myLeagues,
                        availableLeagues = availableLeagues,
                        onLeagueClick = { viewModel.selectLeague(it) },
                        onJoinLeague = { viewModel.joinLeague(it.id) }
                    )
                }
            }
            }
        }
    }

    // Join with code dialog
    if (showJoinDialog) {
        JoinLeagueDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                viewModel.joinLeagueByCode(code)
                showJoinDialog = false
            }
        )
    }

    // Create league dialog
    if (showCreateDialog) {
        CreateLeagueDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPrivateLeague(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun LeaguesListContent(
    myLeagues: List<League>,
    availableLeagues: List<League>,
    onLeagueClick: (League) -> Unit,
    onJoinLeague: (League) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // My Leagues Section
        item {
            Text(
                text = "As Minhas Ligas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (myLeagues.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Ainda nao estas em nenhuma liga.\nEntra na Liga Portugal ou cria uma liga privada!",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(myLeagues) { league ->
                LeagueCard(
                    league = league,
                    isMember = true,
                    onClick = { onLeagueClick(league) },
                    onJoin = null
                )
            }
        }

        // Available Leagues Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ligas Disponiveis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        val notJoinedLeagues = availableLeagues.filter { available ->
            myLeagues.none { it.id == available.id }
        }

        if (notJoinedLeagues.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Ja estas em todas as ligas disponiveis!",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(notJoinedLeagues) { league ->
                LeagueCard(
                    league = league,
                    isMember = false,
                    onClick = { onLeagueClick(league) },
                    onJoin = { onJoinLeague(league) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeagueCard(
    league: League,
    isMember: Boolean,
    onClick: () -> Unit,
    onJoin: (() -> Unit)?
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (league.type) {
                LeagueType.GLOBAL -> Color(0xFF006600).copy(alpha = 0.1f)
                LeagueType.PRIVATE -> Color(0xFF7B1FA2).copy(alpha = 0.1f)
                LeagueType.REGIONAL -> Color(0xFF388E3C).copy(alpha = 0.1f)
                LeagueType.MONTHLY -> Color(0xFFF57C00).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // League icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (league.type) {
                            LeagueType.GLOBAL -> Color(0xFF006600)
                            LeagueType.PRIVATE -> Color(0xFF7B1FA2)
                            LeagueType.REGIONAL -> Color(0xFF388E3C)
                            LeagueType.MONTHLY -> Color(0xFFF57C00)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (league.type) {
                        LeagueType.GLOBAL -> Icons.Default.Public
                        LeagueType.PRIVATE -> Icons.Default.Lock
                        LeagueType.REGIONAL -> Icons.Default.LocationOn
                        LeagueType.MONTHLY -> Icons.Default.CalendarMonth
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = league.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${league.memberCount} membros",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (league.type == LeagueType.PRIVATE && league.code != null && isMember) {
                    Text(
                        text = "Codigo: ${league.code}",
                        fontSize = 11.sp,
                        color = Color(0xFF7B1FA2),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isMember) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Membro",
                        fontSize = 10.sp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (onJoin != null) {
                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF006600)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Entrar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LeagueDetailContent(
    league: League,
    members: List<LeagueMember>,
    userPosition: LeagueMember?,
    totalMemberCount: Int,
    isLargeLeague: Boolean,
    isLoadingMore: Boolean,
    hasMoreMembers: Boolean,
    onLoadMore: () -> Unit,
    onLeaveLeague: () -> Unit,
    onDeleteLeague: () -> Unit
) {
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Detect when user scrolls to bottom for pagination
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= members.size - 5 &&
                    hasMoreMembers &&
                    !isLoadingMore &&
                    isLargeLeague) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // League header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF006600)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (league.type) {
                            LeagueType.GLOBAL -> Icons.Default.EmojiEvents
                            LeagueType.PRIVATE -> Icons.Default.Group
                            LeagueType.REGIONAL -> Icons.Default.LocationOn
                            LeagueType.MONTHLY -> Icons.Default.CalendarMonth
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = league.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$totalMemberCount participantes",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (league.type == LeagueType.PRIVATE && league.code != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Codigo: ${league.code}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // User position card (for large leagues, show user's position separately)
        if (userPosition != null && isLargeLeague) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3949AB).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "A Tua Posicao",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3949AB)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Position badge
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3949AB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${userPosition.rank}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userPosition.teamName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${userPosition.points} pts",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            // Show rank change
                            if (userPosition.rankChange != 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (userPosition.isRising) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (userPosition.isRising) Color(0xFF4CAF50) else Color(0xFFE53935),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = userPosition.rankChangeDisplay,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (userPosition.isRising) Color(0xFF4CAF50) else Color(0xFFE53935)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ranking header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ranking",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isLargeLeague) {
                    Text(
                        text = "A mostrar ${members.size} de $totalMemberCount",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (members.isEmpty() && !isLoadingMore) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Ainda nao ha participantes nesta liga",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(members) { index, member ->
                RankingItem(
                    position = index + 1,
                    member = member
                )
            }

            // Loading more indicator
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Load more button (alternative to infinite scroll)
            if (hasMoreMembers && !isLoadingMore && isLargeLeague) {
                item {
                    OutlinedButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Carregar mais")
                    }
                }
            }
        }

        // Actions
        item {
            Spacer(modifier = Modifier.height(16.dp))

            if (league.type != LeagueType.GLOBAL) {
                OutlinedButton(
                    onClick = { showLeaveConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair da Liga")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Leave confirmation dialog
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            title = { Text("Sair da Liga") },
            text = { Text("Tens a certeza que queres sair de ${league.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveLeague()
                        showLeaveConfirmation = false
                    }
                ) {
                    Text("Sair", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Apagar Liga") },
            text = { Text("Tens a certeza que queres apagar ${league.name}? Esta acao e irreversivel.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteLeague()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Apagar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RankingItem(
    position: Int,
    member: LeagueMember
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (position) {
                1 -> Color(0xFFFFD700).copy(alpha = 0.15f)
                2 -> Color(0xFFC0C0C0).copy(alpha = 0.15f)
                3 -> Color(0xFFCD7F32).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
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
                    .clip(CircleShape)
                    .background(
                        when (position) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (position <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Team info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.teamName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${member.points} pts",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Rank change indicator
            if (member.rankChange != 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (member.isRising) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (member.isRising) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = member.rankChangeDisplay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (member.isRising) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinLeagueDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Entrar numa Liga", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Introduz o codigo de 6 caracteres da liga",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase() },
                    label = { Text("Codigo da Liga") },
                    placeholder = { Text("Ex: ABC123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code) },
                enabled = code.length == 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006600)
                )
            ) {
                Text("Entrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CreateLeagueDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Criar Liga Privada", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Cria uma liga privada e convida os teus amigos com o codigo",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Liga") },
                    placeholder = { Text("Ex: Liga dos Amigos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006600)
                )
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
