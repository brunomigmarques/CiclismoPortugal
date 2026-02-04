package com.ciclismo.portugal.domain.model

data class FantasyTeam(
    val id: String,
    val userId: String,
    val teamName: String,
    val season: Int = SeasonConfig.CURRENT_SEASON,
    val budget: Double = 100.0,
    val totalPoints: Int = 0,
    val freeTransfers: Int = 2,
    val transfersMadeThisWeek: Int = 0,
    val gameweek: Int = 1,
    val wildcardUsed: Boolean = false,
    val wildcardActive: Boolean = false,
    val tripleCaptainUsed: Boolean = false,
    val benchBoostUsed: Boolean = false,
    val benchBoostActive: Boolean = false,
    val benchBoostOriginalBench: String? = null,
    val tripleCaptainActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Bot teams
    val isBot: Boolean = false, // True se for equipa gerada automaticamente
    // Per-race wildcard activation
    val tripleCaptainRaceId: String? = null, // Corrida onde Triple Captain esta ativo
    val benchBoostRaceId: String? = null, // Corrida onde Bench Boost esta ativo
    val wildcardRaceId: String? = null // Corrida onde Wildcard esta ativo
) {
    val displayBudget: String
        get() = "${String.format("%.1f", budget)}M"

    val hasWildcard: Boolean
        get() = !wildcardUsed

    val hasTripleCaptain: Boolean
        get() = !tripleCaptainUsed

    val hasBenchBoost: Boolean
        get() = !benchBoostUsed

    val isBenchBoostActive: Boolean
        get() = benchBoostActive

    val isTripleCaptainActive: Boolean
        get() = tripleCaptainActive

    val isWildcardActive: Boolean
        get() = wildcardActive

    /** True if unlimited transfers are allowed (wildcard active) */
    val hasUnlimitedTransfers: Boolean
        get() = wildcardActive

    /** Calculate how many more free transfers are available */
    val remainingFreeTransfers: Int
        get() = if (hasUnlimitedTransfers) Int.MAX_VALUE else maxOf(0, freeTransfers - transfersMadeThisWeek)

    /** Calculate point penalty for additional transfers */
    fun calculateTransferPenalty(additionalTransfers: Int): Int {
        if (hasUnlimitedTransfers) return 0
        val paidTransfers = maxOf(0, transfersMadeThisWeek + additionalTransfers - freeTransfers)
        return paidTransfers * 4 // 4 points per extra transfer
    }

    /** Check if Triple Captain is active for a specific race */
    fun isTripleCaptainActiveForRace(raceId: String): Boolean =
        tripleCaptainActive && tripleCaptainRaceId == raceId

    /** Check if Bench Boost is active for a specific race */
    fun isBenchBoostActiveForRace(raceId: String): Boolean =
        benchBoostActive && benchBoostRaceId == raceId

    /** Check if Wildcard is active for a specific race */
    fun isWildcardActiveForRace(raceId: String): Boolean =
        wildcardActive && wildcardRaceId == raceId

    /** Check if any wildcard is active for a specific race */
    fun hasActiveWildcardForRace(raceId: String): Boolean =
        isTripleCaptainActiveForRace(raceId) ||
        isBenchBoostActiveForRace(raceId) ||
        isWildcardActiveForRace(raceId)
}
