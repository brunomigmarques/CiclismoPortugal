package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.ciclismo.portugal.domain.model.SeasonConfig

/**
 * Participantes confirmados para uma corrida.
 * Usado para aplicar o boost de preco pre-corrida (+5% nos 5 dias antes).
 */
@Entity(
    tableName = "race_participants",
    primaryKeys = ["raceId", "cyclistId"],
    indices = [Index(value = ["season"])]
)
data class RaceParticipantEntity(
    val raceId: String,
    val cyclistId: String,
    val startListNumber: Int? = null, // Numero de dorsal (se disponivel)
    val status: String = ParticipantStatus.CONFIRMED.name, // CONFIRMED, DNS, DNF, DSQ
    val teamId: String? = null, // Equipa pro do ciclista
    val addedAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
)

/**
 * Status do participante na corrida
 */
enum class ParticipantStatus {
    CONFIRMED,  // Confirmado na startlist
    DNS,        // Did Not Start
    DNF,        // Did Not Finish
    DSQ         // Desqualificado
}
