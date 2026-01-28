package com.ciclismo.portugal.presentation.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.usecase.GetAvailableLocaisUseCase
import com.ciclismo.portugal.domain.usecase.GetAvailableTiposUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val getAvailableTiposUseCase: GetAvailableTiposUseCase,
    private val getAvailableLocaisUseCase: GetAvailableLocaisUseCase
) : ViewModel() {

    private val _availableTipos = MutableStateFlow<List<String>>(emptyList())
    val availableTipos: StateFlow<List<String>> = _availableTipos.asStateFlow()

    private val _availableLocais = MutableStateFlow<List<String>>(emptyList())
    val availableLocais: StateFlow<List<String>> = _availableLocais.asStateFlow()

    init {
        loadAvailableFilters()
    }

    private fun loadAvailableFilters() {
        viewModelScope.launch {
            getAvailableTiposUseCase().collect { tipos ->
                _availableTipos.value = tipos
            }
        }

        viewModelScope.launch {
            getAvailableLocaisUseCase().collect { locais ->
                _availableLocais.value = locais
            }
        }
    }
}
