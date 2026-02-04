package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.Transfer
import java.util.UUID

@Entity(
    tableName = "transfers",
    indices = [Index(value = ["season"])]
)
data class TransferEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teamId: String,
    val cyclistInId: String,
    val cyclistOutId: String,
    val cyclistInName: String, // Para exibicao rapida
    val cyclistOutName: String,
    val priceIn: Double,
    val priceOut: Double,
    val gameweek: Int,
    val pointsCost: Int = 0, // -4 se for transferencia extra
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

fun TransferEntity.toDomain(): Transfer = Transfer(
    id = id,
    teamId = teamId,
    cyclistInId = cyclistInId,
    cyclistOutId = cyclistOutId,
    cyclistInName = cyclistInName,
    cyclistOutName = cyclistOutName,
    priceIn = priceIn,
    priceOut = priceOut,
    gameweek = gameweek,
    pointsCost = pointsCost,
    timestamp = timestamp,
    season = season
)

fun Transfer.toEntity(): TransferEntity = TransferEntity(
    id = id,
    teamId = teamId,
    cyclistInId = cyclistInId,
    cyclistOutId = cyclistOutId,
    cyclistInName = cyclistInName,
    cyclistOutName = cyclistOutName,
    priceIn = priceIn,
    priceOut = priceOut,
    gameweek = gameweek,
    pointsCost = pointsCost,
    timestamp = timestamp,
    season = season
)
