package com.ciclismo.portugal.presentation.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ciclismo.portugal.data.remote.firebase.SyncStatus
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.StageType
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.usecase.SeasonStats
import com.ciclismo.portugal.presentation.theme.AppImages
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.nio.charset.Charset
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminSyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val validatedCount by viewModel.validatedCount.collectAsState()
    val scrapedCyclists by viewModel.scrapedCyclists.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val photoUploadProgress by viewModel.photoUploadProgress.collectAsState()
    val processedRace by viewModel.processedRace.collectAsState()
    val nextRace by viewModel.nextRace.collectAsState()
    val fantasyPointsResult by viewModel.fantasyPointsResult.collectAsState()
    val videoSyncState by viewModel.videoSyncState.collectAsState()

    // Bot team state
    val botTeamState by viewModel.botTeamState.collectAsState()
    val botTeamCount by viewModel.botTeamCount.collectAsState()
    var showDeleteBotsConfirmation by remember { mutableStateOf(false) }

    // AI Quota state
    val aiUsageStats by viewModel.aiUsageStats.collectAsState()
    val aiQuotaResetState by viewModel.aiQuotaResetState.collectAsState()

    // Season management state
    val currentSeason by viewModel.currentSeason.collectAsState()
    val availableSeasons by viewModel.availableSeasons.collectAsState()
    val seasonStats by viewModel.seasonStats.collectAsState()
    var showNewSeasonDialog by remember { mutableStateOf(false) }

    // Admin management state
    val adminsList by viewModel.adminsList.collectAsState()
    val isLoadingAdmins by viewModel.isLoadingAdmins.collectAsState()
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var newAdminUid by remember { mutableStateOf("") }
    var newAdminEmail by remember { mutableStateOf("") }

    // All season races for dropdown selector
    val allSeasonRaces by viewModel.allSeasonRaces.collectAsState()

    // Stage schedule state
    val stageSchedule by viewModel.stageSchedule.collectAsState()
    val stageScheduleImportResult by viewModel.stageScheduleImportResult.collectAsState()
    var stageScheduleText by remember { mutableStateOf("") }
    var showStageScheduleInput by remember { mutableStateOf(false) }

    // Stage results processing state
    val currentStageNumber by viewModel.currentStageNumber.collectAsState()
    val currentStageType by viewModel.currentStageType.collectAsState()
    val stageProcessingResult by viewModel.stageProcessingResult.collectAsState()
    val processedStages by viewModel.processedStages.collectAsState()
    val currentJerseyHolders by viewModel.currentJerseyHolders.collectAsState()
    var stageResultsText by remember { mutableStateOf("") }
    var selectedStageNumber by remember { mutableIntStateOf(1) }
    var stageResultsDropdownExpanded by remember { mutableStateOf(false) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var cyclistToEdit by remember { mutableStateOf<Cyclist?>(null) }
    var showAddCyclistDialog by remember { mutableStateOf(false) }
    var showCsvFormatDialog by remember { mutableStateOf(false) }
    var showPhotoFormatDialog by remember { mutableStateOf(false) }

    // Web scraping state
    var teamUrl by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }
    val teams by viewModel.teams.collectAsState()
    val scrapingProgress by viewModel.scrapingProgress.collectAsState()

    // Website race scraping state
    var raceWebsiteUrl by remember { mutableStateOf("") }
    val scrapedRaceData by viewModel.scrapedRaceData.collectAsState()
    val websiteScrapingState by viewModel.websiteScrapingState.collectAsState()

    val context = LocalContext.current

    // CSV file picker launcher
    val csvFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                // Tenta ler com UTF-8 primeiro, depois outras codificacoes
                val bytes = inputStream?.readBytes() ?: return@let
                inputStream.close()

                // Tenta varias codificacoes
                val content = tryDecodeWithEncodings(bytes)
                val lines = content.lines()

                val cyclists = parseCsvToCyclists(lines)
                if (cyclists.isNotEmpty()) {
                    viewModel.addCyclistsFromCsv(cyclists)
                } else {
                    viewModel.showError("Nenhum ciclista encontrado no ficheiro. Verifica o formato (5 colunas: Nome,Equipa,Ranking,Link,Especialidade)")
                }
            } catch (e: Exception) {
                viewModel.showError("Erro ao ler ficheiro: ${e.message}")
            }
        }
    }

    // Photo picker launcher - allows multiple image selection (for local cyclists)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadPhotosForCyclists(uris, context)
        }
    }

    // Firebase photo picker - uploads to Firebase Storage and updates Firestore cyclists
    val firebasePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadPhotosForFirebaseCyclists(uris, context)
        }
    }

    // Races CSV file picker launcher
    val racesCsvFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes() ?: return@let
                inputStream.close()

                val content = tryDecodeWithEncodings(bytes)
                val lines = content.lines().filter { line -> line.isNotBlank() }

                if (lines.isNotEmpty()) {
                    viewModel.importRacesFromCsv(lines)
                } else {
                    viewModel.showError("Ficheiro CSV vazio")
                }
            } catch (e: Exception) {
                viewModel.showError("Erro ao ler ficheiro: ${e.message}")
            }
        }
    }

    var showRacesCsvFormatDialog by remember { mutableStateOf(false) }

    // Race results state
    var showRaceResultsDialog by remember { mutableStateOf(false) }
    var raceResultsText by remember { mutableStateOf("") }
    val parsedResults by viewModel.parsedResults.collectAsState()
    val currentRaceToProcess by viewModel.currentRaceToProcess.collectAsState()

    // Race selector dropdown state
    var raceDropdownExpanded by remember { mutableStateOf(false) }

    // Pre-fill race name from auto-detected race
    var raceName by remember { mutableStateOf("") }
    LaunchedEffect(currentRaceToProcess) {
        currentRaceToProcess?.let { race ->
            if (raceName.isBlank()) {
                raceName = race.name
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with cycling image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
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
                            text = "Fantasy Admin",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Gestão de dados do Fantasy",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Season Management Card (FIRST SECTION)
            item {
                SeasonManagementCard(
                    currentSeason = currentSeason,
                    availableSeasons = availableSeasons,
                    seasonStats = seasonStats,
                    adminsList = adminsList,
                    isLoadingAdmins = isLoadingAdmins,
                    onSeasonChange = { viewModel.switchSeason(it) },
                    onStartNewSeason = { showNewSeasonDialog = true },
                    onRefreshStats = { viewModel.loadSeasonStats() },
                    onRefreshAdmins = { viewModel.loadAdmins() },
                    onAddAdmin = { showAddAdminDialog = true },
                    onRemoveAdmin = { viewModel.removeAdmin(it) }
                )
            }

            // Status Card
            item {
                StatusCard(
                    syncStatus = syncStatus,
                    pendingCount = pendingCount,
                    validatedCount = validatedCount
                )
            }

            // AI Quota Management Section (moved to top for visibility)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color(0xFF9B59B6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI QUOTA (Debug)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Load stats on first render
                        LaunchedEffect(Unit) {
                            viewModel.loadAiUsageStats()
                        }

                        aiUsageStats?.let { stats ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Pedidos hoje:", fontSize = 14.sp)
                                        Text(
                                            "${stats.dailyCount} / ${stats.dailyLimit}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (stats.dailyCount >= stats.dailyLimit) Color(0xFFE74C3C) else Color(0xFF27AE60)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Restantes:", fontSize = 14.sp)
                                        Text("${stats.remainingToday}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (aiQuotaResetState) {
                            is AiQuotaResetState.Idle -> {
                                Button(
                                    onClick = { viewModel.resetAiQuota() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Repor Quota Diaria")
                                }
                            }
                            is AiQuotaResetState.Resetting -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A repor quota...")
                                }
                            }
                            is AiQuotaResetState.Success -> {
                                Text("Quota reposta!", color = Color(0xFF27AE60), fontWeight = FontWeight.Bold)
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
            }

            // Current State / Progress
            item {
                StateIndicator(uiState = uiState, uploadProgress = uploadProgress)
            }

            // CSV import section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Adicionar Ciclistas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Importa de CSV ou adiciona manualmente",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // CSV Import button (primary option)
                        Button(
                            onClick = { csvFilePicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF27AE60)
                            )
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar CSV")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Format info button
                            OutlinedButton(
                                onClick = { showCsvFormatDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Formato CSV", fontSize = 12.sp)
                            }

                            // Manual add button (secondary)
                            OutlinedButton(
                                onClick = { showAddCyclistDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manual", fontSize = 12.sp)
                            }
                        }

                    }
                }
            }

            // WorldTour Races section
            item {
                val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF006600).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFF006600),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Corridas WorldTour",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF006600)
                            )
                        }
                        Text(
                            text = "Importa corridas via CSV ou carrega calendario pre-definido",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // CSV Import button (primary option)
                        Button(
                            onClick = { racesCsvFilePicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState !is AdminSyncUiState.Uploading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF006600)
                            )
                        ) {
                            if (uiState is AdminSyncUiState.Uploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A carregar...")
                            } else {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importar CSV de Corridas")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Format info button
                            OutlinedButton(
                                onClick = { showRacesCsvFormatDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Formato CSV", fontSize = 12.sp)
                            }

                            // Default races button (secondary)
                            OutlinedButton(
                                onClick = { viewModel.uploadWorldTourRaces() },
                                modifier = Modifier.weight(1f),
                                enabled = uiState !is AdminSyncUiState.Uploading
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Default $currentYear", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ===========================================
            // ADD RACE FROM WEBSITE - New Section
            // ===========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE67E22).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFFE67E22),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Adicionar Prova de Website",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE67E22)
                            )
                        }
                        Text(
                            text = "Extrai informação de qualquer website de prova de ciclismo",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // URL Input
                        OutlinedTextField(
                            value = raceWebsiteUrl,
                            onValueChange = { raceWebsiteUrl = it },
                            label = { Text("URL do Website") },
                            placeholder = { Text("https://exemplo.com/prova") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null)
                            },
                            trailingIcon = {
                                if (raceWebsiteUrl.isNotBlank()) {
                                    IconButton(onClick = { raceWebsiteUrl = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpar")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fetch Button
                        Button(
                            onClick = { viewModel.scrapeRaceFromWebsite(raceWebsiteUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = raceWebsiteUrl.isNotBlank() &&
                                websiteScrapingState !is WebsiteScrapingState.Scraping &&
                                websiteScrapingState !is WebsiteScrapingState.Saving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE67E22)
                            )
                        ) {
                            when (websiteScrapingState) {
                                is WebsiteScrapingState.Scraping -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A extrair dados...")
                                }
                                is WebsiteScrapingState.Saving -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A guardar...")
                                }
                                else -> {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Extrair Dados")
                                }
                            }
                        }

                        // Show scraped data preview
                        scrapedRaceData?.let { data ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Dados Extraídos:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    ScrapedDataField("Nome", data.nome)
                                    ScrapedDataField("Data", formatDateFromTimestamp(data.data))
                                    ScrapedDataField("Local", data.local)
                                    ScrapedDataField("Tipo", data.tipo)
                                    ScrapedDataField("Distâncias", data.distancias)
                                    ScrapedDataField("Preço", data.preco)
                                    if (data.descricao.isNotBlank()) {
                                        ScrapedDataField("Descrição", data.descricao.take(100) + if (data.descricao.length > 100) "..." else "")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.clearScrapedRace() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancelar", fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.saveScrapedRace() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF27AE60)
                                            )
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Guardar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Show status messages
                        when (val state = websiteScrapingState) {
                            is WebsiteScrapingState.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    color = Color(0xFFE74C3C),
                                    fontSize = 12.sp
                                )
                            }
                            is WebsiteScrapingState.Saved -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✓ Prova '${state.raceName}' guardada com sucesso!",
                                    color = Color(0xFF27AE60),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Photo Upload section (only show when there are cyclists loaded)
            if (scrapedCyclists.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF9B59B6).copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF9B59B6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upload de Fotos",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9B59B6)
                                )
                            }
                            Text(
                                text = "Adiciona fotos aos ${scrapedCyclists.size} ciclistas carregados",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Upload photos button
                            Button(
                                onClick = { photoPicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState !is AdminSyncUiState.UploadingPhotos,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9B59B6)
                                )
                            ) {
                                if (uiState is AdminSyncUiState.UploadingPhotos) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A enviar fotos...")
                                } else {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Selecionar Fotos")
                                }
                            }

                            // Show photo upload progress
                            photoUploadProgress?.let { progress ->
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.percentage },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF9B59B6)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${progress.displayText} - ${progress.currentName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9B59B6)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Format info
                            OutlinedButton(
                                onClick = { showPhotoFormatDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Como nomear as fotos", fontSize = 12.sp)
                            }

                            // Show count of cyclists with photos
                            val cyclistsWithPhotos = scrapedCyclists.count { it.photoUrl != null }
                            if (cyclistsWithPhotos > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$cyclistsWithPhotos/${scrapedCyclists.size} ciclistas com foto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF27AE60)
                                )
                            }
                        }
                    }
                }
            }

            // Scraped cyclists section
            if (scrapedCyclists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ciclistas Extraidos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${scrapedCyclists.size} ciclistas | Toca para editar",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(
                            onClick = { viewModel.clearScrapedCyclists() }
                        ) {
                            Text("Limpar", color = Color(0xFFE74C3C))
                        }
                    }
                }

                // Action buttons for scraped cyclists
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.uploadToFirestore() },
                            modifier = Modifier.weight(1f),
                            enabled = scrapedCyclists.isNotEmpty() && uiState !is AdminSyncUiState.Uploading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF006600),
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState is AdminSyncUiState.Uploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload Firestore", color = Color.White)
                            }
                        }
                    }
                }

                // Cyclists list
                items(scrapedCyclists) { cyclist ->
                    CyclistEditableItem(
                        cyclist = cyclist,
                        onEditClick = { cyclistToEdit = cyclist },
                        onDeleteClick = { viewModel.removeCyclist(cyclist.id) }
                    )
                }
            }

            // Firestore actions
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Acoes Firestore",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Validate
            item {
                ActionCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Validar Ciclistas",
                    description = "Marcar $pendingCount ciclistas como validados",
                    buttonText = "Validar",
                    isLoading = uiState is AdminSyncUiState.Validating,
                    enabled = pendingCount > 0 && uiState !is AdminSyncUiState.Validating,
                    onClick = { viewModel.validateAllCyclists() },
                    buttonColor = Color(0xFF2ECC71)
                )
            }

            // Photo Upload for FIREBASE cyclists (show when there are validated cyclists)
            if (validatedCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF27AE60).copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF27AE60),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upload Fotos para Firebase",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF27AE60)
                                )
                            }
                            Text(
                                text = "Adiciona fotos aos $validatedCount ciclistas validados no Firebase",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Upload photos to Firebase button
                            Button(
                                onClick = { firebasePhotoPicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState !is AdminSyncUiState.UploadingPhotos,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF27AE60)
                                )
                            ) {
                                if (uiState is AdminSyncUiState.UploadingPhotos) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A enviar fotos...")
                                } else {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Selecionar Fotos")
                                }
                            }

                            // Show photo upload progress
                            photoUploadProgress?.let { progress ->
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.percentage },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF27AE60)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${progress.displayText} - ${progress.currentName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF27AE60)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Format info
                            OutlinedButton(
                                onClick = { showPhotoFormatDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Formato: apelido-nome-2026.jpg", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Race Results Import
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Importar Resultados de Corrida",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "Cola os resultados no formato: Rnk, Rider, Team, UCI, Pnt, Time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Race selector dropdown
                        Text(
                            text = "Selecionar Corrida",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = raceDropdownExpanded,
                            onExpandedChange = { raceDropdownExpanded = !raceDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentRaceToProcess?.name ?: "Selecionar corrida...",
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = raceDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF27AE60),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                supportingText = currentRaceToProcess?.let { race ->
                                    {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (race.isFinished) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF9E9E9E),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Terminada",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF9E9E9E)
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.DateRange,
                                                    contentDescription = null,
                                                    tint = Color(0xFF27AE60),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    race.formattedStartDate,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF27AE60)
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = raceDropdownExpanded,
                                onDismissRequest = { raceDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 350.dp)
                            ) {
                                if (allSeasonRaces.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Nenhuma corrida encontrada. Sincroniza as corridas primeiro.",
                                                color = Color(0xFFE57373)
                                            )
                                        },
                                        onClick = { raceDropdownExpanded = false }
                                    )
                                } else {
                                    allSeasonRaces.forEach { race ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = race.name,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 14.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = race.formattedStartDate,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (race.isFinished) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Terminada",
                                                            tint = Color(0xFF9E9E9E),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectRaceToProcess(race)
                                                raceName = race.name
                                                raceDropdownExpanded = false
                                            },
                                            leadingIcon = if (currentRaceToProcess?.id == race.id) {
                                                {
                                                    Icon(
                                                        Icons.Default.RadioButtonChecked,
                                                        contentDescription = null,
                                                        tint = Color(0xFF27AE60),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showRaceResultsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentRaceToProcess != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF27AE60)
                            )
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Colar Resultados")
                        }

                        // Show parsed results preview
                        if (parsedResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${parsedResults.size} resultados prontos",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF27AE60)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Preview first 5 results
                            parsedResults.take(5).forEach { result ->
                                val positionText = if (result.status.isNotBlank()) {
                                    result.status.uppercase()  // DNF, DNS, DSQ
                                } else {
                                    "#${result.rank}"
                                }
                                Text(
                                    text = "$positionText ${result.riderName} - ${result.fantasyPoints}pts",
                                    fontSize = 11.sp,
                                    color = if (result.status.isNotBlank())
                                        Color(0xFFE57373)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (parsedResults.size > 5) {
                                Text(
                                    text = "... e mais ${parsedResults.size - 5}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.clearRaceResults() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Limpar")
                                }
                                Button(
                                    onClick = { viewModel.applyRaceResults(raceName, parsedResults) },
                                    modifier = Modifier.weight(1f),
                                    enabled = currentRaceToProcess != null
                                ) {
                                    Text("Aplicar a ${currentRaceToProcess?.name?.take(15) ?: ""}...")
                                }
                            }
                        }
                    }
                }

                // Show race processing results
                if (processedRace != null || nextRace != null || fantasyPointsResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A2E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Resultado do Processamento",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Processed race
                            processedRace?.let { race ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2ECC71),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Corrida encerrada: ${race.name}",
                                        color = Color(0xFF2ECC71),
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Fantasy points result
                            fantasyPointsResult?.let { result ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF39C12),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Fantasy: ${result.teamsUpdated} equipas, ${result.totalPointsAwarded} pts",
                                        color = Color(0xFFF39C12),
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Next race
                            nextRace?.let { race ->
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Proxima Corrida",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = race.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3498DB),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${race.formattedStartDate} • ${race.country}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Stage Schedule Import (for Grand Tours)
            item {
                val isGrandTour = currentRaceToProcess?.type == RaceType.GRAND_TOUR ||
                    currentRaceToProcess?.type == RaceType.STAGE_RACE

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Route,
                                contentDescription = null,
                                tint = Color(0xFF9B59B6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Importar Etapas (Grand Tours)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "Cola o schedule de etapas no formato: Date, Day, Stage | Start - Finish, KM",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Show current race selection requirement
                        if (currentRaceToProcess == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF39C12),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Seleciona uma corrida primeiro (acima)",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF39C12)
                                )
                            }
                        } else {
                            // Show selected race
                            Text(
                                text = "Corrida: ${currentRaceToProcess?.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF27AE60)
                            )

                            if (!isGrandTour) {
                                Text(
                                    text = "⚠️ Esta corrida nao e um Grand Tour/Stage Race",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF39C12)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stage schedule text input
                        OutlinedTextField(
                            value = stageScheduleText,
                            onValueChange = { stageScheduleText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 250.dp),
                            placeholder = {
                                Text(
                                    "Date\tDay\tStage\tKM\n" +
                                    "16/02\tMonday\tStage 1 | City A - City B\t144\n" +
                                    "17/02\tTuesday\tStage 2 (ITT) | City B\t12\n" +
                                    "18/02\tWednesday\tRestday\n" +
                                    "...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            enabled = currentRaceToProcess != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9B59B6),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Parse button
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    stageScheduleText = ""
                                    viewModel.clearStageScheduleImportResult()
                                },
                                modifier = Modifier.weight(1f),
                                enabled = stageScheduleText.isNotBlank()
                            ) {
                                Text("Limpar")
                            }

                            Button(
                                onClick = {
                                    currentRaceToProcess?.let { race ->
                                        val stages = viewModel.parseStageScheduleText(stageScheduleText, race.id)
                                        if (stages.isNotEmpty()) {
                                            viewModel.importStageSchedule(stages)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = currentRaceToProcess != null && stageScheduleText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9B59B6)
                                )
                            ) {
                                Text("Importar Etapas")
                            }
                        }

                        // Show parsed stages preview
                        if (stageSchedule.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Preview: ${stageSchedule.size} etapas",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color(0xFF27AE60)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Column {
                                stageSchedule.take(5).forEach { stage ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row {
                                            Text(
                                                text = stage.stageType.emoji,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${stage.stageNumber}. ${stage.name}",
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 200.dp)
                                            )
                                        }
                                        Text(
                                            text = stage.distanceDisplay,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (stageSchedule.size > 5) {
                                    Text(
                                        text = "... e mais ${stageSchedule.size - 5} etapas",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Show import result
                        stageScheduleImportResult?.let { result ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (result.success) Color(0xFF27AE60) else Color(0xFFE74C3C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = result.message,
                                    fontSize = 12.sp,
                                    color = if (result.success) Color(0xFF27AE60) else Color(0xFFE74C3C)
                                )
                            }
                        }
                    }
                }
            }

            // Stage Results Processing (for Grand Tours)
            item {
                val hasStages = stageSchedule.isNotEmpty()
                val currentStage = stageSchedule.find { it.stageNumber == selectedStageNumber }
                val isStageProcessed = processedStages.contains(selectedStageNumber)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = Color(0xFF3498DB),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Processar Resultados de Etapa",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "Cola os resultados da etapa (posicao, nome, equipa, tempo)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Check if stages are imported
                        if (!hasStages) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF39C12),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Importa primeiro as etapas (secao acima)",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF39C12)
                                )
                            }
                        } else {
                            // Stage selector dropdown
                            Text(
                                text = "Selecionar Etapa",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            ExposedDropdownMenuBox(
                                expanded = stageResultsDropdownExpanded,
                                onExpandedChange = { stageResultsDropdownExpanded = !stageResultsDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = currentStage?.let {
                                        "${it.stageType.emoji} Etapa ${it.stageNumber}: ${it.name.take(30)}${if (it.name.length > 30) "..." else ""}"
                                    } ?: "Selecionar etapa...",
                                    onValueChange = { },
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageResultsDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3498DB),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    supportingText = if (isStageProcessed) {
                                        {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF27AE60),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Etapa ja processada",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF27AE60)
                                                )
                                            }
                                        }
                                    } else null
                                )

                                ExposedDropdownMenu(
                                    expanded = stageResultsDropdownExpanded,
                                    onDismissRequest = { stageResultsDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    stageSchedule.forEach { stage ->
                                        val isProcessed = processedStages.contains(stage.stageNumber)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = stage.stageType.emoji,
                                                            fontSize = 14.sp
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = "Etapa ${stage.stageNumber}",
                                                                fontWeight = FontWeight.Medium,
                                                                fontSize = 14.sp
                                                            )
                                                            Text(
                                                                text = stage.name.take(25),
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                    if (isProcessed) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Processada",
                                                            tint = Color(0xFF27AE60),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedStageNumber = stage.stageNumber
                                                viewModel.setStageNumber(stage.stageNumber)
                                                viewModel.setStageType(stage.stageType)
                                                stageResultsDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Show current stage info
                            currentStage?.let { stage ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${stage.stageType.displayNamePt} • ${stage.distanceDisplay}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (stage.isRestDayAfter) {
                                        Text(
                                            text = "😴 Dia de descanso apos",
                                            fontSize = 11.sp,
                                            color = Color(0xFF9B59B6)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stage results text input
                            OutlinedTextField(
                                value = stageResultsText,
                                onValueChange = { stageResultsText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 250.dp),
                                placeholder = {
                                    Text(
                                        "1, Pogacar, UAE Team Emirates, 4:32:15, GC\n" +
                                        "2, Vingegaard, Visma-LAB, +0:12\n" +
                                        "3, Evenepoel, Soudal-QS, +0:25, YNG\n" +
                                        "...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                enabled = hasStages,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3498DB),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Process buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        stageResultsText = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = stageResultsText.isNotBlank()
                                ) {
                                    Text("Limpar")
                                }

                                Button(
                                    onClick = {
                                        val parsedResults = viewModel.parseStageResultsText(stageResultsText)
                                        if (parsedResults.isNotEmpty()) {
                                            viewModel.applyStageResults(parsedResults)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = hasStages && stageResultsText.isNotBlank() && currentRaceToProcess != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3498DB)
                                    )
                                ) {
                                    Text("Processar Etapa")
                                }
                            }

                            // Show current jersey holders
                            currentJerseyHolders?.let { jerseys ->
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Camisolas Atuais",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    JerseyIndicator("🟡", jerseys.gcLeaderName ?: "-", "Geral")
                                    JerseyIndicator("🟢", jerseys.pointsLeaderName ?: "-", "Pontos")
                                    JerseyIndicator("🔴", jerseys.mountainsLeaderName ?: "-", "Montanha")
                                    JerseyIndicator("⚪", jerseys.youngLeaderName ?: "-", "Jovem")
                                }
                            }

                            // Show processing result
                            stageProcessingResult?.let { result ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF27AE60),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Etapa ${result.stageNumber} processada: ${result.resultsCount} ciclistas, ${result.pointsAwarded} pts",
                                        fontSize = 12.sp,
                                        color = Color(0xFF27AE60)
                                    )
                                }
                            }

                            // Show processed stages progress
                            if (processedStages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val totalStages = stageSchedule.size
                                val processedCount = processedStages.size

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Progresso",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "$processedCount/$totalStages etapas",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    LinearProgressIndicator(
                                        progress = { processedCount.toFloat() / totalStages.toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = Color(0xFF27AE60),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    // Show finalize button when all stages are processed
                                    if (processedCount == totalStages && totalStages > 0) {
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF27AE60).copy(alpha = 0.15f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.EmojiEvents,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFD700),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Todas as etapas processadas!",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF27AE60)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = "Clica para aplicar bonus de Classificacao Geral e finalizar a corrida",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Button(
                                                    onClick = { viewModel.finalizeGrandTour() },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = uiState !is AdminSyncUiState.Uploading,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF27AE60)
                                                    )
                                                ) {
                                                    if (uiState is AdminSyncUiState.Uploading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp,
                                                            color = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.EmojiEvents,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Finalizar Grand Tour")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Delete
            item {
                ActionCard(
                    icon = Icons.Default.Delete,
                    title = "Apagar Todos",
                    description = "Remove todos os ciclistas do Firestore",
                    buttonText = "Apagar",
                    isLoading = uiState is AdminSyncUiState.Deleting,
                    enabled = (pendingCount > 0 || validatedCount > 0) && uiState !is AdminSyncUiState.Deleting,
                    onClick = { showDeleteConfirmation = true },
                    buttonColor = Color(0xFFE74C3C)
                )
            }

            // Clear image cache
            item {
                ActionCard(
                    icon = Icons.Default.Cached,
                    title = "Limpar Cache Fotos",
                    description = "Remove fotos guardadas localmente para libertar espaco",
                    buttonText = "Limpar",
                    isLoading = false,
                    enabled = true,
                    onClick = { viewModel.clearImageCache(context) },
                    buttonColor = Color(0xFFF39C12)
                )
            }

            // Video Sync Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF9B59B6)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sincronizar Videos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Atualiza videos do YouTube para Firestore",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Show sync state
                        when (val state = videoSyncState) {
                            is VideoSyncState.Idle -> {
                                Button(
                                    onClick = { viewModel.forceVideoSync() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF9B59B6)
                                    )
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizar Agora")
                                }
                            }
                            is VideoSyncState.Syncing -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF9B59B6)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("A sincronizar videos...")
                                }
                            }
                            is VideoSyncState.Success -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF27AE60).copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF27AE60)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${state.count} videos sincronizados!",
                                            color = Color(0xFF27AE60),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.resetVideoSyncState() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sincronizar Novamente")
                                }
                            }
                            is VideoSyncState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE74C3C).copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = Color(0xFFE74C3C)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = state.message,
                                            color = Color(0xFFE74C3C),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.forceVideoSync() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Tentar Novamente")
                                }
                            }
                        }
                    }
                }
            }

            // ========== Bot Teams Section ==========
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF16A085).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color(0xFF16A085),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Equipas Bot (Competicao)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Gera 236 equipas ficticias para rankings",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Current bot count
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Equipas bot atuais:",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$botTeamCount / 236",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (botTeamCount >= 236) Color(0xFF27AE60) else Color(0xFFF39C12)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bot state
                        when (val state = botTeamState) {
                            is BotTeamState.Idle -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.generateBotTeams() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF16A085)
                                        ),
                                        enabled = botTeamCount < 236
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gerar Bots")
                                    }
                                    if (botTeamCount > 0) {
                                        OutlinedButton(
                                            onClick = { showDeleteBotsConfirmation = true },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFE74C3C)
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Apagar")
                                        }
                                    }
                                }
                            }
                            is BotTeamState.Generating -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { state.progress / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF16A085)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${state.progress}%",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A085)
                                    )
                                }
                            }
                            is BotTeamState.Deleting -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFFE74C3C)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("A apagar equipas bot...")
                                }
                            }
                            is BotTeamState.Success -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF27AE60).copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF27AE60)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${state.count} equipas bot criadas!",
                                            color = Color(0xFF27AE60),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.resetBotState() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("OK")
                                }
                            }
                            is BotTeamState.Deleted -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF39C12).copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFFF39C12)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${state.count} equipas bot apagadas",
                                            color = Color(0xFFF39C12),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.resetBotState() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("OK")
                                }
                            }
                            is BotTeamState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE74C3C).copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = Color(0xFFE74C3C)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = state.message,
                                            color = Color(0xFFE74C3C),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.resetBotState() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Tentar Novamente")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Delete bots confirmation dialog
    if (showDeleteBotsConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteBotsConfirmation = false },
            title = { Text("Apagar Equipas Bot") },
            text = { Text("Tens a certeza que queres apagar todas as $botTeamCount equipas bot? Esta acao nao pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBotTeams()
                        showDeleteBotsConfirmation = false
                    }
                ) {
                    Text("Apagar", color = Color(0xFFE74C3C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBotsConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar") },
            text = { Text("Tens a certeza que queres apagar todos os ciclistas do Firestore? Esta acao nao pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllCyclists()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Apagar", color = Color(0xFFE74C3C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Edit cyclist dialog
    cyclistToEdit?.let { cyclist ->
        EditCyclistDialog(
            cyclist = cyclist,
            onDismiss = { cyclistToEdit = null },
            onSave = { updatedCyclist ->
                viewModel.updateCyclist(updatedCyclist)
                cyclistToEdit = null
            }
        )
    }

    // Add cyclist manually dialog
    if (showAddCyclistDialog) {
        AddCyclistManuallyDialog(
            onDismiss = { showAddCyclistDialog = false },
            onAdd = { cyclist ->
                viewModel.addCyclistManually(cyclist)
                showAddCyclistDialog = false
            }
        )
    }

    // CSV format info dialog
    if (showCsvFormatDialog) {
        CsvFormatInfoDialog(
            onDismiss = { showCsvFormatDialog = false }
        )
    }

    // Photo format info dialog
    if (showPhotoFormatDialog) {
        PhotoFormatInfoDialog(
            onDismiss = { showPhotoFormatDialog = false }
        )
    }

    // Races CSV format info dialog
    if (showRacesCsvFormatDialog) {
        RacesCsvFormatInfoDialog(
            onDismiss = { showRacesCsvFormatDialog = false }
        )
    }

    // Race Results dialog
    if (showRaceResultsDialog) {
        AlertDialog(
            onDismissRequest = { showRaceResultsDialog = false },
            title = { Text("Importar Resultados") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Show auto-detected race (read-only)
                    if (currentRaceToProcess != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF27AE60).copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF27AE60),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Resultados para:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = currentRaceToProcess!!.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Cola os resultados abaixo (formato: Rnk, Rider, Team, UCI, Pnt, Time)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = raceResultsText,
                        onValueChange = { raceResultsText = it },
                        label = { Text("Resultados") },
                        placeholder = { Text("1\tAndresen Tobias Lund\nDecathlon...\t400\t225\t4:15:25") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 20
                    )

                    if (parsedResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${parsedResults.size} resultados encontrados",
                            color = Color(0xFF27AE60),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            if (raceResultsText.isNotBlank()) {
                                viewModel.parseRaceResults(raceResultsText)
                            }
                        }
                    ) {
                        Text("Analisar")
                    }
                    TextButton(
                        onClick = {
                            showRaceResultsDialog = false
                            raceResultsText = ""
                        }
                    ) {
                        Text("Fechar")
                    }
                }
            }
        )
    }

    // New Season Dialog
    if (showNewSeasonDialog) {
        val nextSeason = currentSeason + 1
        AlertDialog(
            onDismissRequest = { showNewSeasonDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar Nova Temporada")
                }
            },
            text = {
                Column {
                    Text(
                        "Tens a certeza que queres iniciar a temporada $nextSeason?",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Esta acao ira:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Criar nova estrutura no Firestore\n" +
                        "• Criar a Liga Portugal $nextSeason\n" +
                        "• Mudar para a temporada $nextSeason\n" +
                        "• Os dados de $currentSeason continuam acessiveis",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sera necessario carregar novos ciclistas e corridas para a nova temporada.",
                        fontSize = 12.sp,
                        color = Color(0xFFF39C12),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startNewSeason(nextSeason)
                        showNewSeasonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C)
                    )
                ) {
                    Text("Iniciar Temporada $nextSeason")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewSeasonDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add Admin Dialog
    if (showAddAdminDialog) {
        AlertDialog(
            onDismissRequest = { showAddAdminDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Administrador")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Para adicionar um admin, obtem o UID do utilizador no Firebase Console > Authentication > Users",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = newAdminUid,
                        onValueChange = { newAdminUid = it },
                        label = { Text("UID do Firebase") },
                        placeholder = { Text("Ex: abc123xyz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        label = { Text("Email") },
                        placeholder = { Text("user@email.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addAdmin(newAdminUid, newAdminEmail)
                        showAddAdminDialog = false
                        newAdminUid = ""
                        newAdminEmail = ""
                    },
                    enabled = newAdminUid.isNotBlank() && newAdminEmail.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAdminDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SeasonManagementCard(
    currentSeason: Int,
    availableSeasons: List<Int>,
    seasonStats: SeasonStats?,
    adminsList: List<AdminInfo>,
    isLoadingAdmins: Boolean,
    onSeasonChange: (Int) -> Unit,
    onStartNewSeason: () -> Unit,
    onRefreshStats: () -> Unit,
    onRefreshAdmins: () -> Unit,
    onAddAdmin: () -> Unit,
    onRemoveAdmin: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAdminsSection by remember { mutableStateOf(true) }  // Expanded by default

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFB71C1C).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gestao de Temporada",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB71C1C)
                        )
                        Text(
                            text = "Temporada atual: $currentSeason",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(onClick = onRefreshStats) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Atualizar",
                        tint = Color(0xFFB71C1C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Season selector dropdown
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB71C1C)
                    )
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Temporada $currentSeason")
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableSeasons.forEach { season ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Temporada $season")
                                    if (season == currentSeason) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "(atual)",
                                            fontSize = 12.sp,
                                            color = Color(0xFF27AE60)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSeasonChange(season)
                                expanded = false
                            },
                            leadingIcon = {
                                if (season == currentSeason) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF27AE60)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Season statistics
            if (seasonStats != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Estatisticas da Temporada $currentSeason",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SeasonStatItem(
                        label = "Ciclistas",
                        value = seasonStats.cyclistsCount.toString(),
                        icon = Icons.AutoMirrored.Filled.DirectionsBike
                    )
                    SeasonStatItem(
                        label = "Corridas",
                        value = seasonStats.racesCount.toString(),
                        icon = Icons.Default.EmojiEvents
                    )
                    SeasonStatItem(
                        label = "Equipas",
                        value = seasonStats.teamsCount.toString(),
                        icon = Icons.Default.Groups
                    )
                    SeasonStatItem(
                        label = "Ligas",
                        value = seasonStats.leaguesCount.toString(),
                        icon = Icons.Default.Leaderboard
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start new season button
            Button(
                onClick = onStartNewSeason,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB71C1C)
                )
            ) {
                Icon(Icons.Default.NewReleases, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar Nova Temporada (${currentSeason + 1})")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFB71C1C).copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Admin Management Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdminsSection = !showAdminsSection },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gestao de Administradores",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "${adminsList.size} admin(s) registado(s)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    if (showAdminsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0)
                )
            }

            if (showAdminsSection) {
                Spacer(modifier = Modifier.height(12.dp))

                // Admin list
                if (isLoadingAdmins) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF9C27B0)
                        )
                    }
                } else if (adminsList.isEmpty()) {
                    Text(
                        text = "Nenhum admin registado no Firestore",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    adminsList.forEach { admin ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = admin.email,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "UID: ${admin.uid.take(12)}...",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveAdmin(admin.uid) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.RemoveCircle,
                                    contentDescription = "Remover",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefreshAdmins,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atualizar", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onAddAdmin,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonStatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFB71C1C).copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB71C1C)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StatusCard(
    syncStatus: SyncStatus?,
    pendingCount: Int,
    validatedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estado Firestore",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(
                    label = "Validados",
                    value = validatedCount.toString(),
                    color = Color(0xFF2ECC71)
                )
                StatBox(
                    label = "Pendentes",
                    value = pendingCount.toString(),
                    color = Color(0xFFF39C12)
                )
                StatBox(
                    label = "Total",
                    value = (validatedCount + pendingCount).toString(),
                    color = Color(0xFF006600)
                )
            }

            if (syncStatus != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val lastSync = dateFormat.format(Date(syncStatus.lastSyncTimestamp))

                Text(
                    text = "Ultimo sync: $lastSync",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StateIndicator(uiState: AdminSyncUiState, uploadProgress: UploadProgress? = null) {
    val (message, color, icon) = when (uiState) {
        is AdminSyncUiState.Initial -> Triple("Pronto", Color.Gray, Icons.Default.CheckCircle)
        is AdminSyncUiState.Scraping -> Triple("A extrair ciclistas...", Color(0xFF9B59B6), Icons.Default.Download)
        is AdminSyncUiState.ScrapingComplete -> Triple("Extraidos ${uiState.count} ciclistas", Color(0xFF9B59B6), Icons.Default.CheckCircle)
        is AdminSyncUiState.Uploading -> {
            val progressText = uploadProgress?.let { "${it.uploaded}/${it.total}" } ?: ""
            Triple("A enviar... $progressText", Color(0xFF006600), Icons.Default.CloudUpload)
        }
        is AdminSyncUiState.UploadComplete -> Triple("Upload completo: ${uiState.count} ciclistas", Color(0xFF2ECC71), Icons.Default.CloudDone)
        is AdminSyncUiState.UploadingPhotos -> Triple("A enviar fotos...", Color(0xFF9B59B6), Icons.Default.PhotoLibrary)
        is AdminSyncUiState.PhotoUploadComplete -> Triple(
            "${uiState.uploaded}/${uiState.matched} fotos enviadas (${uiState.total} selecionadas)",
            Color(0xFF27AE60),
            Icons.Default.PhotoLibrary
        )
        is AdminSyncUiState.Validating -> Triple("A validar ciclistas...", Color(0xFF006600), Icons.Default.Refresh)
        is AdminSyncUiState.ValidationComplete -> Triple("${uiState.count} ciclistas validados", Color(0xFF2ECC71), Icons.Default.CheckCircle)
        is AdminSyncUiState.Deleting -> Triple("A apagar...", Color(0xFFE74C3C), Icons.Default.Delete)
        is AdminSyncUiState.DeleteComplete -> Triple("Todos os ciclistas apagados", Color(0xFFF39C12), Icons.Default.Delete)
        is AdminSyncUiState.Error -> Triple(uiState.message, Color(0xFFE74C3C), Icons.Default.Error)
        else -> Triple("Pronto", Color.Gray, Icons.Default.CheckCircle)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState is AdminSyncUiState.Uploading || uiState is AdminSyncUiState.Validating || uiState is AdminSyncUiState.Deleting || uiState is AdminSyncUiState.Scraping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = color,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    color = color,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            // Show progress bar during upload
            if (uiState is AdminSyncUiState.Uploading && uploadProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uploadProgress.percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uploadProgress.currentName,
                        fontSize = 11.sp,
                        color = color.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (uploadProgress.totalBatches > 0) {
                        Text(
                            text = uploadProgress.batchText,
                            fontSize = 11.sp,
                            color = color.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonColor: Color = Color(0xFF006600)
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(buttonText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CyclistEditableItem(
    cyclist: Cyclist,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cyclist.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = cyclist.teamName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                // Show age, ranking, speciality
                val infoText = buildString {
                    cyclist.age?.let { append("${it}a") }
                    cyclist.uciRanking?.let {
                        if (isNotEmpty()) append(" | ")
                        append("#$it UCI")
                    }
                    cyclist.speciality?.let {
                        if (isNotEmpty()) append(" | ")
                        append(it)
                    }
                    if (isEmpty()) append(cyclist.nationality)
                }
                Text(
                    text = infoText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = cyclist.displayPrice,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2ECC71)
                )
                Text(
                    text = cyclist.category.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remover",
                    tint = Color(0xFFE74C3C),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCyclistDialog(
    cyclist: Cyclist,
    onDismiss: () -> Unit,
    onSave: (Cyclist) -> Unit
) {
    var price by remember { mutableStateOf(cyclist.price.toString()) }
    var selectedCategory by remember { mutableStateOf(cyclist.category) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Editar Ciclista",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = cyclist.fullName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info (read-only)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Equipa: ${cyclist.teamName}",
                            fontSize = 13.sp
                        )
                        cyclist.age?.let {
                            Text("Idade: $it anos", fontSize = 13.sp)
                        }
                        cyclist.uciRanking?.let {
                            Text("UCI Ranking: #$it", fontSize = 13.sp)
                        }
                        cyclist.speciality?.let {
                            Text("Especialidade: $it", fontSize = 13.sp)
                        }
                    }
                }

                // Price input
                OutlinedTextField(
                    value = price,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            price = newValue
                        }
                    },
                    label = { Text("Preco (M)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CyclistCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (category) {
                                            CyclistCategory.CLIMBER -> "Climber"
                                            CyclistCategory.HILLS -> "Hills"
                                            CyclistCategory.TT -> "TT (Contra-relogio)"
                                            CyclistCategory.SPRINT -> "Sprint"
                                            CyclistCategory.GC -> "GC (General)"
                                            CyclistCategory.ONEDAY -> "Oneday (Classicas)"
                                        }
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newPrice = price.toDoubleOrNull() ?: cyclist.price
                    onSave(
                        cyclist.copy(
                            price = newPrice,
                            category = selectedCategory
                        )
                    )
                }
            ) {
                Text("Guardar", color = Color(0xFF2ECC71))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCyclistManuallyDialog(
    onDismiss: () -> Unit,
    onAdd: (Cyclist) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var uciRanking by remember { mutableStateOf("") }
    var speciality by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("0.0") }
    var selectedCategory by remember { mutableStateOf(CyclistCategory.GC) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Adicionar Ciclista Manualmente",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Nome") },
                        placeholder = { Text("Tadej") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Apelido") },
                        placeholder = { Text("Pogacar") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Equipa") },
                    placeholder = { Text("UAE Team Emirates") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nationality,
                        onValueChange = { nationality = it },
                        label = { Text("Nacionalidade") },
                        placeholder = { Text("Slovenia") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) age = it },
                        label = { Text("Idade") },
                        placeholder = { Text("26") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uciRanking,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) uciRanking = it },
                        label = { Text("UCI Ranking") },
                        placeholder = { Text("1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) price = it },
                        label = { Text("Preco (M)") },
                        placeholder = { Text("15.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = speciality,
                    onValueChange = { speciality = it },
                    label = { Text("Especialidade") },
                    placeholder = { Text("GC, Climber, Sprinter...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedCategory) {
                            CyclistCategory.CLIMBER -> "Climber"
                            CyclistCategory.HILLS -> "Hills"
                            CyclistCategory.TT -> "TT (Contra-relogio)"
                            CyclistCategory.SPRINT -> "Sprint"
                            CyclistCategory.GC -> "GC (General)"
                            CyclistCategory.ONEDAY -> "Oneday (Classicas)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        CyclistCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (category) {
                                            CyclistCategory.CLIMBER -> "Climber"
                                            CyclistCategory.HILLS -> "Hills"
                                            CyclistCategory.TT -> "TT (Contra-relogio)"
                                            CyclistCategory.SPRINT -> "Sprint"
                                            CyclistCategory.GC -> "GC (General)"
                                            CyclistCategory.ONEDAY -> "Oneday (Classicas)"
                                        }
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cyclist = Cyclist(
                        id = "${firstName.lowercase().replace(" ", "-")}-${lastName.lowercase().replace(" ", "-")}-${System.currentTimeMillis()}",
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        fullName = "${firstName.trim()} ${lastName.trim()}",
                        teamId = teamName.lowercase().replace(" ", "-"),
                        teamName = teamName.trim(),
                        nationality = nationality.trim(),
                        photoUrl = null,
                        category = selectedCategory,
                        price = price.toDoubleOrNull() ?: 0.0,
                        totalPoints = 0,
                        form = 0.0,
                        popularity = 0.0,
                        age = age.toIntOrNull(),
                        uciRanking = uciRanking.toIntOrNull(),
                        speciality = speciality.trim().takeIf { it.isNotBlank() }
                    )
                    onAdd(cyclist)
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && teamName.isNotBlank()
            ) {
                Text("Adicionar", color = Color(0xFF2ECC71))
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
private fun CsvFormatInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Formato do Ficheiro CSV",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "O ficheiro CSV deve ter 5 colunas obrigatorias + 1 opcional:",
                    fontSize = 14.sp
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Colunas do CSV:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFFE74C3C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. Nome - Nome completo (ex: POGACAR Tadej)", fontSize = 12.sp)
                        Text("2. Equipa - Nome da equipa", fontSize = 12.sp)
                        Text("3. UCI Ranking - Ranking UCI (numero)", fontSize = 12.sp)
                        Text("4. Link - URL do perfil do ciclista", fontSize = 12.sp, color = Color(0xFF006600))
                        Text("5. Especialidade - CLIMBER, HILLS, TT, SPRINT, GC, ONEDAY", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("6. Foto", fontSize = 12.sp, color = Color(0xFF9B59B6), fontWeight = FontWeight.Medium)
                            Text(" - URL da foto (opcional)", fontSize = 12.sp, color = Color(0xFF9B59B6))
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF27AE60).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Calculado automaticamente no upload:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF27AE60)
                        )
                        Text("• Conversao do nome (APELIDO Nome -> Nome Apelido)", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Preco baseado no ranking por categoria:", fontSize = 12.sp)
                        Text("  - #1 UCI mundial = 22M", fontSize = 11.sp, color = Color(0xFFF39C12))
                        Text("  - Top 2% de cada categoria = 20M", fontSize = 11.sp)
                        Text("  - Ultimos 5% = 1M", fontSize = 11.sp)
                        Text("  - Restantes = proporcional (2M-19M)", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "Exemplos de linha:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A2E)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Sem foto:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "POGACAR Tadej,UAE Team Emirates,1,https://pcs.com/rider/pogacar,GC",
                            fontSize = 9.sp,
                            color = Color(0xFF00FF88)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Com foto:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "POGACAR Tadej,UAE,1,https://pcs.com/pogacar,GC,https://img.com/pogacar.jpg",
                            fontSize = 9.sp,
                            color = Color(0xFF00FF88)
                        )
                    }
                }

                Text(
                    text = "Dica: Exporta do Excel como CSV UTF-8 (separado por virgulas)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", color = Color(0xFF27AE60))
            }
        }
    )
}

@Composable
private fun PhotoFormatInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Como Identificar Fotos",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "O sistema identifica o ciclista automaticamente usando duas fontes:",
                    fontSize = 14.sp
                )

                // Metadata section (priority)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF27AE60).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "1. Metadados da foto",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF27AE60)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PRIORITARIO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color(0xFF27AE60), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("O sistema le as propriedades da imagem:", fontSize = 11.sp)
                        Text("• Titulo (Title)", fontSize = 11.sp, color = Color(0xFF27AE60))
                        Text("• Descricao (Description)", fontSize = 11.sp, color = Color(0xFF27AE60))
                        Text("• Comentario (User Comment)", fontSize = 11.sp, color = Color(0xFF27AE60))
                        Text("• Artista (Artist)", fontSize = 11.sp, color = Color(0xFF27AE60))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dica: No Windows, clica direito na foto > Propriedades > Detalhes > Titulo",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Filename section (fallback)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF9B59B6).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "2. Nome do ficheiro (alternativa)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF9B59B6)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Se nao encontrar metadados, usa o nome do ficheiro:", fontSize = 11.sp)
                        Text("• tadej-pogacar.jpg", fontSize = 11.sp, color = Color(0xFF9B59B6))
                        Text("• pogacar-tadej.jpg", fontSize = 11.sp, color = Color(0xFF9B59B6))
                        Text("• tadejpogacar.jpg", fontSize = 11.sp, color = Color(0xFF9B59B6))
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF39C12).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFF39C12),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dicas:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFFF39C12)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• O nome pode estar em qualquer formato", fontSize = 11.sp)
                        Text("• Ex: \"Tadej Pogacar\" ou \"pogacar tadej\"", fontSize = 11.sp)
                        Text("• Acentos sao ignorados automaticamente", fontSize = 11.sp)
                        Text("• Formatos suportados: JPG, PNG, WEBP", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "As fotos serao armazenadas no Firebase Storage e o URL sera associado ao ciclista.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", color = Color(0xFF9B59B6))
            }
        }
    )
}

@Composable
private fun RacesCsvFormatInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Formato CSV de Corridas",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "O ficheiro CSV deve ter 5 colunas:",
                    fontSize = 14.sp
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Colunas do CSV:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF006600)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. datainicio - Data de inicio (dd.MM)", fontSize = 12.sp)
                        Text("2. datafim - Data de fim (vazio se 1 dia)", fontSize = 12.sp)
                        Text("3. ano - Ano da corrida (ex: 2026)", fontSize = 12.sp)
                        Text("4. nome - Nome da corrida", fontSize = 12.sp)
                        Text("5. url - Link para info (opcional)", fontSize = 12.sp)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF006600).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Detecao automatica:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF006600)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• ID gerado a partir do nome", fontSize = 11.sp)
                        Text("• Tipo inferido pela duracao e nome", fontSize = 11.sp)
                        Text("• Pais extraido do nome da corrida", fontSize = 11.sp)
                        Text("• Categoria detectada (GT, WT, 2.1)", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "Exemplos de linha:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A2E)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Classica (1 dia):",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "13.04,,2026,Paris-Roubaix,https://...",
                            fontSize = 9.sp,
                            color = Color(0xFF00FF88)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Grande Volta:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "05.07,27.07,2026,Tour de France,https://...",
                            fontSize = 9.sp,
                            color = Color(0xFF00FF88)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Volta por etapas:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "06.08,17.08,2026,Volta a Portugal,",
                            fontSize = 9.sp,
                            color = Color(0xFF00FF88)
                        )
                    }
                }

                Text(
                    text = "Dica: A primeira linha pode ser cabecalho (sera ignorada se contiver 'nome' ou 'datainicio')",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", color = Color(0xFF006600))
            }
        }
    )
}

