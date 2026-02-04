package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.SeasonConfig

@Entity(
    tableName = "races",
    indices = [Index(value = ["season"])]
)
data class RaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // ONE_DAY, GRAND_TOUR, STAGE_RACE
    val startDate: Long,
    val endDate: Long?, // null para corridas de um dia
    val stages: Int = 1, // numero de etapas
    val country: String,
    val category: String, // WT (WorldTour), PT (ProTour), 2.1, etc
    val isActive: Boolean = false, // corrida em andamento
    val isFinished: Boolean = false, // corrida encerrada (resultados aplicados)
    val finishedAt: Long? = null, // timestamp de quando a corrida foi encerrada
    val profileUrl: String? = null, // URL da imagem do perfil da corrida
    val imageUrl: String? = null, // URL da imagem/logo da corrida
    val syncedAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

fun RaceEntity.toDomain(): Race = Race(
    id = id,
    name = name,
    type = RaceType.valueOf(type),
    startDate = startDate,
    endDate = endDate,
    stages = stages,
    country = country,
    category = category,
    isActive = isActive,
    isFinished = isFinished,
    finishedAt = finishedAt,
    profileUrl = profileUrl,
    imageUrl = imageUrl,
    season = season
)

fun Race.toEntity(): RaceEntity = RaceEntity(
    id = id,
    name = name,
    type = type.name,
    startDate = startDate,
    endDate = endDate,
    stages = stages,
    country = country,
    category = category,
    isActive = isActive,
    isFinished = isFinished,
    finishedAt = finishedAt,
    profileUrl = profileUrl,
    imageUrl = imageUrl,
    season = season
)
