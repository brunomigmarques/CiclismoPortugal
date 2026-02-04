package com.ciclismo.portugal.domain.model

/**
 * Represents a single stage in a multi-stage race (Grand Tour or Stage Race).
 */
data class Stage(
    val id: String,
    val raceId: String,
    val stageNumber: Int,
    val stageType: StageType,
    val name: String = "", // e.g., "Etapa 5: Lisboa - Serra da Estrela"
    val distance: Double? = null, // in km
    val elevationGain: Int? = null, // in meters
    val startLocation: String = "",
    val finishLocation: String = "",
    val date: Long = 0L,
    val dateString: String = "", // e.g., "16/02" - original date from paste
    val dayOfWeek: String = "", // e.g., "Monday", "Tuesday"
    val isRestDayAfter: Boolean = false,
    val isProcessed: Boolean = false,
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    val displayName: String
        get() = if (name.isNotBlank()) name else "Etapa $stageNumber"

    val distanceDisplay: String
        get() = distance?.let { "${it.toInt()} km" } ?: ""

    val stageTypeEmoji: String
        get() = stageType.emoji

    val fullDisplayName: String
        get() = "$displayName ${stageType.emoji}"
}

/**
 * Types of stages in multi-stage races.
 * Each type has different point multipliers and characteristics.
 */
enum class StageType(
    val displayNamePt: String,
    val emoji: String,
    val pointsMultiplier: Double,
    val description: String
) {
    PROLOGUE(
        "Prologo",
        "⏱️",
        0.5, // Reduced points for prologue
        "Contra-relogio curto de abertura"
    ),
    FLAT(
        "Plana",
        "🏃",
        1.0,
        "Etapa plana - favorece sprinters"
    ),
    HILLY(
        "Ondulada",
        "⛰️",
        1.0,
        "Terreno ondulado - favorece punchers"
    ),
    MOUNTAIN(
        "Montanha",
        "🏔️",
        1.2, // Bonus for mountain stages
        "Etapa de montanha - favorece escaladores"
    ),
    ITT(
        "Contra-Relogio Individual",
        "⏱️",
        1.2, // Bonus for ITT
        "Contra-relogio individual"
    ),
    TTT(
        "Contra-Relogio por Equipas",
        "👥",
        1.0,
        "Contra-relogio por equipas"
    );

    companion object {
        fun fromString(value: String): StageType {
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                it.displayNamePt.equals(value, ignoreCase = true)
            } ?: FLAT
        }
    }
}

/**
 * Points awarded by stage position.
 * Points are multiplied by StageType.pointsMultiplier.
 */
object StagePointsTable {
    // Base points for top 20 positions
    private val basePoints = mapOf(
        1 to 50,
        2 to 40,
        3 to 35,
        4 to 30,
        5 to 25,
        6 to 22,
        7 to 20,
        8 to 18,
        9 to 16,
        10 to 14,
        11 to 12,
        12 to 10,
        13 to 8,
        14 to 6,
        15 to 5,
        16 to 4,
        17 to 3,
        18 to 2,
        19 to 1,
        20 to 1
    )

    /**
     * Get points for a position in a specific stage type.
     */
    fun getPoints(position: Int, stageType: StageType): Int {
        val base = basePoints[position] ?: 0
        return (base * stageType.pointsMultiplier).toInt()
    }

    /**
     * Get all positions that earn points.
     */
    fun getPointsPositions(): List<Int> = basePoints.keys.sorted()
}

/**
 * Jersey bonus points awarded per stage.
 */
object JerseyBonusPoints {
    const val GC_LEADER = 10       // Yellow/Pink/Red jersey
    const val POINTS_LEADER = 5    // Green jersey
    const val MOUNTAINS_LEADER = 5 // Polka dot jersey
    const val YOUNG_LEADER = 3     // White jersey

    /**
     * Calculate total jersey bonus for a cyclist.
     */
    fun calculate(
        isGcLeader: Boolean,
        isPointsLeader: Boolean,
        isMountainsLeader: Boolean,
        isYoungLeader: Boolean
    ): Int {
        var total = 0
        if (isGcLeader) total += GC_LEADER
        if (isPointsLeader) total += POINTS_LEADER
        if (isMountainsLeader) total += MOUNTAINS_LEADER
        if (isYoungLeader) total += YOUNG_LEADER
        return total
    }
}

/**
 * Final GC bonus points at the end of a Grand Tour.
 */
object FinalGcBonusPoints {
    private val bonusPoints = mapOf(
        1 to 200,
        2 to 150,
        3 to 100,
        4 to 80,
        5 to 60,
        6 to 50,
        7 to 40,
        8 to 35,
        9 to 30,
        10 to 25
    )

    /**
     * Get bonus points for final GC position.
     */
    fun getBonus(gcPosition: Int): Int = bonusPoints[gcPosition] ?: 0
}