/**
 * Tenta decodificar bytes com varias codificacoes
 * Prioriza UTF-8 para caracteres especiais como "č" em "Pogačar"
 */
private fun tryDecodeWithEncodings(bytes: ByteArray): String {
    // Primeiro, substitui bytes problematicos por equivalentes ASCII
    // Isto resolve o problema de encoding antes mesmo de decodificar
    val processedBytes = preprocessBytesForAccents(bytes)

    // Remove BOM se existir no inicio dos bytes
    val cleanBytes = when {
        processedBytes.size >= 3 && processedBytes[0] == 0xEF.toByte() && processedBytes[1] == 0xBB.toByte() && processedBytes[2] == 0xBF.toByte() -> {
            // UTF-8 BOM - ficheiro e definitivamente UTF-8
            android.util.Log.d("CSV_ENCODING", "UTF-8 BOM detected")
            return String(processedBytes.sliceArray(3 until processedBytes.size), Charsets.UTF_8)
        }
        processedBytes.size >= 2 && processedBytes[0] == 0xFF.toByte() && processedBytes[1] == 0xFE.toByte() -> {
            // UTF-16 LE BOM
            android.util.Log.d("CSV_ENCODING", "UTF-16 LE BOM detected")
            return String(processedBytes.sliceArray(2 until processedBytes.size), Charsets.UTF_16LE)
        }
        processedBytes.size >= 2 && processedBytes[0] == 0xFE.toByte() && processedBytes[1] == 0xFF.toByte() -> {
            // UTF-16 BE BOM
            android.util.Log.d("CSV_ENCODING", "UTF-16 BE BOM detected")
            return String(processedBytes.sliceArray(2 until processedBytes.size), Charsets.UTF_16BE)
        }
        else -> processedBytes
    }

    // Tenta UTF-8 primeiro
    try {
        val utf8Decoded = String(cleanBytes, Charsets.UTF_8)
        val replacementCount = utf8Decoded.count { it == '\uFFFD' }

        if (replacementCount == 0) {
            android.util.Log.d("CSV_ENCODING", "UTF-8 decoded successfully")
            return utf8Decoded
        }
    } catch (e: Exception) {
        // Ignorar
    }

    // Tenta CP1250 (Windows Central European) - comum em ficheiros Excel
    try {
        val decoded = String(cleanBytes, Charset.forName("Cp1250"))
        android.util.Log.d("CSV_ENCODING", "Using CP1250")
        return decoded
    } catch (e: Exception) {
        // Ignorar
    }

    // Fallback: Windows-1252
    return try {
        String(cleanBytes, Charset.forName("Windows-1252"))
    } catch (e: Exception) {
        String(cleanBytes, Charsets.UTF_8)
    }
}

