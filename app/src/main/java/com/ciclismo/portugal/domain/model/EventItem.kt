package com.ciclismo.portugal.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Unified event item that can represent either a local Prova or a WorldTour Race
 * Used in the main events list (HomeScreen)
 */
sealed class EventItem : Comparable<EventItem> {
    abstract val id: String
    abstract val name: String
    abstract val date: Long
    abstract val endDate: Long?
    abstract val location: String
    abstract val isWorldTour: Boolean

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM", Locale("pt", "PT"))
            return sdf.format(Date(date))
        }

    val formattedFullDate: String
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

    val dayOfMonth: Int
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = date
            return cal.get(Calendar.DAY_OF_MONTH)
        }

    val monthYear: String
        get() {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT"))
            return sdf.format(Date(date)).replaceFirstChar { it.uppercase() }
        }

    val monthYearKey: String
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = date
            return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
        }

    override fun compareTo(other: EventItem): Int = this.date.compareTo(other.date)

    /**
     * Event item for local Portuguese provas (cycling events)
     */
    data class ProvaEvent(
        val prova: Prova
    ) : EventItem() {
        override val id: String = "prova_${prova.id}"
        override val name: String = prova.nome
        override val date: Long = prova.data
        override val endDate: Long? = null
        override val location: String = prova.local
        override val isWorldTour: Boolean = false

        val tipo: String = prova.tipo
        val local: String = prova.local
        val isInCalendar: Boolean = prova.inCalendar
        val originalProva: Prova = prova
    }

    /**
     * Event item for WorldTour races (Fantasy)
     */
    data class RaceEvent(
        val race: Race
    ) : EventItem() {
        override val id: String = "race_${race.id}"
        override val name: String = race.name
        override val date: Long = race.startDate
        override val endDate: Long? = race.endDate
        override val location: String = race.country
        override val isWorldTour: Boolean = true

        val raceType: RaceType = race.type
        val category: String = race.category
        val stages: Int = race.stages
        val isActive: Boolean = race.isActive
        val isGrandTour: Boolean = race.isGrandTour
        val originalRace: Race = race
    }
}

/**
 * Represents a month header in the list
 */
data class MonthHeader(
    val monthYear: String,
    val monthYearKey: String,
    val eventCount: Int
)

/**
 * Extension to convert Prova to EventItem
 */
fun Prova.toEventItem(): EventItem.ProvaEvent = EventItem.ProvaEvent(this)

/**
 * Extension to convert Race to EventItem
 */
fun Race.toEventItem(): EventItem.RaceEvent = EventItem.RaceEvent(this)

/**
 * Group events by month and create a list with headers
 */
fun List<EventItem>.groupByMonth(): List<Any> {
    val result = mutableListOf<Any>()
    val grouped = this.sortedBy { it.date }.groupBy { it.monthYearKey }

    grouped.forEach { (key, events) ->
        val firstEvent = events.first()
        result.add(MonthHeader(
            monthYear = firstEvent.monthYear,
            monthYearKey = key,
            eventCount = events.size
        ))
        result.addAll(events)
    }

    return result
}
