package com.ciclismo.portugal.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.strava.StravaActivity
import com.ciclismo.portugal.data.remote.strava.StravaRepository
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.repository.ProvaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class EventHistoryViewModel @Inject constructor(
    private val provaRepository: ProvaRepository,
    private val stravaRepository: StravaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventHistoryUiState>(EventHistoryUiState.Loading)
    val uiState: StateFlow<EventHistoryUiState> = _uiState.asStateFlow()

    private val _stravaActivities = MutableStateFlow<Map<Long, List<StravaActivity>>>(emptyMap())
    val stravaActivities: StateFlow<Map<Long, List<StravaActivity>>> = _stravaActivities.asStateFlow()

    val isStravaConnected: Boolean
        get() = stravaRepository.isConnected()

    init {
        loadPastEvents()
    }

    private fun loadPastEvents() {
        viewModelScope.launch {
            provaRepository.getCalendarProvas().collect { calendarProvas ->
                val now = System.currentTimeMillis()

                // Filter past events (date is before today)
                val pastEvents = calendarProvas.filter { prova ->
                    prova.data < now
                }.sortedByDescending { it.data } // Most recent first

                if (pastEvents.isEmpty()) {
                    _uiState.value = EventHistoryUiState.Empty
                } else {
                    _uiState.value = EventHistoryUiState.Success(pastEvents)

                    // Load Strava activities for past events if connected
                    if (stravaRepository.isConnected()) {
                        loadStravaActivitiesForEvents(pastEvents)
                    }
                }
            }
        }
    }

    private fun loadStravaActivitiesForEvents(events: List<Prova>) {
        viewModelScope.launch {
            events.forEach { prova ->
                // Convert prova date to Unix timestamp (seconds)
                val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon"))
                calendar.timeInMillis = prova.data
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis / 1000

                val result = stravaRepository.getActivitiesForDate(startOfDay)
                result.onSuccess { activities ->
                    if (activities.isNotEmpty()) {
                        val currentMap = _stravaActivities.value.toMutableMap()
                        currentMap[prova.id] = activities
                        _stravaActivities.value = currentMap
                    }
                }
            }
        }
    }

    fun removeFromHistory(provaId: Long) {
        viewModelScope.launch {
            provaRepository.addToCalendar(provaId, false)
        }
    }
}

sealed class EventHistoryUiState {
    object Loading : EventHistoryUiState()
    object Empty : EventHistoryUiState()
    data class Success(val events: List<Prova>) : EventHistoryUiState()
}