/**
 * Pre-processa bytes para substituir caracteres acentuados por ASCII
 * antes da decodificacao de string
 */
private fun preprocessBytesForAccents(bytes: ByteArray): ByteArray {
    val result = bytes.toMutableList()
    var i = 0

    while (i < result.size) {
        // UTF-8: č = 0xC4 0x8D -> c (0x63)
        if (i + 1 < result.size && result[i] == 0xC4.toByte() && result[i + 1] == 0x8D.toByte()) {
            result[i] = 0x63.toByte() // 'c'
            result.removeAt(i + 1)
            continue
        }
        // UTF-8: Č = 0xC4 0x8C -> C (0x43)
        if (i + 1 < result.size && result[i] == 0xC4.toByte() && result[i + 1] == 0x8C.toByte()) {
            result[i] = 0x43.toByte() // 'C'
            result.removeAt(i + 1)
            continue
        }
        // UTF-8: ž = 0xC5 0xBE -> z (0x7A)
        if (i + 1 < result.size && result[i] == 0xC5.toByte() && result[i + 1] == 0xBE.toByte()) {
            result[i] = 0x7A.toByte() // 'z'
            result.removeAt(i + 1)
            continue
        }
        // UTF-8: š = 0xC5 0xA1 -> s (0x73)
        if (i + 1 < result.size && result[i] == 0xC5.toByte() && result[i + 1] == 0xA1.toByte()) {
            result[i] = 0x73.toByte() // 's'
            result.removeAt(i + 1)
            continue
        }
        // CP1250/ISO-8859-2: č = 0xE8 -> c (0x63)
        if (result[i] == 0xE8.toByte()) {
            result[i] = 0x63.toByte() // 'c'
            i++
            continue
        }
        // CP1250/ISO-8859-2: Č = 0xC8 -> C (0x43)
        if (result[i] == 0xC8.toByte()) {
            result[i] = 0x43.toByte() // 'C'
            i++
            continue
        }
        // CP1250/ISO-8859-2: ž = 0xBE -> z (0x7A)
        if (result[i] == 0xBE.toByte()) {
            result[i] = 0x7A.toByte() // 'z'
            i++
            continue
        }
        // CP1250/ISO-8859-2: š = 0xB9 -> s (0x73)
        if (result[i] == 0xB9.toByte()) {
            result[i] = 0x73.toByte() // 's'
            i++
            continue
        }

        i++
    }

    return result.toByteArray()
}

