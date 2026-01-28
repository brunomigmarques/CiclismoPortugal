package com.ciclismo.portugal.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.usecase.GetCalendarProvasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarProvasUseCase: GetCalendarProvasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadCalendarProvas()
    }

    private fun loadCalendarProvas() {
        viewModelScope.launch {
            getCalendarProvasUseCase()
                .catch { error ->
                    _uiState.value = CalendarUiState.Error(error.message ?: "Erro desconhecido")
                }
                .collect { provas ->
                    _uiState.value = if (provas.isEmpty()) {
                        CalendarUiState.Empty
                    } else {
                        CalendarUiState.Success(provas)
                    }
                }
        }
    }
}

sealed class CalendarUiState {
    object Loading : CalendarUiState()
    object Empty : CalendarUiState()
    data class Success(val provas: List<Prova>) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}
