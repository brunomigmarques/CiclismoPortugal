package com.ciclismo.portugal.data.remote.facebook

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facebook Graph API service for fetching Reels from cycling pages.
 *
 * REQUIREMENTS FOR PRODUCTION:
 * 1. Create a Facebook App at https://developers.facebook.com/
 * 2. Request App Review for permissions:
 *    - pages_read_engagement
 *    - pages_show_list
 * 3. Get a Page Access Token with long-lived token
 * 4. Store the token securely (e.g., Firebase Remote Config or encrypted SharedPreferences)
 *
 * API Endpoint: GET /{page-id}/video_reels
 * Fields: id, description, permalink_url, picture, created_time, length
 */
@Singleton
class FacebookGraphService @Inject constructor() {

    companion object {
        private const val TAG = "FacebookGraphService"
        private const val GRAPH_API_VERSION = "v19.0"
        private const val GRAPH_API_BASE = "https://graph.facebook.com/$GRAPH_API_VERSION"

        // Cycling pages to fetch reels from
        // Using page slugs (usernames) instead of numeric IDs - API accepts both
        val CYCLING_PAGES = listOf(
            // Portuguese Cycling
            FacebookPage("voltaportugal", "Volta a Portugal", "voltaportugal"),
            FacebookPage("fpciclismo", "FPC Ciclismo", "fpciclismo"),

            // International - WorldTour
            FacebookPage("letour", "Tour de France", "letour"),
            FacebookPage("gaborone", "Giro d'Italia", "gaborone"),
            FacebookPage("lavuelta", "La Vuelta", "lavuelta"),
            FacebookPage("UnionCyclisteInternationale", "UCI", "UnionCyclisteInternationale"),

            // Cycling Media
            FacebookPage("globalcyclingnetwork", "GCN", "globalcyclingnetwork"),
            FacebookPage("redbullbike", "Red Bull Bike", "redbullbike"),
            FacebookPage("cyclingnews", "Cyclingnews", "cyclingnews"),
        )
    }

    // Access token should be stored securely and refreshed periodically
    // For now, this is a placeholder - in production, fetch from secure storage
    private var accessToken: String? = null

    /**
     * Set the Facebook Page Access Token.
     * This should be called after retrieving the token from secure storage.
     */
    fun setAccessToken(token: String) {
        accessToken = token
    }

    /**
     * Check if the service is configured with an access token.
     */
    fun isConfigured(): Boolean = !accessToken.isNullOrBlank()

    /**
     * Fetch reels from all configured cycling pages.
     */
    suspend fun fetchAllReels(limit: Int = 10): Result<List<FacebookReel>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.w(TAG, "Facebook Graph API not configured - no access token")
            return@withContext Result.success(emptyList())
        }

        try {
            val allReels = mutableListOf<FacebookReel>()

            for (page in CYCLING_PAGES) {
                if (page.pageId.isBlank()) continue

                val result = fetchReelsFromPage(page, limit)
                result.getOrNull()?.let { reels ->
                    allReels.addAll(reels)
                }
            }

            // Sort by created time (newest first)
            val sortedReels = allReels.sortedByDescending { it.createdTime }

            Log.d(TAG, "Fetched ${sortedReels.size} reels from ${CYCLING_PAGES.size} pages")
            Result.success(sortedReels)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching reels: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch reels from a specific Facebook page.
     * Falls back to videos endpoint if reels endpoint fails.
     */
    suspend fun fetchReelsFromPage(page: FacebookPage, limit: Int = 10): Result<List<FacebookReel>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.success(emptyList())
        }

        // Try video_reels endpoint first, then fall back to videos
        val endpoints = listOf(
            "video_reels" to "id,description,permalink_url,picture,created_time,length",
            "videos" to "id,description,permalink_url,picture,created_time,length,source"
        )

        for ((endpoint, fields) in endpoints) {
            try {
                val url = "$GRAPH_API_BASE/${page.pageId}/$endpoint?fields=$fields&limit=$limit&access_token=$accessToken"

                Log.d(TAG, "Fetching from ${page.name} via /$endpoint")

                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val reels = mutableListOf<FacebookReel>()
                val dataArray = json.optJSONArray("data") ?: continue

                for (i in 0 until dataArray.length()) {
                    val reelJson = dataArray.getJSONObject(i)

                    val reel = FacebookReel(
                        id = reelJson.optString("id"),
                        description = reelJson.optString("description", ""),
                        permalinkUrl = reelJson.optString("permalink_url"),
                        thumbnailUrl = reelJson.optString("picture"),
                        createdTime = reelJson.optString("created_time"),
                        length = reelJson.optDouble("length", 0.0),
                        pageName = page.name,
                        pageId = page.pageId,
                        videoUrl = reelJson.optString("source", "")
                    )

                    if (reel.permalinkUrl.isNotBlank()) {
                        reels.add(reel)
                    }
                }

                if (reels.isNotEmpty()) {
                    Log.d(TAG, "Fetched ${reels.size} videos from ${page.name} via /$endpoint")
                    return@withContext Result.success(reels)
                }

            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch from ${page.name} via /$endpoint: ${e.message}")
                // Continue to next endpoint
            }
        }

        Log.e(TAG, "All endpoints failed for ${page.name}")
        Result.success(emptyList())
    }
}

/**
 * Represents a Facebook Page to fetch reels from.
 */
data class FacebookPage(
    val slug: String,
    val name: String,
    val pageId: String // Facebook numeric page ID
)

/**
 * Represents a Facebook Reel or Video.
 */
data class FacebookReel(
    val id: String,
    val description: String,
    val permalinkUrl: String,
    val thumbnailUrl: String,
    val createdTime: String,
    val length: Double,
    val pageName: String,
    val pageId: String,
    val videoUrl: String = "" // Direct video URL (if available)
) {
    val durationSeconds: Int
        get() = length.toInt()

    val displayDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        }

    val hasDirectVideo: Boolean
        get() = videoUrl.isNotBlank()
}
