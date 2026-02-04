package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.CyclistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CyclistDao {

    @Query("SELECT * FROM cyclists ORDER BY price DESC")
    fun getAllCyclists(): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE id = :id")
    suspend fun getCyclistById(id: String): CyclistEntity?

    @Query("SELECT * FROM cyclists WHERE category = :category ORDER BY price DESC")
    fun getCyclistsByCategory(category: String): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE teamId = :teamId ORDER BY price DESC")
    fun getCyclistsByTeam(teamId: String): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE nationality = :nationality ORDER BY price DESC")
    fun getCyclistsByNationality(nationality: String): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE price <= :maxPrice ORDER BY price DESC")
    fun getCyclistsByMaxPrice(maxPrice: Double): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%' OR teamName LIKE '%' || :query || '%' ORDER BY price DESC")
    fun searchCyclists(query: String): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists ORDER BY totalPoints DESC LIMIT :limit")
    fun getTopCyclists(limit: Int = 20): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists ORDER BY form DESC LIMIT :limit")
    fun getCyclistsInForm(limit: Int = 20): Flow<List<CyclistEntity>>

    @Query("SELECT DISTINCT teamName FROM cyclists ORDER BY teamName ASC")
    fun getDistinctTeams(): Flow<List<String>>

    @Query("SELECT DISTINCT nationality FROM cyclists ORDER BY nationality ASC")
    fun getDistinctNationalities(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCyclist(cyclist: CyclistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCyclists(cyclists: List<CyclistEntity>)

    @Update
    suspend fun updateCyclist(cyclist: CyclistEntity)

    @Query("UPDATE cyclists SET price = :price WHERE id = :cyclistId")
    suspend fun updatePrice(cyclistId: String, price: Double)

    @Query("UPDATE cyclists SET totalPoints = totalPoints + :points WHERE id = :cyclistId")
    suspend fun addPoints(cyclistId: String, points: Int)

    @Query("UPDATE cyclists SET popularity = :popularity WHERE id = :cyclistId")
    suspend fun updatePopularity(cyclistId: String, popularity: Double)

    @Delete
    suspend fun deleteCyclist(cyclist: CyclistEntity)

    @Query("DELETE FROM cyclists")
    suspend fun deleteAllCyclists()

    @Query("SELECT COUNT(*) FROM cyclists")
    suspend fun getCyclistCount(): Int

    // ========== Season-specific queries ==========

    @Query("SELECT * FROM cyclists WHERE season = :season ORDER BY price DESC")
    fun getCyclistsForSeason(season: Int): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE season = :season AND category = :category ORDER BY price DESC")
    fun getCyclistsByCategoryForSeason(category: String, season: Int): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE season = :season AND teamId = :teamId ORDER BY price DESC")
    fun getCyclistsByTeamForSeason(teamId: String, season: Int): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE season = :season ORDER BY totalPoints DESC LIMIT :limit")
    fun getTopCyclistsForSeason(season: Int, limit: Int = 20): Flow<List<CyclistEntity>>

    @Query("SELECT COUNT(*) FROM cyclists WHERE season = :season")
    suspend fun getCyclistCountForSeason(season: Int): Int

    @Query("DELETE FROM cyclists WHERE season = :season")
    suspend fun deleteAllCyclistsForSeason(season: Int)

    @Query("SELECT DISTINCT teamName FROM cyclists WHERE season = :season ORDER BY teamName ASC")
    fun getDistinctTeamsForSeason(season: Int): Flow<List<String>>

    // ========== Dynamic Pricing queries ==========

    @Query("SELECT * FROM cyclists WHERE season = :season ORDER BY price DESC")
    suspend fun getAllCyclistsForSeasonOnce(season: Int): List<CyclistEntity>

    @Query("UPDATE cyclists SET lastPriceUpdate = :timestamp WHERE id = :cyclistId")
    suspend fun updateLastPriceUpdate(cyclistId: String, timestamp: Long)

    @Query("UPDATE cyclists SET priceBoostActive = :active, priceBoostRaceId = :raceId WHERE id = :cyclistId")
    suspend fun updatePriceBoost(cyclistId: String, active: Boolean, raceId: String?)

    @Query("UPDATE cyclists SET basePrice = :basePrice WHERE id = :cyclistId")
    suspend fun updateBasePrice(cyclistId: String, basePrice: Double)

    @Query("SELECT * FROM cyclists WHERE priceBoostActive = 1 AND priceBoostRaceId = :raceId")
    suspend fun getCyclistsWithPriceBoostForRace(raceId: String): List<CyclistEntity>

    @Query("SELECT * FROM cyclists WHERE id IN (:ids)")
    suspend fun getCyclistsByIds(ids: List<String>): List<CyclistEntity>

    @Query("SELECT * FROM cyclists WHERE season = :season ORDER BY popularity DESC LIMIT :limit")
    fun getMostPopularCyclists(season: Int, limit: Int = 20): Flow<List<CyclistEntity>>

    // ========== Cyclist Availability queries ==========

    @Query("UPDATE cyclists SET isDisabled = :isDisabled, disabledReason = :reason, disabledAt = :disabledAt WHERE id = :cyclistId")
    suspend fun updateCyclistAvailability(cyclistId: String, isDisabled: Boolean, reason: String?, disabledAt: Long?)

    @Query("UPDATE cyclists SET isDisabled = 1, disabledReason = :reason, disabledAt = :disabledAt WHERE id = :cyclistId")
    suspend fun disableCyclist(cyclistId: String, reason: String, disabledAt: Long = System.currentTimeMillis())

    @Query("UPDATE cyclists SET isDisabled = 0, disabledReason = NULL, disabledAt = NULL WHERE id = :cyclistId")
    suspend fun enableCyclist(cyclistId: String)

    @Query("SELECT * FROM cyclists WHERE isDisabled = 1 ORDER BY disabledAt DESC")
    fun getDisabledCyclists(): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE season = :season AND isDisabled = 1 ORDER BY disabledAt DESC")
    fun getDisabledCyclistsForSeason(season: Int): Flow<List<CyclistEntity>>

    @Query("SELECT * FROM cyclists WHERE season = :season AND isDisabled = 0 ORDER BY price DESC")
    fun getAvailableCyclistsForSeason(season: Int): Flow<List<CyclistEntity>>

    @Query("SELECT COUNT(*) FROM cyclists WHERE season = :season AND isDisabled = 1")
    suspend fun getDisabledCyclistCountForSeason(season: Int): Int
}
