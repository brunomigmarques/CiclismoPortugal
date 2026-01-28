package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.StravaGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StravaGoalDao {

    @Query("SELECT * FROM strava_goals WHERE year = :year ORDER BY type ASC")
    fun getGoalsByYear(year: Int): Flow<List<StravaGoalEntity>>

    @Query("SELECT * FROM strava_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): StravaGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: StravaGoalEntity): Long

    @Update
    suspend fun update(goal: StravaGoalEntity)

    @Delete
    suspend fun delete(goal: StravaGoalEntity)

    @Query("DELETE FROM strava_goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM strava_goals WHERE year = :year")
    suspend fun deleteByYear(year: Int)

    @Query("DELETE FROM strava_goals")
    suspend fun deleteAll()
}
