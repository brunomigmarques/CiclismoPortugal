package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.SeasonConfig
import java.util.UUID

/**
 * Historico de alteracoes de preco dos ciclistas.
 * Regista todas as mudancas de preco para auditoria e visualizacao de tendencias.
 */
@Entity(
    tableName = "cyclist_price_history",
    indices = [
        Index(value = ["cyclistId"]),
        Index(value = ["season"]),
        Index(value = ["timestamp"])
    ]
)
data class CyclistPriceHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cyclistId: String,
    val oldPrice: Double,
    val newPrice: Double,
    val reason: String, // DEMAND, PRE_RACE_BOOST, RACE_RESET, MANUAL, RESULTS
    val raceId: String? = null, // Corrida associada (se aplicavel)
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    val priceChange: Double
        get() = newPrice - oldPrice

    val priceChangePercent: Double
        get() = if (oldPrice > 0) ((newPrice - oldPrice) / oldPrice) * 100 else 0.0
}

/**
 * Razoes para alteracao de preco
 */
enum class PriceChangeReason {
    DEMAND,         // Alteracao por procura (compras/vendas)
    PRE_RACE_BOOST, // Boost antes de corrida (+1% por dia, max 5%)
    RACE_RESET,     // Reset apos corrida terminar
    RESULTS,        // Ajuste baseado em resultados
    MANUAL,         // Ajuste manual pelo admin
    INITIAL         // Preco inicial ao criar ciclista
}
