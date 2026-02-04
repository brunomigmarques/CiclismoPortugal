package com.ciclismo.portugal.presentation.fantasy.createteam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.data.repository.LeagueRepositoryImpl
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTeamViewModel @Inject constructor(
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val leagueRepository: LeagueRepository,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateTeamUiState>(CreateTeamUiState.Input)
    val uiState: StateFlow<CreateTeamUiState> = _uiState.asStateFlow()

    private val _teamName = MutableStateFlow("")
    val teamName: StateFlow<String> = _teamName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkExistingTeam()
    }

    private fun checkExistingTeam() {
        val user = authService.getCurrentUser() ?: return

        viewModelScope.launch {
            fantasyTeamRepository.getTeamByUserId(user.id)
                .first()?.let { existingTeam ->
                    _uiState.value = CreateTeamUiState.AlreadyHasTeam(existingTeam)
                }
        }
    }

    fun onTeamNameChange(name: String) {
        _teamName.value = name
        _errorMessage.value = null
    }

    fun createTeam() {
        val name = _teamName.value.trim()

        if (name.isBlank()) {
            _errorMessage.value = "Introduz um nome para a equipa"
            return
        }

        if (name.length < 3) {
            _errorMessage.value = "Nome deve ter pelo menos 3 caracteres"
            return
        }

        if (name.length > 30) {
            _errorMessage.value = "Nome deve ter no maximo 30 caracteres"
            return
        }

        val user = authService.getCurrentUser()
        if (user == null) {
            _errorMessage.value = "Utilizador nao autenticado"
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateTeamUiState.Creating

            try {
                val team = fantasyTeamRepository.createTeam(user.id, name)

                // Auto-join the global league "Liga Portugal"
                try {
                    val globalLeague = leagueRepository.ensureGlobalLeagueExists()

                    // Check if already a member
                    if (!leagueRepository.isMember(globalLeague.id, user.id)) {
                        // Join with team name
                        val repo = leagueRepository as? LeagueRepositoryImpl
                        if (repo != null) {
                            repo.joinLeagueWithTeamName(globalLeague.id, user.id, team.id, team.teamName)
                        } else {
                            leagueRepository.joinLeague(globalLeague.id, user.id, team.id)
                        }
                        android.util.Log.d("CreateTeamVM", "Auto-joined Liga Portugal")
                    }
                } catch (e: Exception) {
                    // Don't fail team creation if league join fails
                    android.util.Log.e("CreateTeamVM", "Failed to auto-join Liga Portugal: ${e.message}")
                }

                _uiState.value = CreateTeamUiState.Success(team)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro ao criar equipa"
                _uiState.value = CreateTeamUiState.Input
            }
        }
    }
}

sealed class CreateTeamUiState {
    object Input : CreateTeamUiState()
    object Creating : CreateTeamUiState()
    data class Success(val team: FantasyTeam) : CreateTeamUiState()
    data class AlreadyHasTeam(val team: FantasyTeam) : CreateTeamUiState()
}
