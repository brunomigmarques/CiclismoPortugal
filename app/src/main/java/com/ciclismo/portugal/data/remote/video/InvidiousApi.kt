package com.ciclismo.portugal.data.remote.video

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses Piped API to search for YouTube videos.
 * Piped is an open-source YouTube frontend that provides a REST API
 * returning JSON with video IDs, titles, thumbnails, etc.
 *
 * More stable than Invidious for API access.
 */
@Singleton
class InvidiousApi @Inject constructor() {

    companion object {
        private const val TAG = "InvidiousApi"
        private const val TIMEOUT_MS = 10000

        // Combined list of Invidious and Piped instances
        // More instances = higher chance of success
        private val API_INSTANCES = listOf(
            // Invidious instances (API: /api/v1/search)
            "https://yewtu.be",
            "https://inv.riverside.rocks",
            "https://invidious.snopyta.org",
            "https://vid.puffyan.us",
            "https://invidious.kavin.rocks",
            "https://inv.bp.projectsegfau.lt",
            "https://invidious.flokinet.to",
            "https://invidious.projectsegfau.lt",
            "https://invidious.slipfox.xyz",
            "https://invidious.osi.kr",
            // Piped instances (API: /search)
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.tokhmi.xyz",
            "https://pipedapi.moomoo.me",
            "https://pipedapi.syncpundit.io",
            "https://api-piped.mha.fi"
        )
    }

    /**
     * Search for videos using Invidious/Piped APIs.
     * Returns actual video IDs that can be embedded.
     * Tries multiple instances until one works.
     */
    suspend fun searchVideos(query: String, maxResults: Int = 5): List<InvidiousVideo> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        // Shuffle instances to distribute load
        val shuffledInstances = API_INSTANCES.shuffled()

        for (instance in shuffledInstances) {
            try {
                val videos = if (instance.contains("piped")) {
                    searchWithPiped(instance, encodedQuery, maxResults)
                } else {
                    searchWithInvidious(instance, encodedQuery, maxResults)
                }

                if (videos.isNotEmpty()) {
                    Log.d(TAG, "Found ${videos.size} videos from $instance for query: $query")
                    return@withContext videos
                }
            } catch (e: Exception) {
                Log.w(TAG, "Instance $instance failed: ${e.message}")
            }
        }

        Log.w(TAG, "All instances failed for query: $query")
        return@withContext emptyList()
    }

    /**
     * Search using Invidious API.
     */
    private fun searchWithInvidious(instance: String, encodedQuery: String, maxResults: Int): List<InvidiousVideo> {
        val apiUrl = "$instance/api/v1/search?q=$encodedQuery&type=video"

        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")

        try {
            if (connection.responseCode != 200) {
                throw Exception("HTTP ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parseInvidiousResults(response, maxResults)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parse Invidious API search results.
     */
    private fun parseInvidiousResults(jsonResponse: String, maxResults: Int): List<InvidiousVideo> {
        val videos = mutableListOf<InvidiousVideo>()

        try {
            val jsonArray = JSONArray(jsonResponse)

            for (i in 0 until minOf(jsonArray.length(), maxResults)) {
                try {
                    val item = jsonArray.getJSONObject(i)
                    if (item.optString("type") != "video") continue

                    val videoId = item.getString("videoId")
                    if (videoId.length != 11) continue

                    videos.add(
                        InvidiousVideo(
                            videoId = videoId,
                            title = item.optString("title", ""),
                            channelName = item.optString("author", ""),
                            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                            durationSeconds = item.optInt("lengthSeconds", 0),
                            viewCount = item.optLong("viewCount", 0),
                            publishedText = item.optString("publishedText", "")
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse Invidious item: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Invidious results: ${e.message}")
        }

        return videos
    }

    /**
     * Search using Piped API.
     */
    private fun searchWithPiped(instance: String, encodedQuery: String, maxResults: Int): List<InvidiousVideo> {
        val apiUrl = "$instance/search?q=$encodedQuery&filter=videos"

        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")

        try {
            if (connection.responseCode != 200) {
                throw Exception("HTTP ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parsePipedResults(response, maxResults)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parse Piped API search results.
     */
    private fun parsePipedResults(jsonResponse: String, maxResults: Int): List<InvidiousVideo> {
        val videos = mutableListOf<InvidiousVideo>()

        try {
            val json = JSONObject(jsonResponse)
            val items = json.optJSONArray("items") ?: return emptyList()

            for (i in 0 until minOf(items.length(), maxResults)) {
                try {
                    val item = items.getJSONObject(i)
                    val url = item.optString("url", "")
                    val videoId = url.substringAfter("v=").take(11)
                    if (videoId.length != 11) continue

                    val thumbnail = item.optString("thumbnail", "")

                    videos.add(
                        InvidiousVideo(
                            videoId = videoId,
                            title = item.optString("title", ""),
                            channelName = item.optString("uploaderName", ""),
                            thumbnailUrl = if (thumbnail.isNotBlank()) thumbnail
                                else "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                            durationSeconds = item.optInt("duration", 0),
                            viewCount = item.optLong("views", 0),
                            publishedText = ""
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse Piped item: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Piped results: ${e.message}")
        }

        return videos
    }

    /**
     * Get video details by ID using Piped.
     */
    suspend fun getVideoDetails(videoId: String): InvidiousVideo? = withContext(Dispatchers.IO) {
        for (instance in API_INSTANCES.filter { it.contains("piped") }) {
            try {
                val apiUrl = "$instance/streams/$videoId"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                try {
                    if (connection.responseCode != 200) continue

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)

                    return@withContext InvidiousVideo(
                        videoId = videoId,
                        title = json.optString("title", ""),
                        channelName = json.optString("uploader", ""),
                        thumbnailUrl = json.optString("thumbnailUrl", "https://img.youtube.com/vi/$videoId/hqdefault.jpg"),
                        durationSeconds = json.optInt("duration", 0),
                        viewCount = json.optLong("views", 0),
                        publishedText = ""
                    )
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped instance $instance failed for video $videoId: ${e.message}")
            }
        }

        return@withContext null
    }
}

/**
 * Video data from Invidious API.
 */
data class InvidiousVideo(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val viewCount: Long = 0,
    val publishedText: String = ""
)
