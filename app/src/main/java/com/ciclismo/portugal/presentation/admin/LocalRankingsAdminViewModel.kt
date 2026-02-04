package com.ciclismo.portugal.presentation.admin

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.firebase.LocalRankingFirestoreService
import com.ciclismo.portugal.domain.model.LocalRanking
import com.ciclismo.portugal.domain.model.RankingPointsSystem
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.UserRaceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalRankingsAdminViewModel @Inject constructor(
    private val rankingService: LocalRankingFirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocalRankingsAdminUiState>(LocalRankingsAdminUiState.Loading)
    val uiState: StateFlow<LocalRankingsAdminUiState> = _uiState.asStateFlow()

    private val _rankings = MutableStateFlow<List<LocalRanking>>(emptyList())
    val rankings: StateFlow<List<LocalRanking>> = _rankings.asStateFlow()

    // Form fields for creating/editing ranking
    val rankingName = mutableStateOf("")
    val rankingDescription = mutableStateOf("")
    val selectedRaceType = mutableStateOf<UserRaceType?>(null)
    val selectedRegion = mutableStateOf("")
    val selectedPointsSystem = mutableStateOf(RankingPointsSystem.STANDARD)
    val selectedRaces = mutableStateListOf<SelectedRace>()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    init {
        loadRankings()
    }

    fun loadRankings() {
        viewModelScope.launch {
            _uiState.value = LocalRankingsAdminUiState.Loading
            try {
                val result = rankingService.getActiveRankings(SeasonConfig.CURRENT_SEASON)
                _rankings.value = result
                _uiState.value = if (result.isEmpty()) {
                    LocalRankingsAdminUiState.Empty
                } else {
                    LocalRankingsAdminUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = LocalRankingsAdminUiState.Error(e.message ?: "Erro ao carregar rankings")
            }
        }
    }

    fun createRanking() {
        if (rankingName.value.isBlank()) {
            _uiState.value = LocalRankingsAdminUiState.Error("Nome do ranking e obrigatorio")
            return
        }

        viewModelScope.launch {
            _isCreating.value = true
            try {
                val ranking = LocalRanking(
                    name = rankingName.value.trim(),
                    description = rankingDescription.value.trim().takeIf { it.isNotBlank() },
                    season = SeasonConfig.CURRENT_SEASON,
                    raceType = selectedRaceType.value,
                    region = selectedRegion.value.trim().takeIf { it.isNotBlank() },
                    selectedRaceIds = selectedRaces.map { it.id },
                    selectedRaceNames = selectedRaces.map { it.name },
                    pointsSystem = selectedPointsSystem.value,
                    isActive = true
                )

                val result = rankingService.saveRanking(ranking)
                if (result.isSuccess) {
                    clearForm()
                    loadRankings()
                } else {
                    _uiState.value = LocalRankingsAdminUiState.Error("Erro ao criar ranking")
                }
            } catch (e: Exception) {
                _uiState.value = LocalRankingsAdminUiState.Error(e.message ?: "Erro ao criar ranking")
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun deleteRanking(rankingId: String) {
        viewModelScope.launch {
            try {
                val result = rankingService.deleteRanking(rankingId)
                if (result.isSuccess) {
                    loadRankings()
                }
            } catch (e: Exception) {
                _uiState.value = LocalRankingsAdminUiState.Error(e.message ?: "Erro ao apagar ranking")
            }
        }
    }

    fun toggleRankingActive(ranking: LocalRanking) {
        viewModelScope.launch {
            try {
                val updated = ranking.copy(
                    isActive = !ranking.isActive,
                    updatedAt = System.currentTimeMillis()
                )
                rankingService.saveRanking(updated)
                loadRankings()
            } catch (e: Exception) {
                _uiState.value = LocalRankingsAdminUiState.Error(e.message ?: "Erro ao atualizar ranking")
            }
        }
    }

    fun addRace(id: String, name: String) {
        if (selectedRaces.none { it.id == id }) {
            selectedRaces.add(SelectedRace(id, name))
        }
    }

    fun removeRace(id: String) {
        selectedRaces.removeAll { it.id == id }
    }

    fun clearForm() {
        rankingName.value = ""
        rankingDescription.value = ""
        selectedRaceType.value = null
        selectedRegion.value = ""
        selectedPointsSystem.value = RankingPointsSystem.STANDARD
        selectedRaces.clear()
    }
}

data class SelectedRace(
    val id: String,
    val name: String
)

sealed class LocalRankingsAdminUiState {
    data object Loading : LocalRankingsAdminUiState()
    data object Empty : LocalRankingsAdminUiState()
    data object Success : LocalRankingsAdminUiState()
    data class Error(val message: String) : LocalRankingsAdminUiState()
}
