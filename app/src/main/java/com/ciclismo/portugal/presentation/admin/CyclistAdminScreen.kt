package com.ciclismo.portugal.presentation.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.presentation.theme.AppImages
import java.nio.charset.Charset
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclistAdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminSyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val validatedCount by viewModel.validatedCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val photoUploadProgress by viewModel.photoUploadProgress.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var showCsvFormatDialog by remember { mutableStateOf(false) }
    var showPhotoFormatDialog by remember { mutableStateOf(false) }
    var showAddCyclistDialog by remember { mutableStateOf(false) }
    var cyclistToEdit by remember { mutableStateOf<Cyclist?>(null) }

    // CSV file picker
    val csvFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes() ?: return@let
                inputStream.close()

                val content = tryDecodeWithEncodings(bytes)
                val lines = content.lines()

                val cyclists = parseCsvToCyclists(lines)
                if (cyclists.isNotEmpty()) {
                    viewModel.addCyclistsFromCsv(cyclists)
                } else {
                    viewModel.showError("Nenhum ciclista encontrado no ficheiro")
                }
            } catch (e: Exception) {
                viewModel.showError("Erro ao ler ficheiro: ${e.message}")
            }
        }
    }

    // Photo picker for Firebase
    val firebasePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadPhotosForFirebaseCyclists(uris, context)
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
                model = AppImages.ROAD_CYCLING,
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
                                Color(0xFF006600).copy(alpha = 0.7f),
                                Color(0xFF004400).copy(alpha = 0.85f)
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
                            text = "Gestao de Ciclistas",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$validatedCount validados • $pendingCount pendentes",
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
            // Search Card - Main feature for finding and editing cyclists
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3498DB).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF3498DB),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Procurar e Editar Ciclistas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3498DB)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Pesquisa ciclistas da epoca atual para editar detalhes",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search TextField
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Ex: Pogacar, Vingegaard...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF3498DB)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Limpar",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    if (searchQuery.isNotEmpty()) {
                                        viewModel.searchCyclists(searchQuery)
                                    }
                                }
                            )
                        )

                        // Search progress
                        if (isSearching) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF3498DB)
                            )
                        }
                    }
                }
            }

            // Search Results
            if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Resultados (${searchResults.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3498DB)
                    )
                }

                items(searchResults, key = { it.id }) { cyclist ->
                    CyclistSearchResultItem(
                        cyclist = cyclist,
                        onEditClick = { cyclistToEdit = cyclist }
                    )
                }
            } else if (searchQuery.length >= 2 && !isSearching) {
                item {
                    Text(
                        text = "Nenhum ciclista encontrado para \"$searchQuery\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Divider
            if (searchResults.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // Status Card
            item {
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
                    }
                }
            }

            // CSV Import Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF27AE60).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = Color(0xFF27AE60),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Importar Ciclistas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF27AE60)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Importa de CSV ou adiciona manualmente",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                            OutlinedButton(
                                onClick = { showCsvFormatDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Formato CSV", fontSize = 12.sp)
                            }

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

            // Photo Upload Card (only show if there are validated cyclists)
            if (validatedCount > 0) {
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
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upload Fotos para Firebase",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2196F3)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Adiciona fotos aos $validatedCount ciclistas validados",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { firebasePhotoPicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState !is AdminSyncUiState.UploadingPhotos,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3)
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

                            // Photo upload progress
                            photoUploadProgress?.let { progress ->
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.percentage },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${progress.displayText} - ${progress.currentName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2196F3)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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

            // Validate Card
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
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF9C27B0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Validar Ciclistas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9C27B0)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Marca $pendingCount ciclistas pendentes como validados para o Fantasy",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.validateAllCyclists() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = pendingCount > 0 && uiState !is AdminSyncUiState.Validating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0)
                            )
                        ) {
                            if (uiState is AdminSyncUiState.Validating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A validar...")
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Validar Todos ($pendingCount)")
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // CSV Format Dialog
    if (showCsvFormatDialog) {
        AlertDialog(
            onDismissRequest = { showCsvFormatDialog = false },
            title = { Text("Formato CSV") },
            text = {
                Column {
                    Text("O ficheiro CSV deve ter 5 colunas:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Nome completo")
                    Text("2. Equipa")
                    Text("3. Ranking/Pontos")
                    Text("4. Link (opcional)")
                    Text("5. Especialidade (GC, Climber, Sprint, TT, Hills, OneDay)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Exemplo:\nTadej Pogacar,UAE Team Emirates,11535,,GC",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCsvFormatDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }

    // Photo Format Dialog
    if (showPhotoFormatDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoFormatDialog = false },
            title = { Text("Formato das Fotos") },
            text = {
                Column {
                    Text("Nome do ficheiro deve seguir o formato:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "apelido-nome-ano.jpg",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Exemplos:")
                    Text("• pogacar-tadej-2026.jpg")
                    Text("• vingegaard-jonas-2026.jpg")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "O sistema ira tentar fazer match com o ciclista pelo nome.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoFormatDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }

    // Edit Cyclist Dialog
    cyclistToEdit?.let { cyclist ->
        EditCyclistDialog(
            cyclist = cyclist,
            onDismiss = { cyclistToEdit = null },
            onSave = { updatedCyclist ->
                viewModel.updateCyclistInFirebase(updatedCyclist)
                cyclistToEdit = null
            },
            onToggleAvailability = { cyclistId, isDisabled, reason ->
                viewModel.toggleCyclistAvailability(cyclistId, isDisabled, reason)
            }
        )
    }
}

@Composable
private fun CyclistSearchResultItem(
    cyclist: Cyclist,
    onEditClick: () -> Unit
) {
    val isDisabled = cyclist.isDisabled

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) {
                Color(0xFFE74C3C).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDisabled) Color(0xFFE74C3C).copy(alpha = 0.2f)
                        else getCategoryColor(cyclist.category).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!cyclist.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = cyclist.photoUrl,
                        contentDescription = cyclist.fullName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isDisabled) 0.5f else 1f
                    )
                } else {
                    Text(
                        text = cyclist.lastName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (isDisabled) Color(0xFFE74C3C) else getCategoryColor(cyclist.category)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cyclist.fullName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDisabled) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (isDisabled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE74C3C).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "INDISPONÍVEL",
                                fontSize = 9.sp,
                                color = Color(0xFFE74C3C),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getCategoryColor(cyclist.category).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = getCategoryDisplayName(cyclist.category),
                            fontSize = 11.sp,
                            color = getCategoryColor(cyclist.category),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = cyclist.teamName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Show disabled reason if applicable
                if (isDisabled && !cyclist.disabledReason.isNullOrBlank()) {
                    Text(
                        text = "Motivo: ${cyclist.disabledReason}",
                        fontSize = 11.sp,
                        color = Color(0xFFE74C3C).copy(alpha = 0.8f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Price
            Text(
                text = cyclist.displayPrice,
                fontWeight = FontWeight.Bold,
                color = if (isDisabled) Color.Gray else Color(0xFF006600),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Edit button
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFF3498DB)
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
    onSave: (Cyclist) -> Unit,
    onToggleAvailability: ((String, Boolean, String?) -> Unit)? = null
) {
    var firstName by remember { mutableStateOf(cyclist.firstName) }
    var lastName by remember { mutableStateOf(cyclist.lastName) }
    var teamName by remember { mutableStateOf(cyclist.teamName) }
    var selectedCategory by remember { mutableStateOf(cyclist.category) }
    var priceText by remember { mutableStateOf(cyclist.price.toString()) }
    var expanded by remember { mutableStateOf(false) }

    // Availability state
    var isDisabled by remember { mutableStateOf(cyclist.isDisabled) }
    var disabledReason by remember { mutableStateOf(cyclist.disabledReason ?: "") }
    var showDisableReasonDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Editar Ciclista",
                    fontWeight = FontWeight.Bold
                )
                if (isDisabled) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE74C3C).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "INDISPONÍVEL",
                            fontSize = 10.sp,
                            color = Color(0xFFE74C3C),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name display (read-only for identification)
                Text(
                    text = cyclist.fullName,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                // ========== AVAILABILITY SECTION ==========
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDisabled) {
                            Color(0xFFE74C3C).copy(alpha = 0.1f)
                        } else {
                            Color(0xFF2ECC71).copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDisabled) Icons.Default.PersonOff else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isDisabled) Color(0xFFE74C3C) else Color(0xFF2ECC71),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDisabled) "Indisponível" else "Disponível",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isDisabled) Color(0xFFE74C3C) else Color(0xFF2ECC71)
                                )
                            }
                            Switch(
                                checked = !isDisabled,
                                onCheckedChange = { isAvailable ->
                                    if (!isAvailable) {
                                        // Disabling - show reason dialog
                                        showDisableReasonDialog = true
                                    } else {
                                        // Enabling
                                        isDisabled = false
                                        disabledReason = ""
                                        onToggleAvailability?.invoke(cyclist.id, false, null)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2ECC71),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE74C3C)
                                )
                            )
                        }

                        if (isDisabled && disabledReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Motivo: $disabledReason",
                                fontSize = 12.sp,
                                color = Color(0xFFE74C3C).copy(alpha = 0.8f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDisabled) {
                                "Ciclista não aparece no mercado"
                            } else {
                                "Ciclista disponível para compra"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                HorizontalDivider()

                // Category dropdown
                Text(
                    text = "Categoria",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = getCategoryDisplayName(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = getCategoryColor(selectedCategory),
                            unfocusedBorderColor = getCategoryColor(selectedCategory).copy(alpha = 0.5f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CyclistCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(
                                                    getCategoryColor(category),
                                                    CircleShape
                                                )
                                        )
                                        Text(getCategoryDisplayName(category))
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Preco (M)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { Text("M") }
                )

                // Team
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Equipa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Current points (read-only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pontos Fantasy:",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${cyclist.totalPoints} pts",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006600)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: cyclist.price
                    val updatedCyclist = cyclist.copy(
                        firstName = firstName,
                        lastName = lastName,
                        fullName = "$firstName $lastName",
                        teamName = teamName,
                        category = selectedCategory,
                        price = price,
                        isDisabled = isDisabled,
                        disabledReason = if (isDisabled) disabledReason else null,
                        disabledAt = if (isDisabled && cyclist.disabledAt == null) System.currentTimeMillis() else cyclist.disabledAt
                    )
                    onSave(updatedCyclist)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006600)
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Disable Reason Dialog
    if (showDisableReasonDialog) {
        DisableReasonDialog(
            onDismiss = { showDisableReasonDialog = false },
            onConfirm = { reason ->
                isDisabled = true
                disabledReason = reason
                showDisableReasonDialog = false
                onToggleAvailability?.invoke(cyclist.id, true, reason)
            }
        )
    }
}

@Composable
private fun DisableReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var customReason by remember { mutableStateOf("") }

    val predefinedReasons = listOf(
        "Lesão",
        "Abandono da Volta",
        "Suspensão",
        "Doença",
        "Desqualificação",
        "Fim de contrato",
        "Outro"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PersonOff,
                    contentDescription = null,
                    tint = Color(0xFFE74C3C),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Desativar Ciclista", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Seleciona o motivo da indisponibilidade:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                predefinedReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFE74C3C)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason)
                    }
                }

                if (selectedReason == "Outro") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Especificar motivo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Outro") {
                        customReason.ifBlank { "Outro" }
                    } else {
                        selectedReason
                    }
                    onConfirm(finalReason)
                },
                enabled = selectedReason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE74C3C)
                )
            ) {
                Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Desativar")
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
private fun StatBox(
    label: String,
    value: String,
    color: Color
) {
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

private fun getCategoryColor(category: CyclistCategory): Color {
    return when (category) {
        CyclistCategory.GC -> Color(0xFFFFD700)
        CyclistCategory.CLIMBER -> Color(0xFFE74C3C)
        CyclistCategory.SPRINT -> Color(0xFF2ECC71)
        CyclistCategory.TT -> Color(0xFF3498DB)
        CyclistCategory.HILLS -> Color(0xFFF39C12)
        CyclistCategory.ONEDAY -> Color(0xFF9B59B6)
    }
}

private fun getCategoryDisplayName(category: CyclistCategory): String {
    return when (category) {
        CyclistCategory.GC -> "Lider (GC)"
        CyclistCategory.CLIMBER -> "Escalador"
        CyclistCategory.SPRINT -> "Sprinter"
        CyclistCategory.TT -> "Contra-Relogio"
        CyclistCategory.HILLS -> "Puncher"
        CyclistCategory.ONEDAY -> "Classico"
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
            // Check if content looks valid (no replacement characters)
            if (!content.contains('\uFFFD')) {
                return content
            }
        } catch (_: Exception) { }
    }

    return String(bytes, Charsets.UTF_8)
}

// Helper function to parse CSV to cyclists
private fun parseCsvToCyclists(lines: List<String>): List<Cyclist> {
    val cyclists = mutableListOf<Cyclist>()

    for (line in lines) {
        if (line.isBlank()) continue

        val parts = line.split(",", ";", "\t").map { it.trim() }
        if (parts.size < 2) continue

        // Skip header line
        if (parts[0].equals("Name", ignoreCase = true) ||
            parts[0].equals("Nome", ignoreCase = true)) continue

        val name = parts.getOrNull(0) ?: continue
        val team = parts.getOrNull(1) ?: ""
        val rankingStr = parts.getOrNull(2) ?: "0"
        val link = parts.getOrNull(3) ?: ""
        val categoryStr = parts.getOrNull(4) ?: "GC"

        val category = when (categoryStr.uppercase()) {
            "GC", "LEADER", "LIDER" -> CyclistCategory.GC
            "CLIMBER", "ESCALADOR", "MOUNTAIN" -> CyclistCategory.CLIMBER
            "SPRINT", "SPRINTER" -> CyclistCategory.SPRINT
            "TT", "TIMETRAIL", "CONTRARRELOGIO" -> CyclistCategory.TT
            "HILLS", "PUNCHER", "HILLY" -> CyclistCategory.HILLS
            "ONEDAY", "CLASSIC", "CLASSICO" -> CyclistCategory.ONEDAY
            else -> CyclistCategory.GC
        }

        val ranking = rankingStr.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
        val price = calculatePriceFromRanking(ranking)

        val nameParts = name.split(" ")
        val firstName = nameParts.firstOrNull() ?: name
        val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else name

        cyclists.add(
            Cyclist(
                id = generateCyclistId(name),
                firstName = firstName,
                lastName = lastName,
                fullName = name,
                teamId = generateTeamId(team),
                teamName = team,
                nationality = "",
                photoUrl = null,
                category = category,
                price = price
            )
        )
    }

    return cyclists
}

private fun generateCyclistId(name: String): String {
    val normalized = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
        .replace("[^a-z0-9\\s]".toRegex(), "")
        .replace("\\s+".toRegex(), "-")
    return "cyclist-$normalized-${System.currentTimeMillis() % 10000}"
}

private fun generateTeamId(team: String): String {
    val normalized = Normalizer.normalize(team.lowercase(), Normalizer.Form.NFD)
        .replace("[^a-z0-9\\s]".toRegex(), "")
        .replace("\\s+".toRegex(), "-")
    return "team-$normalized"
}

private fun calculatePriceFromRanking(ranking: Int): Double {
    return when {
        ranking >= 10000 -> 15.0
        ranking >= 8000 -> 12.0
        ranking >= 6000 -> 10.0
        ranking >= 4000 -> 8.0
        ranking >= 2000 -> 6.0
        ranking >= 1000 -> 5.0
        else -> 4.0
    }
}
