package com.ciclismo.portugal.domain.model

data class LeagueMember(
    val leagueId: String,
    val userId: String,
    val teamId: String,
    val teamName: String,
    val rank: Int = 0,
    val points: Int = 0,
    val previousRank: Int = 0,
    val joinedAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    val rankChange: Int
        get() = previousRank - rank // Positivo = subiu, Negativo = desceu

    val isRising: Boolean
        get() = rankChange > 0

    val isFalling: Boolean
        get() = rankChange < 0

    val rankChangeDisplay: String
        get() = when {
            rankChange > 0 -> "+$rankChange"
            rankChange < 0 -> "$rankChange"
            else -> "-"
        }
}
