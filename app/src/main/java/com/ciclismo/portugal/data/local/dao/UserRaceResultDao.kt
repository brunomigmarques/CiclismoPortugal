package com.ciclismo.portugal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ciclismo.portugal.data.local.entity.UserRaceResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRaceResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: UserRaceResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<UserRaceResultEntity>)

    @Update
    suspend fun update(result: UserRaceResultEntity)

    @Delete
    suspend fun delete(result: UserRaceResultEntity)

    @Query("DELETE FROM user_race_results WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM user_race_results WHERE userId = :userId ORDER BY raceDate DESC")
    fun getResultsForUser(userId: String): Flow<List<UserRaceResultEntity>>

    @Query("SELECT * FROM user_race_results WHERE userId = :userId ORDER BY raceDate DESC LIMIT :limit")
    fun getRecentResultsForUser(userId: String, limit: Int): Flow<List<UserRaceResultEntity>>

    @Query("SELECT * FROM user_race_results WHERE id = :id")
    suspend fun getResultById(id: String): UserRaceResultEntity?

    @Query("SELECT * FROM user_race_results WHERE userId = :userId AND raceType = :raceType ORDER BY raceDate DESC")
    fun getResultsByType(userId: String, raceType: String): Flow<List<UserRaceResultEntity>>

    @Query("SELECT * FROM user_race_results WHERE userId = :userId AND raceDate >= :startDate AND raceDate <= :endDate ORDER BY raceDate DESC")
    fun getResultsInDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<UserRaceResultEntity>>

    @Query("SELECT COUNT(*) FROM user_race_results WHERE userId = :userId")
    suspend fun getResultCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM user_race_results WHERE userId = :userId AND position <= 3 AND position IS NOT NULL")
    suspend fun getPodiumCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM user_race_results WHERE userId = :userId AND position = 1")
    suspend fun getWinCount(userId: String): Int

    @Query("SELECT AVG(position) FROM user_race_results WHERE userId = :userId AND position IS NOT NULL")
    suspend fun getAveragePosition(userId: String): Float?

    @Query("SELECT SUM(distance) FROM user_race_results WHERE userId = :userId AND distance IS NOT NULL")
    suspend fun getTotalDistance(userId: String): Float?

    @Query("SELECT SUM(elevation) FROM user_race_results WHERE userId = :userId AND elevation IS NOT NULL")
    suspend fun getTotalElevation(userId: String): Int?

    @Query("DELETE FROM user_race_results WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // For rankings - get all results for a specific race
    @Query("SELECT * FROM user_race_results WHERE raceName = :raceName AND raceDate = :raceDate ORDER BY position ASC")
    fun getResultsForRace(raceName: String, raceDate: Long): Flow<List<UserRaceResultEntity>>

    // Get distinct race names for autocomplete
    @Query("SELECT DISTINCT raceName FROM user_race_results ORDER BY raceName ASC")
    suspend fun getAllRaceNames(): List<String>

    // Get distinct locations for autocomplete
    @Query("SELECT DISTINCT raceLocation FROM user_race_results ORDER BY raceLocation ASC")
    suspend fun getAllLocations(): List<String>

    // Get distinct categories for autocomplete
    @Query("SELECT DISTINCT category FROM user_race_results WHERE category IS NOT NULL ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
}
