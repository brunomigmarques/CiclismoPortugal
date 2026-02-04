package com.ciclismo.portugal.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Race(
    val id: String,
    val name: String,
    val type: RaceType,
    val startDate: Long,
    val endDate: Long?,
    val stages: Int = 1,
    val country: String,
    val category: String,
    val isActive: Boolean = false,
    val isFinished: Boolean = false,
    val finishedAt: Long? = null,
    val profileUrl: String? = null,
    val imageUrl: String? = null,
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    val formattedStartDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT"))
            return sdf.format(Date(startDate))
        }

    val formattedDateRange: String
        get() {
            val sdf = SimpleDateFormat("dd MMM", Locale("pt", "PT"))
            return if (endDate != null && endDate != startDate) {
                "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}"
            } else {
                formattedStartDate
            }
        }

    val isGrandTour: Boolean
        get() = type == RaceType.GRAND_TOUR

    val isOneDay: Boolean
        get() = type == RaceType.ONE_DAY

    val displayDate: String
        get() {
            // Calculate days using calendar days, not raw milliseconds
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val raceCal = Calendar.getInstance().apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = raceCal.timeInMillis - todayCal.timeInMillis
            val daysUntil = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            return when {
                daysUntil < 0 -> "Em curso"
                daysUntil == 0 -> "Hoje"
                daysUntil == 1 -> "Amanha"
                daysUntil < 7 -> "Em $daysUntil dias"
                else -> formattedStartDate
            }
        }
}

enum class RaceType {
    ONE_DAY,      // Corridas de um dia (Classicas)
    GRAND_TOUR,   // Giro, Tour, Vuelta
    STAGE_RACE    // Outras corridas por etapas
}
