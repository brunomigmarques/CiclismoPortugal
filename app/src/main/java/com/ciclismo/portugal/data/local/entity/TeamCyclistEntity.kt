package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.TeamCyclist

@Entity(
    tableName = "team_cyclists",
    primaryKeys = ["teamId", "cyclistId"],
    indices = [Index(value = ["season"])]
)
data class TeamCyclistEntity(
    val teamId: String,
    val cyclistId: String,
    val isActive: Boolean = false, // 8 ativos por corrida
    val isCaptain: Boolean = false, // 1 capitao (pontos x2)
    val purchasePrice: Double, // Preco quando foi comprado
    val purchasedAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

fun TeamCyclistEntity.toDomain(): TeamCyclist = TeamCyclist(
    teamId = teamId,
    cyclistId = cyclistId,
    isActive = isActive,
    isCaptain = isCaptain,
    purchasePrice = purchasePrice,
    purchasedAt = purchasedAt,
    season = season
)

fun TeamCyclist.toEntity(): TeamCyclistEntity = TeamCyclistEntity(
    teamId = teamId,
    cyclistId = cyclistId,
    isActive = isActive,
    isCaptain = isCaptain,
    purchasePrice = purchasePrice,
    purchasedAt = purchasedAt,
    season = season
)
