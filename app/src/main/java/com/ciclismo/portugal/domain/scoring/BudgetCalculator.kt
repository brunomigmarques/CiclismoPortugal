package com.ciclismo.portugal.domain.scoring

import com.ciclismo.portugal.domain.model.RaceType
import kotlin.math.ceil

/**
 * Calculates budget earnings from race results.
 *
 * Budget Rules:
 * - Players cannot have negative balance (minimum is 0)
 * - One-day races: 50M total prize pool distributed proportionally
 * - Multi-day races (stage races/grand tours): 20M per stage distributed proportionally
 *
 * Prize distribution:
 * - Only the TOP 50% of league participants earn money (promotes competition!)
 * - Distribution is proportional and descending based on finishing position
 * - 1st place gets the most, last "in the money" position gets the least
 * - Formula: Position i out of M (eligible positions) gets (M - i + 1) shares
 */
object BudgetCalculator {

    // ========== PRIZE POOLS ==========
    const val ONE_DAY_PRIZE_POOL = 50.0 // Million - total pool for one-day races
    const val STAGE_PRIZE_POOL = 20.0   // Million - pool per stage for multi-day races
    const val GC_FINAL_BONUS_POOL = 30.0 // Million - bonus pool for Grand Tour final GC

    // ========== EARNING THRESHOLD ==========
    /** Only top 50% of participants earn money - promotes competition! */
    const val EARNING_THRESHOLD_PERCENTAGE = 0.50 // 50% - "finish in top half to earn"

    /**
     * Calculate how many positions are "in the money" for a given number of participants.
     * Uses ceiling to ensure at least 1 position earns (for small leagues).
     *
     * Examples:
     * - 10 participants -> top 5 earn
     * - 7 participants -> top 4 earn (ceil of 3.5)
     * - 3 participants -> top 2 earn (ceil of 1.5)
     * - 1 participant -> top 1 earns
     */
    fun getEligiblePositions(totalParticipants: Int): Int {
        if (totalParticipants < 1) return 0
        return ceil(totalParticipants * EARNING_THRESHOLD_PERCENTAGE).toInt().coerceAtLeast(1)
    }

    /**
     * Check if a position is eligible to earn money.
     */
    fun isEligibleForEarnings(position: Int?, totalParticipants: Int): Boolean {
        if (position == null || position < 1) return false
        return position <= getEligiblePositions(totalParticipants)
    }

    /**
     * Calculate proportional budget earning for a one-day race.
     * Prize pool is distributed across top 50% of league participants.
     *
     * @param teamPosition The team's finishing position (1-based)
     * @param totalParticipants Total number of teams in the league participating in this race
     * @return Budget earned in millions (0 if not in top 50%)
     */
    fun calculateOneDayBudgetEarning(teamPosition: Int?, totalParticipants: Int): Double {
        return calculateProportionalEarning(teamPosition, totalParticipants, ONE_DAY_PRIZE_POOL)
    }

    /**
     * Calculate proportional budget earning for a stage race stage.
     * Prize pool is distributed across top 50% of league participants.
     *
     * @param teamPosition The team's finishing position in this stage (1-based)
     * @param totalParticipants Total number of teams in the league participating in this race
     * @return Budget earned in millions (0 if not in top 50%)
     */
    fun calculateStageBudgetEarning(teamPosition: Int?, totalParticipants: Int): Double {
        return calculateProportionalEarning(teamPosition, totalParticipants, STAGE_PRIZE_POOL)
    }

    /**
     * Calculate proportional budget earning for Grand Tour final GC bonus.
     *
     * @param teamPosition The team's final GC position (1-based)
     * @param totalParticipants Total number of teams in the league participating in this race
     * @return Budget bonus earned in millions (0 if not in top 50%)
     */
    fun calculateGcFinalBudgetBonus(teamPosition: Int?, totalParticipants: Int): Double {
        return calculateProportionalEarning(teamPosition, totalParticipants, GC_FINAL_BONUS_POOL)
    }

    /**
     * Core proportional distribution algorithm.
     * Distributes the prize pool across TOP 50% of participants based on finishing position.
     *
     * Formula (for M eligible positions out of N total):
     * - Position 1 gets M shares
     * - Position 2 gets M-1 shares
     * - Position M gets 1 share
     * - Positions M+1 to N get 0 shares (not in the money!)
     * - Total shares = M*(M+1)/2
     * - Earning = (shares / totalShares) * prizePool
     *
     * Example: 10 participants, 50M prize pool
     * - Eligible: top 5 (positions 1-5)
     * - Position 1: 5/15 * 50M = 16.67M
     * - Position 2: 4/15 * 50M = 13.33M
     * - Position 3: 3/15 * 50M = 10.00M
     * - Position 4: 2/15 * 50M = 6.67M
     * - Position 5: 1/15 * 50M = 3.33M
     * - Positions 6-10: 0M (not in top half!)
     *
     * @param position The finishing position (1-based)
     * @param totalParticipants Total number of participants
     * @param prizePool Total prize pool to distribute
     * @return Proportional share of the prize pool (0 if not in top 50%)
     */
    fun calculateProportionalEarning(position: Int?, totalParticipants: Int, prizePool: Double): Double {
        if (position == null || position < 1 || totalParticipants < 1) return 0.0

        // Calculate how many positions earn money
        val eligiblePositions = getEligiblePositions(totalParticipants)

        // If position is outside the earning threshold, return 0
        if (position > eligiblePositions) return 0.0

        // Shares for this position (1st gets M shares, 2nd gets M-1, etc.)
        val shares = eligiblePositions - position + 1

        // Total shares among eligible positions = M*(M+1)/2
        val totalShares = eligiblePositions * (eligiblePositions + 1) / 2.0

        // Proportional earning
        return (shares / totalShares) * prizePool
    }

