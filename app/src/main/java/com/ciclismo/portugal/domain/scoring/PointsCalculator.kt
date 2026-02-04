package com.ciclismo.portugal.domain.scoring

import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.model.RaceType

/**
 * Calculates fantasy points for cycling races
 * Based on real fantasy cycling scoring systems
 */
object PointsCalculator {

    // ========== ONE-DAY RACE POINTS ==========
    // Monuments and WorldTour one-day races
    fun calculateOneDayPoints(position: Int?): Int {
        if (position == null) return 0
        return when (position) {
            1 -> 100
            2 -> 70
            3 -> 50
            4 -> 40
            5 -> 35
            6 -> 30
            7 -> 25
            8 -> 20
            9 -> 15
            10 -> 10
            in 11..20 -> 5
            in 21..30 -> 2
            else -> 0
        }
    }

    // ========== STAGE RACE POINTS ==========
    // Grand Tours and other stage races
    fun calculateStagePoints(position: Int?): Int {
        if (position == null) return 0
        return when (position) {
            1 -> 50
            2 -> 35
            3 -> 25
            4 -> 18
            5 -> 12
            6 -> 10
            7 -> 8
            8 -> 6
            9 -> 4
            10 -> 2
            in 11..20 -> 1
            else -> 0
        }
    }

    // ========== JERSEY POINTS (per day) ==========
    const val JERSEY_GC_LEADER = 10        // Yellow/Pink/Red jersey
    const val JERSEY_MOUNTAINS = 5         // Polka dots / Mountains
    const val JERSEY_POINTS = 5            // Green jersey / Points
    const val JERSEY_YOUNG = 3             // White jersey / Young rider

    // ========== FINAL GC BONUS POINTS ==========
    fun calculateGcFinalBonus(position: Int?): Int {
        if (position == null) return 0
        return when (position) {
            1 -> 200
            2 -> 150
            3 -> 100
            4 -> 75
            5 -> 50
            6 -> 40
            7 -> 30
            8 -> 25
            9 -> 20
            10 -> 15
            in 11..20 -> 10
            else -> 0
        }
    }

    // ========== SPECIAL BONUSES ==========
    const val BREAKAWAY_50KM = 5           // In breakaway for 50+ km
    const val KOM_TOP = 3                  // First over a KOM point
    const val INTERMEDIATE_SPRINT = 2      // First at intermediate sprint
    const val HAT_TRICK = 50               // 3 stage wins in same race

    // ========== CAPTAIN MULTIPLIER ==========
    fun applyCaptain(points: Int): Int = points * 2
    fun applyTripleCaptain(points: Int): Int = points * 3

    /**
     * Calculate total points for a race result
     */
    fun calculateResultPoints(result: RaceResult, raceType: RaceType): Int {
        var points = when (raceType) {
            RaceType.ONE_DAY -> calculateOneDayPoints(result.position)
            RaceType.GRAND_TOUR, RaceType.STAGE_RACE -> calculateStagePoints(result.position)
        }

        // Add jersey bonuses
        if (result.isGcLeader) points += JERSEY_GC_LEADER
        if (result.isMountainsLeader) points += JERSEY_MOUNTAINS
        if (result.isPointsLeader) points += JERSEY_POINTS
        if (result.isYoungLeader) points += JERSEY_YOUNG

        // Add bonus points (KOM, breakaway, etc.)
        points += result.bonusPoints

        return points
    }

    /**
     * Calculate points for a cyclist with captain status
     */
    fun calculateCyclistPoints(
        result: RaceResult,
        raceType: RaceType,
        isCaptain: Boolean,
        isTripleCaptain: Boolean = false
    ): Int {
        val basePoints = calculateResultPoints(result, raceType)

        return when {
            isTripleCaptain -> applyTripleCaptain(basePoints)
            isCaptain -> applyCaptain(basePoints)
            else -> basePoints
        }
    }

    /**
     * Get points description for UI display
     */
    fun getPointsBreakdown(result: RaceResult, raceType: RaceType): List<PointsBreakdownItem> {
        val breakdown = mutableListOf<PointsBreakdownItem>()

        // Position points
        val positionPoints = when (raceType) {
            RaceType.ONE_DAY -> calculateOneDayPoints(result.position)
            RaceType.GRAND_TOUR, RaceType.STAGE_RACE -> calculateStagePoints(result.position)
        }
        if (positionPoints > 0) {
            breakdown.add(PointsBreakdownItem(
                label = result.position?.let { "${it}º lugar" } ?: "Posição",
                points = positionPoints
            ))
        }

        // Jersey bonuses
        if (result.isGcLeader) {
            breakdown.add(PointsBreakdownItem("Jersey Líder (GC)", JERSEY_GC_LEADER))
        }
        if (result.isMountainsLeader) {
            breakdown.add(PointsBreakdownItem("Jersey Montanha", JERSEY_MOUNTAINS))
        }
        if (result.isPointsLeader) {
            breakdown.add(PointsBreakdownItem("Jersey Pontos", JERSEY_POINTS))
        }
        if (result.isYoungLeader) {
            breakdown.add(PointsBreakdownItem("Jersey Jovem", JERSEY_YOUNG))
        }

        // Bonus points
        if (result.bonusPoints > 0) {
            breakdown.add(PointsBreakdownItem("Bónus", result.bonusPoints))
        }

        return breakdown
    }
}

data class PointsBreakdownItem(
    val label: String,
    val points: Int
)
