package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.Prova
import kotlinx.coroutines.flow.Flow

interface ProvaRepository {
    fun getProvas(): Flow<List<Prova>>
    fun getCalendarProvas(): Flow<List<Prova>>
    fun searchProvas(query: String): Flow<List<Prova>>
    suspend fun getProvaById(id: Long): Prova?
    suspend fun addToCalendar(provaId: Long, add: Boolean)
    suspend fun toggleNotifications(provaId: Long, enabled: Boolean)
    suspend fun syncProvas(): Result<Unit>
    suspend fun deleteOldProvas()
    fun getAvailableTipos(): Flow<List<String>>
    fun getAvailableLocais(): Flow<List<String>>

    // Admin methods
    fun getAllProvasAdmin(): Flow<List<Prova>>
    suspend fun hideProva(provaId: Long)
    suspend fun showProva(provaId: Long)
    suspend fun deleteProva(provaId: Long)
    suspend fun clearAllProvas()
}
