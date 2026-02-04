package com.ciclismo.portugal.data.remote.video

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scrapes YouTube search results to get actual video IDs for embedded playback.
 * Uses Jsoup to parse YouTube's search results page.
 */
@Singleton
class YouTubeScraper @Inject constructor() {

    companion object {
        private const val TAG = "YouTubeScraper"
        private const val YOUTUBE_SEARCH_URL = "https://www.youtube.com/results?search_query="
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val TIMEOUT_MS = 10000
    }

    /**
     * Search YouTube and return a list of videos with actual video IDs.
     *
     * @param query Search query
     * @param maxResults Maximum number of results to return
     * @return List of scraped videos with video IDs
     */
    suspend fun searchVideos(query: String, maxResults: Int = 5): List<ScrapedVideo> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$YOUTUBE_SEARCH_URL$encodedQuery"

            Log.d(TAG, "Searching YouTube: $query")

            val doc: Document = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get()

            // YouTube embeds video data in JSON within script tags
            val scripts = doc.select("script")
            val videos = mutableListOf<ScrapedVideo>()

            for (script in scripts) {
                val scriptContent = script.html()
                if (scriptContent.contains("var ytInitialData")) {
                    // Extract video IDs from ytInitialData
                    val videoIds = extractVideoIds(scriptContent, maxResults)
                    for (videoId in videoIds) {
                        videos.add(
                            ScrapedVideo(
                                videoId = videoId,
                                title = "",
                                channelName = "",
                                thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                                durationSeconds = 0
                            )
                        )
                    }
                    break
                }
            }

            // If we got video IDs, try to get more metadata
            if (videos.isNotEmpty()) {
                return@withContext enrichVideoMetadata(videos, doc)
            }

            // Fallback: try regex patterns on page content
            return@withContext extractVideosFromPage(doc, maxResults)

        } catch (e: Exception) {
            Log.e(TAG, "Error searching YouTube: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Extract video IDs from YouTube's ytInitialData JSON.
     */
    private fun extractVideoIds(scriptContent: String, maxResults: Int): List<String> {
        val videoIds = mutableListOf<String>()

        // Pattern to find video IDs in the JSON data
        val videoIdPattern = Regex("\"videoId\":\\s*\"([a-zA-Z0-9_-]{11})\"")
        val matches = videoIdPattern.findAll(scriptContent)

        val seenIds = mutableSetOf<String>()
        for (match in matches) {
            val videoId = match.groupValues[1]
            if (videoId !in seenIds) {
                seenIds.add(videoId)
                videoIds.add(videoId)
                if (videoIds.size >= maxResults) break
            }
        }

        return videoIds
    }

    /**
     * Try to extract video metadata from the page.
     */
    private fun enrichVideoMetadata(videos: List<ScrapedVideo>, doc: Document): List<ScrapedVideo> {
        val enriched = mutableListOf<ScrapedVideo>()
        val scripts = doc.select("script")

        var jsonData = ""
        for (script in scripts) {
            val content = script.html()
            if (content.contains("var ytInitialData")) {
                jsonData = content
                break
            }
        }

        for (video in videos) {
            // Try to find title and channel for this video ID
            val titlePattern = Regex("\"videoId\":\\s*\"${video.videoId}\"[^}]*\"title\":\\s*\\{[^}]*\"text\":\\s*\"([^\"]+)\"")
            val channelPattern = Regex("\"videoId\":\\s*\"${video.videoId}\"[^}]*\"ownerText\"[^}]*\"text\":\\s*\"([^\"]+)\"")

            val titleMatch = titlePattern.find(jsonData)
            val channelMatch = channelPattern.find(jsonData)

            enriched.add(
                video.copy(
                    title = titleMatch?.groupValues?.getOrNull(1) ?: "",
                    channelName = channelMatch?.groupValues?.getOrNull(1) ?: ""
                )
            )
        }

        return enriched
    }

    /**
     * Fallback extraction from page HTML.
     */
    private fun extractVideosFromPage(doc: Document, maxResults: Int): List<ScrapedVideo> {
        val videos = mutableListOf<ScrapedVideo>()

        // Try to find video links in the page
        val videoPattern = Regex("/watch\\?v=([a-zA-Z0-9_-]{11})")
        val pageContent = doc.html()
        val matches = videoPattern.findAll(pageContent)

        val seenIds = mutableSetOf<String>()
        for (match in matches) {
            val videoId = match.groupValues[1]
            if (videoId !in seenIds) {
                seenIds.add(videoId)
                videos.add(
                    ScrapedVideo(
                        videoId = videoId,
                        title = "",
                        channelName = "",
                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        durationSeconds = 0
                    )
                )
                if (videos.size >= maxResults) break
            }
        }

        return videos
    }
}

/**
 * Represents a video scraped from YouTube.
 */
data class ScrapedVideo(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationSeconds: Int
)
