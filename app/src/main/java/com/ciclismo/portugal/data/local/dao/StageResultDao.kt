package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.GcStandingEntity
import com.ciclismo.portugal.data.local.entity.StageResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for stage results and GC standings.
 */
@Dao
interface StageResultDao {

    // ==================== STAGE RESULTS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageResult(result: StageResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageResults(results: List<StageResultEntity>)

    @Query("SELECT * FROM stage_results WHERE raceId = :raceId AND stageNumber = :stageNumber ORDER BY position ASC")
    fun getStageResults(raceId: String, stageNumber: Int): Flow<List<StageResultEntity>>

    @Query("SELECT * FROM stage_results WHERE raceId = :raceId AND stageNumber = :stageNumber ORDER BY position ASC")
    suspend fun getStageResultsOnce(raceId: String, stageNumber: Int): List<StageResultEntity>

    @Query("SELECT * FROM stage_results WHERE raceId = :raceId AND cyclistId = :cyclistId ORDER BY stageNumber ASC")
    fun getCyclistStageResults(raceId: String, cyclistId: String): Flow<List<StageResultEntity>>

    @Query("SELECT * FROM stage_results WHERE raceId = :raceId AND cyclistId = :cyclistId ORDER BY stageNumber ASC")
    suspend fun getCyclistStageResultsOnce(raceId: String, cyclistId: String): List<StageResultEntity>

    @Query("SELECT * FROM stage_results WHERE raceId = :raceId ORDER BY stageNumber ASC, position ASC")
    fun getAllRaceStageResults(raceId: String): Flow<List<StageResultEntity>>

    @Query("SELECT DISTINCT stageNumber FROM stage_results WHERE raceId = :raceId ORDER BY stageNumber ASC")
    suspend fun getProcessedStages(raceId: String): List<Int>

    @Query("SELECT MAX(stageNumber) FROM stage_results WHERE raceId = :raceId")
    suspend fun getLastProcessedStage(raceId: String): Int?

    @Query("SELECT COUNT(*) FROM stage_results WHERE raceId = :raceId AND stageNumber = :stageNumber")
    suspend fun getStageResultCount(raceId: String, stageNumber: Int): Int

    @Query("DELETE FROM stage_results WHERE raceId = :raceId AND stageNumber = :stageNumber")
    suspend fun deleteStageResults(raceId: String, stageNumber: Int)

    @Query("DELETE FROM stage_results WHERE raceId = :raceId")
    suspend fun deleteAllRaceStageResults(raceId: String)

    @Query("DELETE FROM stage_results WHERE season = :season")
    suspend fun deleteAllStageResultsForSeason(season: Int)

    // ==================== GC STANDINGS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGcStanding(standing: GcStandingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGcStandings(standings: List<GcStandingEntity>)

    @Query("SELECT * FROM gc_standings WHERE raceId = :raceId ORDER BY gcPosition ASC")
    fun getGcStandings(raceId: String): Flow<List<GcStandingEntity>>

    @Query("SELECT * FROM gc_standings WHERE raceId = :raceId ORDER BY gcPosition ASC")
    suspend fun getGcStandingsOnce(raceId: String): List<GcStandingEntity>

    @Query("SELECT * FROM gc_standings WHERE raceId = :raceId AND cyclistId = :cyclistId LIMIT 1")
    suspend fun getCyclistGcStanding(raceId: String, cyclistId: String): GcStandingEntity?

    @Query("SELECT * FROM gc_standings WHERE raceId = :raceId AND isGcLeader = 1 LIMIT 1")
    suspend fun getGcLeader(raceId: String): GcStandingEntity?

    @Query("SELECT * FROM gc_standings WHERE raceId = :raceId AND isPointsLeader = 1 LIMIT 1")
    suspend fun getPointsLeader(raceId: String): GcStandingEntity?

    @Query("SELECT * FROM gc_standings WHERE raceId = :raceId AND isMountainsLeader = 1 LIMIT 1")
    suspend fun getMountainsLeader(raceId: String): GcStandingEntity?

    @Query("SELECT * FROM gc_standings WHERE raceId = :raceId AND isYoungLeader = 1 LIMIT 1")
    suspend fun getYoungLeader(raceId: String): GcStandingEntity?

    @Query("DELETE FROM gc_standings WHERE raceId = :raceId")
    suspend fun deleteGcStandings(raceId: String)

    @Query("DELETE FROM gc_standings WHERE season = :season")
    suspend fun deleteAllGcStandingsForSeason(season: Int)

    // ==================== AGGREGATE QUERIES ====================

    /**
     * Get total points earned by a cyclist across all stages in a race.
     */
    @Query("SELECT COALESCE(SUM(points + jerseyBonus), 0) FROM stage_results WHERE raceId = :raceId AND cyclistId = :cyclistId")
    suspend fun getCyclistTotalPoints(raceId: String, cyclistId: String): Int

    /**
     * Get number of stage wins for a cyclist in a race.
     */
    @Query("SELECT COUNT(*) FROM stage_results WHERE raceId = :raceId AND cyclistId = :cyclistId AND position = 1")
    suspend fun getCyclistStageWins(raceId: String, cyclistId: String): Int

    /**
     * Get number of podium finishes (top 3) for a cyclist in a race.
     */
    @Query("SELECT COUNT(*) FROM stage_results WHERE raceId = :raceId AND cyclistId = :cyclistId AND position <= 3")
    suspend fun getCyclistPodiums(raceId: String, cyclistId: String): Int

    /**
     * Get the top performers for a race (by total points).
     */
    @Query("""
        SELECT cyclistId, SUM(points + jerseyBonus) as totalPoints
        FROM stage_results
        WHERE raceId = :raceId
        GROUP BY cyclistId
        ORDER BY totalPoints DESC
        LIMIT :limit
    """)
    suspend fun getTopPerformers(raceId: String, limit: Int = 10): List<CyclistPointsSummary>
}

/**
 * Summary of cyclist points for aggregate queries.
 */
data class CyclistPointsSummary(
    val cyclistId: String,
    val totalPoints: Int
)
