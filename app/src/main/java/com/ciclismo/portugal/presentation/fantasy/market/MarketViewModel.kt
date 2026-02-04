package com.ciclismo.portugal.presentation.fantasy.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.data.local.ai.AiCoordinator
import com.ciclismo.portugal.domain.repository.AuthRepository
import com.ciclismo.portugal.domain.repository.CyclistRepository
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.rules.CyclistEligibility
import com.ciclismo.portugal.domain.rules.FantasyGameRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val cyclistRepository: CyclistRepository,
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val authRepository: AuthRepository,
    private val aiCoordinator: AiCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CyclistCategory?>(null)
    val selectedCategory: StateFlow<CyclistCategory?> = _selectedCategory.asStateFlow()

    private val _sortBy = MutableStateFlow(SortOption.PRICE_DESC)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    private val _showOnlyAffordable = MutableStateFlow(false)
    val showOnlyAffordable: StateFlow<Boolean> = _showOnlyAffordable.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _teamInfo = MutableStateFlow<TeamInfo?>(null)
    val teamInfo: StateFlow<TeamInfo?> = _teamInfo.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Draft mode - pending changes before committing
    private val _pendingAdditions = MutableStateFlow<Set<String>>(emptySet()) // Cyclist IDs to add
    private val _pendingRemovals = MutableStateFlow<Set<String>>(emptySet())  // Cyclist IDs to remove
    val pendingAdditions: StateFlow<Set<String>> = _pendingAdditions.asStateFlow()
    val pendingRemovals: StateFlow<Set<String>> = _pendingRemovals.asStateFlow()

    // Show confirmation dialog when team is complete
    private val _showTeamConfirmation = MutableStateFlow(false)
    val showTeamConfirmation: StateFlow<Boolean> = _showTeamConfirmation.asStateFlow()

    // Track if there are pending changes
    val hasPendingChanges: StateFlow<Boolean> = combine(
        _pendingAdditions, _pendingRemovals
    ) { additions, removals ->
        additions.isNotEmpty() || removals.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Effective team info (actual + pending changes)
    private val _effectiveTeamInfo = MutableStateFlow<TeamInfo?>(null)
    val effectiveTeamInfo: StateFlow<TeamInfo?> = _effectiveTeamInfo.asStateFlow()

    // Trigger to force refresh team info
    private val _refreshTrigger = MutableStateFlow(0)

    private var teamInfoJob: Job? = null

    init {
        checkAuth()
        loadTeamInfo()
        loadCyclists()
        observePendingChanges()
        observeTransfersForAi()
    }

    /**
     * Observe transfer count changes and notify AI coordinator
     * for contextual suggestions (e.g., transfer penalty warnings)
     */
    private fun observeTransfersForAi() {
        viewModelScope.launch {
            try {
                transferCount.collect { count ->
                    try {
                        aiCoordinator.updatePendingTransfers(count)
                    } catch (e: Exception) {
                        android.util.Log.e("MarketVM", "Error updating AI coordinator: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MarketVM", "Error in observeTransfersForAi: ${e.message}")
            }
        }
    }

    private fun checkAuth() {
        viewModelScope.launch {
            _isAuthenticated.value = authRepository.getCurrentUser() != null
        }
    }

    private fun loadTeamInfo() {
        // Cancel previous job to avoid multiple collections
        teamInfoJob?.cancel()
        teamInfoJob = viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _teamInfo.value = null
                return@launch
            }

            // Use flatMapLatest to properly chain the flows
            fantasyTeamRepository.getTeamByUserId(user.id)
                .flatMapLatest { team ->
                    if (team != null) {
                        fantasyTeamRepository.getTeamCyclistsWithDetails(team.id)
                            .map { cyclists ->
                                val categoryCount = cyclists.groupBy { it.second.category }
                                    .mapValues { it.value.size }
                                val proTeamCount = cyclists.groupBy { it.second.teamId }
                                    .mapValues { it.value.size }
                                val cyclistIds = cyclists.map { it.first.cyclistId }.toSet()

                                TeamInfo(
                                    team = team,
                                    categoryCount = categoryCount,
                                    proTeamCount = proTeamCount,
                                    cyclistIds = cyclistIds,
                                    teamSize = cyclists.size
                                )
                            }
                    } else {
                        flowOf(null)
                    }
                }
                .collect { info ->
                    android.util.Log.d("MarketVM", "TeamInfo updated: size=${info?.teamSize}, categories=${info?.categoryCount}")
                    _teamInfo.value = info
                }
        }
    }

    private fun refreshTeamInfo() {
        // Trigger a refresh by incrementing the trigger
        _refreshTrigger.value++
        loadTeamInfo()
    }

    /**
     * Observe pending changes and calculate effective team info
     */
    private fun observePendingChanges() {
        viewModelScope.launch {
            combine(
                _teamInfo,
                _pendingAdditions,
                _pendingRemovals,
                cyclistRepository.getValidatedCyclists()
            ) { baseTeam, additions, removals, allCyclists ->
                if (baseTeam == null) return@combine null

                val cyclistMap = allCyclists.associateBy { it.id }

                // Calculate effective cyclists: base - removals + additions
                val effectiveCyclistIds = baseTeam.cyclistIds
                    .minus(removals)
                    .plus(additions)

                val effectiveCyclists = effectiveCyclistIds.mapNotNull { cyclistMap[it] }

                // Calculate effective budget
                val removedCost = removals.sumOf { id -> cyclistMap[id]?.price ?: 0.0 }
                val addedCost = additions.sumOf { id -> cyclistMap[id]?.price ?: 0.0 }
                val effectiveBudget = baseTeam.team.budget + removedCost - addedCost

                // Calculate category counts
                val categoryCount = effectiveCyclists.groupBy { it.category }
                    .mapValues { it.value.size }
                val proTeamCount = effectiveCyclists.groupBy { it.teamId }
                    .mapValues { it.value.size }

                TeamInfo(
                    team = baseTeam.team.copy(budget = effectiveBudget),
                    categoryCount = categoryCount,
                    proTeamCount = proTeamCount,
                    cyclistIds = effectiveCyclistIds,
                    teamSize = effectiveCyclists.size
                )
            }.collect { effectiveInfo ->
                _effectiveTeamInfo.value = effectiveInfo
                android.util.Log.d("MarketVM", "Effective team: size=${effectiveInfo?.teamSize}, pending add=${_pendingAdditions.value.size}, remove=${_pendingRemovals.value.size}")

                // Show confirmation when team is complete
                if (effectiveInfo != null && effectiveInfo.teamSize >= FantasyGameRules.TEAM_SIZE) {
                    if (_pendingAdditions.value.isNotEmpty() || _pendingRemovals.value.isNotEmpty()) {
                        _showTeamConfirmation.value = true
                    }
                }
            }
        }
    }

    private fun loadCyclists() {
        viewModelScope.launch {
            android.util.Log.d("MarketVM", "Starting to load cyclists from Firestore...")

            try {
            // Combine pending state into a single flow (to stay within 5 param limit for combine)
            val pendingStateFlow = combine(_pendingAdditions, _pendingRemovals) { adds, removes ->
                adds to removes
            }

            combine(
                cyclistRepository.getValidatedCyclists()
                    .catch { e ->
                        android.util.Log.e("MarketVM", "Error loading cyclists: ${e.message}")
                        emit(emptyList())
                    },
                _searchQuery,
                _selectedCategory,
                combine(_sortBy, _showOnlyAffordable) { sort, affordable -> sort to affordable },
                combine(_effectiveTeamInfo, pendingStateFlow) { team, pending -> team to pending }
            ) { cyclists, query, category, sortAndAffordable, teamAndPending ->
                val (sort, showOnlyAffordable) = sortAndAffordable
                val (effectiveTeam, pending) = teamAndPending
                val (additions, removals) = pending

                android.util.Log.d("MarketVM", "Received ${cyclists.size} validated cyclists from Firestore")
                val filtered = filterAndSortCyclists(cyclists, query, category, sort)

                // Add eligibility info to each cyclist using effective team state
                val withEligibility = filtered.map { cyclist ->
                    val eligibility = checkEligibility(cyclist, effectiveTeam)
                    // Check if cyclist is in pending additions or removals
                    val isPendingAdd = cyclist.id in additions
                    val isPendingRemove = cyclist.id in removals
                    CyclistWithEligibility(cyclist, eligibility, isPendingAdd, isPendingRemove)
                }.let { list ->
                    // Filter by budget if toggle is on
                    if (showOnlyAffordable && effectiveTeam != null) {
                        list.filter { it.isInEffectiveTeam || it.cyclist.price <= effectiveTeam.team.budget }
                    } else {
                        list
                    }
                }

                // Sort: selected cyclists first (in team or pending add), then by selected sort option
                withEligibility.sortedWith(
                    compareByDescending<CyclistWithEligibility> { it.isInEffectiveTeam }
                        .thenByDescending { it.isPendingAdd }
                        .thenBy { it.isPendingRemove }
                        .thenComparator { a, b ->
                            when (sort) {
                                SortOption.PRICE_DESC -> b.cyclist.price.compareTo(a.cyclist.price)
                                SortOption.PRICE_ASC -> a.cyclist.price.compareTo(b.cyclist.price)
                                SortOption.POINTS_DESC -> b.cyclist.totalPoints.compareTo(a.cyclist.totalPoints)
                                SortOption.NAME_ASC -> a.cyclist.lastName.compareTo(b.cyclist.lastName)
                                SortOption.FORM_DESC -> b.cyclist.form.compareTo(a.cyclist.form)
                            }
                        }
                )
            }.collect { cyclistsWithEligibility ->
                android.util.Log.d("MarketVM", "After filtering: ${cyclistsWithEligibility.size} cyclists")
                _uiState.value = if (cyclistsWithEligibility.isEmpty()) {
                    MarketUiState.Empty
                } else {
                    MarketUiState.Success(cyclistsWithEligibility)
                }
            }
            } catch (e: Exception) {
                android.util.Log.e("MarketVM", "Error in loadCyclists: ${e.message}")
                _uiState.value = MarketUiState.Error("Erro ao carregar ciclistas: ${e.message}")
            }
        }
    }

    private fun checkEligibility(cyclist: Cyclist, teamInfo: TeamInfo?): CyclistEligibility {
        // If no team, all cyclists are eligible (user needs to create team first)
        if (teamInfo == null) {
            return CyclistEligibility.Eligible
        }

        // Check if already in team
        if (cyclist.id in teamInfo.cyclistIds) {
            return CyclistEligibility.AlreadyInTeam
        }

        return FantasyGameRules.canAddCyclist(
            cyclistCategory = cyclist.category,
            cyclistPrice = cyclist.price,
            cyclistTeamId = cyclist.teamId,
            currentBudget = teamInfo.team.budget,
            currentTeamSize = teamInfo.teamSize,
            categoryCount = teamInfo.categoryCount,
            proTeamCount = teamInfo.proTeamCount
        )
    }

    private fun filterAndSortCyclists(
        cyclists: List<Cyclist>,
        query: String,
        category: CyclistCategory?,
        sort: SortOption
    ): List<Cyclist> {
        return cyclists
            .filter { cyclist ->
                val matchesQuery = query.isBlank() ||
                        cyclist.fullName.contains(query, ignoreCase = true) ||
                        cyclist.teamName.contains(query, ignoreCase = true)
                val matchesCategory = category == null || cyclist.category == category
                matchesQuery && matchesCategory
            }
            .sortedWith(
                when (sort) {
                    SortOption.PRICE_DESC -> compareByDescending { it.price }
                    SortOption.PRICE_ASC -> compareBy { it.price }
                    SortOption.POINTS_DESC -> compareByDescending { it.totalPoints }
                    SortOption.NAME_ASC -> compareBy { it.lastName }
                    SortOption.FORM_DESC -> compareByDescending { it.form }
                }
            )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: CyclistCategory?) {
        _selectedCategory.value = category
    }

    fun onSortSelected(sort: SortOption) {
        _sortBy.value = sort
    }

    fun toggleShowOnlyAffordable() {
        _showOnlyAffordable.value = !_showOnlyAffordable.value
    }

    fun syncCyclists() {
        viewModelScope.launch {
            _isRefreshing.value = true
            cyclistRepository.syncFromFirestore()
            _isRefreshing.value = false
        }
    }

    /**
     * Add cyclist to pending additions (draft mode)
     * Changes are only committed when user confirms the team
     */
    fun buyCyclist(cyclist: Cyclist) {
        val effectiveTeam = _effectiveTeamInfo.value
        if (effectiveTeam == null) {
            _message.value = "Precisas de criar uma equipa primeiro"
            return
        }

        // Check eligibility against effective team state
        val eligibility = checkEligibility(cyclist, effectiveTeam)
        when (eligibility) {
            is CyclistEligibility.AlreadyInTeam -> {
                _message.value = "Este ciclista já está na tua equipa"
                return
            }
            is CyclistEligibility.InsufficientBudget -> {
                _message.value = "Orçamento insuficiente"
                return
            }
            is CyclistEligibility.TeamFull -> {
                _message.value = "Equipa completa (15 ciclistas)"
                return
            }
            is CyclistEligibility.CategoryFull -> {
                _message.value = "Categoria ${getCategoryDisplayName(cyclist.category)} completa"
                return
            }
            is CyclistEligibility.TooManyFromSameTeam -> {
                _message.value = "Máximo 3 ciclistas da mesma equipa"
                return
            }
            is CyclistEligibility.Eligible -> {
                // Proceed with pending addition
            }
        }

        // If cyclist was in pending removals, just remove from there
        if (cyclist.id in _pendingRemovals.value) {
            _pendingRemovals.value = _pendingRemovals.value - cyclist.id
            _message.value = "${cyclist.lastName} restaurado"
        } else {
            // Add to pending additions
            _pendingAdditions.value = _pendingAdditions.value + cyclist.id
            _message.value = "${cyclist.lastName} adicionado (pendente)"
        }
    }

    /**
     * Add cyclist to pending removals (draft mode)
     * Changes are only committed when user confirms the team
     */
    fun sellCyclist(cyclist: Cyclist) {
        // If cyclist was in pending additions, just remove from there
        if (cyclist.id in _pendingAdditions.value) {
            _pendingAdditions.value = _pendingAdditions.value - cyclist.id
            _message.value = "${cyclist.lastName} removido"
        } else {
            // Add to pending removals (only if cyclist is in actual team)
            val actualTeam = _teamInfo.value
            if (actualTeam != null && cyclist.id in actualTeam.cyclistIds) {
                _pendingRemovals.value = _pendingRemovals.value + cyclist.id
                _message.value = "${cyclist.lastName} marcado para venda"
            }
        }
    }

    // Track if confirmation is in progress
    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    // Transfer tracking
    companion object {
        const val TRANSFER_PENALTY_POINTS = 4 // Points penalty per extra transfer
    }

    // Calculate transfer count (additions + removals = total transfers)
    val transferCount: StateFlow<Int> = combine(
        _pendingAdditions, _pendingRemovals
    ) { additions, removals ->
        additions.size + removals.size
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Remaining free transfers
    val remainingFreeTransfers: StateFlow<Int> = _teamInfo.map { info ->
        info?.team?.remainingFreeTransfers ?: 2
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    // Has unlimited transfers (wildcard/free hit active)
    val hasUnlimitedTransfers: StateFlow<Boolean> = _teamInfo.map { info ->
        info?.team?.hasUnlimitedTransfers == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Calculate transfer penalty
    val transferPenalty: StateFlow<Int> = combine(
        transferCount, remainingFreeTransfers, hasUnlimitedTransfers
    ) { count, freeTransfers, unlimited ->
        if (unlimited) 0
        else {
            val paidTransfers = maxOf(0, count - freeTransfers)
            paidTransfers * TRANSFER_PENALTY_POINTS
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Show transfer penalty warning dialog
    private val _showTransferPenaltyWarning = MutableStateFlow(false)
    val showTransferPenaltyWarning: StateFlow<Boolean> = _showTransferPenaltyWarning.asStateFlow()

    // Show wildcard dialog
    private val _showWildcardDialog = MutableStateFlow(false)
    val showWildcardDialog: StateFlow<Boolean> = _showWildcardDialog.asStateFlow()

    /**
     * Check transfers and show warning if exceeding free transfers
     */
    fun checkAndConfirmTeam() {
        val penalty = transferPenalty.value
        val unlimited = hasUnlimitedTransfers.value

        if (penalty > 0 && !unlimited) {
            _showTeamConfirmation.value = false
            _showTransferPenaltyWarning.value = true
        } else {
            confirmTeam()
        }
    }

    fun dismissTransferPenaltyWarning() {
        _showTransferPenaltyWarning.value = false
    }

    fun showWildcardOptions() {
        _showTransferPenaltyWarning.value = false
        _showWildcardDialog.value = true
    }

    fun dismissWildcardDialogMarket() {
        _showWildcardDialog.value = false
    }

    fun activateWildcardFromMarket() {
        viewModelScope.launch {
            val team = _teamInfo.value?.team ?: return@launch
            fantasyTeamRepository.useWildcard(team.id).fold(
                onSuccess = {
                    _showWildcardDialog.value = false
                    _message.value = "Wildcard ativado! Transferencias ilimitadas."
                    refreshTeamInfo()
                    confirmTeam()
                },
                onFailure = { error ->
                    _message.value = "Erro ao ativar wildcard: ${error.message}"
                }
            )
        }
    }

    fun confirmWithPenaltyFromMarket() {
        _showTransferPenaltyWarning.value = false
        confirmTeam()
    }

    /**
     * Commit all pending changes to database
     */
    fun confirmTeam() {
        // Prevent multiple simultaneous confirmations
        if (_isConfirming.value) {
            android.util.Log.w("MarketVM", "confirmTeam: Already confirming, ignoring")
            return
        }

        viewModelScope.launch {
            _isConfirming.value = true
            _showTeamConfirmation.value = false

            val team = _teamInfo.value?.team
            if (team == null) {
                android.util.Log.e("MarketVM", "confirmTeam: No team found!")
                _message.value = "Erro: Equipa não encontrada"
                _isConfirming.value = false
                return@launch
            }

            val additions = _pendingAdditions.value.toList()
            val removals = _pendingRemovals.value.toList()

            if (additions.isEmpty() && removals.isEmpty()) {
                android.util.Log.d("MarketVM", "confirmTeam: No changes to apply")
                _message.value = "Sem alterações pendentes"
                _isConfirming.value = false
                return@launch
            }

            android.util.Log.d("MarketVM", "confirmTeam: teamId=${team.id}, additions=${additions.size}, removals=${removals.size}")

            // Get all cyclists for price info
            val allCyclists = cyclistRepository.getValidatedCyclists().first()
            val cyclistMap = allCyclists.associateBy { it.id }

            var successCount = 0
            var failCount = 0
            val errors = mutableListOf<String>()

            // First, apply removals (one at a time with small delay to avoid race conditions)
            for (cyclistId in removals) {
                android.util.Log.d("MarketVM", "Removing cyclist: $cyclistId")
                fantasyTeamRepository.removeCyclistFromTeam(team.id, cyclistId).fold(
                    onSuccess = {
                        successCount++
                        android.util.Log.d("MarketVM", "Removed cyclist: $cyclistId")
                    },
                    onFailure = { error ->
                        failCount++
                        errors.add("Remover: ${error.message}")
                        android.util.Log.e("MarketVM", "Failed to remove cyclist $cyclistId: ${error.message}")
                    }
                )
                // Small delay to avoid race conditions with Firestore
                kotlinx.coroutines.delay(100)
            }

            // Then, apply additions (one at a time with small delay)
            for (cyclistId in additions) {
                val cyclist = cyclistMap[cyclistId]
                if (cyclist == null) {
                    android.util.Log.e("MarketVM", "Cyclist not found: $cyclistId")
                    failCount++
                    errors.add("Ciclista não encontrado")
                    continue
                }
                android.util.Log.d("MarketVM", "Adding cyclist: ${cyclist.fullName} (${cyclist.id})")
                fantasyTeamRepository.addCyclistToTeam(team.id, cyclist).fold(
                    onSuccess = {
                        successCount++
                        android.util.Log.d("MarketVM", "Added cyclist: ${cyclist.fullName}")
                    },
                    onFailure = { error ->
                        failCount++
                        errors.add("Adicionar ${cyclist.lastName}: ${error.message}")
                        android.util.Log.e("MarketVM", "Failed to add cyclist ${cyclist.fullName}: ${error.message}")
                    }
                )
                // Small delay to avoid race conditions with Firestore
                kotlinx.coroutines.delay(100)
            }

            android.util.Log.d("MarketVM", "confirmTeam: success=$successCount, failed=$failCount")

            // Apply transfer penalty if needed
            val penalty = transferPenalty.value
            val totalTransfers = transferCount.value
            if (failCount == 0 && (penalty > 0 || totalTransfers > 0)) {
                val currentTeam = _teamInfo.value?.team
                if (currentTeam != null) {
                    val newTransferCount = currentTeam.transfersMadeThisWeek + totalTransfers
                    val updatedTeam = currentTeam.copy(
                        transfersMadeThisWeek = newTransferCount,
                        totalPoints = currentTeam.totalPoints - penalty
                    )
                    fantasyTeamRepository.updateTeam(updatedTeam)
                    android.util.Log.d("MarketVM", "Updated team: transfers=$newTransferCount, penalty=$penalty")
                }
            }

            // Clear pending regardless of result
            _pendingAdditions.value = emptySet()
            _pendingRemovals.value = emptySet()

            // Refresh team info
            refreshTeamInfo()

            // Notify AI coordinator of significant team change
            aiCoordinator.refreshTriggers()

            // Show result message with penalty info
            val penaltyMsg = if (penalty > 0) " (-$penalty pts)" else ""
            _message.value = if (failCount == 0) {
                "Equipa confirmada! ($successCount alterações)$penaltyMsg"
            } else if (successCount > 0) {
                "Parcialmente confirmada: $successCount OK, $failCount erros$penaltyMsg"
            } else {
                "Erro: ${errors.firstOrNull() ?: "Falha ao confirmar"}"
            }

            _isConfirming.value = false
        }
    }

    /**
     * Discard all pending changes
     */
    fun discardChanges() {
        _pendingAdditions.value = emptySet()
        _pendingRemovals.value = emptySet()
        _showTeamConfirmation.value = false
        _message.value = "Alterações descartadas"
    }

    /**
     * Dismiss the confirmation dialog without discarding changes
     */
    fun dismissConfirmation() {
        _showTeamConfirmation.value = false
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun getCategoryDisplayName(category: CyclistCategory): String {
        return when (category) {
            CyclistCategory.GC -> "GC"
            CyclistCategory.CLIMBER -> "Climber"
            CyclistCategory.SPRINT -> "Sprint"
            CyclistCategory.TT -> "TT"
            CyclistCategory.HILLS -> "Puncher"
            CyclistCategory.ONEDAY -> "Clássicas"
        }
    }
}

data class TeamInfo(
    val team: FantasyTeam,
    val categoryCount: Map<CyclistCategory, Int>,
    val proTeamCount: Map<String, Int>,
    val cyclistIds: Set<String>,
    val teamSize: Int
)

data class CyclistWithEligibility(
    val cyclist: Cyclist,
    val eligibility: CyclistEligibility,
    val isPendingAdd: Boolean = false,
    val isPendingRemove: Boolean = false
) {
    val isEligible: Boolean
        get() = eligibility is CyclistEligibility.Eligible

    val isInTeam: Boolean
        get() = eligibility is CyclistEligibility.AlreadyInTeam

    // In effective team (either already in team or pending add, and not pending remove)
    val isInEffectiveTeam: Boolean
        get() = (isInTeam || isPendingAdd) && !isPendingRemove
}

sealed class MarketUiState {
    object Loading : MarketUiState()
    object Empty : MarketUiState()
    data class Success(val cyclists: List<CyclistWithEligibility>) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

enum class SortOption(val label: String) {
    PRICE_DESC("Preço (maior)"),
    PRICE_ASC("Preço (menor)"),
    POINTS_DESC("Pontos"),
    NAME_ASC("Nome"),
    FORM_DESC("Forma")
}
