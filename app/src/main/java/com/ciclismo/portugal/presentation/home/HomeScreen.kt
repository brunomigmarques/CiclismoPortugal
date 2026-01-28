package com.ciclismo.portugal.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ciclismo.portugal.presentation.ads.BannerAdView
import com.ciclismo.portugal.presentation.filters.FiltersDialog
import com.ciclismo.portugal.presentation.home.components.ProvaCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProvaClick: (Long) -> Unit,
    onAdminAccess: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val filters by viewModel.filters.collectAsState()

    var showFiltersDialog by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    if (showFiltersDialog) {
        FiltersDialog(
            currentFilters = filters,
            onDismiss = { showFiltersDialog = false },
            onApply = { newFilters ->
                viewModel.applyFilters(newFilters)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Provas de Ciclismo",
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastTapTime < 500) {
                                tapCount++
                                if (tapCount >= 5) {
                                    onAdminAccess()
                                    tapCount = 0
                                }
                            } else {
                                tapCount = 1
                            }
                            lastTapTime = currentTime
                        }
                    )
                },
                actions = {
                    // Botão de Filtros
                    IconButton(onClick = { showFiltersDialog = true }) {
                        Badge(
                            containerColor = if (filters.isActive()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtros",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    // Botão de Refresh
                    IconButton(onClick = { viewModel.syncProvas() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Pesquisar provas...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                },
                singleLine = true
            )

            // Active Filters Indicator
            if (filters.isActive()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Filtros ativos:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    filters.tipo?.let {
                        AssistChip(
                            onClick = { },
                            label = { Text(it, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                    filters.local?.let {
                        AssistChip(
                            onClick = { },
                            label = { Text(it, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("Limpar", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Content
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is HomeUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhuma prova encontrada",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isRefreshing) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }

                is HomeUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Banner Ad no topo
                        item {
                            BannerAdView(modifier = Modifier.padding(bottom = 8.dp))
                        }

                        if (isRefreshing) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        items(
                            items = state.provas,
                            key = { it.id }
                        ) { prova ->
                            ProvaCard(
                                prova = prova,
                                onClick = { onProvaClick(prova.id) }
                            )
                        }

                        // Banner Ad adicional a cada 10 items
                        if (state.provas.size > 10) {
                            item {
                                BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Erro: ${state.message}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.syncProvas() }) {
                                Text("Tentar novamente")
                            }
                        }
                    }
                }
            }
        }
    }
}
