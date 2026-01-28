package com.ciclismo.portugal.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.model.ProvaFilters
import com.ciclismo.portugal.domain.usecase.GetProvasUseCase
import com.ciclismo.portugal.domain.usecase.SearchProvasUseCase
import com.ciclismo.portugal.domain.usecase.SyncProvasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProvasUseCase: GetProvasUseCase,
    private val searchProvasUseCase: SearchProvasUseCase,
    private val syncProvasUseCase: SyncProvasUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _filters = MutableStateFlow(ProvaFilters.empty())
    val filters: StateFlow<ProvaFilters> = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProvas()
        // Sync provas on initialization
        syncProvas()
    }

    private fun loadProvas() {
        viewModelScope.launch {
            combine(
                searchQuery,
                filters,
                getProvasUseCase()
            ) { query, currentFilters, provas ->
                var filteredProvas = provas

                // Aplica busca por texto
                if (query.isNotBlank()) {
                    filteredProvas = filteredProvas.filter {
                        it.nome.contains(query, ignoreCase = true) ||
                                it.local.contains(query, ignoreCase = true)
                    }
                }

                // Aplica filtros
                if (currentFilters.isActive()) {
                    filteredProvas = filteredProvas.filter { currentFilters.matchesProva(it) }
                }

                filteredProvas
            }.catch { error ->
                _uiState.value = HomeUiState.Error(error.message ?: "Erro desconhecido")
            }.collect { provas ->
                _uiState.value = if (provas.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(provas)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun applyFilters(newFilters: ProvaFilters) {
        _filters.value = newFilters
    }

    fun clearFilters() {
        _filters.value = ProvaFilters.empty()
    }

    fun syncProvas() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncProvasUseCase()
            _isRefreshing.value = false
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Empty : HomeUiState()
    data class Success(val provas: List<Prova>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