/**
 * Parse CSV lines to list of Cyclists
 * Expected format: Nome,Equipa,UCI Ranking,Link,Especialidade,Foto (opcional)
 * Price is calculated automatically before upload based on category rankings
 */
private fun parseCsvToCyclists(lines: List<String>): List<Cyclist> {
    val cyclists = mutableListOf<Cyclist>()

    // Skip empty lines and potential header
    val dataLines = lines.filter { it.isNotBlank() }
    val startIndex = if (dataLines.firstOrNull()?.lowercase()?.contains("nome") == true ||
        dataLines.firstOrNull()?.lowercase()?.contains("name") == true ||
        dataLines.firstOrNull()?.lowercase()?.contains("equipa") == true) 1 else 0

    for (i in startIndex until dataLines.size) {
        val line = dataLines[i]
        try {
            // Handle both comma and semicolon separators (Excel sometimes uses semicolons)
            val separator = if (line.contains(';')) ';' else ','
            val parts = line.split(separator).map { it.trim().removeSurrounding("\"") }

            if (parts.size < 5) continue // Need Nome, Equipa, Ranking, Link, Especialidade

            // Parse nome (pode estar no formato "APELIDO Nome")
            var fullName = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: continue
            // Limpa caracteres especiais do nome
            fullName = cleanSpecialCharacters(fullName)
            fullName = convertNameFormat(fullName)

            var teamName = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: continue
            teamName = cleanSpecialCharacters(teamName)

            val uciRanking = parts.getOrNull(2)?.trim()?.toIntOrNull()
            val profileUrl = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() }
            val specialityStr = parts.getOrNull(4)?.uppercase()?.trim()?.let { cleanSpecialCharacters(it) }

            // Coluna 6 opcional: URL da foto
            val photoUrl = parts.getOrNull(5)?.trim()?.takeIf {
                it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://"))
            }

            // Determina categoria baseado na especialidade (6 categorias: Climber, Hills, TT, Sprint, GC, Oneday)
            val category = when (specialityStr) {
                "CLIMBER", "CLIMBING", "MOUNTAINS" -> CyclistCategory.CLIMBER
                "HILLS", "PUNCHEUR", "PUNCHER" -> CyclistCategory.HILLS
                "TT", "TIME TRIAL", "TIMETRIAL", "ITT" -> CyclistCategory.TT
                "SPRINT", "SPRINTER" -> CyclistCategory.SPRINT
                "GC", "GENERAL CLASSIFICATION", "STAGE RACES" -> CyclistCategory.GC
                "ONEDAY", "ONE DAY", "CLASSICS", "CLASSIC" -> CyclistCategory.ONEDAY
                else -> continue // Salta ciclistas sem categoria valida
            }

            // Separa primeiro nome e apelido
            val nameParts = fullName.split(" ", limit = 2)
            val firstName = nameParts.getOrNull(0) ?: fullName
            val lastName = nameParts.getOrNull(1) ?: ""

            val cyclist = Cyclist(
                id = "${firstName.lowercase().replace(" ", "-")}-${lastName.lowercase().replace(" ", "-")}-${System.currentTimeMillis()}-${i}",
                firstName = firstName,
                lastName = lastName,
                fullName = fullName,
                teamId = teamName.lowercase().replace(" ", "-"),
                teamName = teamName,
                nationality = "",
                photoUrl = photoUrl,
                category = category,
                price = 0.0, // Preco sera calculado automaticamente antes do upload
                totalPoints = 0,
                form = 0.0,
                popularity = 0.0,
                age = null,
                uciRanking = uciRanking,
                speciality = specialityStr,
                profileUrl = profileUrl
            )

            cyclists.add(cyclist)
        } catch (e: Exception) {
            // Skip invalid lines
            continue
        }
    }

    return cyclists
}

