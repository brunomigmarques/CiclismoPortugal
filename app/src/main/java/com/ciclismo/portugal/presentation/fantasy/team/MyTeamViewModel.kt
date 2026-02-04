package com.ciclismo.portugal.presentation.fantasy.team

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.local.ai.AiCoordinator
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.TeamCyclist
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.repository.StageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyTeamViewModel @Inject constructor(
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val raceRepository: RaceRepository,
    private val stageRepository: StageRepository,
    private val authService: AuthService,
    private val aiCoordinator: AiCoordinator
) : ViewModel() {

    companion object {
        private const val TAG = "MyTeamVM"
    }

    private val _uiState = MutableStateFlow<MyTeamUiState>(MyTeamUiState.Loading)
    val uiState: StateFlow<MyTeamUiState> = _uiState.asStateFlow()

    private val _selectedCyclist = MutableStateFlow<Pair<TeamCyclist, Cyclist>?>(null)
    val selectedCyclist: StateFlow<Pair<TeamCyclist, Cyclist>?> = _selectedCyclist.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadTeam()
    }

    private fun loadTeam() {
        val user = authService.getCurrentUser()
        if (user == null) {
            _uiState.value = MyTeamUiState.NotAuthenticated
            return
        }

        viewModelScope.launch {
            // Combine team, cyclists, and next race
            combine(
                fantasyTeamRepository.getTeamByUserId(user.id),
                flow {
                    fantasyTeamRepository.getTeamByUserId(user.id).collect { team ->
                        if (team != null) {
                            fantasyTeamRepository.getTeamCyclistsWithDetails(team.id).collect { emit(it) }
                        } else {
                            emit(emptyList<Pair<TeamCyclist, Cyclist>>())
                        }
                    }
                },
                raceRepository.getUpcomingRaces().map { races ->
                    races.minByOrNull { it.startDate }
                }
            ) { team, cyclists, nextRace ->
                Triple(team, cyclists, nextRace)
            }
                .catch { e ->
                    _uiState.value = MyTeamUiState.Error(e.message ?: "Erro ao carregar equipa")
                }
                .collect { (team, cyclists, nextRace) ->
                    if (team == null) {
                        _uiState.value = MyTeamUiState.NoTeam
                    } else {
                        // For stage races, get the next unprocessed stage
                        val nextStage = if (nextRace != null && nextRace.type != RaceType.ONE_DAY) {
                            getNextStage(nextRace)
                        } else null

                        _uiState.value = MyTeamUiState.Success(
                            team = team,
                            cyclists = cyclists,
                            activeCyclists = cyclists.filter { it.first.isActive },
                            benchCyclists = cyclists.filter { !it.first.isActive },
                            nextRace = nextRace,
                            nextStage = nextStage
                        )
                    }
                }
        }
    }

    /**
     * Get the next unprocessed stage for a multi-stage race.
     * Returns the first stage that hasn't been processed yet.
     */
    private suspend fun getNextStage(race: Race): Stage? {
        return try {
            val stages = stageRepository.getStageSchedule(race.id)
            if (stages.isEmpty()) {
                Log.d(TAG, "No stages found for race ${race.id}")
                return null
            }

            // Find the first unprocessed stage
            val nextUnprocessed = stages
                .sortedBy { it.stageNumber }
                .firstOrNull { !it.isProcessed }

            Log.d(TAG, "Next stage for ${race.name}: ${nextUnprocessed?.displayName ?: "all processed"}")
            nextUnprocessed
        } catch (e: Exception) {
            Log.e(TAG, "Error getting next stage: ${e.message}")
            null
        }
    }

    fun refreshTeam() {
        loadTeam()
    }

    fun selectCyclist(cyclist: Pair<TeamCyclist, Cyclist>?) {
        _selectedCyclist.value = cyclist
    }

    fun setCaptain(cyclistId: String) {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        viewModelScope.launch {
            fantasyTeamRepository.setCaptain(currentState.team.id, cyclistId)
                .onSuccess {
                    _snackbarMessage.emit("Capitao definido!")
                    refreshTeam()
                    // Notify AI coordinator - captain set resolves "no captain" trigger
                    aiCoordinator.refreshTriggers()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao definir capitao")
                }
        }
    }

    fun toggleActive(cyclistId: String, isActive: Boolean) {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        viewModelScope.launch {
            fantasyTeamRepository.setActive(currentState.team.id, cyclistId, isActive)
                .onSuccess {
                    refreshTeam()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao alterar estado")
                }
        }
    }

    fun removeCyclist(cyclistId: String) {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        viewModelScope.launch {
            fantasyTeamRepository.removeCyclistFromTeam(currentState.team.id, cyclistId)
                .onSuccess {
                    _snackbarMessage.emit("Ciclista removido da equipa")
                    _selectedCyclist.value = null
                    refreshTeam()
                    // Notify AI coordinator - team composition changed
                    aiCoordinator.refreshTriggers()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao remover ciclista")
                }
        }
    }

    fun cancelTripleCaptain() {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        viewModelScope.launch {
            fantasyTeamRepository.cancelTripleCaptain(currentState.team.id)
                .onSuccess {
                    _snackbarMessage.emit("Triple Captain cancelado!")
                    refreshTeam()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao cancelar Triple Captain")
                }
        }
    }

    fun cancelBenchBoost() {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        viewModelScope.launch {
            fantasyTeamRepository.cancelBenchBoost(currentState.team.id)
                .onSuccess {
                    _snackbarMessage.emit("Bench Boost cancelado!")
                    refreshTeam()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao cancelar Bench Boost")
                }
        }
    }

    // ========== Per-Race Wildcard Activation ==========

    fun activateTripleCaptainForRace(raceId: String) {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        if (currentState.team.tripleCaptainUsed) {
            viewModelScope.launch {
                _snackbarMessage.emit("Triple Captain ja foi usado esta temporada")
            }
            return
        }

        viewModelScope.launch {
            fantasyTeamRepository.activateTripleCaptainForRace(currentState.team.id, raceId)
                .onSuccess {
                    _snackbarMessage.emit("Triple Captain ativado para esta corrida!")
                    refreshTeam()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao ativar Triple Captain")
                }
        }
    }

    fun activateBenchBoostForRace(raceId: String) {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        if (currentState.team.benchBoostUsed) {
            viewModelScope.launch {
                _snackbarMessage.emit("Bench Boost ja foi usado esta temporada")
            }
            return
        }

        viewModelScope.launch {
            fantasyTeamRepository.activateBenchBoostForRace(currentState.team.id, raceId)
                .onSuccess {
                    _snackbarMessage.emit("Bench Boost ativado para esta corrida!")
                    refreshTeam()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao ativar Bench Boost")
                }
        }
    }

    fun activateWildcardForRace(raceId: String) {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        if (currentState.team.wildcardUsed) {
            viewModelScope.launch {
                _snackbarMessage.emit("Wildcard ja foi usado esta temporada")
            }
            return
        }

        viewModelScope.launch {
            fantasyTeamRepository.activateWildcardForRace(currentState.team.id, raceId)
                .onSuccess {
                    _snackbarMessage.emit("Wildcard ativado! Transferencias ilimitadas")
                    refreshTeam()
                    // Notify AI coordinator - wildcard affects transfer suggestions
                    aiCoordinator.refreshTriggers()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao ativar Wildcard")
                }
        }
    }

    fun cancelWildcard() {
        val currentState = _uiState.value as? MyTeamUiState.Success ?: return

        viewModelScope.launch {
            fantasyTeamRepository.cancelWildcard(currentState.team.id)
                .onSuccess {
                    _snackbarMessage.emit("Wildcard cancelado")
                    refreshTeam()
                }
                .onFailure { e ->
                    _snackbarMessage.emit(e.message ?: "Erro ao cancelar Wildcard")
                }
        }
    }

}

sealed class MyTeamUiState {
    object Loading : MyTeamUiState()
    object NotAuthenticated : MyTeamUiState()
    object NoTeam : MyTeamUiState()
    data class Success(
        val team: FantasyTeam,
        val cyclists: List<Pair<TeamCyclist, Cyclist>>,
        val activeCyclists: List<Pair<TeamCyclist, Cyclist>>,
        val benchCyclists: List<Pair<TeamCyclist, Cyclist>>,
        val nextRace: Race? = null,
        val nextStage: Stage? = null
    ) : MyTeamUiState() {
        val teamValue: Double
            get() = cyclists.sumOf { it.second.price }

        val activeCount: Int
            get() = activeCyclists.size

        val captain: Pair<TeamCyclist, Cyclist>?
            get() = cyclists.find { it.first.isCaptain }
    }
    data class Error(val message: String) : MyTeamUiState()
}
