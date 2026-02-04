package com.ciclismo.portugal.domain.model

data class TeamCyclist(
    val teamId: String,
    val cyclistId: String,
    val isActive: Boolean = false,
    val isCaptain: Boolean = false,
    val purchasePrice: Double,
    val purchasedAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

// Combinacao de TeamCyclist com dados do Cyclist para exibicao
data class TeamCyclistWithDetails(
    val teamCyclist: TeamCyclist,
    val cyclist: Cyclist,
    val currentValue: Double, // Valor atual (pode ser diferente do preco de compra)
    val pointsThisWeek: Int = 0
) {
    val profitLoss: Double
        get() = currentValue - teamCyclist.purchasePrice

    val profitLossPercentage: Double
        get() = if (teamCyclist.purchasePrice > 0) {
            (profitLoss / teamCyclist.purchasePrice) * 100
        } else 0.0
}
