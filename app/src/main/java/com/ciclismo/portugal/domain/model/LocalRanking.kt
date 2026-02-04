package com.ciclismo.portugal.domain.model

import java.util.UUID

/**
 * Represents a local/regional ranking for amateur cyclists.
 * Admin can create rankings by selecting which races count toward the ranking.
 */
data class LocalRanking(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String?,
    val season: Int = SeasonConfig.CURRENT_SEASON,
    val raceType: UserRaceType?, // Optional filter by race type
    val region: String?, // Optional filter by region
    val selectedRaceIds: List<String> = emptyList(), // IDs of races that count
    val selectedRaceNames: List<String> = emptyList(), // Names for display
    val pointsSystem: RankingPointsSystem = RankingPointsSystem.STANDARD,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a user's position in a local ranking.
 */
data class LocalRankingEntry(
    val id: String = UUID.randomUUID().toString(),
    val rankingId: String,
    val odoo: String, // User ID for matching current user
    val userId: String,
    val userName: String,
    val userPhotoUrl: String?,
    val teamName: String? = null, // Fantasy team name
    val region: String? = null, // User's region (e.g., "Norte", "Lisboa")
    val country: String = "PT", // Country code (default Portugal)
    val totalPoints: Int,
    val racesParticipated: Int,
    val wins: Int,
    val podiums: Int,
    val bestPosition: Int?,
    val position: Int, // Current position in ranking
    val previousPosition: Int?, // Previous position (for movement indicator)
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /** Get country flag emoji based on country code */
    val countryFlag: String
        get() = when (country.uppercase()) {
            "PT" -> "🇵🇹"
            "ES" -> "🇪🇸"
            "FR" -> "🇫🇷"
            "IT" -> "🇮🇹"
            "DE" -> "🇩🇪"
            "GB", "UK" -> "🇬🇧"
            "BR" -> "🇧🇷"
            "US" -> "🇺🇸"
            "NL" -> "🇳🇱"
            "BE" -> "🇧🇪"
            else -> "🏳️"
        }
    val positionChange: Int
        get() = if (previousPosition != null) previousPosition - position else 0

    val positionChangeDisplay: String
        get() = when {
            positionChange > 0 -> "+$positionChange"
            positionChange < 0 -> "$positionChange"
            else -> "="
        }
}

/**
 * Points systems for local rankings.
 */
enum class RankingPointsSystem(val displayName: String) {
    STANDARD("Padrao (25-20-16...)"),
    F1("Formula 1 (25-18-15...)"),
    LINEAR("Linear (10-9-8...)"),
    PARTICIPATION("Participacao (pontos por terminar)")
}

/**
 * Helper object for calculating points based on the ranking system.
 */
object RankingPointsCalculator {

    private val STANDARD_POINTS = listOf(25, 20, 16, 13, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
    private val F1_POINTS = listOf(25, 18, 15, 12, 10, 8, 6, 4, 2, 1)
    private val LINEAR_POINTS = listOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

    fun calculatePoints(position: Int?, system: RankingPointsSystem): Int {
        if (position == null || position <= 0) return 0

        return when (system) {
            RankingPointsSystem.STANDARD -> {
                STANDARD_POINTS.getOrElse(position - 1) { 0 }
            }
            RankingPointsSystem.F1 -> {
                F1_POINTS.getOrElse(position - 1) { 0 }
            }
            RankingPointsSystem.LINEAR -> {
                if (position <= 10) LINEAR_POINTS[position - 1] else 0
            }
            RankingPointsSystem.PARTICIPATION -> {
                // 1 point for finishing, bonus for top 10
                1 + if (position <= 10) (11 - position) else 0
            }
        }
    }

    fun getPointsTable(system: RankingPointsSystem): List<Pair<Int, Int>> {
        return when (system) {
            RankingPointsSystem.STANDARD -> STANDARD_POINTS.mapIndexed { i, pts -> i + 1 to pts }
            RankingPointsSystem.F1 -> F1_POINTS.mapIndexed { i, pts -> i + 1 to pts }
            RankingPointsSystem.LINEAR -> LINEAR_POINTS.mapIndexed { i, pts -> i + 1 to pts }
            RankingPointsSystem.PARTICIPATION -> (1..10).map { i -> i to (12 - i) }
        }
    }
}