/**
 * Remove caracteres de controlo e normaliza caracteres especiais para ASCII
 * Usa Normalizer para remover diacriticos de forma robusta
 */
private fun cleanSpecialCharacters(text: String): String {
    // Primeiro limpa caracteres de controlo
    var cleaned = text
        .replace("\uFEFF", "") // BOM
        .replace("\uFFFE", "") // Reverse BOM
        .replace("\u00A0", " ") // Non-breaking space
        .replace(Regex("[\\p{Cc}&&[^\\t\\n\\r]]"), "") // Control characters except tab/newline

    // Substitui "?" por "c" quando aparece entre letras (encoding falhado de "č")
    // Ex: "POGA?AR" -> "POGACAR"
    cleaned = cleaned.replace(Regex("(?<=[A-Za-z])\\?(?=[A-Za-z])"), "c")

    // Usa Normalizer para remover diacriticos (č -> c, ž -> z, etc.)
    cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")

    // Substitui caracteres de replacement por nada
    cleaned = cleaned.replace("\uFFFD", "")

    return cleaned.trim()
}

/**
 * Converte nome do formato "APELIDO Nome" para "Nome Apelido"
 */
private fun convertNameFormat(name: String): String {
    val cleanName = name.trim()
    val words = cleanName.split(" ")

    if (words.size < 2) return cleanName

    // Encontra o indice do primeiro nome (primeira palavra que nao e toda maiuscula)
    var firstNameIdx = -1
    for (i in words.indices) {
        val word = words[i]
        if (word.isNotEmpty() && word != word.uppercase() && word[0].isUpperCase()) {
            firstNameIdx = i
            break
        }
    }

    return if (firstNameIdx > 0) {
        val firstNames = words.subList(firstNameIdx, words.size).joinToString(" ")
        val lastNames = words.subList(0, firstNameIdx).joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
        "$firstNames $lastNames"
    } else {
        cleanName
    }
}

/**
 * Small composable showing a jersey indicator with emoji, name and label.
 */
@Composable
private fun JerseyIndicator(
    emoji: String,
    name: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp
        )
        Text(
            text = name.take(10),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Helper composable for displaying scraped data fields
 */
@Composable
private fun ScrapedDataField(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.3f)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Format timestamp to readable date string
 */
private fun formatDateFromTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Data não detectada"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
    return sdf.format(Date(timestamp))
}

