package com.ciclismo.portugal.presentation.results

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.model.ResultSource
import com.ciclismo.portugal.domain.model.UserRaceResult
import com.ciclismo.portugal.domain.model.UserRaceType
import com.ciclismo.portugal.domain.repository.UserRaceResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddResultViewModel @Inject constructor(
    private val repository: UserRaceResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddResultUiState>(AddResultUiState.Idle)
    val uiState: StateFlow<AddResultUiState> = _uiState.asStateFlow()

    // Form fields
    val raceName = mutableStateOf("")
    val raceDate = mutableStateOf(System.currentTimeMillis())
    val raceLocation = mutableStateOf("")
    val raceType = mutableStateOf(UserRaceType.GRAVEL)
    val position = mutableStateOf("")
    val totalParticipants = mutableStateOf("")
    val categoryPosition = mutableStateOf("")
    val categoryTotalParticipants = mutableStateOf("")
    val category = mutableStateOf("")
    val distance = mutableStateOf("")
    val elevation = mutableStateOf("")
    val finishTime = mutableStateOf("")
    val bibNumber = mutableStateOf("")
    val avgSpeed = mutableStateOf("")
    val resultSource = mutableStateOf(ResultSource.MANUAL)
    val sourceUrl = mutableStateOf("")
    val eventUrl = mutableStateOf("")
    val organizerName = mutableStateOf("")
    val notes = mutableStateOf("")

    fun isFormValid(): Boolean {
        return raceName.value.isNotBlank() &&
                raceLocation.value.isNotBlank()
    }

    fun saveResult() {
        if (!isFormValid()) {
            _uiState.value = AddResultUiState.Error("Preencha os campos obrigatorios")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddResultUiState.Saving

            try {
                val result = UserRaceResult(
                    userId = "", // Will be set by repository
                    raceName = raceName.value.trim(),
                    raceDate = raceDate.value,
                    raceLocation = raceLocation.value.trim(),
                    raceType = raceType.value,
                    distance = distance.value.toFloatOrNull(),
                    elevation = elevation.value.toIntOrNull(),
                    bibNumber = bibNumber.value.toIntOrNull(),
                    position = position.value.toIntOrNull(),
                    totalParticipants = totalParticipants.value.toIntOrNull(),
                    categoryPosition = categoryPosition.value.toIntOrNull(),
                    categoryTotalParticipants = categoryTotalParticipants.value.toIntOrNull(),
                    category = category.value.trim().takeIf { it.isNotBlank() },
                    finishTime = finishTime.value.trim().takeIf { it.isNotBlank() },
                    avgSpeed = avgSpeed.value.toFloatOrNull(),
                    resultSource = resultSource.value,
                    sourceUrl = sourceUrl.value.trim().takeIf { it.isNotBlank() },
                    eventUrl = eventUrl.value.trim().takeIf { it.isNotBlank() },
                    organizerName = organizerName.value.trim().takeIf { it.isNotBlank() },
                    notes = notes.value.trim().takeIf { it.isNotBlank() }
                )

                val saveResult = repository.saveResult(result)

                if (saveResult.isSuccess) {
                    _uiState.value = AddResultUiState.Success
                } else {
                    _uiState.value = AddResultUiState.Error(
                        saveResult.exceptionOrNull()?.message ?: "Erro ao guardar resultado"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AddResultUiState.Error(e.message ?: "Erro inesperado")
            }
        }
    }

    fun clearForm() {
        raceName.value = ""
        raceDate.value = System.currentTimeMillis()
        raceLocation.value = ""
        raceType.value = UserRaceType.GRAVEL
        position.value = ""
        totalParticipants.value = ""
        categoryPosition.value = ""
        categoryTotalParticipants.value = ""
        category.value = ""
        distance.value = ""
        elevation.value = ""
        finishTime.value = ""
        bibNumber.value = ""
        avgSpeed.value = ""
        resultSource.value = ResultSource.MANUAL
        sourceUrl.value = ""
        eventUrl.value = ""
        organizerName.value = ""
        notes.value = ""
        _uiState.value = AddResultUiState.Idle
    }
}
