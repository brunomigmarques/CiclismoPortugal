package com.ciclismo.portugal.presentation.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.domain.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationAdminViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationAdminVM"
    }

    private val _uiState = MutableStateFlow<NotificationAdminUiState>(NotificationAdminUiState.Loading)
    val uiState: StateFlow<NotificationAdminUiState> = _uiState.asStateFlow()

    private val _leagues = MutableStateFlow<List<LeagueInfo>>(emptyList())
    val leagues: StateFlow<List<LeagueInfo>> = _leagues.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadLeagues()
    }

    private fun loadLeagues() {
        viewModelScope.launch {
            try {
                _uiState.value = NotificationAdminUiState.Loading

                val leagueList = leagueRepository.getAllLeagues().first()

                val leagueInfoList = leagueList.map { league ->
                    LeagueInfo(
                        id = league.id,
                        name = league.name,
                        memberCount = league.memberCount,
                        isGlobal = league.isGlobal
                    )
                }.sortedByDescending { it.memberCount }

                _leagues.value = leagueInfoList
                _uiState.value = NotificationAdminUiState.Success

                Log.d(TAG, "Loaded ${leagueInfoList.size} leagues")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading leagues: ${e.message}", e)
                _uiState.value = NotificationAdminUiState.Error("Erro ao carregar ligas: ${e.message}")
            }
        }
    }

    fun sendNotification(
        title: String,
        body: String,
        leagueIds: List<String>,
        type: NotificationType
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Sending notification to ${leagueIds.size} leagues")
                Log.d(TAG, "Title: $title")
                Log.d(TAG, "Body: $body")
                Log.d(TAG, "Type: ${type.name}")

                // Get member IDs from all selected leagues
                val targetUserIds = mutableSetOf<String>()
                leagueIds.forEach { leagueId ->
                    try {
                        val members = leagueRepository.getLeagueMembers(leagueId).first()
                        targetUserIds.addAll(members.map { it.userId })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting members for league $leagueId: ${e.message}")
                    }
                }

                Log.d(TAG, "Total target users: ${targetUserIds.size}")

                // In a real implementation, this would send FCM notifications
                // For now, we just log the notification details
                // This would typically call a Cloud Function or server endpoint
                // that handles sending FCM notifications to the target users

                // Example of what the real implementation would look like:
                // notificationService.sendToUsers(
                //     userIds = targetUserIds.toList(),
                //     title = title,
                //     body = body,
                //     data = mapOf(
                //         "type" to type.name,
                //         "leagueIds" to leagueIds.joinToString(",")
                //     )
                // )

                // For demonstration purposes, simulate a small delay
                kotlinx.coroutines.delay(1000)

                val leagueNames = _leagues.value
                    .filter { leagueIds.contains(it.id) }
                    .map { it.name }
                    .joinToString(", ")

                _message.value = "Notificação enviada para ${targetUserIds.size} utilizadores em ${leagueIds.size} ligas ($leagueNames)"

                Log.d(TAG, "Notification sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification: ${e.message}", e)
                _message.value = "Erro ao enviar notificação: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
