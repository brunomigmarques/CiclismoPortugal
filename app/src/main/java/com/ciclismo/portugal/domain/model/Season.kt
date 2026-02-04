package com.ciclismo.portugal.domain.model

/**
 * Season configuration and constants for the Fantasy Cycling app.
 * Each season represents a calendar year of races.
 */
object SeasonConfig {
    /**
     * Current active season (year).
     * All new teams, races, and results will be associated with this season.
     */
    const val CURRENT_SEASON: Int = 2026

    /**
     * First season supported by the app.
     * Used for validation and historical queries.
     */
    const val FIRST_SEASON: Int = 2026

    /**
     * Check if a season is valid (within supported range)
     */
    fun isValidSeason(season: Int): Boolean = season >= FIRST_SEASON && season <= CURRENT_SEASON

    /**
     * Get list of all supported seasons (for UI dropdown/selection)
     */
    fun getAllSeasons(): List<Int> = (FIRST_SEASON..CURRENT_SEASON).toList().reversed()
}

/**
 * Represents a season summary for a fantasy team.
 * Used to display historical performance.
 */
data class SeasonSummary(
    val season: Int,
    val teamName: String,
    val totalPoints: Int,
    val racesParticipated: Int,
    val bestRacePoints: Int,
    val bestRaceName: String,
    val rank: Int? = null // Ranking na liga (se aplicavel)
)

/**
 * Represents a race result for display in historical view.
 */
data class TeamRaceHistoryItem(
    val raceId: String,
    val raceName: String,
    val raceDate: Long,
    val pointsEarned: Int,
    val rank: Int? = null, // Posicao na liga nessa corrida
    val season: Int
) {
    /**
     * Check if this race is likely a Grand Tour or stage race based on name patterns.
     * Used for UI display hints (actual verification happens when loading details).
     */
    val isLikelyGrandTour: Boolean
        get() {
            val lowerName = raceName.lowercase()
            return GRAND_TOUR_PATTERNS.any { pattern -> lowerName.contains(pattern) }
        }

    companion object {
        // Common Grand Tour and stage race name patterns
        private val GRAND_TOUR_PATTERNS = listOf(
            "tour de france",
            "giro d'italia",
            "giro de italia",
            "vuelta a espana",
            "vuelta a españa",
            "tour of",
            "volta a",
            "volta ao",
            "tour de suisse",
            "tour de romandie",
            "paris-nice",
            "tirreno-adriatico",
            "dauphine",
            "dauphiné",
            "criterium du dauphine",
            "uae tour",
            "tour of the alps",
            "tour down under",
            "tour de pologne",
            "tour of britain",
            "tour of california",
            "tour of oman",
            "tour of turkey",
            "tour of slovenia",
            "tour of norway",
            "tour of denmark",
            "tour of basque",
            "itzulia",
            "benelux tour",
            "deutschland tour",
            "tour of guangxi"
        )
    }
}
