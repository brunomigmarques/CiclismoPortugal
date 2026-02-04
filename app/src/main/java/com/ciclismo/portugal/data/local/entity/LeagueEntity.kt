package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.League
import com.ciclismo.portugal.domain.model.LeagueType
import com.ciclismo.portugal.domain.model.SeasonConfig
import java.util.UUID

@Entity(
    tableName = "leagues",
    indices = [Index(value = ["season"])]
)
data class LeagueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // GLOBAL, PRIVATE, REGIONAL, MONTHLY
    val code: String?, // Codigo de 6 caracteres para ligas privadas
    val ownerId: String?, // null para ligas globais/regionais
    val region: String?, // Para ligas regionais (Lisboa, Porto, etc)
    val memberCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

fun LeagueEntity.toDomain(): League = League(
    id = id,
    name = name,
    type = LeagueType.valueOf(type),
    code = code,
    ownerId = ownerId,
    region = region,
    memberCount = memberCount,
    createdAt = createdAt,
    season = season
)

fun League.toEntity(): LeagueEntity = LeagueEntity(
    id = id,
    name = name,
    type = type.name,
    code = code,
    ownerId = ownerId,
    region = region,
    memberCount = memberCount,
    createdAt = createdAt,
    season = season
)
