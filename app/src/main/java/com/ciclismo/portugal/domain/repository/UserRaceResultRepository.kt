package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.data.remote.firebase.UserRaceStats
import com.ciclismo.portugal.domain.model.UserRaceType
import com.ciclismo.portugal.domain.model.UserRaceResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user's personal race results.
 * Handles local storage and Firebase sync.
 */
interface UserRaceResultRepository {

    /**
     * Get all results for the current user as a Flow
     */
    fun getResults(): Flow<List<UserRaceResult>>

    /**
     * Get recent results for the current user
     */
    fun getRecentResults(limit: Int = 10): Flow<List<UserRaceResult>>

    /**
     * Get a specific result by ID
     */
    suspend fun getResultById(id: String): UserRaceResult?

    /**
     * Get results filtered by race type
     */
    fun getResultsByType(raceType: UserRaceType): Flow<List<UserRaceResult>>

    /**
     * Get results in a date range
     */
    fun getResultsInDateRange(startDate: Long, endDate: Long): Flow<List<UserRaceResult>>

    /**
     * Save a new race result (local + Firebase)
     */
    suspend fun saveResult(result: UserRaceResult): Result<Unit>

    /**
     * Update an existing race result
     */
    suspend fun updateResult(result: UserRaceResult): Result<Unit>

    /**
     * Delete a race result
     */
    suspend fun deleteResult(id: String): Result<Unit>

    /**
     * Get user statistics
     */
    suspend fun getUserStats(): UserRaceStats

    /**
     * Sync local results with Firebase
     */
    suspend fun syncWithFirebase(): Result<Unit>

    /**
     * Get all unique race names (for autocomplete)
     */
    suspend fun getAllRaceNames(): List<String>

    /**
     * Get all unique locations (for autocomplete)
     */
    suspend fun getAllLocations(): List<String>

    /**
     * Get all unique categories (for autocomplete)
     */
    suspend fun getAllCategories(): List<String>
}
