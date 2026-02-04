package com.ciclismo.portugal.presentation.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.firebase.LocalRankingFirestoreService
import com.ciclismo.portugal.domain.model.LocalRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankingsListViewModel @Inject constructor(
    private val firestoreService: LocalRankingFirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingsListUiState>(RankingsListUiState.Loading)
    val uiState: StateFlow<RankingsListUiState> = _uiState.asStateFlow()

    private val _rankings = MutableStateFlow<List<LocalRanking>>(emptyList())
    val rankings: StateFlow<List<LocalRanking>> = _rankings.asStateFlow()

    init {
        loadRankings()
    }

    fun loadRankings() {
        viewModelScope.launch {
            _uiState.value = RankingsListUiState.Loading
            try {
                val activeRankings = firestoreService.getActiveRankings()
                _rankings.value = activeRankings

                _uiState.value = if (activeRankings.isEmpty()) {
                    RankingsListUiState.Empty
                } else {
                    RankingsListUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = RankingsListUiState.Error(e.message ?: "Erro ao carregar rankings")
            }
        }
    }
}
