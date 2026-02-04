package com.ciclismo.portugal.data.remote.video

import android.util.Log
import com.ciclismo.portugal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube Data API v3 client for searching videos.
 * This is the official Google API - reliable and fast.
 *
 * Free tier: 10,000 units/day
 * Search costs 100 units per request
 * = ~100 searches per day
 */
@Singleton
class YouTubeDataApi @Inject constructor() {

    companion object {
        private const val TAG = "YouTubeDataApi"
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3"
        private const val TIMEOUT_MS = 10000
    }

    private val apiKey: String get() = BuildConfig.YOUTUBE_API_KEY

    /**
     * Check if API key is configured.
     */
    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "YOUR_API_KEY_HERE"
    }

    /**
     * Search for videos on YouTube.
     *
     * @param query Search query
     * @param maxResults Maximum number of results (1-50)
     * @return List of videos with real video IDs
     */
    suspend fun searchVideos(query: String, maxResults: Int = 5): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.w(TAG, "YouTube API key not configured")
            return@withContext emptyList()
        }

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val apiUrl = "$BASE_URL/search?part=snippet&type=video&q=$encodedQuery&maxResults=$maxResults&key=$apiKey"

            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            try {
                if (connection.responseCode != 200) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e(TAG, "API error ${connection.responseCode}: $error")
                    return@withContext emptyList()
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext parseSearchResults(response)

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Parse YouTube Data API search results.
     */
    private fun parseSearchResults(jsonResponse: String): List<YouTubeVideo> {
        val videos = mutableListOf<YouTubeVideo>()

        try {
            val json = JSONObject(jsonResponse)
            val items = json.optJSONArray("items") ?: return emptyList()

            for (i in 0 until items.length()) {
                try {
                    val item = items.getJSONObject(i)
                    val id = item.getJSONObject("id")
                    val snippet = item.getJSONObject("snippet")

                    val videoId = id.optString("videoId", "")
                    if (videoId.length != 11) continue

                    val title = snippet.optString("title", "")
                    val channelTitle = snippet.optString("channelTitle", "")
                    val description = snippet.optString("description", "")
                    val publishedAt = snippet.optString("publishedAt", "")

                    // Get best thumbnail
                    val thumbnails = snippet.optJSONObject("thumbnails")
                    val thumbnailUrl = thumbnails?.optJSONObject("high")?.optString("url")
                        ?: thumbnails?.optJSONObject("medium")?.optString("url")
                        ?: thumbnails?.optJSONObject("default")?.optString("url")
                        ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    videos.add(
                        YouTubeVideo(
                            videoId = videoId,
                            title = title,
                            channelName = channelTitle,
                            description = description,
                            thumbnailUrl = thumbnailUrl,
                            publishedAt = publishedAt
                        )
                    )

                    Log.d(TAG, "Found video: $title ($videoId)")

                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse video item: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse search results: ${e.message}")
        }

        return videos
    }

    /**
     * Get video details by ID.
     */
    suspend fun getVideoDetails(videoId: String): YouTubeVideo? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null

        try {
            val apiUrl = "$BASE_URL/videos?part=snippet,contentDetails&id=$videoId&key=$apiKey"

            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            try {
                if (connection.responseCode != 200) return@withContext null

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val items = json.optJSONArray("items")

                if (items == null || items.length() == 0) return@withContext null

                val item = items.getJSONObject(0)
                val snippet = item.getJSONObject("snippet")
                val contentDetails = item.optJSONObject("contentDetails")

                val thumbnails = snippet.optJSONObject("thumbnails")
                val thumbnailUrl = thumbnails?.optJSONObject("high")?.optString("url")
                    ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                return@withContext YouTubeVideo(
                    videoId = videoId,
                    title = snippet.optString("title", ""),
                    channelName = snippet.optString("channelTitle", ""),
                    description = snippet.optString("description", ""),
                    thumbnailUrl = thumbnailUrl,
                    publishedAt = snippet.optString("publishedAt", ""),
                    duration = contentDetails?.optString("duration", "") ?: ""
                )

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Get video details failed: ${e.message}")
            return@withContext null
        }
    }
}

/**
 * Video data from YouTube Data API.
 */
data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val channelName: String,
    val description: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val duration: String = ""
)
