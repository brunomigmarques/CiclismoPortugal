package com.ciclismo.portugal.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.repository.ProvaRepository
import com.ciclismo.portugal.domain.usecase.AddToCalendarUseCase
import com.ciclismo.portugal.domain.usecase.GetProvaByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getProvaByIdUseCase: GetProvaByIdUseCase,
    private val addToCalendarUseCase: AddToCalendarUseCase,
    private val provaRepository: ProvaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val provaId: Long = savedStateHandle.get<Long>("provaId") ?: 0L

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _showConflictDialog = MutableStateFlow<ConflictDialogState>(ConflictDialogState.Hidden)
    val showConflictDialog: StateFlow<ConflictDialogState> = _showConflictDialog.asStateFlow()

    init {
        loadProva()
    }

    private fun loadProva() {
        viewModelScope.launch {
            try {
                val prova = getProvaByIdUseCase(provaId)
                _uiState.value = if (prova != null) {
                    DetailsUiState.Success(prova)
                } else {
                    DetailsUiState.Error("Prova não encontrada")
                }
            } catch (e: Exception) {
                _uiState.value = DetailsUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun toggleCalendar() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DetailsUiState.Success) {
                val prova = currentState.prova
                val newValue = !prova.inCalendar

                // If adding to calendar, check for conflicts
                if (newValue) {
                    val conflict = checkForConflict(prova)
                    if (conflict != null) {
                        _showConflictDialog.value = ConflictDialogState.Showing(conflict)
                        return@launch
                    }
                }

                // No conflict or removing from calendar
                addToCalendarUseCase(prova.id, newValue)
                _uiState.value = DetailsUiState.Success(
                    prova.copy(inCalendar = newValue)
                )
            }
        }
    }

    fun confirmAddWithConflict() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DetailsUiState.Success) {
                val prova = currentState.prova
                addToCalendarUseCase(prova.id, true)
                _uiState.value = DetailsUiState.Success(
                    prova.copy(inCalendar = true)
                )
                _showConflictDialog.value = ConflictDialogState.Hidden
            }
        }
    }

    fun dismissConflictDialog() {
        _showConflictDialog.value = ConflictDialogState.Hidden
    }

    private suspend fun checkForConflict(currentProva: Prova): Prova? {
        val calendarProvas = provaRepository.getCalendarProvas().first()

        // Get date without time for comparison
        val currentDate = getDateOnly(currentProva.data)

        return calendarProvas.firstOrNull { calendarProva ->
            calendarProva.id != currentProva.id &&
            getDateOnly(calendarProva.data) == currentDate
        }
    }

    private fun getDateOnly(timestamp: Long): Long {
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon"))
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

sealed class DetailsUiState {
    object Loading : DetailsUiState()
    data class Success(val prova: Prova) : DetailsUiState()
    data class Error(val message: String) : DetailsUiState()
}

sealed class ConflictDialogState {
    object Hidden : ConflictDialogState()
    data class Showing(val conflictingProva: Prova) : ConflictDialogState()
}
