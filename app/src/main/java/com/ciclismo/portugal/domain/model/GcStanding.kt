package com.ciclismo.portugal.domain.model

/**
 * General Classification (GC) standing for a cyclist in a multi-stage race.
 * Tracks accumulated time, points, and standings across all stages.
 */
data class GcStanding(
    val id: String,
    val raceId: String,
    val cyclistId: String,
    val gcPosition: Int,
    val gcTime: String = "",              // Total time (e.g., "72:34:12")
    val gcGap: String = "",               // Gap to leader (e.g., "+2:45" or "-")
    val totalPoints: Int = 0,             // Sum of all stage points
    val stageWins: Int = 0,               // Number of stage victories
    val stagePodiums: Int = 0,            // Number of top-3 finishes
    val lastUpdatedStage: Int = 0,        // Last stage processed
    val isGcLeader: Boolean = false,
    val isMountainsLeader: Boolean = false,
    val isPointsLeader: Boolean = false,
    val isYoungLeader: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    /**
     * Check if cyclist holds any classification jersey.
     */
    val hasJersey: Boolean
        get() = isGcLeader || isMountainsLeader || isPointsLeader || isYoungLeader

    /**
     * Get jersey emojis for display.
     */
    val jerseyEmojis: String
        get() = buildString {
            if (isGcLeader) append("🟡")
            if (isPointsLeader) append("🟢")
            if (isMountainsLeader) append("🔴")
            if (isYoungLeader) append("⚪")
        }

    /**
     * Display string for GC position.
     */
    val positionDisplay: String
        get() = "${gcPosition}º"

    /**
     * Display string for gap (leader shows "-").
     */
    val gapDisplay: String
        get() = if (gcPosition == 1) "-" else gcGap.ifBlank { "-" }

    companion object {
        /**
         * Create an empty standing for initialization.
         */
        fun empty(raceId: String, cyclistId: String, season: Int = SeasonConfig.CURRENT_SEASON): GcStanding {
            return GcStanding(
                id = "${raceId}_${cyclistId}_gc",
                raceId = raceId,
                cyclistId = cyclistId,
                gcPosition = 0,
                season = season
            )
        }
    }
}

/**
 * Points classification standing (Green Jersey equivalent).
 */
data class PointsStanding(
    val id: String,
    val raceId: String,
    val cyclistId: String,
    val position: Int,
    val totalPoints: Int = 0,
    val lastUpdatedStage: Int = 0,
    val season: Int = SeasonConfig.CURRENT_SEASON
)

/**
 * Mountains classification standing (Polka Dot Jersey equivalent).
 */
data class MountainsStanding(
    val id: String,
    val raceId: String,
    val cyclistId: String,
    val position: Int,
    val totalPoints: Int = 0,
    val lastUpdatedStage: Int = 0,
    val season: Int = SeasonConfig.CURRENT_SEASON
)

/**
 * Young rider classification standing (White Jersey equivalent).
 */
data class YoungStanding(
    val id: String,
    val raceId: String,
    val cyclistId: String,
    val position: Int,
    val gcTime: String = "",
    val gcGap: String = "",
    val lastUpdatedStage: Int = 0,
    val season: Int = SeasonConfig.CURRENT_SEASON
)

/**
 * Complete race standings summary with all classifications.
 */
data class RaceStandingsSummary(
    val raceId: String,
    val raceName: String,
    val currentStage: Int,
    val totalStages: Int,
    val gcLeader: CyclistStandingInfo?,
    val pointsLeader: CyclistStandingInfo?,
    val mountainsLeader: CyclistStandingInfo?,
    val youngLeader: CyclistStandingInfo?,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (totalStages > 0) currentStage.toFloat() / totalStages else 0f
}

/**
 * Cyclist info for standings display.
 */
data class CyclistStandingInfo(
    val cyclistId: String,
    val name: String,
    val teamName: String,
    val photoUrl: String? = null,
    val value: String // Time, points, or gap depending on classification
)
