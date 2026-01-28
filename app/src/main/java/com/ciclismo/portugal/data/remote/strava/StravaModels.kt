package com.ciclismo.portugal.data.remote.strava

import com.google.gson.annotations.SerializedName

data class StravaTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_at")
    val expiresAt: Long,
    @SerializedName("athlete")
    val athlete: StravaAthlete? = null
)

data class StravaAthlete(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String?,
    @SerializedName("firstname")
    val firstName: String?,
    @SerializedName("lastname")
    val lastName: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("profile")
    val profileImageUrl: String?,
    @SerializedName("sex")
    val sex: String?
)

data class StravaActivity(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("distance")
    val distance: Float, // meters
    @SerializedName("moving_time")
    val movingTime: Int, // seconds
    @SerializedName("elapsed_time")
    val elapsedTime: Int, // seconds
    @SerializedName("total_elevation_gain")
    val elevationGain: Float, // meters
    @SerializedName("type")
    val type: String, // Ride, Run, etc
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("start_date_local")
    val startDateLocal: String,
    @SerializedName("average_speed")
    val averageSpeed: Float? = null, // meters/second
    @SerializedName("max_speed")
    val maxSpeed: Float? = null, // meters/second
    @SerializedName("average_heartrate")
    val averageHeartRate: Float? = null,
    @SerializedName("max_heartrate")
    val maxHeartRate: Float? = null
)

data class StravaStats(
    @SerializedName("recent_ride_totals")
    val recentRideTotals: StravaActivityTotals,
    @SerializedName("ytd_ride_totals")
    val ytdRideTotals: StravaActivityTotals,
    @SerializedName("all_ride_totals")
    val allRideTotals: StravaActivityTotals
)

data class StravaActivityTotals(
    @SerializedName("count")
    val count: Int,
    @SerializedName("distance")
    val distance: Float, // meters
    @SerializedName("moving_time")
    val movingTime: Int, // seconds
    @SerializedName("elapsed_time")
    val elapsedTime: Int, // seconds
    @SerializedName("elevation_gain")
    val elevationGain: Float // meters
)
