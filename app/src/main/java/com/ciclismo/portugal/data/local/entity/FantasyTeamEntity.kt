package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.SeasonConfig
import java.util.UUID

@Entity(
    tableName = "fantasy_teams",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["season"]),
        Index(value = ["userId", "season"])
    ]
)
data class FantasyTeamEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val teamName: String,
    val season: Int = SeasonConfig.CURRENT_SEASON, // Temporada (ano)
    val budget: Double = 100.0, // Budget inicial 100M
    val totalPoints: Int = 0,
    val freeTransfers: Int = 2, // 2 transferencias gratuitas por semana
    val transfersMadeThisWeek: Int = 0, // Transferencias feitas esta semana
    val gameweek: Int = 1,
    val wildcardUsed: Boolean = false,
    val wildcardActive: Boolean = false, // Wildcard ativo (transferencias ilimitadas)
    val tripleCaptainUsed: Boolean = false,
    val benchBoostUsed: Boolean = false,
    val benchBoostActive: Boolean = false, // Bench Boost ativo para esta corrida
    val benchBoostOriginalBench: String? = null, // IDs dos ciclistas que estavam no banco (separados por virgula)
    val tripleCaptainActive: Boolean = false, // Triple Captain ativo para a próxima corrida
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Bot teams
    val isBot: Boolean = false, // True se for uma equipa gerada automaticamente (bot)
    // Per-race wildcard activation
    val tripleCaptainRaceId: String? = null, // Corrida onde Triple Captain esta ativo
    val benchBoostRaceId: String? = null, // Corrida onde Bench Boost esta ativo
    val wildcardRaceId: String? = null // Corrida onde Wildcard esta ativo
)

fun FantasyTeamEntity.toDomain(): FantasyTeam = FantasyTeam(
    id = id,
    userId = userId,
    teamName = teamName,
    season = season,
    budget = budget,
    totalPoints = totalPoints,
    freeTransfers = freeTransfers,
    transfersMadeThisWeek = transfersMadeThisWeek,
    gameweek = gameweek,
    wildcardUsed = wildcardUsed,
    wildcardActive = wildcardActive,
    tripleCaptainUsed = tripleCaptainUsed,
    benchBoostUsed = benchBoostUsed,
    benchBoostActive = benchBoostActive,
    benchBoostOriginalBench = benchBoostOriginalBench,
    tripleCaptainActive = tripleCaptainActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isBot = isBot,
    tripleCaptainRaceId = tripleCaptainRaceId,
    benchBoostRaceId = benchBoostRaceId,
    wildcardRaceId = wildcardRaceId
)

fun FantasyTeam.toEntity(): FantasyTeamEntity = FantasyTeamEntity(
    id = id,
    userId = userId,
    teamName = teamName,
    season = season,
    budget = budget,
    totalPoints = totalPoints,
    freeTransfers = freeTransfers,
    transfersMadeThisWeek = transfersMadeThisWeek,
    gameweek = gameweek,
    wildcardUsed = wildcardUsed,
    wildcardActive = wildcardActive,
    tripleCaptainUsed = tripleCaptainUsed,
    benchBoostUsed = benchBoostUsed,
    benchBoostActive = benchBoostActive,
    benchBoostOriginalBench = benchBoostOriginalBench,
    tripleCaptainActive = tripleCaptainActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isBot = isBot,
    tripleCaptainRaceId = tripleCaptainRaceId,
    benchBoostRaceId = benchBoostRaceId,
    wildcardRaceId = wildcardRaceId
)
