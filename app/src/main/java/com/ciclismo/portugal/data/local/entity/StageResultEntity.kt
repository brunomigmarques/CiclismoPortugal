package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.StageResult
import com.ciclismo.portugal.domain.model.StageType

/**
 * Room entity for stage results in multi-stage races.
 */
@Entity(
    tableName = "stage_results",
    indices = [
        Index(value = ["raceId", "stageNumber"]),
        Index(value = ["raceId", "cyclistId"]),
        Index(value = ["season"])
    ]
)
data class StageResultEntity(
    @PrimaryKey
    val id: String,
    val raceId: String,
    val stageNumber: Int,
    val stageType: String,
    val cyclistId: String,
    val position: Int?,
    val points: Int = 0,
    val jerseyBonus: Int = 0,
    val isGcLeader: Boolean = false,
    val isMountainsLeader: Boolean = false,
    val isPointsLeader: Boolean = false,
    val isYoungLeader: Boolean = false,
    val time: String = "",
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    fun toDomain(): StageResult = StageResult(
        id = id,
        raceId = raceId,
        stageNumber = stageNumber,
        stageType = StageType.fromString(stageType),
        cyclistId = cyclistId,
        position = position,
        points = points,
        jerseyBonus = jerseyBonus,
        isGcLeader = isGcLeader,
        isMountainsLeader = isMountainsLeader,
        isPointsLeader = isPointsLeader,
        isYoungLeader = isYoungLeader,
        time = time,
        status = status,
        timestamp = timestamp,
        season = season
    )

    companion object {
        fun fromDomain(result: StageResult): StageResultEntity = StageResultEntity(
            id = result.id,
            raceId = result.raceId,
            stageNumber = result.stageNumber,
            stageType = result.stageType.name,
            cyclistId = result.cyclistId,
            position = result.position,
            points = result.points,
            jerseyBonus = result.jerseyBonus,
            isGcLeader = result.isGcLeader,
            isMountainsLeader = result.isMountainsLeader,
            isPointsLeader = result.isPointsLeader,
            isYoungLeader = result.isYoungLeader,
            time = result.time,
            status = result.status,
            timestamp = result.timestamp,
            season = result.season
        )
    }
}

/**
 * Room entity for GC standings.
 */
@Entity(
    tableName = "gc_standings",
    indices = [
        Index(value = ["raceId"]),
        Index(value = ["raceId", "cyclistId"], unique = true),
        Index(value = ["season"])
    ]
)
data class GcStandingEntity(
    @PrimaryKey
    val id: String,
    val raceId: String,
    val cyclistId: String,
    val gcPosition: Int,
    val gcTime: String = "",
    val gcGap: String = "",
    val totalPoints: Int = 0,
    val stageWins: Int = 0,
    val stagePodiums: Int = 0,
    val lastUpdatedStage: Int = 0,
    val isGcLeader: Boolean = false,
    val isMountainsLeader: Boolean = false,
    val isPointsLeader: Boolean = false,
    val isYoungLeader: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    fun toDomain(): com.ciclismo.portugal.domain.model.GcStanding =
        com.ciclismo.portugal.domain.model.GcStanding(
            id = id,
            raceId = raceId,
            cyclistId = cyclistId,
            gcPosition = gcPosition,
            gcTime = gcTime,
            gcGap = gcGap,
            totalPoints = totalPoints,
            stageWins = stageWins,
            stagePodiums = stagePodiums,
            lastUpdatedStage = lastUpdatedStage,
            isGcLeader = isGcLeader,
            isMountainsLeader = isMountainsLeader,
            isPointsLeader = isPointsLeader,
            isYoungLeader = isYoungLeader,
            timestamp = timestamp,
            season = season
        )

    companion object {
        fun fromDomain(standing: com.ciclismo.portugal.domain.model.GcStanding): GcStandingEntity =
            GcStandingEntity(
                id = standing.id,
                raceId = standing.raceId,
                cyclistId = standing.cyclistId,
                gcPosition = standing.gcPosition,
                gcTime = standing.gcTime,
                gcGap = standing.gcGap,
                totalPoints = standing.totalPoints,
                stageWins = standing.stageWins,
                stagePodiums = standing.stagePodiums,
                lastUpdatedStage = standing.lastUpdatedStage,
                isGcLeader = standing.isGcLeader,
                isMountainsLeader = standing.isMountainsLeader,
                isPointsLeader = standing.isPointsLeader,
                isYoungLeader = standing.isYoungLeader,
                timestamp = standing.timestamp,
                season = standing.season
            )
    }
}
