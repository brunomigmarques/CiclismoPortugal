package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.model.SeasonConfig

@Entity(
    tableName = "cyclists",
    indices = [Index(value = ["season"])]
)
data class CyclistEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val teamId: String,
    val teamName: String,
    val nationality: String,
    val photoUrl: String?,
    val category: String, // SPRINTER, CLIMBER, CLASSICS, GC, ROULEUR
    val price: Double, // Em milhoes (ex: 12.5)
    val totalPoints: Int = 0,
    val form: Double = 0.0, // Media das ultimas 5 corridas
    val popularity: Double = 0.0, // % de equipas que o tem
    val syncedAt: Long = System.currentTimeMillis(),
    val age: Int? = null,
    val uciRanking: Int? = null,
    val speciality: String? = null,
    val profileUrl: String? = null, // Link ProCyclingStats/CyclingRanking
    val season: Int = SeasonConfig.CURRENT_SEASON,
    // Dynamic pricing fields
    val basePrice: Double = 0.0, // Preco original antes de ajustes
    val priceBoostActive: Boolean = false, // Boost pre-corrida ativo
    val priceBoostRaceId: String? = null, // ID da corrida que ativou o boost
    val lastPriceUpdate: Long = System.currentTimeMillis(), // Ultima atualizacao de preco
    // Availability status
    val isDisabled: Boolean = false, // Ciclista indisponivel (lesao, abandono, etc)
    val disabledReason: String? = null, // Motivo da indisponibilidade
    val disabledAt: Long? = null // Data em que foi desativado
)

fun CyclistEntity.toDomain(): Cyclist = Cyclist(
    id = id,
    firstName = firstName,
    lastName = lastName,
    fullName = "$firstName $lastName",
    teamId = teamId,
    teamName = teamName,
    nationality = nationality,
    photoUrl = photoUrl,
    category = CyclistCategory.valueOf(category),
    price = price,
    totalPoints = totalPoints,
    form = form,
    popularity = popularity,
    age = age,
    uciRanking = uciRanking,
    speciality = speciality,
    profileUrl = profileUrl,
    season = season,
    basePrice = basePrice,
    priceBoostActive = priceBoostActive,
    priceBoostRaceId = priceBoostRaceId,
    lastPriceUpdate = lastPriceUpdate,
    isDisabled = isDisabled,
    disabledReason = disabledReason,
    disabledAt = disabledAt
)

fun Cyclist.toEntity(): CyclistEntity = CyclistEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    teamId = teamId,
    teamName = teamName,
    nationality = nationality,
    photoUrl = photoUrl,
    category = category.name,
    price = price,
    totalPoints = totalPoints,
    form = form,
    popularity = popularity,
    age = age,
    uciRanking = uciRanking,
    speciality = speciality,
    profileUrl = profileUrl,
    season = season,
    basePrice = basePrice,
    priceBoostActive = priceBoostActive,
    priceBoostRaceId = priceBoostRaceId,
    lastPriceUpdate = lastPriceUpdate,
    isDisabled = isDisabled,
    disabledReason = disabledReason,
    disabledAt = disabledAt
)
