package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.UserRaceType
import com.ciclismo.portugal.domain.model.ResultSource
import com.ciclismo.portugal.domain.model.UserRaceResult
import java.util.UUID

@Entity(
    tableName = "user_race_results",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["raceDate"]),
        Index(value = ["raceType"])
    ]
)
data class UserRaceResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val raceName: String,
    val raceDate: Long,
    val raceLocation: String,
    val raceType: String, // Stored as string for Room
    val distance: Float?,
    val elevation: Int?,
    val bibNumber: Int?,
    val position: Int?,
    val totalParticipants: Int?,
    val categoryPosition: Int?,
    val categoryTotalParticipants: Int?,
    val category: String?,
    val finishTime: String?,
    val avgSpeed: Float?,
    val resultSource: String, // Stored as string for Room
    val sourceUrl: String?,
    val eventUrl: String?,
    val organizerName: String?,
    val notes: String?,
    val timestamp: Long = System.currentTimeMillis()
)

fun UserRaceResultEntity.toDomain(): UserRaceResult = UserRaceResult(
    id = id,
    userId = userId,
    raceName = raceName,
    raceDate = raceDate,
    raceLocation = raceLocation,
    raceType = try { UserRaceType.valueOf(raceType) } catch (e: Exception) { UserRaceType.OTHER },
    distance = distance,
    elevation = elevation,
    bibNumber = bibNumber,
    position = position,
    totalParticipants = totalParticipants,
    categoryPosition = categoryPosition,
    categoryTotalParticipants = categoryTotalParticipants,
    category = category,
    finishTime = finishTime,
    avgSpeed = avgSpeed,
    resultSource = try { ResultSource.valueOf(resultSource) } catch (e: Exception) { ResultSource.MANUAL },
    sourceUrl = sourceUrl,
    eventUrl = eventUrl,
    organizerName = organizerName,
    notes = notes,
    timestamp = timestamp
)

fun UserRaceResult.toEntity(): UserRaceResultEntity = UserRaceResultEntity(
    id = id,
    userId = userId,
    raceName = raceName,
    raceDate = raceDate,
    raceLocation = raceLocation,
    raceType = raceType.name,
    distance = distance,
    elevation = elevation,
    bibNumber = bibNumber,
    position = position,
    totalParticipants = totalParticipants,
    categoryPosition = categoryPosition,
    categoryTotalParticipants = categoryTotalParticipants,
    category = category,
    finishTime = finishTime,
    avgSpeed = avgSpeed,
    resultSource = resultSource.name,
    sourceUrl = sourceUrl,
    eventUrl = eventUrl,
    organizerName = organizerName,
    notes = notes,
    timestamp = timestamp
)
