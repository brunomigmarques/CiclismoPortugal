package com.ciclismo.portugal.presentation.fantasy.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.League
import com.ciclismo.portugal.domain.model.LeagueMember
import com.ciclismo.portugal.domain.model.LeagueType
import com.ciclismo.portugal.domain.repository.AuthRepository
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LeaguesViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository,
    private val authRepository: AuthRepository,
    private val fantasyTeamRepository: FantasyTeamRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LeaguesViewModel"
        private const val PAGE_SIZE = 50
        private const val LARGE_LEAGUE_THRESHOLD = 100 // Use pagination for leagues > 100 members
    }

    private val _uiState = MutableStateFlow<LeaguesUiState>(LeaguesUiState.Loading)
    val uiState: StateFlow<LeaguesUiState> = _uiState.asStateFlow()

    private val _myLeagues = MutableStateFlow<List<League>>(emptyList())
    val myLeagues: StateFlow<List<League>> = _myLeagues.asStateFlow()

    private val _availableLeagues = MutableStateFlow<List<League>>(emptyList())
    val availableLeagues: StateFlow<List<League>> = _availableLeagues.asStateFlow()

    private val _selectedLeague = MutableStateFlow<League?>(null)
    val selectedLeague: StateFlow<League?> = _selectedLeague.asStateFlow()

    private val _leagueMembers = MutableStateFlow<List<LeagueMember>>(emptyList())
    val leagueMembers: StateFlow<List<LeagueMember>> = _leagueMembers.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Pagination state for large leagues
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreMembers = MutableStateFlow(true)
    val hasMoreMembers: StateFlow<Boolean> = _hasMoreMembers.asStateFlow()

    private val _totalMemberCount = MutableStateFlow(0)
    val totalMemberCount: StateFlow<Int> = _totalMemberCount.asStateFlow()

    // Current user's position in the selected league
    private val _userPosition = MutableStateFlow<LeagueMember?>(null)
    val userPosition: StateFlow<LeagueMember?> = _userPosition.asStateFlow()

    // Track if this is a large league that needs pagination
    private val _isLargeLeague = MutableStateFlow(false)
    val isLargeLeague: StateFlow<Boolean> = _isLargeLeague.asStateFlow()

    init {
        loadLeagues()
    }

    private fun loadLeagues() {
        viewModelScope.launch {
            _uiState.value = LeaguesUiState.Loading

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = LeaguesUiState.Error("Utilizador nao autenticado")
                    return@launch
                }

                // Ensure global league exists
                try {
                    leagueRepository.ensureGlobalLeagueExists()
                } catch (e: Exception) {
                    android.util.Log.e("LeaguesVM", "Error ensuring global league: ${e.message}")
                }

                // Load user's leagues with error handling
                leagueRepository.getLeaguesForUser(currentUser.id)
                    .catch { e ->
                        android.util.Log.e("LeaguesVM", "Error loading user leagues: ${e.message}")
                        emit(emptyList())
                    }
                    .collect { leagues ->
                        _myLeagues.value = leagues
                    }
            } catch (e: Exception) {
                android.util.Log.e("LeaguesVM", "Error in loadLeagues: ${e.message}")
                _uiState.value = LeaguesUiState.Error(e.message ?: "Erro ao carregar ligas")
            }
        }

        viewModelScope.launch {
            // Load all available leagues with error handling
            leagueRepository.getAllLeagues()
                .catch { e ->
                    android.util.Log.e("LeaguesVM", "Error loading all leagues: ${e.message}")
                    emit(emptyList())
                }
                .collect { leagues ->
                    _availableLeagues.value = leagues
                    _uiState.value = LeaguesUiState.Success
                }
        }
    }

    fun selectLeague(league: League) {
        _selectedLeague.value = league
        _leagueMembers.value = emptyList()
        _hasMoreMembers.value = true
        _isLargeLeague.value = league.memberCount > LARGE_LEAGUE_THRESHOLD
        _totalMemberCount.value = league.memberCount

        loadLeagueMembers(league.id)
        loadUserPosition(league.id)
    }

    fun clearSelectedLeague() {
        _selectedLeague.value = null
        _leagueMembers.value = emptyList()
        _userPosition.value = null
        _hasMoreMembers.value = true
        _isLargeLeague.value = false
        _totalMemberCount.value = 0
    }

    private fun loadUserPosition(leagueId: String) {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser() ?: return@launch
                val position = leagueRepository.getUserPositionInLeague(leagueId, currentUser.id)
                _userPosition.value = position
                android.util.Log.d(TAG, "User position loaded: rank=${position?.rank}, points=${position?.points}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading user position: ${e.message}")
            }
        }
    }

    private fun loadLeagueMembers(leagueId: String) {
        val league = _selectedLeague.value ?: return

        if (league.memberCount > LARGE_LEAGUE_THRESHOLD) {
            // Use pagination for large leagues
            loadLeagueMembersPaginated(leagueId, isInitialLoad = true)
        } else {
            // Use real-time flow for small leagues
            viewModelScope.launch {
                leagueRepository.getLeagueMembers(leagueId)
                    .catch { e ->
                        android.util.Log.e(TAG, "Error loading league members: ${e.message}")
                        emit(emptyList())
                    }
                    .collect { members ->
                        _leagueMembers.value = members
                        _hasMoreMembers.value = false // All loaded for small leagues
                    }
            }
        }
    }

    private fun loadLeagueMembersPaginated(leagueId: String, isInitialLoad: Boolean) {
        viewModelScope.launch {
            if (!isInitialLoad && (_isLoadingMore.value || !_hasMoreMembers.value)) {
                return@launch
            }

            _isLoadingMore.value = true

            try {
                val currentMembers = _leagueMembers.value
                val lastMember = if (isInitialLoad) null else currentMembers.lastOrNull()

                val newMembers = leagueRepository.getLeagueMembersPaginated(
                    leagueId = leagueId,
                    pageSize = PAGE_SIZE,
                    lastPoints = lastMember?.points,
                    lastUserId = lastMember?.userId
                )

                android.util.Log.d(TAG, "Loaded ${newMembers.size} members (paginated)")

                if (isInitialLoad) {
                    _leagueMembers.value = newMembers
                } else {
                    _leagueMembers.value = currentMembers + newMembers
                }

                _hasMoreMembers.value = newMembers.size >= PAGE_SIZE
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading paginated members: ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * Load more members when user scrolls to bottom
     */
    fun loadMoreMembers() {
        val leagueId = _selectedLeague.value?.id ?: return
        if (_isLargeLeague.value) {
            loadLeagueMembersPaginated(leagueId, isInitialLoad = false)
        }
    }

    fun joinLeague(leagueId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch

            // Get user's team
            val team = fantasyTeamRepository.getTeamByUserId(currentUser.id).first()
            if (team == null) {
                _message.value = "Precisas de criar uma equipa primeiro"
                return@launch
            }

            // Use the repository's joinLeagueWithTeamName if available, or regular joinLeague
            val repo = leagueRepository as? com.ciclismo.portugal.data.repository.LeagueRepositoryImpl
            val result = if (repo != null) {
                repo.joinLeagueWithTeamName(leagueId, currentUser.id, team.id, team.teamName)
            } else {
                leagueRepository.joinLeague(leagueId, currentUser.id, team.id)
            }

            result.fold(
                onSuccess = {
                    _message.value = "Entraste na liga com sucesso!"
                    loadLeagues()
                },
                onFailure = { error ->
                    _message.value = error.message ?: "Erro ao entrar na liga"
                }
            )
        }
    }

    fun joinLeagueByCode(code: String) {
        viewModelScope.launch {
            val league = leagueRepository.getLeagueByCode(code)
            if (league == null) {
                _message.value = "Codigo de liga invalido"
                return@launch
            }

            joinLeague(league.id)
        }
    }

    fun leaveLeague(leagueId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch

            // Check if it's a global league
            val league = leagueRepository.getLeagueById(leagueId)
            if (league?.type == LeagueType.GLOBAL) {
                _message.value = "Nao podes sair da Liga Portugal"
                return@launch
            }

            leagueRepository.leaveLeague(leagueId, currentUser.id).fold(
                onSuccess = {
                    _message.value = "Saiste da liga"
                    loadLeagues()
                },
                onFailure = { error ->
                    _message.value = error.message ?: "Erro ao sair da liga"
                }
            )
        }
    }

    fun createPrivateLeague(name: String) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch

            // Check if user has a team first
            val team = fantasyTeamRepository.getTeamByUserId(currentUser.id).first()
            if (team == null) {
                _message.value = "Precisas de criar uma equipa primeiro"
                return@launch
            }

            // Check if user already owns a private league (limit: 1 per season)
            val userOwnedPrivateLeagues = _availableLeagues.value.filter {
                it.ownerId == currentUser.id && it.type == LeagueType.PRIVATE
            }
            if (userOwnedPrivateLeagues.isNotEmpty()) {
                _message.value = "Só podes criar uma liga privada por época"
                return@launch
            }

            val code = leagueRepository.generateUniqueCode()

            val league = League(
                id = UUID.randomUUID().toString(),
                name = name,
                type = LeagueType.PRIVATE,
                code = code,
                ownerId = currentUser.id,
                region = null,
                memberCount = 0, // Will be incremented when joining
                createdAt = System.currentTimeMillis()
            )

            leagueRepository.createLeague(league).fold(
                onSuccess = { createdLeague ->
                    // Auto-join the league with team name
                    val repo = leagueRepository as? com.ciclismo.portugal.data.repository.LeagueRepositoryImpl
                    if (repo != null) {
                        repo.joinLeagueWithTeamName(createdLeague.id, currentUser.id, team.id, team.teamName)
                    } else {
                        leagueRepository.joinLeague(createdLeague.id, currentUser.id, team.id)
                    }
                    _message.value = "Liga criada! Codigo: ${createdLeague.code}"
                    loadLeagues()
                },
                onFailure = { error ->
                    _message.value = error.message ?: "Erro ao criar liga"
                }
            )
        }
    }

    fun deleteLeague(leagueId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch

            val league = leagueRepository.getLeagueById(leagueId)
            if (league?.ownerId != currentUser.id) {
                _message.value = "Apenas o dono pode apagar a liga"
                return@launch
            }

            leagueRepository.deleteLeague(leagueId).fold(
                onSuccess = {
                    _message.value = "Liga apagada"
                    clearSelectedLeague()
                    loadLeagues()
                },
                onFailure = { error ->
                    _message.value = error.message ?: "Erro ao apagar liga"
                }
            )
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /**
     * Add fake teams to a league for testing/demo purposes
     * Only available for admin users (checks owner status)
     */
    fun addFakeTeamsToLeague(count: Int = 236) {
        viewModelScope.launch {
            val league = _selectedLeague.value ?: return@launch

            _message.value = "A criar $count equipas de teste..."

            val result = leagueRepository.addFakeTeamsToLeague(league.id, count)

            result.fold(
                onSuccess = { addedCount ->
                    _message.value = "Criadas $addedCount equipas de teste!"
                    // Refresh the league data
                    val updatedLeague = leagueRepository.getLeagueById(league.id)
                    if (updatedLeague != null) {
                        _selectedLeague.value = updatedLeague
                        _totalMemberCount.value = updatedLeague.memberCount
                        _isLargeLeague.value = updatedLeague.memberCount > LARGE_LEAGUE_THRESHOLD
                    }
                    // Reload members
                    loadLeagueMembers(league.id)
                },
                onFailure = { error ->
                    _message.value = "Erro ao criar equipas: ${error.message}"
                }
            )
        }
    }

    /**
     * Remove all fake teams from a league
     */
    fun removeFakeTeamsFromLeague() {
        viewModelScope.launch {
            val league = _selectedLeague.value ?: return@launch

            _message.value = "A remover equipas de teste..."

            val result = leagueRepository.removeFakeTeamsFromLeague(league.id)

            result.fold(
                onSuccess = { removedCount ->
                    _message.value = "Removidas $removedCount equipas de teste!"
                    // Refresh the league data
                    val updatedLeague = leagueRepository.getLeagueById(league.id)
                    if (updatedLeague != null) {
                        _selectedLeague.value = updatedLeague
                        _totalMemberCount.value = updatedLeague.memberCount
                        _isLargeLeague.value = updatedLeague.memberCount > LARGE_LEAGUE_THRESHOLD
                    }
                    // Reload members
                    loadLeagueMembers(league.id)
                },
                onFailure = { error ->
                    _message.value = "Erro ao remover equipas: ${error.message}"
                }
            )
        }
    }
}

sealed class LeaguesUiState {
    object Loading : LeaguesUiState()
    object Success : LeaguesUiState()
    data class Error(val message: String) : LeaguesUiState()
}
