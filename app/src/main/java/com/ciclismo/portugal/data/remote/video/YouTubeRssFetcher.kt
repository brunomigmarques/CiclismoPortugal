package com.ciclismo.portugal.data.remote.video

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches videos from YouTube channel RSS feeds.
 * RSS feeds provide actual video IDs that can be embedded.
 */
@Singleton
class YouTubeRssFetcher @Inject constructor() {

    companion object {
        private const val TAG = "YouTubeRssFetcher"
        private const val TIMEOUT_MS = 10000

        // YouTube channel IDs for cycling content (verified working)
        val CYCLING_CHANNELS = listOf(
            ChannelInfo("UCuTaETsuCOkJ0H_GAztWt0Q", "GCN Racing", "🚴"),           // GCN Racing - WORKS
            ChannelInfo("UCN9Nj4tjXbVTLYWN0EKly_Q", "GCN em Português", "🇵🇹"),   // GCN em Português - WORKS
            ChannelInfo("UCcADwedDGRjYgpSufpIPqfg", "GCN Show", "🚴"),             // GCN Show (main channel)
            ChannelInfo("UC3m6V_5yNRAEDAXbLX0hJFQ", "SRAM", "🔧"),                 // SRAM cycling tech
            ChannelInfo("UC6TJhRqR8SRzswE6XUw4rVw", "Dylan Johnson", "🚴"),        // Dylan Johnson cycling
            ChannelInfo("UCt3otc2YCmVBQirrxq7lxnw", "CyclingTips", "🚴"),          // CyclingTips
            ChannelInfo("UCPutRKXNnvGHBk2eLmx6fkA", "Wahoo", "🔧"),                // Wahoo Fitness
            ChannelInfo("UCO_PY7PK5XNqvJnVKQKLOzA", "NorCal Cycling", "🚴")        // NorCal Cycling
        )
    }

    /**
     * Fetch latest videos from cycling YouTube channels via RSS.
     * Returns videos with real video IDs that can be embedded.
     */
    suspend fun fetchLatestVideos(maxVideosPerChannel: Int = 3): List<RssVideo> = withContext(Dispatchers.IO) {
        val allVideos = mutableListOf<RssVideo>()

        for (channel in CYCLING_CHANNELS) {
            try {
                val videos = fetchChannelVideos(channel, maxVideosPerChannel)
                allVideos.addAll(videos)
                Log.d(TAG, "Fetched ${videos.size} videos from ${channel.name}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch from ${channel.name}: ${e.message}")
                // Continue to next channel
            }
        }

        // Sort by publish date (most recent first)
        return@withContext allVideos.sortedByDescending { it.publishedAt }
    }

    /**
     * Fetch videos from a specific channel's RSS feed.
     */
    private suspend fun fetchChannelVideos(channel: ChannelInfo, maxVideos: Int): List<RssVideo> {
        val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=${channel.channelId}"

        val doc = Jsoup.connect(rssUrl)
            .timeout(TIMEOUT_MS)
            .ignoreContentType(true)
            .get()

        val entries = doc.select("entry")
        val videos = mutableListOf<RssVideo>()

        for (entry in entries.take(maxVideos)) {
            try {
                val videoId = entry.select("yt|videoId").text()
                    .ifBlank { entry.select("id").text().substringAfterLast(":") }

                if (videoId.length == 11) { // Valid YouTube video ID
                    val title = entry.select("title").text()
                    val publishedText = entry.select("published").text()
                    val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    val publishedAt = try {
                        java.time.Instant.parse(publishedText).toEpochMilli()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    videos.add(
                        RssVideo(
                            videoId = videoId,
                            title = title,
                            channelName = channel.name,
                            channelEmoji = channel.emoji,
                            thumbnailUrl = thumbnailUrl,
                            publishedAt = publishedAt
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse entry: ${e.message}")
            }
        }

        return videos
    }

    /**
     * Search for videos related to a specific topic from RSS feeds.
     */
    suspend fun searchVideos(query: String, maxResults: Int = 5): List<RssVideo> = withContext(Dispatchers.IO) {
        val allVideos = fetchLatestVideos(5)
        val queryLower = query.lowercase()
        val keywords = queryLower.split(" ", "+").filter { it.length > 2 }

        // Filter videos that match any keyword in the query
        return@withContext allVideos
            .filter { video ->
                val titleLower = video.title.lowercase()
                keywords.any { keyword -> titleLower.contains(keyword) }
            }
            .take(maxResults)
    }
}

data class ChannelInfo(
    val channelId: String,
    val name: String,
    val emoji: String
)

data class RssVideo(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelEmoji: String,
    val thumbnailUrl: String,
    val publishedAt: Long
)
