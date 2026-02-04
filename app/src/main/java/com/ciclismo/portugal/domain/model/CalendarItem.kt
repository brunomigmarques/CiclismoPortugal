package com.ciclismo.portugal.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified calendar item that can represent either a local Prova or a Fantasy Race
 */
sealed class CalendarItem {
    abstract val id: String
    abstract val name: String
    abstract val date: Long
    abstract val endDate: Long?
    abstract val location: String
    abstract val isFantasy: Boolean

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT"))
            return sdf.format(Date(date))
        }

    val formattedDateRange: String
        get() {
            val sdf = SimpleDateFormat("dd MMM", Locale("pt", "PT"))
            return if (endDate != null && endDate != date) {
                "${sdf.format(Date(date))} - ${sdf.format(Date(endDate!!))}"
            } else {
                formattedDate
            }
        }

    /**
     * Calendar item for local Portuguese provas (cycling events)
     */
    data class ProvaItem(
        val prova: Prova
    ) : CalendarItem() {
        override val id: String = "prova_${prova.id}"
        override val name: String = prova.nome
        override val date: Long = prova.data
        override val endDate: Long? = null
        override val location: String = prova.local
        override val isFantasy: Boolean = false

        val tipo: String = prova.tipo
        val reminderDays: Int = prova.reminderDays
    }

    /**
     * Calendar item for Fantasy races (WorldTour)
     */
    data class RaceItem(
        val race: Race
    ) : CalendarItem() {
        override val id: String = "race_${race.id}"
        override val name: String = race.name
        override val date: Long = race.startDate
        override val endDate: Long? = race.endDate
        override val location: String = race.country
        override val isFantasy: Boolean = true

        val raceType: RaceType = race.type
        val category: String = race.category
        val stages: Int = race.stages
        val isActive: Boolean = race.isActive
        val isGrandTour: Boolean = race.isGrandTour
        val imageUrl: String? = race.imageUrl ?: RaceImageMapper.getImageUrl(race.name, race.id)
    }
}

/**
 * Extension to convert Prova to CalendarItem
 */
fun Prova.toCalendarItem(): CalendarItem.ProvaItem = CalendarItem.ProvaItem(this)

/**
 * Extension to convert Race to CalendarItem
 */
fun Race.toCalendarItem(): CalendarItem.RaceItem = CalendarItem.RaceItem(this)
