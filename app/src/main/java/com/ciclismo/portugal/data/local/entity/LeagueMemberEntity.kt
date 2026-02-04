package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.ciclismo.portugal.domain.model.LeagueMember
import com.ciclismo.portugal.domain.model.SeasonConfig

@Entity(
    tableName = "league_members",
    primaryKeys = ["leagueId", "userId"],
    indices = [
        Index(value = ["season"]),
        Index(value = ["leagueId", "season"])
    ]
)
data class LeagueMemberEntity(
    val leagueId: String,
    val userId: String,
    val teamId: String,
    val teamName: String,
    val rank: Int = 0,
    val points: Int = 0,
    val previousRank: Int = 0, // Para mostrar mudanca de posicao
    val joinedAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

fun LeagueMemberEntity.toDomain(): LeagueMember = LeagueMember(
    leagueId = leagueId,
    userId = userId,
    teamId = teamId,
    teamName = teamName,
    rank = rank,
    points = points,
    previousRank = previousRank,
    joinedAt = joinedAt,
    season = season
)

fun LeagueMember.toEntity(): LeagueMemberEntity = LeagueMemberEntity(
    leagueId = leagueId,
    userId = userId,
    teamId = teamId,
    teamName = teamName,
    rank = rank,
    points = points,
    previousRank = previousRank,
    joinedAt = joinedAt,
    season = season
)
