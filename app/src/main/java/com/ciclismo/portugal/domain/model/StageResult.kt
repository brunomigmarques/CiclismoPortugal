package com.ciclismo.portugal.domain.model

/**
 * Result of a single cyclist in a specific stage.
 * Contains position, points, and jersey status for that stage.
 */
data class StageResult(
    val id: String,
    val raceId: String,
    val stageNumber: Int,
    val stageType: StageType,
    val cyclistId: String,
    val position: Int?,
    val points: Int = 0,          // Points earned from position
    val jerseyBonus: Int = 0,     // Bonus from holding jerseys
    val isGcLeader: Boolean = false,
    val isMountainsLeader: Boolean = false,
    val isPointsLeader: Boolean = false,
    val isYoungLeader: Boolean = false,
    val time: String = "",        // Stage time or gap (e.g., "+0:23" or "4:32:15")
    val status: String = "",      // DNF, DNS, DSQ, OTL, or empty for normal finish
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    /**
     * Total points for this stage (position + jersey bonus).
     */
    val totalPoints: Int
        get() = points + jerseyBonus

    /**
     * Check if cyclist holds any jersey.
     */
    val hasJersey: Boolean
        get() = isGcLeader || isMountainsLeader || isPointsLeader || isYoungLeader

    /**
     * Check if cyclist finished the stage normally.
     */
    val didFinish: Boolean
        get() = status.isBlank() || status.uppercase() !in listOf("DNF", "DNS", "DSQ", "OTL")

    /**
     * Display string for position.
     */
    val positionDisplay: String
        get() = when {
            position != null && position > 0 -> "${position}º"
            status.isNotBlank() -> status.uppercase()
            else -> "-"
        }

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

    companion object {
        /**
         * Calculate points for a position using the stage type multiplier.
         */
        fun calculatePoints(position: Int?, stageType: StageType): Int {
            return position?.let { StagePointsTable.getPoints(it, stageType) } ?: 0
        }

        /**
         * Calculate jersey bonus for given jersey statuses.
         */
        fun calculateJerseyBonus(
            isGcLeader: Boolean,
            isPointsLeader: Boolean,
            isMountainsLeader: Boolean,
            isYoungLeader: Boolean
        ): Int {
            return JerseyBonusPoints.calculate(
                isGcLeader, isPointsLeader, isMountainsLeader, isYoungLeader
            )
        }
    }
}

/**
 * Status codes for stage results.
 */
object StageResultStatus {
    const val DNF = "DNF"   // Did Not Finish
    const val DNS = "DNS"   // Did Not Start
    const val DSQ = "DSQ"   // Disqualified
    const val OTL = "OTL"   // Outside Time Limit

    val allStatuses = listOf(DNF, DNS, DSQ, OTL)

    fun isValidStatus(status: String): Boolean =
        status.isBlank() || status.uppercase() in allStatuses
}
