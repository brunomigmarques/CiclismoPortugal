package com.ciclismo.portugal.presentation.race

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.repository.CyclistRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.repository.StageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RaceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val raceRepository: RaceRepository,
    private val cyclistRepository: CyclistRepository,
    private val stageRepository: StageRepository
) : ViewModel() {

    companion object {
        private const val TAG = "RaceDetailsViewModel"
    }

    private val raceId: String = savedStateHandle.get<String>("raceId") ?: ""

    private val _uiState = MutableStateFlow<RaceDetailsUiState>(RaceDetailsUiState.Loading)
    val uiState: StateFlow<RaceDetailsUiState> = _uiState.asStateFlow()

    private val _raceResults = MutableStateFlow<List<RaceResultWithCyclist>>(emptyList())
    val raceResults: StateFlow<List<RaceResultWithCyclist>> = _raceResults.asStateFlow()

    private val _isLoadingResults = MutableStateFlow(false)
    val isLoadingResults: StateFlow<Boolean> = _isLoadingResults.asStateFlow()

    private val _stages = MutableStateFlow<List<Stage>>(emptyList())
    val stages: StateFlow<List<Stage>> = _stages.asStateFlow()

    private val _isLoadingStages = MutableStateFlow(false)
    val isLoadingStages: StateFlow<Boolean> = _isLoadingStages.asStateFlow()

    init {
        loadRace()
        loadResults()
    }

    private fun loadRace() {
        viewModelScope.launch {
            _uiState.value = RaceDetailsUiState.Loading

            try {
                val race = raceRepository.getRaceById(raceId)
                if (race != null) {
                    _uiState.value = RaceDetailsUiState.Success(race)
                    // Load stages for multi-stage races
                    if (race.type == RaceType.GRAND_TOUR || race.type == RaceType.STAGE_RACE) {
                        loadStages()
                    }
                } else {
                    _uiState.value = RaceDetailsUiState.Error("Corrida não encontrada")
                }
            } catch (e: Exception) {
                _uiState.value = RaceDetailsUiState.Error(e.message ?: "Erro ao carregar corrida")
            }
        }
    }

    private fun loadStages() {
        viewModelScope.launch {
            _isLoadingStages.value = true
            try {
                val stageList = stageRepository.getStageSchedule(raceId)
                _stages.value = stageList.sortedBy { it.stageNumber }
            } catch (e: Exception) {
                // Stages are optional, don't show error
            } finally {
                _isLoadingStages.value = false
            }
        }
    }

    private fun loadResults() {
        viewModelScope.launch {
            _isLoadingResults.value = true
            try {
                Log.d(TAG, "loadResults: Loading results for race $raceId")

                // Get results from repository
                val results = raceRepository.getRaceResultsOnce(raceId)
                    .sortedBy { it.position ?: Int.MAX_VALUE }

                Log.d(TAG, "loadResults: Found ${results.size} results")

                if (results.isNotEmpty()) {
                    // Get all cyclists for current season to map names
                    val cyclists = cyclistRepository.getCyclistsForSeason(SeasonConfig.CURRENT_SEASON).first()
                    Log.d(TAG, "loadResults: Found ${cyclists.size} cyclists for season ${SeasonConfig.CURRENT_SEASON}")

                    // If no cyclists for current season, try getting all
                    val allCyclists = if (cyclists.isEmpty()) {
                        Log.d(TAG, "loadResults: No cyclists for current season, trying all cyclists")
                        cyclistRepository.getAllCyclists().first()
                    } else {
                        cyclists
                    }

                    val cyclistMap = allCyclists.associateBy { it.id }
                    Log.d(TAG, "loadResults: Cyclist map has ${cyclistMap.size} entries")

                    // Map results with cyclist info
                    _raceResults.value = results.map { result ->
                        val cyclist = cyclistMap[result.cyclistId]
                        if (cyclist == null) {
                            Log.d(TAG, "loadResults: Cyclist not found for ID ${result.cyclistId}")
                        }
                        RaceResultWithCyclist(
                            result = result,
                            cyclistName = cyclist?.fullName ?: "Ciclista #${result.cyclistId.takeLast(4)}",
                            cyclistTeam = cyclist?.teamName ?: "",
                            cyclistPhotoUrl = cyclist?.photoUrl
                        )
                    }

                    Log.d(TAG, "loadResults: Mapped ${_raceResults.value.size} results with cyclist info")
                } else {
                    Log.d(TAG, "loadResults: No results found for race $raceId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadResults: Error loading results - ${e.message}", e)
                // Results are optional, don't show error
            } finally {
                _isLoadingResults.value = false
            }
        }
    }
}

data class RaceResultWithCyclist(
    val result: RaceResult,
    val cyclistName: String,
    val cyclistTeam: String,
    val cyclistPhotoUrl: String? = null
)

sealed class RaceDetailsUiState {
    object Loading : RaceDetailsUiState()
    data class Success(val race: Race) : RaceDetailsUiState()
    data class Error(val message: String) : RaceDetailsUiState()
}
