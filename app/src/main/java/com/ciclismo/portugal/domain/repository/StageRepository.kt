package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.GcStanding
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.StageResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for stage results and GC standings.
 * Handles both local cache and Firebase synchronization.
 */
interface StageRepository {

    // ==================== STAGE RESULTS ====================

    /**
     * Save stage results to both local DB and Firebase.
     */
    suspend fun saveStageResults(results: List<StageResult>): Result<Int>

    /**
     * Get stage results for a specific race and stage number.
     */
    fun getStageResults(
        raceId: String,
        stageNumber: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Flow<List<StageResult>>

    /**
     * Get stage results (one-time query, not Flow).
     */
    suspend fun getStageResultsOnce(
        raceId: String,
        stageNumber: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<StageResult>

    /**
     * Get all stage results for a cyclist in a race.
     */
    fun getCyclistStageResults(
        raceId: String,
        cyclistId: String
    ): Flow<List<StageResult>>

    /**
     * Get stages that have been processed for a race.
     */
    suspend fun getProcessedStages(raceId: String): List<Int>

    /**
     * Delete stage results for a specific stage (for reprocessing).
     */
    suspend fun deleteStageResults(raceId: String, stageNumber: Int)

    /**
     * Sync stage results from Firebase to local DB.
     */
    suspend fun syncStageResults(
        raceId: String,
        stageNumber: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int>

    // ==================== GC STANDINGS ====================

    /**
     * Save GC standings to both local DB and Firebase.
     */
    suspend fun saveGcStandings(standings: List<GcStanding>): Result<Int>

    /**
     * Get current GC standings for a race.
     */
    fun getGcStandings(raceId: String): Flow<List<GcStanding>>

    /**
     * Get GC standings (one-time query).
     */
    suspend fun getGcStandingsOnce(raceId: String): List<GcStanding>

    /**
     * Get GC standing for a specific cyclist.
     */
    suspend fun getCyclistGcStanding(raceId: String, cyclistId: String): GcStanding?

    /**
     * Get current jersey holders for a race.
     */
    suspend fun getJerseyHolders(raceId: String): JerseyHolders

    /**
     * Sync GC standings from Firebase to local DB.
     */
    suspend fun syncGcStandings(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int>

    // ==================== RACE STATUS ====================

    /**
     * Update the current stage number for a race.
     */
    suspend fun updateRaceCurrentStage(
        raceId: String,
        currentStage: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit>

    /**
     * Mark a race as completed (after final stage).
     */
    suspend fun markRaceCompleted(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit>

    // ==================== STAGE SCHEDULE ====================

    /**
     * Get all stages for a multi-stage race.
     */
    suspend fun getStageSchedule(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<Stage>

    // ==================== AGGREGATE QUERIES ====================

    /**
     * Get total fantasy points earned by a cyclist across all stages.
     */
    suspend fun getCyclistTotalPoints(raceId: String, cyclistId: String): Int

    /**
     * Get the top performing cyclists by total points.
     */
    suspend fun getTopPerformers(raceId: String, limit: Int = 10): List<Pair<String, Int>>
}

/**
 * Data class holding current jersey holders for a race.
 */
data class JerseyHolders(
    val gcLeaderId: String? = null,
    val pointsLeaderId: String? = null,
    val mountainsLeaderId: String? = null,
    val youngLeaderId: String? = null
)
