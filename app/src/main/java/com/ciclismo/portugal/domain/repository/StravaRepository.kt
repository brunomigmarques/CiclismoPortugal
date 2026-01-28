package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.StravaGoal
import com.ciclismo.portugal.domain.model.StravaProfile
import com.ciclismo.portugal.domain.model.StravaYearStats
import kotlinx.coroutines.flow.Flow

interface StravaRepository {
    // Auth
    suspend fun exchangeAuthCode(code: String): Result<Unit>
    suspend fun disconnectStrava()
    fun isConnected(): Flow<Boolean>
    suspend fun refreshTokenIfNeeded(): Result<Unit>

    // Profile
    suspend fun getProfile(): Result<StravaProfile>
    fun getStoredProfile(): Flow<StravaProfile?>

    // Stats
    suspend fun getYearStats(year: Int): Result<StravaYearStats>
    suspend fun syncStats(): Result<Unit>

    // Goals
    fun getGoals(year: Int): Flow<List<StravaGoal>>
    suspend fun saveGoal(goal: StravaGoal)
    suspend fun deleteGoal(goalId: Long)
    suspend fun updateGoalProgress(year: Int)
}
