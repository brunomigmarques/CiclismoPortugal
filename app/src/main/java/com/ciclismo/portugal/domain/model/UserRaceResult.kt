package com.ciclismo.portugal.domain.model

import java.util.UUID

/**
 * Represents a user's personal race result from local/amateur events.
 * Different from RaceResult which is used for Fantasy (pro cyclists).
 */
data class UserRaceResult(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val raceName: String,
    val raceDate: Long,
    val raceLocation: String,
    val raceType: UserRaceType,
    val distance: Float?, // in km
    val elevation: Int?, // in meters
    val bibNumber: Int?,
    val position: Int?,
    val totalParticipants: Int?,
    val categoryPosition: Int?,
    val categoryTotalParticipants: Int?,
    val category: String?, // e.g., "M40", "ELITE", "GRAVEL"
    val finishTime: String?, // "HH:MM:SS" format
    val avgSpeed: Float?, // km/h
    val resultSource: ResultSource,
    val sourceUrl: String?, // URL to results page
    val eventUrl: String?, // URL to event page
    val organizerName: String?,
    val notes: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val positionDisplay: String
        get() = when {
            position != null -> "${position}º"
            else -> "-"
        }

    val categoryPositionDisplay: String
        get() = when {
            categoryPosition != null && categoryTotalParticipants != null ->
                "${categoryPosition}º de $categoryTotalParticipants"
            categoryPosition != null -> "${categoryPosition}º"
            else -> "-"
        }

    val overallPositionDisplay: String
        get() = when {
            position != null && totalParticipants != null ->
                "${position}º de $totalParticipants"
            position != null -> positionDisplay
            else -> "-"
        }

    val distanceDisplay: String
        get() = distance?.let { "${it}km" } ?: "-"

    val elevationDisplay: String
        get() = elevation?.let { "${it}m D+" } ?: "-"
}

enum class UserRaceType(val displayName: String) {
    GRAVEL("Gravel"),
    BTT("BTT"),
    ROAD("Estrada"),
    CICLOTURISMO("Cicloturismo"),
    TRAIL("Trail Running"),
    TRIATHLON("Triatlo"),
    OTHER("Outro")
}

enum class ResultSource(val displayName: String) {
    STOP_AND_GO("Stop and Go"),
    CRONO_RACE("CronoRace"),
    RACE_FINDER("RaceFinder"),
    MANUAL("Manual"),
    OTHER("Outro")
}
