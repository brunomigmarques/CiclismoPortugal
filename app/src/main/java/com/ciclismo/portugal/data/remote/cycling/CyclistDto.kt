package com.ciclismo.portugal.data.remote.cycling

import com.ciclismo.portugal.data.local.entity.CyclistEntity
import com.ciclismo.portugal.domain.model.CyclistCategory

data class CyclistDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val teamId: String,
    val teamName: String,
    val nationality: String,
    val photoUrl: String?,
    val uciRanking: Int?,
    val points: Int,
    val age: Int?,
    val speciality: String?, // GC, Sprinter, Climber, etc.
    val profileUrl: String? = null // Link to ProCyclingStats profile
) {
    fun toEntity(): CyclistEntity {
        val category = when (speciality?.lowercase()) {
            "climber", "climbing", "mountains" -> CyclistCategory.CLIMBER
            "hills", "puncheur", "puncher" -> CyclistCategory.HILLS
            "tt", "time trial", "timetrial", "itt" -> CyclistCategory.TT
            "sprint", "sprinter" -> CyclistCategory.SPRINT
            "gc", "general classification", "stage races" -> CyclistCategory.GC
            "oneday", "one day", "classics", "classic", "one day races", "cobbles" -> CyclistCategory.ONEDAY
            else -> CyclistCategory.GC // Default to GC
        }

        // Calculate price based on UCI ranking and points
        val basePrice = when {
            uciRanking != null && uciRanking <= 10 -> 15.0
            uciRanking != null && uciRanking <= 25 -> 12.0
            uciRanking != null && uciRanking <= 50 -> 10.0
            uciRanking != null && uciRanking <= 100 -> 8.0
            uciRanking != null && uciRanking <= 200 -> 6.0
            points > 2000 -> 7.0
            points > 1000 -> 5.5
            points > 500 -> 4.5
            else -> 4.0
        }

        return CyclistEntity(
            id = id,
            firstName = firstName,
            lastName = lastName,
            teamId = teamId,
            teamName = teamName,
            nationality = nationality,
            photoUrl = photoUrl,
            category = category.name,
            price = basePrice,
            totalPoints = 0,
            form = 0.0,
            popularity = 0.0,
            syncedAt = System.currentTimeMillis(),
            age = age,
            uciRanking = uciRanking,
            speciality = speciality,
            profileUrl = profileUrl
        )
    }
}

data class RaceDto(
    val id: String,
    val name: String,
    val startDate: Long,
    val endDate: Long?,
    val country: String,
    val category: String, // WT, Pro, 1.1, etc.
    val stages: Int,
    val profileUrl: String?
)

data class RaceResultDto(
    val raceId: String,
    val cyclistId: String,
    val cyclistName: String,
    val position: Int,
    val stageNumber: Int?,
    val time: String?,
    val points: Int
)
