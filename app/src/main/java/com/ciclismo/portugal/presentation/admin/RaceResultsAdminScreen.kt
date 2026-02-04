package com.ciclismo.portugal.presentation.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.StageType
import com.ciclismo.portugal.presentation.theme.AppImages
import java.nio.charset.Charset
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceResultsAdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminSyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allSeasonRaces by viewModel.allSeasonRaces.collectAsState()
    val currentRaceToProcess by viewModel.currentRaceToProcess.collectAsState()
    val parsedResults by viewModel.parsedResults.collectAsState()
    val processedRace by viewModel.processedRace.collectAsState()
    val fantasyPointsResult by viewModel.fantasyPointsResult.collectAsState()

    // Stage management
    val stageSchedule by viewModel.stageSchedule.collectAsState()
    val stageScheduleImportResult by viewModel.stageScheduleImportResult.collectAsState()
    val currentStageNumber by viewModel.currentStageNumber.collectAsState()
    val currentStageType by viewModel.currentStageType.collectAsState()
    val stageProcessingResult by viewModel.stageProcessingResult.collectAsState()
    val processedStages by viewModel.processedStages.collectAsState()
    val currentJerseyHolders by viewModel.currentJerseyHolders.collectAsState()

    val context = LocalContext.current

    var showRacesCsvFormatDialog by remember { mutableStateOf(false) }
    var showRaceResultsDialog by remember { mutableStateOf(false) }
    var showStageScheduleDialog by remember { mutableStateOf(false) }
    var showStageResultsDialog by remember { mutableStateOf(false) }

    var raceDropdownExpanded by remember { mutableStateOf(false) }
    var stageTypeDropdownExpanded by remember { mutableStateOf(false) }

    var raceName by remember { mutableStateOf("") }
    var raceResultsText by remember { mutableStateOf("") }
    var stageScheduleText by remember { mutableStateOf("") }
    var stageResultsText by remember { mutableStateOf("") }
    var selectedStageNumber by remember { mutableIntStateOf(1) }

    // Pre-fill race name from selected race
    LaunchedEffect(currentRaceToProcess) {
        currentRaceToProcess?.let { race ->
            if (raceName.isBlank()) {
                raceName = race.name
            }
        }
    }

    // Races CSV file picker
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            AsyncImage(
                model = AppImages.WORLDTOUR_HEADER,
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
                                Color(0xFFFFD700).copy(alpha = 0.7f),
                                Color(0xFFFF8C00).copy(alpha = 0.85f)
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
                    Column {
                        Text(
                            text = "Corridas e Resultados",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${allSeasonRaces.size} corridas na temporada",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WorldTour Races Import Card
            item {
                val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF006600).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFF006600),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Corridas WorldTour",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF006600)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Importa corridas via CSV ou carrega calendario pre-definido",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                            OutlinedButton(
                                onClick = { showRacesCsvFormatDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Formato CSV", fontSize = 12.sp)
                            }

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

            // Race Results Import Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Importar Resultados",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB8860B)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Cola os resultados no formato: Pos, Nome, Equipa, Tempo",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Race selector
                        Text(
                            text = "Selecionar Corrida",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
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
                                    focusedBorderColor = Color(0xFFB8860B),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
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
                                                "Nenhuma corrida. Importa corridas primeiro.",
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
                                                            text = "${race.formattedStartDate} • ${race.stages} etapas",
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
                                            }
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
                                containerColor = Color(0xFFB8860B)
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

                            parsedResults.take(3).forEach { result ->
                                Text(
                                    text = "#${result.rank} ${result.riderName} - ${result.fantasyPoints}pts",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (parsedResults.size > 3) {
                                Text(
                                    text = "... e mais ${parsedResults.size - 3}",
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
                                    Text("Aplicar")
                                }
                            }
                        }
                    }
                }
            }

            // Stage Schedule Import Card (for multi-stage races)
            if (currentRaceToProcess?.stages ?: 0 > 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Route,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Percurso de Etapas",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2196F3)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Importa o percurso das ${currentRaceToProcess?.stages ?: 0} etapas",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showStageScheduleDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3)
                                )
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Colar Percurso")
                            }

                            // Show imported stages
                            if (stageSchedule.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "${stageSchedule.size} etapas importadas",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF27AE60)
                                )

                                stageSchedule.take(3).forEach { stage ->
                                    Text(
                                        text = "E${stage.stageNumber}: ${stage.stageType.displayNamePt} - ${stage.distanceDisplay}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            stageScheduleImportResult?.let { result ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = result.message,
                                    fontSize = 12.sp,
                                    color = if (result.success) Color(0xFF27AE60) else Color(0xFFE57373)
                                )
                            }
                        }
                    }
                }

                // Stage Results Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF9C27B0).copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFF9C27B0),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resultados de Etapa",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9C27B0)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Processa resultados de uma etapa especifica",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stage number selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Etapa:", fontWeight = FontWeight.Medium)

                                OutlinedTextField(
                                    value = selectedStageNumber.toString(),
                                    onValueChange = {
                                        it.toIntOrNull()?.let { num ->
                                            if (num in 1..(currentRaceToProcess?.stages ?: 21)) {
                                                selectedStageNumber = num
                                            }
                                        }
                                    },
                                    modifier = Modifier.width(80.dp),
                                    singleLine = true
                                )

                                Text("de ${currentRaceToProcess?.stages ?: 0}")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Stage type selector
                            ExposedDropdownMenuBox(
                                expanded = stageTypeDropdownExpanded,
                                onExpandedChange = { stageTypeDropdownExpanded = !stageTypeDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = currentStageType.displayNamePt,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Tipo de Etapa") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageTypeDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = stageTypeDropdownExpanded,
                                    onDismissRequest = { stageTypeDropdownExpanded = false }
                                ) {
                                    StageType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(type.emoji)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(type.displayNamePt)
                                                }
                                            },
                                            onClick = {
                                                viewModel.setStageType(type)
                                                stageTypeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showStageResultsDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9C27B0)
                                )
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Colar Resultados da Etapa $selectedStageNumber")
                            }

                            stageProcessingResult?.let { result ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Etapa ${result.stageNumber}: ${result.resultsCount} resultados, ${result.pointsAwarded} pontos",
                                    fontSize = 12.sp,
                                    color = Color(0xFF27AE60)
                                )
                            }
                        }
                    }
                }
            }

            // Fantasy Points Calculation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE91E63).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Calcular Pontos Fantasy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE91E63)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Calcula e atualiza os pontos de todas as equipas Fantasy",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { /* TODO: Implement calculateFantasyPoints */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE91E63)
                            )
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calcular Pontos")
                        }

                        fantasyPointsResult?.let { result ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${result.teamsUpdated} equipas atualizadas, ${result.totalPointsAwarded} pontos atribuidos",
                                fontSize = 12.sp,
                                color = Color(0xFF27AE60)
                            )
                        }
                    }
                }
            }

            // Processing results card
            if (processedRace != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A2E)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Ultimo Processamento",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF27AE60),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = processedRace!!.name,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Race Results Dialog
    if (showRaceResultsDialog) {
        AlertDialog(
            onDismissRequest = { showRaceResultsDialog = false },
            title = { Text("Colar Resultados") },
            text = {
                Column {
                    Text(
                        text = "Corrida: ${currentRaceToProcess?.name ?: ""}",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Formato: Pos, Nome, Equipa, Tempo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = raceResultsText,
                        onValueChange = { raceResultsText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Cola os resultados aqui...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.parseRaceResults(raceResultsText)
                        showRaceResultsDialog = false
                        raceResultsText = ""
                    }
                ) {
                    Text("Processar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRaceResultsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Stage Schedule Dialog
    if (showStageScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showStageScheduleDialog = false },
            title = { Text("Importar Percurso") },
            text = {
                Column {
                    Text(
                        text = "Formato: Data, Dia, Etapa | Partida - Chegada, Km",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stageScheduleText,
                        onValueChange = { stageScheduleText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Cola o percurso aqui...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentRaceToProcess?.let { race ->
                            val stages = viewModel.parseStageScheduleText(stageScheduleText, race.id)
                            viewModel.importStageSchedule(stages)
                        }
                        showStageScheduleDialog = false
                        stageScheduleText = ""
                    }
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStageScheduleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Stage Results Dialog
    if (showStageResultsDialog) {
        AlertDialog(
            onDismissRequest = { showStageResultsDialog = false },
            title = { Text("Resultados da Etapa $selectedStageNumber") },
            text = {
                Column {
                    Text(
                        text = "Tipo: ${currentStageType.displayNamePt}",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stageResultsText,
                        onValueChange = { stageResultsText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Cola os resultados aqui...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentRaceToProcess?.let { race ->
                            val parsedResults = viewModel.parseStageResultsText(stageResultsText)
                            viewModel.applyStageResults(parsedResults)
                        }
                        showStageResultsDialog = false
                        stageResultsText = ""
                    }
                ) {
                    Text("Processar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStageResultsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // CSV Format Dialog
    if (showRacesCsvFormatDialog) {
        AlertDialog(
            onDismissRequest = { showRacesCsvFormatDialog = false },
            title = { Text("Formato CSV de Corridas") },
            text = {
                Column {
                    Text("Colunas esperadas:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Nome da corrida")
                    Text("2. Pais")
                    Text("3. Data inicio (DD/MM/YYYY)")
                    Text("4. Data fim (DD/MM/YYYY)")
                    Text("5. Categoria (WT, Pro, 1, 2)")
                    Text("6. Numero de etapas")
                    Text("7. Tipo (GT, Stage, OneDay)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showRacesCsvFormatDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }
}

// Helper function to decode CSV with different encodings
private fun tryDecodeWithEncodings(bytes: ByteArray): String {
    val encodings = listOf(
        Charsets.UTF_8,
        Charset.forName("ISO-8859-1"),
        Charset.forName("Windows-1252")
    )

    for (encoding in encodings) {
        try {
            val content = String(bytes, encoding)
            if (!content.contains('\uFFFD')) {
                return content
            }
        } catch (_: Exception) { }
    }

    return String(bytes, Charsets.UTF_8)
}
