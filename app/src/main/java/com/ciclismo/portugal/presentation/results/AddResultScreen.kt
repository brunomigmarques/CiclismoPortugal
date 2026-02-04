package com.ciclismo.portugal.presentation.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.ResultSource
import com.ciclismo.portugal.domain.model.UserRaceType
import com.ciclismo.portugal.presentation.theme.AppImages
import com.ciclismo.portugal.presentation.theme.PortugueseGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResultScreen(
    onNavigateBack: () -> Unit,
    onResultSaved: () -> Unit,
    viewModel: AddResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var raceTypeExpanded by remember { mutableStateOf(false) }
    var resultSourceExpanded by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = viewModel.raceDate.value
    )

    LaunchedEffect(uiState) {
        if (uiState is AddResultUiState.Success) {
            onResultSaved()
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
                            text = "Adicionar Resultado",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Regista uma nova prova",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Form content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Race Name
            OutlinedTextField(
                value = viewModel.raceName.value,
                onValueChange = { viewModel.raceName.value = it },
                label = { Text("Nome da Prova *") },
                placeholder = { Text("Ex: RaceNature Albufeira") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.EmojiEvents, null) }
            )

            // Date picker
            OutlinedTextField(
                value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(viewModel.raceDate.value)),
                onValueChange = { },
                label = { Text("Data da Prova *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.EditCalendar, "Escolher data")
                    }
                }
            )

            // Location
            OutlinedTextField(
                value = viewModel.raceLocation.value,
                onValueChange = { viewModel.raceLocation.value = it },
                label = { Text("Local *") },
                placeholder = { Text("Ex: Albufeira, Algarve") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
            )

            // Race Type dropdown
            ExposedDropdownMenuBox(
                expanded = raceTypeExpanded,
                onExpandedChange = { raceTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.raceType.value.displayName,
                    onValueChange = { },
                    label = { Text("Tipo de Prova *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = raceTypeExpanded) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, null) }
                )
                ExposedDropdownMenu(
                    expanded = raceTypeExpanded,
                    onDismissRequest = { raceTypeExpanded = false }
                ) {
                    UserRaceType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                viewModel.raceType.value = type
                                raceTypeExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Position section
            Text(
                "Resultado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.position.value,
                    onValueChange = { viewModel.position.value = it.filter { c -> c.isDigit() } },
                    label = { Text("Posicao") },
                    placeholder = { Text("Ex: 42") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = viewModel.totalParticipants.value,
                    onValueChange = { viewModel.totalParticipants.value = it.filter { c -> c.isDigit() } },
                    label = { Text("Total") },
                    placeholder = { Text("Ex: 500") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.categoryPosition.value,
                    onValueChange = { viewModel.categoryPosition.value = it.filter { c -> c.isDigit() } },
                    label = { Text("Pos. Categoria") },
                    placeholder = { Text("Ex: 5") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = viewModel.category.value,
                    onValueChange = { viewModel.category.value = it },
                    label = { Text("Categoria") },
                    placeholder = { Text("Ex: M40") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            HorizontalDivider()

            // Race details
            Text(
                "Detalhes da Prova",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.distance.value,
                    onValueChange = { viewModel.distance.value = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Distancia (km)") },
                    placeholder = { Text("Ex: 65") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = viewModel.elevation.value,
                    onValueChange = { viewModel.elevation.value = it.filter { c -> c.isDigit() } },
                    label = { Text("Desnivel (m)") },
                    placeholder = { Text("Ex: 1200") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.finishTime.value,
                    onValueChange = { viewModel.finishTime.value = it },
                    label = { Text("Tempo") },
                    placeholder = { Text("HH:MM:SS") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.bibNumber.value,
                    onValueChange = { viewModel.bibNumber.value = it.filter { c -> c.isDigit() } },
                    label = { Text("Dorsal") },
                    placeholder = { Text("Ex: 123") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Result source dropdown
            ExposedDropdownMenuBox(
                expanded = resultSourceExpanded,
                onExpandedChange = { resultSourceExpanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.resultSource.value.displayName,
                    onValueChange = { },
                    label = { Text("Fonte do Resultado") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resultSourceExpanded) },
                    leadingIcon = { Icon(Icons.Default.Source, null) }
                )
                ExposedDropdownMenu(
                    expanded = resultSourceExpanded,
                    onDismissRequest = { resultSourceExpanded = false }
                ) {
                    ResultSource.entries.forEach { source ->
                        DropdownMenuItem(
                            text = { Text(source.displayName) },
                            onClick = {
                                viewModel.resultSource.value = source
                                resultSourceExpanded = false
                            }
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = viewModel.notes.value,
                onValueChange = { viewModel.notes.value = it },
                label = { Text("Notas") },
                placeholder = { Text("Observacoes sobre a prova...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = { viewModel.saveResult() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is AddResultUiState.Saving && viewModel.isFormValid(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortugueseGreen
                )
            ) {
                if (uiState is AddResultUiState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GUARDAR RESULTADO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error message
            if (uiState is AddResultUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (uiState as AddResultUiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.raceDate.value = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

sealed class AddResultUiState {
    data object Idle : AddResultUiState()
    data object Saving : AddResultUiState()
    data object Success : AddResultUiState()
    data class Error(val message: String) : AddResultUiState()
}
