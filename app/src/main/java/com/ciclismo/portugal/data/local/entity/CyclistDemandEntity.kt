package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.ciclismo.portugal.domain.model.SeasonConfig

/**
 * Tracking de procura de ciclistas.
 * Regista compras e vendas por periodo para calcular ajustes de preco.
 */
@Entity(
    tableName = "cyclist_demand",
    primaryKeys = ["cyclistId", "periodStart"],
    indices = [Index(value = ["season"])]
)
data class CyclistDemandEntity(
    val cyclistId: String,
    val periodStart: Long, // Inicio do periodo de tracking (ex: inicio do dia)
    val buyCount: Int = 0, // Numero de compras neste periodo
    val sellCount: Int = 0, // Numero de vendas neste periodo
    val ownershipCount: Int = 0, // Numero de equipas que tem este ciclista
    val totalTeams: Int = 0, // Total de equipas no jogo
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    /**
     * Percentagem de ownership (popularidade)
     */
    val ownershipPercent: Double
        get() = if (totalTeams > 0) (ownershipCount.toDouble() / totalTeams) * 100 else 0.0

    /**
     * Net demand (compras - vendas)
     */
    val netDemand: Int
        get() = buyCount - sellCount

    /**
     * Indica se ha mais compras que vendas
     */
    val isPositiveDemand: Boolean
        get() = netDemand > 0
}
