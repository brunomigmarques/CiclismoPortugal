package com.ciclismo.portugal.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Transfer(
    val id: String,
    val teamId: String,
    val cyclistInId: String,
    val cyclistOutId: String,
    val cyclistInName: String,
    val cyclistOutName: String,
    val priceIn: Double,
    val priceOut: Double,
    val gameweek: Int,
    val pointsCost: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    val isPaid: Boolean
        get() = pointsCost > 0

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("pt", "PT"))
            return sdf.format(Date(timestamp))
        }

    val netCost: Double
        get() = priceIn - priceOut
}
