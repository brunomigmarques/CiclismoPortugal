package com.ciclismo.portugal.data.remote.strava

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Strava OAuth authentication and API calls.
 * Handles token storage, refresh, and activity retrieval.
 */
@Singleton
class StravaRepository @Inject constructor(
    private val stravaApiService: StravaApiService,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        // Strava OAuth configuration
        const val CLIENT_ID = "95514"
        const val CLIENT_SECRET = "2da175b04f83748ab69b8afba2551b39718be925"
        // Using localhost redirect - Android intercepts this before browser loads
        const val REDIRECT_URI = "http://localhost/strava/callback"
        const val AUTHORIZATION_URL = "https://www.strava.com/oauth/mobile/authorize"

        // SharedPreferences keys
        private const val KEY_ACCESS_TOKEN = "strava_access_token"
        private const val KEY_REFRESH_TOKEN = "strava_refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "strava_token_expires_at"
        private const val KEY_ATHLETE_ID = "strava_athlete_id"
        private const val KEY_ATHLETE_NAME = "strava_athlete_name"
        private const val KEY_ATHLETE_PHOTO = "strava_athlete_photo"

        // Scopes needed for the app
        const val SCOPES = "read,activity:read"
    }

    /**
     * Check if user is connected to Strava
     */
    fun isConnected(): Boolean {
        val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        return !accessToken.isNullOrEmpty()
    }

    /**
     * Get connected athlete info
     */
    fun getConnectedAthlete(): StravaAthleteInfo? {
        if (!isConnected()) return null

        val id = sharedPreferences.getLong(KEY_ATHLETE_ID, 0L)
        val name = sharedPreferences.getString(KEY_ATHLETE_NAME, null) ?: return null
        val photo = sharedPreferences.getString(KEY_ATHLETE_PHOTO, null)

        return StravaAthleteInfo(id, name, photo)
    }

    /**
     * Build OAuth authorization URL for Strava login
     */
    fun buildAuthorizationUrl(): String {
        val encodedRedirectUri = java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8")
        return "$AUTHORIZATION_URL?" +
            "client_id=$CLIENT_ID&" +
            "redirect_uri=$encodedRedirectUri&" +
            "response_type=code&" +
            "approval_prompt=auto&" +
            "scope=$SCOPES"
    }

    /**
     * Exchange authorization code for access token
     */
    suspend fun exchangeCodeForToken(code: String): Result<StravaAthlete> = withContext(Dispatchers.IO) {
        try {
            val response = stravaApiService.exchangeToken(
                clientId = CLIENT_ID,
                clientSecret = CLIENT_SECRET,
                code = code
            )

            if (response.isSuccessful) {
                val tokenResponse = response.body()!!

                // Save tokens
                saveTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAt = tokenResponse.expiresAt
                )

                // Save athlete info
                tokenResponse.athlete?.let { athlete ->
                    saveAthleteInfo(athlete)
                }

                Result.success(tokenResponse.athlete!!)
            } else {
                Result.failure(Exception("Failed to exchange code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get valid access token, refreshing if needed
     */
    private suspend fun getValidAccessToken(): String? {
        val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = sharedPreferences.getLong(KEY_TOKEN_EXPIRES_AT, 0L)

        // Check if token is expired (with 5 minute buffer)
        val currentTime = System.currentTimeMillis() / 1000
        if (currentTime >= expiresAt - 300) {
            // Token expired or about to expire, refresh it
            val refreshed = refreshAccessToken()
            if (!refreshed) return null
            return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        }

        return accessToken
    }

    /**
     * Refresh expired access token
     */
    private suspend fun refreshAccessToken(): Boolean {
        val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null) ?: return false

        return try {
            val response = stravaApiService.refreshToken(
                clientId = CLIENT_ID,
                clientSecret = CLIENT_SECRET,
                refreshToken = refreshToken
            )

            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                saveTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAt = tokenResponse.expiresAt
                )
                true
            } else {
                // If refresh fails, user needs to re-authenticate
                disconnect()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get activities for a specific date range
     * @param after Unix timestamp - only return activities after this date
     * @param before Unix timestamp - only return activities before this date
     */
    suspend fun getActivities(after: Long? = null, before: Long? = null): Result<List<StravaActivity>> =
        withContext(Dispatchers.IO) {
            try {
                val accessToken = getValidAccessToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val response = stravaApiService.getActivities(
                    authorization = "Bearer $accessToken",
                    after = after,
                    before = before
                )

                if (response.isSuccessful) {
                    val activities = response.body() ?: emptyList()
                    // Filter only cycling activities
                    val cyclingActivities = activities.filter {
                        it.type in listOf("Ride", "VirtualRide", "GravelRide", "MountainBikeRide")
                    }
                    Result.success(cyclingActivities)
                } else {
                    Result.failure(Exception("Failed to get activities: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get activities for a specific date (race day)
     * @param date Unix timestamp of the race date (start of day)
     */
    suspend fun getActivitiesForDate(date: Long): Result<List<StravaActivity>> {
        // Get activities from start of day to end of day
        val startOfDay = date
        val endOfDay = date + (24 * 60 * 60) // Add 24 hours in seconds

        return getActivities(after = startOfDay, before = endOfDay)
    }

    /**
     * Get athlete stats
     */
    suspend fun getAthleteStats(): Result<StravaStats> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getValidAccessToken()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val athleteId = sharedPreferences.getLong(KEY_ATHLETE_ID, 0L)
            if (athleteId == 0L) {
                return@withContext Result.failure(Exception("No athlete ID"))
            }

            val response = stravaApiService.getAthleteStats(
                authorization = "Bearer $accessToken",
                athleteId = athleteId
            )

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect from Strava (logout)
     */
    fun disconnect() {
        sharedPreferences.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRES_AT)
            remove(KEY_ATHLETE_ID)
            remove(KEY_ATHLETE_NAME)
            remove(KEY_ATHLETE_PHOTO)
            apply()
        }
    }

    private fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            apply()
        }
    }

    private fun saveAthleteInfo(athlete: StravaAthlete) {
        val displayName = listOfNotNull(athlete.firstName, athlete.lastName)
            .joinToString(" ")
            .ifEmpty { athlete.username ?: "Athlete" }

        sharedPreferences.edit().apply {
            putLong(KEY_ATHLETE_ID, athlete.id)
            putString(KEY_ATHLETE_NAME, displayName)
            putString(KEY_ATHLETE_PHOTO, athlete.profileImageUrl)
            apply()
        }
    }
}

/**
 * Simple data class to hold connected athlete info
 */
data class StravaAthleteInfo(
    val id: Long,
    val name: String,
    val photoUrl: String?
)