    /**
     * Calculate total budget earning for a race result.
     *
     * @param teamPosition The team's finishing position
     * @param totalParticipants Total teams in the league for this race
     * @param raceType Type of race (ONE_DAY, STAGE_RACE, GRAND_TOUR)
     * @param isGcFinal Whether this is the final GC result (for Grand Tours)
     * @return Total budget earned (0 if not in top 50%)
     */
    fun calculateBudgetEarning(
        teamPosition: Int?,
        totalParticipants: Int,
        raceType: RaceType,
        isGcFinal: Boolean = false
    ): Double {
        val earning = when (raceType) {
            RaceType.ONE_DAY -> calculateOneDayBudgetEarning(teamPosition, totalParticipants)
            RaceType.STAGE_RACE -> calculateStageBudgetEarning(teamPosition, totalParticipants)
            RaceType.GRAND_TOUR -> {
                val stageEarning = calculateStageBudgetEarning(teamPosition, totalParticipants)
                val gcBonus = if (isGcFinal) calculateGcFinalBudgetBonus(teamPosition, totalParticipants) else 0.0
                stageEarning + gcBonus
            }
        }
        return earning
    }

    /**
     * Update team budget ensuring it never goes negative.
     */
    fun calculateNewBudget(currentBudget: Double, earning: Double, expense: Double = 0.0): Double {
        val newBudget = currentBudget + earning - expense
        return maxOf(0.0, newBudget) // Budget cannot be negative
    }

    /**
     * Check if a team can afford a purchase.
     */
    fun canAfford(currentBudget: Double, price: Double): Boolean {
        return currentBudget >= price
    }

    /**
     * Get earnings description for UI display.
     */
    fun getEarningsBreakdown(
        teamPosition: Int?,
        totalParticipants: Int,
        raceType: RaceType,
        isGcFinal: Boolean = false
    ): List<BudgetBreakdownItem> {
        val breakdown = mutableListOf<BudgetBreakdownItem>()
        val eligiblePositions = getEligiblePositions(totalParticipants)

        val positionEarning = when (raceType) {
            RaceType.ONE_DAY -> calculateOneDayBudgetEarning(teamPosition, totalParticipants)
            RaceType.STAGE_RACE, RaceType.GRAND_TOUR -> calculateStageBudgetEarning(teamPosition, totalParticipants)
        }

        if (positionEarning > 0) {
            val label = when (raceType) {
                RaceType.ONE_DAY -> "${teamPosition}º de $eligiblePositions pagos (corrida)"
                else -> "${teamPosition}º de $eligiblePositions pagos (etapa)"
            }
            breakdown.add(BudgetBreakdownItem(label, positionEarning))
        }

        if (isGcFinal && raceType == RaceType.GRAND_TOUR) {
            val gcBonus = calculateGcFinalBudgetBonus(teamPosition, totalParticipants)
            if (gcBonus > 0) {
                breakdown.add(BudgetBreakdownItem("Bónus GC Final", gcBonus))
            }
        }

        return breakdown
    }

    /**
     * Preview earnings distribution for a league.
     * Useful for showing users how the prize pool will be distributed.
     *
     * @param totalParticipants Number of participants in the league
     * @param prizePool Total prize pool
     * @return List of position -> earning pairs (only eligible positions shown)
     */
    fun previewDistribution(totalParticipants: Int, prizePool: Double): List<Pair<Int, Double>> {
        val eligiblePositions = getEligiblePositions(totalParticipants)
        return (1..eligiblePositions).map { position ->
            position to calculateProportionalEarning(position, totalParticipants, prizePool)
        }
    }

    /**
     * Get a human-readable description of the earning threshold.
     */
    fun getEarningThresholdDescription(totalParticipants: Int): String {
        val eligiblePositions = getEligiblePositions(totalParticipants)
        val percentage = (EARNING_THRESHOLD_PERCENTAGE * 100).toInt()
        return "Top $eligiblePositions de $totalParticipants (${percentage}%) ganham dinheiro"
    }
}

data class BudgetBreakdownItem(
    val label: String,
    val amount: Double
) {
    val displayAmount: String
        get() = String.format("%.1fM", amount)
}
