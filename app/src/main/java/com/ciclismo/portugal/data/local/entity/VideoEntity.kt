package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ciclismo.portugal.data.remote.video.CyclingVideo
import com.ciclismo.portugal.data.remote.video.VideoSource

/**
 * Local cache for cycling videos.
 * Videos from previous sessions are shown immediately while new ones load.
 */
@Entity(tableName = "cached_videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val channelName: String,
    val durationSeconds: Int,
    val source: String, // VideoSource enum name
    val cachedAt: Long = System.currentTimeMillis()
)

fun VideoEntity.toDomain(): CyclingVideo = CyclingVideo(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    videoUrl = videoUrl,
    channelName = channelName,
    durationSeconds = durationSeconds,
    source = try { VideoSource.valueOf(source) } catch (e: Exception) { VideoSource.YOUTUBE }
)

fun CyclingVideo.toEntity(): VideoEntity = VideoEntity(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    videoUrl = videoUrl,
    channelName = channelName,
    durationSeconds = durationSeconds,
    source = source.name,
    cachedAt = System.currentTimeMillis()
)
