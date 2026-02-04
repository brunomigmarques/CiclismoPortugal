package com.ciclismo.portugal.presentation.fantasy.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ciclismo.portugal.presentation.fantasy.wizard.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCreationWizardScreen(
    onNavigateBack: () -> Unit,
    onTeamCreated: () -> Unit,
    viewModel: TeamCreationWizardViewModel = hiltViewModel()
) {
    // Collect step number directly from ViewModel to avoid null issues
    val stepNumber by viewModel.currentStepNumber.collectAsState()
    val currentStepSelections by viewModel.currentStepSelections.collectAsState()
    val allSelections by viewModel.selections.collectAsState()
    val remainingBudget by viewModel.remainingBudget.collectAsState()
    val isCurrentStepComplete by viewModel.isCurrentStepComplete.collectAsState()
    val totalSelectedCount by viewModel.totalSelectedCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isConfirming by viewModel.isConfirming.collectAsState()
    val teamCreated by viewModel.teamCreated.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredCyclists by viewModel.filteredCyclists.collectAsState()

    // Transfer tracking state
    val transferCount by viewModel.transferCount.collectAsState()
    val transferPenalty by viewModel.transferPenalty.collectAsState()
    val remainingFreeTransfers by viewModel.remainingFreeTransfers.collectAsState()
    val existingTeam by viewModel.existingTeam.collectAsState()
    val showTransferWarning by viewModel.showTransferWarning.collectAsState()
    val showWildcardDialog by viewModel.showWildcardDialog.collectAsState()

    // Draft persistence state
    val showDraftDialog by viewModel.showDraftDialog.collectAsState()
    val draftAge by viewModel.draftAge.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Get the step object safely using the step number
    val safeStep = remember(stepNumber) {
        WizardStep.fromStepNumber(stepNumber)
    }

    // Navigate when team is created
    LaunchedEffect(teamCreated) {
        if (teamCreated) {
            onTeamCreated()
        }
    }

    // Collect messages
    LaunchedEffect(Unit) {
        viewModel.message.collect { message ->
            snackbarHostState.showSnackbar(message)
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
            // Header with progress
            WizardHeader(
                currentStep = safeStep,
                remainingBudget = remainingBudget,
                totalSelected = totalSelectedCount,
                onBack = {
                    if (stepNumber > 1) {
                        viewModel.goToPreviousStep()
                    } else {
                        onNavigateBack()
                    }
                }
            )

            // Main content area - use stepNumber for comparison instead of 'is' check
            if (stepNumber == WizardStep.Review.stepNumber) {
                WizardReviewContent(
                    allSelections = allSelections,
                    remainingBudget = remainingBudget,
                    onEditStep = { step -> viewModel.goToStep(step) },
                    onConfirm = { viewModel.checkAndConfirmTeam() },
                    isConfirming = isConfirming,
                    // Transfer tracking
                    isEditMode = viewModel.isEditMode,
                    transferCount = transferCount,
                    transferPenalty = transferPenalty,
                    remainingFreeTransfers = remainingFreeTransfers,
                    hasUnlimitedTransfers = existingTeam?.hasUnlimitedTransfers == true
                )
            } else {
                // Compute selected IDs from collected state to trigger recomposition
                val selectedIds = allSelections.values.flatten().map { it.id }.toSet()

                WizardSelectionContent(
                    step = safeStep,
                    cyclists = filteredCyclists,
                    selectedCyclists = currentStepSelections,
                    selectedIds = selectedIds,
                    searchQuery = searchQuery,
                    isLoading = isLoading,
                    isCurrentStepComplete = isCurrentStepComplete,
                    canSelectCyclist = { viewModel.canSelectCyclist(it) },
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    onSelectCyclist = { viewModel.selectCyclist(it) },
                    onDeselectCyclist = { viewModel.deselectCyclist(it) },
                    onBack = {
                        if (stepNumber > 1) {
                            viewModel.goToPreviousStep()
                        } else {
                            onNavigateBack()
                        }
                    },
                    onNext = { viewModel.goToNextStep() }
                )
            }
        }
    }

    // Transfer warning dialog
    if (showTransferWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTransferWarning() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFF39C12),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Penalizacao de Transferencias") },
            text = {
                Column {
                    Text(
                        "Vais fazer $transferCount transferencias, mas so tens $remainingFreeTransfers gratis esta semana."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Penalizacao: -$transferPenalty pontos",
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Alternativa: Ativa um Wildcard para transferencias ilimitadas sem penalizacao.",
                        fontSize = 13.sp,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmWithPenalty() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFF39C12)
                    )
                ) {
                    Text("Aceitar Penalizacao")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.dismissTransferWarning() }) {
                        Text("Cancelar")
                    }
                    TextButton(onClick = { viewModel.showWildcardOptions() }) {
                        Text("Usar Wildcard", color = androidx.compose.ui.graphics.Color(0xFF9C27B0))
                    }
                }
            }
        )
    }

    // Wildcard activation dialog
    if (showWildcardDialog) {
        val hasWildcard = existingTeam?.hasWildcard == true

        AlertDialog(
            onDismissRequest = { viewModel.dismissWildcardDialog() },
            icon = {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFF9C27B0),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Ativar Wildcard") },
            text = {
                Column {
                    Text("Usa o Wildcard para transferencias ilimitadas sem penalizacao:")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Wildcard option
                    Button(
                        onClick = { viewModel.activateWildcard() },
                        enabled = hasWildcard,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasWildcard) androidx.compose.ui.graphics.Color(0xFF9C27B0) else androidx.compose.ui.graphics.Color.Gray
                        )
                    ) {
                        Text(if (hasWildcard) "Wildcard (2x por temporada)" else "Wildcard (usado)")
                    }

                    if (!hasWildcard) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Nao tens wildcards disponiveis. Aceita a penalizacao ou cancela.",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color(0xFFE57373)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissWildcardDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Draft restore dialog
    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.discardDraft() },
            icon = {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFF006600),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Rascunho encontrado") },
            text = {
                Column {
                    Text(
                        "Tens um rascunho guardado $draftAge."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Queres continuar de onde paraste ou comecar do zero?",
                        fontSize = 14.sp,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.restoreDraft() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF006600)
                    )
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar rascunho")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.discardDraft() }) {
                    Text("Comecar do zero")
                }
            }
        )
    }
}

