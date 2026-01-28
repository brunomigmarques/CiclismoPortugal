package com.ciclismo.portugal.presentation.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    provaId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val conflictDialogState by viewModel.showConflictDialog.collectAsState()
    val context = LocalContext.current

    // Conflict Dialog
    if (conflictDialogState is ConflictDialogState.Showing) {
        val conflictingProva = (conflictDialogState as ConflictDialogState.Showing).conflictingProva
        AlertDialog(
            onDismissRequest = { viewModel.dismissConflictDialog() },
            title = { Text("Conflito de Data") },
            text = {
                Text(
                    "Já tem o evento \"${conflictingProva.nome}\" no calendário para a mesma data. " +
                    "Tem certeza que deseja adicionar este evento também?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAddWithConflict() }
                ) {
                    Text("Adicionar Mesmo Assim")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissConflictDialog() }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Prova") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is DetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DetailsUiState.Success -> {
                ProvaDetailsContent(
                    prova = state.prova,
                    onToggleCalendar = { viewModel.toggleCalendar() },
                    onOpenRegistration = { url ->
                        url?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            context.startActivity(intent)
                        }
                    },
                    onExportToCalendar = { prova ->
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = Uri.parse("content://com.android.calendar/events")
                            putExtra("title", prova.nome)
                            putExtra("eventLocation", prova.local)
                            putExtra("description", "${prova.descricao}\n\nTipo: ${prova.tipo}\nDistâncias: ${prova.distancias}\nOrganizador: ${prova.organizador}")
                            putExtra("beginTime", prova.data)
                            putExtra("endTime", prova.data + (6 * 60 * 60 * 1000)) // 6 horas depois
                            putExtra("allDay", false)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback se não houver app de calendário
                            android.widget.Toast.makeText(
                                context,
                                "Não foi possível abrir o calendário",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onShare = { prova ->
                        val shareText = "Confira esta prova: ${prova.nome} em ${prova.local}"
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partilhar prova"))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is DetailsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Erro: ${state.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvaDetailsContent(
    prova: Prova,
    onToggleCalendar: () -> Unit,
    onOpenRegistration: (String?) -> Unit,
    onExportToCalendar: (Prova) -> Unit,
    onShare: (Prova) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event Image
        prova.imageUrl?.let { imageUrl ->
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .align(Alignment.CenterHorizontally)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = prova.nome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Title
        Text(
            text = prova.nome,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Type Chip
        AssistChip(
            onClick = {},
            label = { Text(prova.tipo) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = getProvaTypeColor(prova.tipo)
            )
        )

        Divider()

        // Information Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow("📅 Data", formatDate(prova.data))
                InfoRow("📍 Local", prova.local)
                InfoRow("🚴 Distâncias", prova.distancias)
                InfoRow("💰 Preço", prova.preco)
                prova.prazoInscricao?.let {
                    InfoRow("⏰ Prazo Inscrição", formatDate(it))
                }
                InfoRow("👥 Organizador", prova.organizador)
            }
        }

        // Description
        if (prova.descricao.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Descrição",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prova.descricao,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Action Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Registration Button
            prova.urlInscricao?.let { url ->
                Button(
                    onClick = { onOpenRegistration(url) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abrir Inscrição")
                }
            }

            // Calendar Button
            OutlinedButton(
                onClick = onToggleCalendar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (prova.inCalendar) "Remover do Calendário" else "Adicionar ao Calendário"
                )
            }

            // Export to External Calendar Button
            OutlinedButton(
                onClick = { onExportToCalendar(prova) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar para Google/Outlook")
            }

            // Share Button
            OutlinedButton(
                onClick = { onShare(prova) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Partilhar")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getProvaTypeColor(tipo: String): androidx.compose.ui.graphics.Color {
    return when (tipo) {
        "Estrada" -> TypeEstrada
        "BTT" -> TypeBTT
        "Pista" -> TypePista
        "Ciclocross" -> TypeCiclocross
        "Gran Fondo" -> TypeGranFondo
        else -> Primary
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
    sdf.timeZone = TimeZone.getTimeZone("Europe/Lisbon")
    return sdf.format(Date(timestamp))
}
