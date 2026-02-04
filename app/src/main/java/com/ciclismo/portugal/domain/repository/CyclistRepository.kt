package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import kotlinx.coroutines.flow.Flow

interface CyclistRepository {
    fun getAllCyclists(): Flow<List<Cyclist>>
    fun getCyclistById(id: String): Flow<Cyclist?>
    fun getCyclistsByTeam(teamId: String): Flow<List<Cyclist>>
    fun getCyclistsByCategory(category: CyclistCategory): Flow<List<Cyclist>>
    fun searchCyclists(query: String): Flow<List<Cyclist>>
    fun getCyclistsByPriceRange(minPrice: Double, maxPrice: Double): Flow<List<Cyclist>>

    /**
     * Get validated cyclists from Firestore (shared data)
     * Falls back to local database if Firestore is empty
     */
    fun getValidatedCyclists(): Flow<List<Cyclist>>

    suspend fun syncCyclists(): Result<Int>
    suspend fun syncCyclistDetails(cyclistId: String): Result<Cyclist>

    /**
     * Sync validated cyclists from Firestore to local database
     */
    suspend fun syncFromFirestore(): Result<Int>

    // ========== Cyclist Availability (Admin) ==========

    /**
     * Get all disabled/unavailable cyclists for the current season
     */
    fun getDisabledCyclists(): Flow<List<Cyclist>>

    /**
     * Get available cyclists for the current season (not disabled)
     */
    fun getAvailableCyclists(): Flow<List<Cyclist>>

    /**
     * Get all cyclists for a specific season
     */
    fun getCyclistsForSeason(season: Int): Flow<List<Cyclist>>

    /**
     * Disable a cyclist (injury, dropout, suspension, etc)
     * @param cyclistId ID of the cyclist to disable
     * @param reason Reason for disabling (e.g., "Lesão", "Abandono", "Suspensão")
     */
    suspend fun disableCyclist(cyclistId: String, reason: String): Result<Unit>

    /**
     * Enable a previously disabled cyclist
     * @param cyclistId ID of the cyclist to enable
     */
    suspend fun enableCyclist(cyclistId: String): Result<Unit>

    /**
     * Get the count of disabled cyclists for the current season
     */
    suspend fun getDisabledCyclistCount(): Int

    // ========== Cyclist Price Management (Admin) ==========

    /**
     * Update a cyclist's full details in Firestore (Admin only)
     * @param cyclist The updated cyclist data
     */
    suspend fun updateCyclist(cyclist: Cyclist): Result<Unit>

    /**
     * Update only the price of a cyclist in Firestore (Admin only)
     * @param cyclistId ID of the cyclist
     * @param newPrice New price value
     */
    suspend fun updateCyclistPrice(cyclistId: String, newPrice: Double): Result<Unit>
}
