package com.ciclismo.portugal.presentation.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.data.remote.firebase.LocalRankingFirestoreService
import com.ciclismo.portugal.domain.model.LocalRanking
import com.ciclismo.portugal.domain.model.LocalRankingEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalRankingViewModel @Inject constructor(
    private val firestoreService: LocalRankingFirestoreService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocalRankingUiState>(LocalRankingUiState.Loading)
    val uiState: StateFlow<LocalRankingUiState> = _uiState.asStateFlow()

    private val _ranking = MutableStateFlow<LocalRanking?>(null)
    val ranking: StateFlow<LocalRanking?> = _ranking.asStateFlow()

    private val _entries = MutableStateFlow<List<LocalRankingEntry>>(emptyList())
    val entries: StateFlow<List<LocalRankingEntry>> = _entries.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Index of the current user in the entries list (for auto-scrolling) */
    private val _currentUserIndex = MutableStateFlow<Int?>(null)
    val currentUserIndex: StateFlow<Int?> = _currentUserIndex.asStateFlow()

    /** The current user's entry (for highlighting) */
    private val _currentUserEntry = MutableStateFlow<LocalRankingEntry?>(null)
    val currentUserEntry: StateFlow<LocalRankingEntry?> = _currentUserEntry.asStateFlow()

    private var currentRankingId: String? = null

    fun loadRanking(rankingId: String) {
        currentRankingId = rankingId
        viewModelScope.launch {
            _uiState.value = LocalRankingUiState.Loading
            _currentUserIndex.value = null
            _currentUserEntry.value = null

            try {
                val rankingData = firestoreService.getRankingById(rankingId)
                if (rankingData == null) {
                    _uiState.value = LocalRankingUiState.Error("Ranking nao encontrado")
                    return@launch
                }

                _ranking.value = rankingData

                val entriesData = firestoreService.getRankingEntries(rankingId)
                _entries.value = entriesData

                // Find current user's position in the ranking
                val currentUserId = authService.getCurrentUser()?.id
                if (currentUserId != null) {
                    val userIndex = entriesData.indexOfFirst { it.odoo == currentUserId }
                    if (userIndex >= 0) {
                        _currentUserEntry.value = entriesData[userIndex]
                        // Account for podium section: if user is in top 3, index stays same
                        // If user is position 4+, we need to account for podium item (index 0) + spacer (index 1)
                        // So actual list index = if position <= 3: position - 1, else: (position - 1) - 3 + 2 = position - 2
                        // But since podium takes items 0 and 1, and rest start at 2, we calculate:
                        _currentUserIndex.value = if (userIndex < 3) {
                            0 // Scroll to top to see podium
                        } else {
                            userIndex - 3 + 2 // Account for podium item and spacer
                        }
                    }
                }

                _uiState.value = if (entriesData.isEmpty()) {
                    LocalRankingUiState.Empty
                } else {
                    LocalRankingUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = LocalRankingUiState.Error(e.message ?: "Erro ao carregar ranking")
            }
        }
    }

    /**
     * Add fake entries to the ranking for testing purposes.
     */
    fun addFakeEntries(count: Int = 236) {
        val rankingId = currentRankingId ?: return
        viewModelScope.launch {
            _message.value = "A criar $count participantes de teste..."

            val result = firestoreService.addFakeEntriesToRanking(rankingId, count)

            result.fold(
                onSuccess = { addedCount ->
                    _message.value = "Criados $addedCount participantes de teste!"
                    // Reload entries
                    loadRanking(rankingId)
                },
                onFailure = { error ->
                    _message.value = "Erro ao criar participantes: ${error.message}"
                }
            )
        }
    }

    /**
     * Remove all fake entries from the ranking.
     */
    fun removeFakeEntries() {
        val rankingId = currentRankingId ?: return
        viewModelScope.launch {
            _message.value = "A remover participantes de teste..."

            val result = firestoreService.removeFakeEntriesFromRanking(rankingId)

            result.fold(
                onSuccess = { removedCount ->
                    _message.value = "Removidos $removedCount participantes de teste!"
                    // Reload entries
                    loadRanking(rankingId)
                },
                onFailure = { error ->
                    _message.value = "Erro ao remover participantes: ${error.message}"
                }
            )
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
