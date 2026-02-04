package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.TeamRaceHistoryItem

/**
 * Guarda os pontos que cada equipa ganhou em cada corrida.
 * Permite mostrar o histórico de pontos por corrida e por temporada.
 */
@Entity(
    tableName = "team_race_results",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["raceId"]),
        Index(value = ["season"]),
        Index(value = ["teamId", "raceId"], unique = true),
        Index(value = ["teamId", "season"])
    ]
)
data class TeamRaceResultEntity(
    @PrimaryKey val id: String,
    val teamId: String,
    val raceId: String,
    val raceName: String,
    val pointsEarned: Int,
    val raceDate: Long = 0L, // Data da corrida para ordenacao
    val season: Int = SeasonConfig.CURRENT_SEASON, // Temporada (ano)
    val processedAt: Long = System.currentTimeMillis(),
    // Cyclist breakdown as JSON string: [{"cyclistId":"...", "name":"...", "teamName":"...", "points":10, "isCaptain":true, "position":1}]
    val cyclistBreakdownJson: String? = null,
    val captainName: String? = null,
    val wasTripleCaptainActive: Boolean = false,
    val wasBenchBoostActive: Boolean = false
)

/**
 * Convert to domain model for UI display
 */
fun TeamRaceResultEntity.toHistoryItem(): TeamRaceHistoryItem = TeamRaceHistoryItem(
    raceId = raceId,
    raceName = raceName,
    raceDate = raceDate,
    pointsEarned = pointsEarned,
    rank = null,
    season = season
)
