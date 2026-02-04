package com.ciclismo.portugal.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.presentation.theme.AppImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceReductionAdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: PriceReductionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Reduction percentage slider
    var reductionPercent by remember { mutableFloatStateOf(10f) }

    // Selected category (null = all)
    var selectedCategory by remember { mutableStateOf<CyclistCategory?>(null) }

    // Confirmation dialog
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    model = AppImages.ROAD_CYCLING,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay with admin red/orange tint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE65100).copy(alpha = 0.7f),
                                    Color(0xFFFF6F00).copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Header content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
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
                            text = "Redução de Preços",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Ajustar preços dos ciclistas",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Estatísticas Atuais",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        when (uiState) {
                            is PriceReductionUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is PriceReductionUiState.Updating -> {
                                // Show updating stats while operation is in progress
                                val updating = (uiState as PriceReductionUiState.Updating)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem(
                                        label = "Progresso",
                                        value = "${updating.currentIndex}/${updating.totalCyclists}",
                                        icon = Icons.Default.Sync
                                    )
                                    StatItem(
                                        label = "Sucesso",
                                        value = updating.successCount.toString(),
                                        icon = Icons.Default.Check
                                    )
                                }
                            }
                            is PriceReductionUiState.Success -> {
                                val stats = (uiState as PriceReductionUiState.Success)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem(
                                        label = "Total Ciclistas",
                                        value = stats.totalCyclists.toString(),
                                        icon = Icons.Default.People
                                    )
                                    StatItem(
                                        label = "Preço Médio",
                                        value = "${String.format("%.1f", stats.averagePrice)}M",
                                        icon = Icons.Default.Euro
                                    )
                                }
                            }
                            is PriceReductionUiState.Error -> {
                                Text(
                                    "Erro ao carregar dados",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Category selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Selecionar Categoria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Escolhe uma categoria específica ou aplica a todos os ciclistas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // All cyclists option
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Todos os Ciclistas") },
                            leadingIcon = {
                                if (selectedCategory == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyclistCategory.entries.take(3).forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(getCategoryDisplayName(category)) },
                                    leadingIcon = {
                                        if (selectedCategory == category) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = getCategoryColor(category).copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyclistCategory.entries.drop(3).forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(getCategoryDisplayName(category)) },
                                    leadingIcon = {
                                        if (selectedCategory == category) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = getCategoryColor(category).copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Reduction percentage slider
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Percentagem de Redução",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE53935).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "-${reductionPercent.toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = reductionPercent,
                            onValueChange = { reductionPercent = it },
                            valueRange = 5f..50f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE53935),
                                activeTrackColor = Color(0xFFE53935)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("5%", style = MaterialTheme.typography.bodySmall)
                            Text("50%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Preview card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Preview,
                                contentDescription = null,
                                tint = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pré-visualização",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val categoryText = selectedCategory?.let { getCategoryDisplayName(it) } ?: "todos os ciclistas"
                        Text(
                            "Os preços de $categoryText serão reduzidos em ${reductionPercent.toInt()}%.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Exemplo: 10.0M → ${String.format("%.1f", 10.0 * (1 - reductionPercent / 100))}M",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        enabled = uiState is PriceReductionUiState.Success
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aplicar Redução")
                    }
                }

                // Warning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Atenção: Esta ação afeta todos os utilizadores e não pode ser revertida automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Confirmation dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text("Confirmar Redução de Preços") },
                text = {
                    Column {
                        val categoryText = selectedCategory?.let { getCategoryDisplayName(it) } ?: "todos os ciclistas"
                        Text(
                            "Tens a certeza que queres reduzir os preços de $categoryText em ${reductionPercent.toInt()}%?"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Esta ação não pode ser revertida automaticamente.",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.applyPriceReduction(
                                reductionPercent = reductionPercent.toInt(),
                                category = selectedCategory
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("Confirmar Redução")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun getCategoryDisplayName(category: CyclistCategory): String {
    return when (category) {
        CyclistCategory.CLIMBER -> "Climber"
        CyclistCategory.HILLS -> "Puncher"
        CyclistCategory.TT -> "TT"
        CyclistCategory.SPRINT -> "Sprint"
        CyclistCategory.GC -> "GC"
        CyclistCategory.ONEDAY -> "Clássicas"
    }
}

private fun getCategoryColor(category: CyclistCategory): Color {
    return when (category) {
        CyclistCategory.CLIMBER -> Color(0xFFE74C3C)
        CyclistCategory.HILLS -> Color(0xFF9B59B6)
        CyclistCategory.TT -> Color(0xFF3498DB)
        CyclistCategory.SPRINT -> Color(0xFF2ECC71)
        CyclistCategory.GC -> Color(0xFFF39C12)
        CyclistCategory.ONEDAY -> Color(0xFF1ABC9C)
    }
}
