package com.ciclismo.portugal.presentation.fantasy.wizard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.local.WizardDraftStorage
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.repository.AuthRepository
import com.ciclismo.portugal.domain.repository.CyclistRepository
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.rules.CyclistEligibility
import com.ciclismo.portugal.domain.rules.FantasyGameRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamCreationWizardViewModel @Inject constructor(
    private val cyclistRepository: CyclistRepository,
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val authRepository: AuthRepository,
    private val draftStorage: WizardDraftStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "WizardVM"
        const val TRANSFER_PENALTY_POINTS = 4 // Points penalty per extra transfer
    }

    // Team name from navigation argument
    val teamName: String = savedStateHandle.get<String>("teamName") ?: ""

    // Edit mode - true when editing existing team (not creating new)
    val isEditMode: Boolean = savedStateHandle.get<Boolean>("isEditMode") ?: false

    // Current wizard step - use step NUMBER (primitive Int) to avoid null issues
    private val _currentStepNumber = MutableStateFlow(1)
    val currentStepNumber: StateFlow<Int> = _currentStepNumber.asStateFlow()

    // Original team cyclists (for tracking transfers in edit mode)
    private val _originalCyclistIds = MutableStateFlow<Set<String>>(emptySet())

    // Track transfers made
    private val _transfersIn = MutableStateFlow<Set<String>>(emptySet()) // New cyclists added
    private val _transfersOut = MutableStateFlow<Set<String>>(emptySet()) // Cyclists removed

    // Existing team info for transfer calculations
    private val _existingTeam = MutableStateFlow<com.ciclismo.portugal.domain.model.FantasyTeam?>(null)
    val existingTeam: StateFlow<com.ciclismo.portugal.domain.model.FantasyTeam?> = _existingTeam.asStateFlow()

    // Transfer tracking
    val transferCount: StateFlow<Int> = combine(_transfersIn, _transfersOut) { tIn, tOut ->
        tIn.size + tOut.size
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Transfer penalty calculation
    val transferPenalty: StateFlow<Int> = combine(transferCount, _existingTeam) { count, team ->
        if (team == null) return@combine 0
        if (team.hasUnlimitedTransfers) return@combine 0
        val freeTransfers = team.freeTransfers - team.transfersMadeThisWeek
        val paidTransfers = maxOf(0, count - freeTransfers)
        paidTransfers * TRANSFER_PENALTY_POINTS
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Remaining free transfers
    val remainingFreeTransfers: StateFlow<Int> = _existingTeam.map { team ->
        team?.remainingFreeTransfers ?: 2
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    // Show transfer warning dialog
    private val _showTransferWarning = MutableStateFlow(false)
    val showTransferWarning: StateFlow<Boolean> = _showTransferWarning.asStateFlow()

    // Show wildcard activation dialog
    private val _showWildcardDialog = MutableStateFlow(false)
    val showWildcardDialog: StateFlow<Boolean> = _showWildcardDialog.asStateFlow()

    // Helper to get the current step object (derived from step number)
    private val _currentStep: WizardStep
        get() = WizardStep.fromStepNumber(_currentStepNumber.value)

    // Selections per category
    private val _selections = MutableStateFlow<Map<CyclistCategory, List<Cyclist>>>(emptyMap())
    val selections: StateFlow<Map<CyclistCategory, List<Cyclist>>> = _selections.asStateFlow()

    // All validated cyclists (loaded once)
    private val _allCyclists = MutableStateFlow<List<Cyclist>>(emptyList())

    // Available cyclists for current step (filtered by category)
    private val _availableCyclists = MutableStateFlow<List<Cyclist>>(emptyList())
    val availableCyclists: StateFlow<List<Cyclist>> = _availableCyclists.asStateFlow()

    // Budget tracking
    private val _remainingBudget = MutableStateFlow(FantasyGameRules.INITIAL_BUDGET)
    val remainingBudget: StateFlow<Double> = _remainingBudget.asStateFlow()

    // Pro team count tracking (max 3 from same team)
    private val _proTeamCount = MutableStateFlow<Map<String, Int>>(emptyMap())

    // UI States
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    private val _teamCreated = MutableStateFlow(false)
    val teamCreated: StateFlow<Boolean> = _teamCreated.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    // Search within current category
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Derived state: filtered cyclists for display (combines available + search)
    val filteredCyclists: StateFlow<List<Cyclist>> = combine(
        _availableCyclists, _searchQuery
    ) { available, query ->
        if (query.isBlank()) {
            available
        } else {
            available.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.teamName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Derived state: current step's selected cyclists
    val currentStepSelections: StateFlow<List<Cyclist>> = combine(
        _currentStepNumber, _selections
    ) { stepNumber, selections ->
        val step = WizardStep.fromStepNumber(stepNumber)
        step.category?.let { selections[it] } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived state: is current step complete?
    val isCurrentStepComplete: StateFlow<Boolean> = combine(
        _currentStepNumber, currentStepSelections
    ) { stepNumber, selections ->
        val step = WizardStep.fromStepNumber(stepNumber)
        selections.size >= step.requiredCount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Derived state: total selected count
    val totalSelectedCount: StateFlow<Int> = _selections.map { selections ->
        selections.values.sumOf { it.size }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Derived state: spent budget
    val spentBudget: StateFlow<Double> = _selections.map { selections ->
        selections.values.flatten().sumOf { it.price }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Draft restoration state
    private val _hasDraft = MutableStateFlow(false)
    val hasDraft: StateFlow<Boolean> = _hasDraft.asStateFlow()

    private val _draftAge = MutableStateFlow<String?>(null)
    val draftAge: StateFlow<String?> = _draftAge.asStateFlow()

    // Show draft restore dialog
    private val _showDraftDialog = MutableStateFlow(false)
    val showDraftDialog: StateFlow<Boolean> = _showDraftDialog.asStateFlow()

    init {
        loadAllCyclists()
        if (isEditMode) {
            loadExistingTeam()
        } else {
            // Check for saved draft when creating new team
            checkForDraft()
        }
    }

    private fun checkForDraft() {
        if (draftStorage.hasDraft()) {
            _hasDraft.value = true
            _draftAge.value = draftStorage.getDraftAgeDescription()
            _showDraftDialog.value = true
            Log.d(TAG, "Found draft from ${_draftAge.value}")
        }
    }

    /**
     * Restore draft selections after cyclists are loaded.
     */
    fun restoreDraft() {
        val draft = draftStorage.loadDraft() ?: return

        viewModelScope.launch {
            // Wait for cyclists to be loaded
            _allCyclists.first { it.isNotEmpty() }

            val cyclistMap = _allCyclists.value.associateBy { it.id }
            val restoredSelections = mutableMapOf<CyclistCategory, List<Cyclist>>()

            draft.selections.forEach { (categoryName, cyclistIds) ->
                try {
                    val category = CyclistCategory.valueOf(categoryName)
                    val cyclists = cyclistIds.mapNotNull { cyclistMap[it] }
                    if (cyclists.isNotEmpty()) {
                        restoredSelections[category] = cyclists
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring category $categoryName: ${e.message}")
                }
            }

            if (restoredSelections.isNotEmpty()) {
                _selections.value = restoredSelections

                // Recalculate budget
                val usedBudget = restoredSelections.values.flatten().sumOf { it.price }
                _remainingBudget.value = FantasyGameRules.INITIAL_BUDGET - usedBudget

                // Recalculate pro team counts
                val proTeamCounts = restoredSelections.values.flatten()
                    .groupBy { it.teamId }
                    .mapValues { it.value.size }
                _proTeamCount.value = proTeamCounts

                // Restore step
                _currentStepNumber.value = draft.currentStep
                filterCyclistsForCurrentStep()

                Log.d(TAG, "Restored draft: ${restoredSelections.values.flatten().size} cyclists, step ${draft.currentStep}")
                _message.emit("Rascunho restaurado!")
            }

            _showDraftDialog.value = false
        }
    }

    /**
     * Discard the saved draft and start fresh.
     */
    fun discardDraft() {
        draftStorage.clearDraft()
        _hasDraft.value = false
        _showDraftDialog.value = false
        Log.d(TAG, "Draft discarded")
    }

    /**
     * Save current selections as draft.
     */
    private fun saveDraft() {
        if (isEditMode) return // Don't save drafts in edit mode

        val selections = _selections.value
        if (selections.isEmpty()) {
            draftStorage.clearDraft()
            return
        }

        val selectionsIds = selections.mapValues { (_, cyclists) ->
            cyclists.map { it.id }
        }

        draftStorage.saveDraft(
            teamName = teamName,
            selections = selectionsIds,
            currentStep = _currentStepNumber.value
        )
    }

    private fun loadExistingTeam() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch

            // Get existing team
            fantasyTeamRepository.getTeamByUserId(user.id)
                .firstOrNull()
                ?.let { team ->
                    _existingTeam.value = team
                    Log.d(TAG, "Loaded existing team: ${team.teamName}, freeTransfers: ${team.remainingFreeTransfers}")

                    // Load existing cyclists
                    fantasyTeamRepository.getTeamCyclistsWithDetails(team.id)
                        .firstOrNull()
                        ?.let { teamCyclists ->
                            val cyclistMap = _allCyclists.value.associateBy { it.id }

                            // Store original cyclist IDs
                            _originalCyclistIds.value = teamCyclists.map { it.second.id }.toSet()

                            // Pre-populate selections by category
                            val selectionsByCategory = teamCyclists
                                .mapNotNull { (_, cyclist) -> cyclistMap[cyclist.id] ?: cyclist }
                                .groupBy { it.category }

                            _selections.value = selectionsByCategory

                            // Calculate used budget from existing team
                            val usedBudget = teamCyclists.sumOf { it.second.price }
                            _remainingBudget.value = FantasyGameRules.INITIAL_BUDGET - usedBudget

                            // Update pro team counts
                            val proTeamCounts = teamCyclists
                                .groupBy { it.second.teamId }
                                .mapValues { it.value.size }
                            _proTeamCount.value = proTeamCounts

                            Log.d(TAG, "Pre-populated ${teamCyclists.size} cyclists, budget: ${_remainingBudget.value}M")
                        }
                }
        }
    }

    private fun loadAllCyclists() {
        viewModelScope.launch {
            _isLoading.value = true
            cyclistRepository.getValidatedCyclists()
                .catch { e ->
                    Log.e(TAG, "Error loading cyclists: ${e.message}")
                    _message.emit("Erro ao carregar ciclistas")
                }
                .collect { cyclists ->
                    _allCyclists.value = cyclists.sortedByDescending { it.price }
                    Log.d(TAG, "Loaded ${cyclists.size} cyclists")
                    filterCyclistsForCurrentStep()
                    _isLoading.value = false
                }
        }
    }

    private fun filterCyclistsForCurrentStep() {
        val step = _currentStep
        val category = step.category ?: return

        val filtered = _allCyclists.value
            .filter { it.category == category }
            .sortedByDescending { it.price }

        _availableCyclists.value = filtered
        Log.d(TAG, "Filtered ${filtered.size} cyclists for category $category")
    }

    fun selectCyclist(cyclist: Cyclist) {
        val step = _currentStep
        val category = step.category ?: return
        val currentSelections = _selections.value[category] ?: emptyList()

        // Check if already selected
        if (currentSelections.any { it.id == cyclist.id }) {
            viewModelScope.launch {
                _message.emit("Ciclista ja selecionado")
            }
            return
        }

        // Check category limit
        if (currentSelections.size >= step.requiredCount) {
            viewModelScope.launch {
                _message.emit("Ja selecionaste ${step.requiredCount} ciclistas desta categoria")
            }
            return
        }

        // Check budget
        if (cyclist.price > _remainingBudget.value) {
            viewModelScope.launch {
                _message.emit("Orcamento insuficiente (${String.format("%.1f", _remainingBudget.value)}M disponivel)")
            }
            return
        }

        // Check pro team limit (max 3 from same team)
        val currentFromTeam = _proTeamCount.value[cyclist.teamId] ?: 0
        if (currentFromTeam >= FantasyGameRules.MAX_FROM_SAME_PRO_TEAM) {
            viewModelScope.launch {
                _message.emit("Maximo ${FantasyGameRules.MAX_FROM_SAME_PRO_TEAM} ciclistas da mesma equipa")
            }
            return
        }

        // Add selection
        val newSelections = _selections.value.toMutableMap()
        newSelections[category] = currentSelections + cyclist
        _selections.value = newSelections

        // Update budget
        _remainingBudget.value -= cyclist.price

        // Update pro team count
        val newProTeamCount = _proTeamCount.value.toMutableMap()
        newProTeamCount[cyclist.teamId] = currentFromTeam + 1
        _proTeamCount.value = newProTeamCount

        // Track transfer if in edit mode
        if (isEditMode && cyclist.id !in _originalCyclistIds.value) {
            _transfersIn.value = _transfersIn.value + cyclist.id
            Log.d(TAG, "Transfer IN: ${cyclist.fullName}")
        }

        // Save draft for persistence
        saveDraft()

        Log.d(TAG, "Selected ${cyclist.fullName} - Budget: ${_remainingBudget.value}M, Transfers: ${transferCount.value}")
    }

    fun deselectCyclist(cyclist: Cyclist) {
        // Find which category this cyclist belongs to
        val category = cyclist.category
        val currentSelections = _selections.value[category] ?: emptyList()

        if (!currentSelections.any { it.id == cyclist.id }) return

        // Remove selection
        val newSelections = _selections.value.toMutableMap()
        newSelections[category] = currentSelections.filter { it.id != cyclist.id }
        _selections.value = newSelections

        // Restore budget
        _remainingBudget.value += cyclist.price

        // Update pro team count
        val currentFromTeam = _proTeamCount.value[cyclist.teamId] ?: 1
        val newProTeamCount = _proTeamCount.value.toMutableMap()
        if (currentFromTeam > 1) {
            newProTeamCount[cyclist.teamId] = currentFromTeam - 1
        } else {
            newProTeamCount.remove(cyclist.teamId)
        }
        _proTeamCount.value = newProTeamCount

        // Track transfer if in edit mode
        if (isEditMode) {
            if (cyclist.id in _originalCyclistIds.value) {
                // Removing original cyclist = transfer out
                _transfersOut.value = _transfersOut.value + cyclist.id
                Log.d(TAG, "Transfer OUT: ${cyclist.fullName}")
            } else {
                // Removing newly added cyclist = undo transfer in
                _transfersIn.value = _transfersIn.value - cyclist.id
                Log.d(TAG, "Undo Transfer IN: ${cyclist.fullName}")
            }
        }

        // Save draft for persistence
        saveDraft()

        Log.d(TAG, "Deselected ${cyclist.fullName} - Budget: ${_remainingBudget.value}M, Transfers: ${transferCount.value}")
    }

    fun goToNextStep() {
        val currentNumber = _currentStepNumber.value
        if (currentNumber < WizardStep.Review.stepNumber) {
            val nextNumber = currentNumber + 1
            val nextStep = WizardStep.fromStepNumber(nextNumber)
            // Pre-filter cyclists for next step BEFORE changing step
            // This ensures data is ready when UI recomposes
            nextStep.category?.let { category ->
                val filtered = _allCyclists.value
                    .filter { it.category == category }
                    .sortedByDescending { it.price }
                _availableCyclists.value = filtered
                Log.d(TAG, "Pre-filtered ${filtered.size} cyclists for step $nextNumber ($category)")
            }
            _searchQuery.value = ""
            _currentStepNumber.value = nextNumber
        }
    }

    fun goToPreviousStep() {
        val currentNumber = _currentStepNumber.value
        if (currentNumber > 1) {
            val prevNumber = currentNumber - 1
            val prevStep = WizardStep.fromStepNumber(prevNumber)
            // Pre-filter cyclists for previous step BEFORE changing step
            prevStep.category?.let { category ->
                val filtered = _allCyclists.value
                    .filter { it.category == category }
                    .sortedByDescending { it.price }
                _availableCyclists.value = filtered
            }
            _searchQuery.value = ""
            _currentStepNumber.value = prevNumber
        }
    }

    fun goToStep(targetStepNumber: Int) {
        if (targetStepNumber in 1..WizardStep.Review.stepNumber) {
            val targetStep = WizardStep.fromStepNumber(targetStepNumber)
            // Pre-filter cyclists for target step BEFORE changing step
            targetStep.category?.let { category ->
                val filtered = _allCyclists.value
                    .filter { it.category == category }
                    .sortedByDescending { it.price }
                _availableCyclists.value = filtered
            }
            _searchQuery.value = ""
            _currentStepNumber.value = targetStepNumber
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredCyclists(): List<Cyclist> {
        val query = _searchQuery.value
        return if (query.isBlank()) {
            _availableCyclists.value
        } else {
            _availableCyclists.value.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.teamName.contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * Check transfers and show warning if exceeding free transfers
     */
    fun checkAndConfirmTeam() {
        val team = _existingTeam.value
        val penalty = transferPenalty.value

        if (isEditMode && team != null && penalty > 0 && !team.hasUnlimitedTransfers) {
            // Show transfer warning dialog
            _showTransferWarning.value = true
        } else {
            // No penalty or unlimited transfers - proceed directly
            confirmTeam()
        }
    }

    fun dismissTransferWarning() {
        _showTransferWarning.value = false
    }

    fun dismissWildcardDialog() {
        _showWildcardDialog.value = false
    }

    fun showWildcardOptions() {
        _showTransferWarning.value = false
        _showWildcardDialog.value = true
    }

    fun activateWildcard() {
        viewModelScope.launch {
            val team = _existingTeam.value ?: return@launch
            fantasyTeamRepository.useWildcard(team.id).fold(
                onSuccess = {
                    _existingTeam.value = team.copy(wildcardUsed = true, wildcardActive = true)
                    _showWildcardDialog.value = false
                    _message.emit("Wildcard ativado! Transferencias ilimitadas.")
                    confirmTeam()
                },
                onFailure = { error ->
                    _message.emit("Erro ao ativar wildcard: ${error.message}")
                }
            )
        }
    }

    fun confirmWithPenalty() {
        _showTransferWarning.value = false
        confirmTeam()
    }

    fun confirmTeam() {
        viewModelScope.launch {
            _isConfirming.value = true

            val user = authRepository.getCurrentUser()
            if (user == null) {
                _message.emit("Utilizador nao autenticado")
                _isConfirming.value = false
                return@launch
            }

            try {
                // Check if user already has a team
                val existingTeam = fantasyTeamRepository.getTeamByUserId(user.id)
                    .firstOrNull()

                val team = if (existingTeam != null) {
                    if (isEditMode) {
                        // Edit mode: only remove/add changed cyclists
                        Log.d(TAG, "Edit mode: applying ${_transfersOut.value.size} removals, ${_transfersIn.value.size} additions")

                        // Remove cyclists that were removed (transfers out)
                        coroutineScope {
                            _transfersOut.value.map { cyclistId ->
                                async {
                                    fantasyTeamRepository.removeCyclistFromTeam(existingTeam.id, cyclistId)
                                }
                            }.awaitAll()
                        }

                        // Update team with transfer count for penalty tracking
                        val penalty = transferPenalty.value
                        val newTransferCount = existingTeam.transfersMadeThisWeek + transferCount.value
                        val updatedTeam = existingTeam.copy(
                            transfersMadeThisWeek = newTransferCount,
                            totalPoints = existingTeam.totalPoints - penalty // Apply penalty
                        )
                        fantasyTeamRepository.updateTeam(updatedTeam)

                        if (penalty > 0) {
                            Log.d(TAG, "Applied transfer penalty: -$penalty points")
                        }

                        existingTeam
                    } else {
                        // Full rebuild mode (with wildcard/free hit active) - clear all cyclists
                        Log.d(TAG, "Full rebuild: clearing all ${existingTeam.id} cyclists")

                        val currentCyclists = fantasyTeamRepository.getTeamCyclists(existingTeam.id)
                            .firstOrNull() ?: emptyList()

                        coroutineScope {
                            currentCyclists.map { tc ->
                                async {
                                    fantasyTeamRepository.removeCyclistFromTeam(existingTeam.id, tc.cyclistId)
                                }
                            }.awaitAll()
                        }
                        Log.d(TAG, "Removed ${currentCyclists.size} cyclists from existing team")

                        // Reset team budget to initial
                        val updatedTeam = existingTeam.copy(
                            budget = com.ciclismo.portugal.domain.rules.FantasyGameRules.INITIAL_BUDGET
                        )
                        fantasyTeamRepository.updateTeam(updatedTeam)

                        existingTeam
                    }
                } else {
                    // Create new team
                    Log.d(TAG, "Creating team: $teamName for user ${user.id}")
                    fantasyTeamRepository.createTeam(user.id, teamName)
                }
                Log.d(TAG, "Team ready with ID: ${team.id}")

                // Determine which cyclists to add
                val cyclistsToAdd = if (isEditMode && existingTeam != null) {
                    // In edit mode, only add new cyclists (transfers in)
                    _selections.value.values.flatten().filter { it.id in _transfersIn.value }
                } else {
                    // Full creation/rebuild - add all selected
                    _selections.value.values.flatten()
                }

                Log.d(TAG, "Adding ${cyclistsToAdd.size} cyclists to team")

                // Add cyclists in parallel
                val results = coroutineScope {
                    cyclistsToAdd.map { cyclist ->
                        async {
                            fantasyTeamRepository.addCyclistToTeam(team.id, cyclist)
                        }
                    }.awaitAll()
                }

                val successCount = results.count { it.isSuccess }
                results.forEachIndexed { index, result ->
                    result.onSuccess {
                        Log.d(TAG, "Added ${cyclistsToAdd[index].lastName}")
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to add ${cyclistsToAdd[index].lastName}: ${error.message}")
                    }
                }

                val penalty = transferPenalty.value
                val penaltyMsg = if (penalty > 0) " (-$penalty pontos)" else ""

                if (successCount == cyclistsToAdd.size) {
                    val message = when {
                        isEditMode -> "Equipa atualizada!$penaltyMsg"
                        existingTeam != null -> "Equipa recriada com sucesso!"
                        else -> "Equipa criada com sucesso!"
                    }
                    _message.emit(message)
                    _teamCreated.value = true
                    // Clear draft on successful team creation
                    draftStorage.clearDraft()
                } else {
                    _message.emit("Equipa atualizada com $successCount/${cyclistsToAdd.size} ciclistas$penaltyMsg")
                    _teamCreated.value = true
                    // Clear draft on successful team creation
                    draftStorage.clearDraft()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error creating/updating team: ${e.message}", e)
                _message.emit("Erro ao criar equipa: ${e.message}")
            } finally {
                _isConfirming.value = false
            }
        }
    }

    // Check if a cyclist can be selected (for UI feedback)
    fun canSelectCyclist(cyclist: Cyclist): CyclistEligibility {
        val step = _currentStep
        val category = step.category ?: return CyclistEligibility.Eligible
        val currentSelections = _selections.value[category] ?: emptyList()

        // Already selected in this category
        if (currentSelections.any { it.id == cyclist.id }) {
            return CyclistEligibility.AlreadyInTeam
        }

        // Check if selected in another category (shouldn't happen, but safety check)
        val allSelected = _selections.value.values.flatten()
        if (allSelected.any { it.id == cyclist.id }) {
            return CyclistEligibility.AlreadyInTeam
        }

        // Category full
        if (currentSelections.size >= step.requiredCount) {
            return CyclistEligibility.CategoryFull(category, step.requiredCount)
        }

        // Budget
        if (cyclist.price > _remainingBudget.value) {
            return CyclistEligibility.InsufficientBudget(_remainingBudget.value, cyclist.price)
        }

        // Pro team limit
        val currentFromTeam = _proTeamCount.value[cyclist.teamId] ?: 0
        if (currentFromTeam >= FantasyGameRules.MAX_FROM_SAME_PRO_TEAM) {
            return CyclistEligibility.TooManyFromSameTeam(
                cyclist.teamId,
                FantasyGameRules.MAX_FROM_SAME_PRO_TEAM
            )
        }

        return CyclistEligibility.Eligible
    }

    fun isCyclistSelected(cyclist: Cyclist): Boolean {
        val allSelected = _selections.value.values.flatten()
        return allSelected.any { it.id == cyclist.id }
    }
}