@Composable
private fun WizardSelectionContent(
    step: WizardStep,
    cyclists: List<com.ciclismo.portugal.domain.model.Cyclist>,
    selectedCyclists: List<com.ciclismo.portugal.domain.model.Cyclist>,
    selectedIds: Set<String>,
    searchQuery: String,
    isLoading: Boolean,
    isCurrentStepComplete: Boolean,
    canSelectCyclist: (com.ciclismo.portugal.domain.model.Cyclist) -> com.ciclismo.portugal.domain.rules.CyclistEligibility,
    onSearchChange: (String) -> Unit,
    onSelectCyclist: (com.ciclismo.portugal.domain.model.Cyclist) -> Unit,
    onDeselectCyclist: (com.ciclismo.portugal.domain.model.Cyclist) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val categoryColor = step.category?.let { getCategoryColor(it) }
        ?: androidx.compose.ui.graphics.Color(0xFF4CAF50)

    Column(modifier = Modifier.fillMaxSize()) {
        // Selection slots indicator
        SelectionSlotsIndicator(
            selected = selectedCyclists.size,
            required = step.requiredCount,
            categoryColor = categoryColor
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Pesquisar ciclista ou equipa...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Available cyclists list
        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = categoryColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("A carregar ciclistas...")
                }
            }
        } else if (cyclists.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank())
                        "Nenhum resultado para \"$searchQuery\""
                    else
                        "Nenhum ciclista disponivel nesta categoria"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cyclists, key = { it.id }) { cyclist ->
                    val isSelected = selectedIds.contains(cyclist.id)
                    val eligibility = canSelectCyclist(cyclist)

                    WizardCyclistCard(
                        cyclist = cyclist,
                        isSelected = isSelected,
                        eligibility = eligibility,
                        onSelect = { onSelectCyclist(cyclist) },
                        onDeselect = { onDeselectCyclist(cyclist) }
                    )
                }
            }
        }

        // Selected cyclists row at bottom
        if (selectedCyclists.isNotEmpty()) {
            SelectedCyclistsRow(
                cyclists = selectedCyclists,
                categoryColor = categoryColor,
                onRemove = onDeselectCyclist
            )
        }

        // Navigation buttons
        WizardNavigationButtons(
            canGoBack = step.stepNumber > 1,
            canGoNext = isCurrentStepComplete,
            currentCount = selectedCyclists.size,
            requiredCount = step.requiredCount,
            onBack = onBack,
            onNext = onNext
        )
    }
}
