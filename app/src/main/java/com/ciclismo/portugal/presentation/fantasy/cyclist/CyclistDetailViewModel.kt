package com.ciclismo.portugal.presentation.fantasy.cyclist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.repository.AuthRepository
import com.ciclismo.portugal.domain.repository.CyclistRepository
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CyclistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cyclistRepository: CyclistRepository,
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val cyclistId: String = savedStateHandle.get<String>("cyclistId") ?: ""

    private val _uiState = MutableStateFlow<CyclistDetailUiState>(CyclistDetailUiState.Loading)
    val uiState: StateFlow<CyclistDetailUiState> = _uiState.asStateFlow()

    private val _cyclist = MutableStateFlow<Cyclist?>(null)
    val cyclist: StateFlow<Cyclist?> = _cyclist.asStateFlow()

    private val _myTeam = MutableStateFlow<FantasyTeam?>(null)
    val myTeam: StateFlow<FantasyTeam?> = _myTeam.asStateFlow()

    private val _isInMyTeam = MutableStateFlow(false)
    val isInMyTeam: StateFlow<Boolean> = _isInMyTeam.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        loadCyclist()
        loadMyTeam()
    }

    private fun loadCyclist() {
        viewModelScope.launch {
            _uiState.value = CyclistDetailUiState.Loading

            try {
                cyclistRepository.getCyclistById(cyclistId).collect { cyclist ->
                    if (cyclist != null) {
                        _cyclist.value = cyclist
                        _uiState.value = CyclistDetailUiState.Success
                    } else {
                        _uiState.value = CyclistDetailUiState.Error("Ciclista nao encontrado")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = CyclistDetailUiState.Error(e.message ?: "Erro ao carregar ciclista")
            }
        }
    }

    private fun loadMyTeam() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            _isAuthenticated.value = currentUser != null

            if (currentUser == null) return@launch

            fantasyTeamRepository.getTeamByUserId(currentUser.id).collect { team ->
                _myTeam.value = team
                // Check if cyclist is already in team
                if (team != null && cyclistId.isNotEmpty()) {
                    _isInMyTeam.value = fantasyTeamRepository.isCyclistInTeam(team.id, cyclistId)
                }
            }
        }
    }

    fun buyCyclist() {
        viewModelScope.launch {
            val cyclist = _cyclist.value ?: return@launch
            val team = _myTeam.value

            if (team == null) {
                _message.value = "Precisas de criar uma equipa primeiro"
                return@launch
            }

            // Check if already in team
            if (_isInMyTeam.value) {
                _message.value = "Este ciclista ja esta na tua equipa"
                return@launch
            }

            // Check team size limit (15 cyclists)
            val teamSize = fantasyTeamRepository.getTeamSize(team.id)
            if (teamSize >= 15) {
                _message.value = "Equipa cheia! Vende um ciclista primeiro"
                return@launch
            }

            // Check budget
            if (team.budget < cyclist.price) {
                _message.value = "Orcamento insuficiente! Tens ${String.format("%.1f", team.budget)}M"
                return@launch
            }

            fantasyTeamRepository.addCyclistToTeam(team.id, cyclist).fold(
                onSuccess = {
                    _message.value = "${cyclist.fullName} adicionado a equipa!"
                    _isInMyTeam.value = true
                    loadMyTeam()
                },
                onFailure = { error ->
                    _message.value = error.message ?: "Erro ao comprar ciclista"
                }
            )
        }
    }

    fun sellCyclist() {
        viewModelScope.launch {
            val cyclist = _cyclist.value ?: return@launch
            val team = _myTeam.value ?: return@launch

            fantasyTeamRepository.removeCyclistFromTeam(team.id, cyclist.id).fold(
                onSuccess = {
                    _message.value = "${cyclist.fullName} vendido!"
                    _isInMyTeam.value = false
                    loadMyTeam()
                },
                onFailure = { error ->
                    _message.value = error.message ?: "Erro ao vender ciclista"
                }
            )
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

sealed class CyclistDetailUiState {
    object Loading : CyclistDetailUiState()
    object Success : CyclistDetailUiState()
    data class Error(val message: String) : CyclistDetailUiState()
}
