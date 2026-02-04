package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.model.SeasonConfig
import java.util.UUID

@Entity(
    tableName = "race_results",
    indices = [
        Index(value = ["season"]),
        Index(value = ["raceId", "season"])
    ]
)
data class RaceResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val raceId: String,
    val cyclistId: String,
    val stageNumber: Int?, // null para corridas de um dia
    val position: Int?, // null se DNF/DNS/DSQ
    val points: Int = 0,
    val bonusPoints: Int = 0, // KOM, breakaway, etc
    val isGcLeader: Boolean = false, // Jersey amarelo/rosa/vermelho
    val isMountainsLeader: Boolean = false, // Jersey bolinhas
    val isPointsLeader: Boolean = false, // Jersey verde
    val isYoungLeader: Boolean = false, // Jersey branco
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

fun RaceResultEntity.toDomain(): RaceResult = RaceResult(
    id = id,
    raceId = raceId,
    cyclistId = cyclistId,
    stageNumber = stageNumber,
    position = position,
    points = points,
    bonusPoints = bonusPoints,
    isGcLeader = isGcLeader,
    isMountainsLeader = isMountainsLeader,
    isPointsLeader = isPointsLeader,
    isYoungLeader = isYoungLeader,
    timestamp = timestamp,
    season = season
)

fun RaceResult.toEntity(): RaceResultEntity = RaceResultEntity(
    id = id,
    raceId = raceId,
    cyclistId = cyclistId,
    stageNumber = stageNumber,
    position = position,
    points = points,
    bonusPoints = bonusPoints,
    isGcLeader = isGcLeader,
    isMountainsLeader = isMountainsLeader,
    isPointsLeader = isPointsLeader,
    isYoungLeader = isYoungLeader,
    timestamp = timestamp,
    season = season
)
