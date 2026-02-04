package com.ciclismo.portugal.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.firebase.UserRaceStats
import com.ciclismo.portugal.domain.model.UserRaceResult
import com.ciclismo.portugal.domain.repository.UserRaceResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyResultsViewModel @Inject constructor(
    private val repository: UserRaceResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyResultsUiState>(MyResultsUiState.Loading)
    val uiState: StateFlow<MyResultsUiState> = _uiState.asStateFlow()

    private val _results = MutableStateFlow<List<UserRaceResult>>(emptyList())
    val results: StateFlow<List<UserRaceResult>> = _results.asStateFlow()

    private val _stats = MutableStateFlow(UserRaceStats())
    val stats: StateFlow<UserRaceStats> = _stats.asStateFlow()

    init {
        loadResults()
        loadStats()
        syncWithFirebase()
    }

    private fun loadResults() {
        viewModelScope.launch {
            repository.getResults()
                .catch { e ->
                    _uiState.value = MyResultsUiState.Error(e.message ?: "Erro ao carregar resultados")
                }
                .collect { resultsList ->
                    _results.value = resultsList
                    _uiState.value = if (resultsList.isEmpty()) {
                        MyResultsUiState.Empty
                    } else {
                        MyResultsUiState.Success
                    }
                }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            _stats.value = repository.getUserStats()
        }
    }

    private fun syncWithFirebase() {
        viewModelScope.launch {
            repository.syncWithFirebase()
        }
    }

    fun refresh() {
        _uiState.value = MyResultsUiState.Loading
        loadResults()
        loadStats()
    }

    fun deleteResult(id: String) {
        viewModelScope.launch {
            repository.deleteResult(id)
            loadStats()
        }
    }
}
