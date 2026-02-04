package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM cached_videos ORDER BY cachedAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM cached_videos ORDER BY cachedAt DESC")
    suspend fun getAllVideosSync(): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM cached_videos")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM cached_videos")
    suspend fun deleteAll()

    /**
     * Delete old videos (older than specified timestamp)
     */
    @Query("DELETE FROM cached_videos WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Replace all cached videos with new ones
     */
    @Transaction
    suspend fun replaceAll(videos: List<VideoEntity>) {
        deleteAll()
        insertAll(videos)
    }
}
