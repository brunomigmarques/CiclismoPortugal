package com.ciclismo.portugal.domain.model

data class RaceResult(
    val id: String,
    val raceId: String,
    val cyclistId: String,
    val stageNumber: Int?,
    val position: Int?,
    val points: Int = 0,
    val bonusPoints: Int = 0,
    val isGcLeader: Boolean = false,
    val isMountainsLeader: Boolean = false,
    val isPointsLeader: Boolean = false,
    val isYoungLeader: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON,
    val status: String = "" // DNF, DNS, DSQ, or empty for normal finish
) {
    val totalPoints: Int
        get() = points + bonusPoints

    val hasJersey: Boolean
        get() = isGcLeader || isMountainsLeader || isPointsLeader || isYoungLeader

    val positionDisplay: String
        get() = when {
            position != null -> "${position}º"
            status.isNotBlank() -> status.uppercase()
            else -> "DNP"
        }
}
