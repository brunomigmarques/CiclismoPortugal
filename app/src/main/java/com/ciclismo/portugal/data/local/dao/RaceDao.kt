package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.RaceEntity
import com.ciclismo.portugal.data.local.entity.RaceResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {

    // ========== Race Operations ==========

    @Query("SELECT * FROM races WHERE startDate >= :fromDate ORDER BY startDate ASC")
    fun getUpcomingRaces(fromDate: Long = System.currentTimeMillis()): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE isActive = 1 ORDER BY startDate ASC")
    fun getActiveRaces(): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE id = :raceId")
    suspend fun getRaceById(raceId: String): RaceEntity?

    @Query("SELECT * FROM races WHERE type = :type ORDER BY startDate ASC")
    fun getRacesByType(type: String): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE category = :category ORDER BY startDate ASC")
    fun getRacesByCategory(category: String): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE startDate BETWEEN :startDate AND :endDate ORDER BY startDate ASC")
    fun getRacesInDateRange(startDate: Long, endDate: Long): Flow<List<RaceEntity>>

    /**
     * Check if there's a race happening today (for team freeze)
     * A race is happening today if:
     * - startDate <= today AND endDate >= today
     */
    @Query("""
        SELECT * FROM races
        WHERE isActive = 1
        AND startDate <= :endOfDay
        AND endDate >= :startOfDay
        LIMIT 1
    """)
    suspend fun getRaceHappeningToday(startOfDay: Long, endOfDay: Long): RaceEntity?

    /**
     * Get races starting tomorrow (for deadline notifications)
     */
    @Query("""
        SELECT * FROM races
        WHERE isActive = 1
        AND startDate BETWEEN :tomorrowStart AND :tomorrowEnd
        ORDER BY startDate ASC
    """)
    suspend fun getRacesStartingTomorrow(tomorrowStart: Long, tomorrowEnd: Long): List<RaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRace(race: RaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRaces(races: List<RaceEntity>)

    @Update
    suspend fun updateRace(race: RaceEntity)

    @Query("UPDATE races SET isActive = :isActive WHERE id = :raceId")
    suspend fun setRaceActive(raceId: String, isActive: Boolean)

    @Query("UPDATE races SET isFinished = :isFinished, finishedAt = :finishedAt, isActive = 0 WHERE id = :raceId")
    suspend fun setRaceFinished(raceId: String, isFinished: Boolean, finishedAt: Long?)

    @Query("SELECT * FROM races WHERE isFinished = 1 ORDER BY finishedAt DESC")
    fun getFinishedRaces(): Flow<List<RaceEntity>>

    @Query("SELECT isFinished FROM races WHERE id = :raceId")
    suspend fun isRaceFinished(raceId: String): Boolean?

    @Delete
    suspend fun deleteRace(race: RaceEntity)

    @Query("DELETE FROM races WHERE endDate < :date AND isActive = 0")
    suspend fun deleteOldRaces(date: Long)

    // ========== RaceResult Operations ==========

    @Query("SELECT * FROM race_results WHERE raceId = :raceId ORDER BY stageNumber ASC, position ASC")
    fun getRaceResults(raceId: String): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE raceId = :raceId AND stageNumber = :stageNumber ORDER BY position ASC")
    fun getStageResults(raceId: String, stageNumber: Int): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE cyclistId = :cyclistId ORDER BY timestamp DESC")
    fun getCyclistResults(cyclistId: String): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE cyclistId = :cyclistId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCyclistResults(cyclistId: String, limit: Int = 5): Flow<List<RaceResultEntity>>

    @Query("SELECT SUM(points + bonusPoints) FROM race_results WHERE cyclistId = :cyclistId")
    suspend fun getTotalPointsForCyclist(cyclistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: RaceResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllResults(results: List<RaceResultEntity>)

    @Delete
    suspend fun deleteResult(result: RaceResultEntity)

    @Query("DELETE FROM race_results WHERE raceId = :raceId")
    suspend fun deleteRaceResults(raceId: String)

    @Query("DELETE FROM race_results WHERE timestamp < :date")
    suspend fun deleteOldResults(date: Long)

    // ========== Season-specific queries ==========

    @Query("SELECT * FROM races WHERE season = :season ORDER BY startDate ASC")
    fun getRacesForSeason(season: Int): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE season = :season AND startDate >= :fromDate ORDER BY startDate ASC")
    fun getUpcomingRacesForSeason(season: Int, fromDate: Long = System.currentTimeMillis()): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE season = :season AND isActive = 1 ORDER BY startDate ASC")
    fun getActiveRacesForSeason(season: Int): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE season = :season AND isFinished = 1 ORDER BY finishedAt DESC")
    fun getFinishedRacesForSeason(season: Int): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE season = :season AND isFinished = 0 AND isActive = 0 ORDER BY startDate ASC LIMIT 1")
    suspend fun getNextUnfinishedRaceForSeason(season: Int): RaceEntity?

    @Query("SELECT COUNT(*) FROM races WHERE season = :season")
    suspend fun getRaceCountForSeason(season: Int): Int

    @Query("DELETE FROM races WHERE season = :season")
    suspend fun deleteAllRacesForSeason(season: Int)

    // Season-specific race results
    @Query("SELECT * FROM race_results WHERE season = :season AND raceId = :raceId ORDER BY position ASC")
    fun getRaceResultsForSeason(raceId: String, season: Int): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE season = :season AND cyclistId = :cyclistId ORDER BY timestamp DESC")
    fun getCyclistResultsForSeason(cyclistId: String, season: Int): Flow<List<RaceResultEntity>>

    // ========== Dynamic Pricing queries ==========

    /**
     * Get races starting within a date range (for pre-race price boosts)
     */
    @Query("""
        SELECT * FROM races
        WHERE season = :season
        AND isActive = 1
        AND isFinished = 0
        AND startDate BETWEEN :fromDate AND :toDate
        ORDER BY startDate ASC
    """)
    suspend fun getUpcomingRacesInRangeOnce(fromDate: Long, toDate: Long, season: Int): List<RaceEntity>

    /**
     * Get recently finished races (for resetting price boosts)
     */
    @Query("""
        SELECT * FROM races
        WHERE season = :season
        AND isFinished = 1
        ORDER BY finishedAt DESC
    """)
    suspend fun getFinishedRacesForSeasonOnce(season: Int): List<RaceEntity>
}
