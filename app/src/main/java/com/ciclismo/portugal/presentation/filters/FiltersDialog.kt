package com.ciclismo.portugal.presentation.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ciclismo.portugal.domain.model.ProvaFilters
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersDialog(
    currentFilters: ProvaFilters,
    onDismiss: () -> Unit,
    onApply: (ProvaFilters) -> Unit,
    viewModel: FiltersViewModel = hiltViewModel()
) {
    var selectedTipo by remember { mutableStateOf(currentFilters.tipo ?: "Todos") }
    var selectedLocal by remember { mutableStateOf(currentFilters.local ?: "Todos") }

    // Carrega tipos e locais dinâmicos dos dados
    val availableTipos by viewModel.availableTipos.collectAsState()
    val availableLocais by viewModel.availableLocais.collectAsState()

    // Adiciona "Todos" como primeira opção
    val tipos = remember(availableTipos) {
        listOf("Todos") + availableTipos
    }
    val locais = remember(availableLocais) {
        listOf("Todos") + availableLocais
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtros Avançados") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filtro por Tipo
                Text(
                    "Tipo de Prova",
                    style = MaterialTheme.typography.titleMedium
                )

                tipos.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { tipo ->
                            FilterChip(
                                selected = selectedTipo == tipo,
                                onClick = { selectedTipo = tipo },
                                label = { Text(tipo) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Divider()

                // Filtro por Local
                Text(
                    "Local/Região",
                    style = MaterialTheme.typography.titleMedium
                )

                locais.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { local ->
                            FilterChip(
                                selected = selectedLocal == local,
                                onClick = { selectedLocal = local },
                                label = { Text(local) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val filters = ProvaFilters(
                        tipo = if (selectedTipo == "Todos") null else selectedTipo,
                        local = if (selectedLocal == "Todos") null else selectedLocal
                    )
                    onApply(filters)
                    onDismiss()
                }
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
