package com.ciclismo.portugal.data.remote.video

import android.util.Log
import com.ciclismo.portugal.data.local.dao.VideoDao
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.local.entity.toEntity
import com.ciclismo.portugal.data.remote.firebase.VideoFirestoreService
import com.ciclismo.portugal.domain.model.NewsArticle
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.repository.NewsRepository
import com.ciclismo.portugal.domain.repository.ProvaRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for cycling video content.
 *
 * Architecture:
 * 1. Load from FIRESTORE first (shared cache for all users)
 * 2. If Firestore empty, fallback to local DB cache
 * 3. VideoSyncWorker auto-updates Firestore every 6 hours in background
 * 4. All video IDs are fetched from YouTube API based on races/news in database
 *
 * This ensures all users get videos instantly from Firestore without API quota issues.
 */
@Singleton
class CyclingVideosRepository @Inject constructor(
    private val raceRepository: RaceRepository,
    private val provaRepository: ProvaRepository,
    private val newsRepository: NewsRepository,
    private val youtubeScraper: YouTubeScraper,
    private val youtubeRssFetcher: YouTubeRssFetcher,
    private val invidiousApi: InvidiousApi,
    private val youtubeDataApi: YouTubeDataApi,
    private val videoFirestoreService: VideoFirestoreService,
    private val videoDao: VideoDao
) {
    companion object {
        private const val TAG = "CyclingVideosRepo"
        private const val MAX_RACE_VIDEOS = 9      // Max 9 videos from races (3 races x 3 videos)
        private const val MAX_NEWS_VIDEOS = 6      // Max 6 videos from news
        private const val VIDEOS_PER_RACE = 3      // Up to 3 videos per race
        private const val MAX_RACES = 3            // 3 next races
    }

    private val _videos = MutableStateFlow<List<CyclingVideo>>(emptyList())
    val videos: Flow<List<CyclingVideo>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    // Cache to avoid repeated API calls
    private var cachedVideos: List<CyclingVideo>? = null
    private var lastFetchTime: Long = 0
    private val cacheValidityMs = 15 * 60 * 1000L // 15 minutes memory cache

    /**
     * Get videos for display.
     *
     * Priority:
     * 1. FIRESTORE first (shared cache, updated every 6 hours by WorkManager)
     * 2. Local DB cache as fallback for offline
     * 3. Curated videos as last resort
     */
    suspend fun getVideosForCurrentContent(): List<CyclingVideo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Return in-memory cached if valid (15 minutes)
        if (cachedVideos != null && (now - lastFetchTime) < cacheValidityMs) {
            Log.d(TAG, "Returning ${cachedVideos!!.size} videos from memory cache")
            return@withContext cachedVideos!!
        }

        _isLoading.value = true

        try {
            // 2. Load from FIRESTORE first (shared cache for all users)
            Log.d(TAG, "Loading videos from Firestore (shared cache)...")
            val firestoreVideos = try {
                videoFirestoreService.getVideosOnce()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore read failed: ${e.message}")
                emptyList()
            }

            val hasValidFirestoreCache = firestoreVideos.isNotEmpty() &&
                firestoreVideos.any { !it.id.startsWith("search_") }

            if (hasValidFirestoreCache) {
                Log.d(TAG, "Got ${firestoreVideos.size} videos from Firestore")
                cachedVideos = firestoreVideos
                lastFetchTime = now
                _videos.value = firestoreVideos

                // Save to local DB for offline access
                try {
                    videoDao.replaceAll(firestoreVideos.map { it.toEntity() })
                    Log.d(TAG, "Saved Firestore videos to local cache")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save to local cache: ${e.message}")
                }

                return@withContext firestoreVideos
            }

            // 3. Firestore empty/failed - try local DB cache
            Log.d(TAG, "Firestore empty, trying local DB cache...")
            val localCachedVideos = try {
                videoDao.getAllVideosSync().map { it.toDomain() }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load from local cache: ${e.message}")
                emptyList()
            }

            val hasLocalCache = localCachedVideos.isNotEmpty() &&
                localCachedVideos.any { !it.id.startsWith("search_") }

            if (hasLocalCache) {
                Log.d(TAG, "Returning ${localCachedVideos.size} videos from local DB cache")
                cachedVideos = localCachedVideos
                lastFetchTime = now
                _videos.value = localCachedVideos
                return@withContext localCachedVideos
            }

            // 4. No cache anywhere - fetch fresh and seed Firestore
            Log.d(TAG, "No cache available, fetching fresh videos to seed Firestore...")
            val freshVideos = fetchVideosFromApi()

            if (freshVideos.isNotEmpty()) {
                cachedVideos = freshVideos
                lastFetchTime = now
                _videos.value = freshVideos

                // Save to both local and Firestore
                try {
                    videoDao.replaceAll(freshVideos.map { it.toEntity() })
                    videoFirestoreService.uploadVideos(freshVideos)
                    Log.d(TAG, "Seeded ${freshVideos.size} videos to Firestore and local cache")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to seed caches: ${e.message}")
                }

                return@withContext freshVideos
            }

            // 5. Everything failed - use curated fallback
            Log.w(TAG, "No videos from any source, using curated fallback")
            return@withContext getCuratedVideos()

        } catch (e: Exception) {
            Log.e(TAG, "Error getting videos: ${e.message}")
            return@withContext getCuratedVideos()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Sync videos to Firestore (called by VideoSyncWorker every 6 hours).
     * Fetches fresh videos from YouTube API based on calendar and news,
     * then uploads to Firestore for all users.
     */
    suspend fun syncVideosToFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting Firestore video sync...")

        try {
            // Check if sync is needed (Firestore cache validity is 6 hours)
            val isCacheValid = videoFirestoreService.isCacheValid()
            if (isCacheValid) {
                Log.d(TAG, "Firestore cache is still valid, skipping sync")
                return@withContext Result.success(0)
            }

            // Fetch fresh videos from API
            val freshVideos = fetchVideosFromApi()

            if (freshVideos.isEmpty()) {
                Log.w(TAG, "No videos fetched from API, sync skipped")
                return@withContext Result.failure(Exception("No videos fetched"))
            }

            // Upload to Firestore
            val result = videoFirestoreService.uploadVideos(freshVideos)

            if (result.isSuccess) {
                Log.d(TAG, "Synced ${freshVideos.size} videos to Firestore")

                // Also update local cache
                try {
                    videoDao.replaceAll(freshVideos.map { it.toEntity() })
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update local cache after sync: ${e.message}")
                }

                // Update in-memory cache
                cachedVideos = freshVideos
                lastFetchTime = System.currentTimeMillis()
                _videos.value = freshVideos
            }

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing videos to Firestore: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Fetch videos from YouTube Data API.
     *
     * New logic:
     * - 3 next races, each with up to 3 videos (max 9 race videos)
     * - If a race has no videos, skip to next race
     * - Remaining 4 videos from news topics
     */
    private suspend fun fetchVideosFromApi(): List<CyclingVideo> {
        val allVideos = mutableListOf<CyclingVideo>()
        val seenIds = mutableSetOf<String>()

        // Build topics from calendar and news
        val (raceTopics, newsTopics) = buildSearchTopics()
        Log.d(TAG, "Built ${raceTopics.size} race topics and ${newsTopics.size} news topics")

        // 1. Fetch videos for races (up to 3 races, 3 videos each = max 9)
        var raceVideosCount = 0
        for (topic in raceTopics.take(MAX_RACES + 2)) { // Take extra in case some have no results
            if (raceVideosCount >= MAX_RACE_VIDEOS) break

            val videos = fetchVideosForTopic(topic, VIDEOS_PER_RACE)
            if (videos.isNotEmpty()) {
                for (video in videos) {
                    if (video.id !in seenIds && raceVideosCount < MAX_RACE_VIDEOS) {
                        seenIds.add(video.id)
                        allVideos.add(video)
                        raceVideosCount++
                    }
                }
                Log.d(TAG, "Added ${videos.size} videos for race: ${topic.displayTitle} (total race videos: $raceVideosCount)")
            } else {
                Log.d(TAG, "No videos found for race: ${topic.displayTitle}, skipping to next")
            }
        }

        // 2. Fetch videos for news (up to 4 videos)
        var newsVideosCount = 0
        for (topic in newsTopics) {
            if (newsVideosCount >= MAX_NEWS_VIDEOS) break

            val videos = fetchVideosForTopic(topic, 1) // 1 video per news topic
            for (video in videos) {
                if (video.id !in seenIds && newsVideosCount < MAX_NEWS_VIDEOS) {
                    seenIds.add(video.id)
                    allVideos.add(video)
                    newsVideosCount++
                }
            }
        }

        Log.d(TAG, "Total videos fetched: ${allVideos.size} (races: $raceVideosCount, news: $newsVideosCount)")
        return allVideos
    }

    /**
     * Fetch videos for a specific topic from YouTube API.
     */
    private suspend fun fetchVideosForTopic(topic: SearchTopic, maxVideos: Int): List<CyclingVideo> {
        try {
            // 1. Try YouTube Data API first (official, reliable)
            if (youtubeDataApi.isConfigured()) {
                val ytVideos = youtubeDataApi.searchVideos(topic.query, maxVideos)
                if (ytVideos.isNotEmpty()) {
                    Log.d(TAG, "YouTube API found ${ytVideos.size} videos for: ${topic.query}")
                    return ytVideos.map { video ->
                        Log.d(TAG, "  Video ID: ${video.videoId}, Title: ${video.title}")
                        CyclingVideo(
                            id = video.videoId,
                            title = video.title.ifBlank { topic.displayTitle },
                            description = topic.description,
                            thumbnailUrl = video.thumbnailUrl,
                            videoUrl = "https://www.youtube.com/watch?v=${video.videoId}",
                            channelName = video.channelName.ifBlank { topic.source },
                            durationSeconds = 0,
                            source = VideoSource.YOUTUBE
                        )
                    }
                } else {
                    Log.w(TAG, "YouTube API returned empty for: ${topic.query}")
                }
            } else {
                Log.w(TAG, "YouTube API not configured")
            }

            // 2. Fallback to Invidious/Piped (free but unreliable)
            val apiVideos = invidiousApi.searchVideos(topic.query, maxVideos)
            if (apiVideos.isNotEmpty()) {
                Log.d(TAG, "Invidious found ${apiVideos.size} videos for: ${topic.query}")
                return apiVideos.map { video ->
                    CyclingVideo(
                        id = video.videoId,
                        title = video.title.ifBlank { topic.displayTitle },
                        description = topic.description,
                        thumbnailUrl = video.thumbnailUrl,
                        videoUrl = "https://www.youtube.com/watch?v=${video.videoId}",
                        channelName = video.channelName.ifBlank { topic.source },
                        durationSeconds = video.durationSeconds,
                        source = VideoSource.YOUTUBE
                    )
                }
            }

            // 3. Return empty - don't use search URLs as fallback
            return emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error for topic ${topic.query}: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Build search topics from race calendar and news.
     *
     * Returns a Pair:
     * - First: Race topics (for up to 9 videos from 3 races)
     * - Second: News topics (for up to 6 videos)
     */
    private suspend fun buildSearchTopics(): Pair<List<SearchTopic>, List<SearchTopic>> {
        val raceTopics = mutableListOf<SearchTopic>()
        val newsTopics = mutableListOf<SearchTopic>()

        val now = System.currentTimeMillis()
        // Start of today (midnight) to include races happening today
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val twoMonthsFromNow = now + (60L * 24 * 60 * 60 * 1000)
        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)

        // Collect all upcoming races (provas + WorldTour) and sort by date
        data class UnifiedRace(
            val name: String,
            val date: Long,
            val query: String,
            val displayTitle: String,
            val source: String,
            val isProva: Boolean
        )

        val allRaces = mutableListOf<UnifiedRace>()

        // 1. Get Portuguese provas
        try {
            var upcomingProvas: List<Prova> = emptyList()
            var retryCount = 0
            val maxRetries = 3

            while (upcomingProvas.isEmpty() && retryCount < maxRetries) {
                if (retryCount > 0) {
                    Log.d(TAG, "Retry $retryCount: waiting for provas sync...")
                    kotlinx.coroutines.delay(2000L)
                }

                val allProvas = provaRepository.getProvas().first()
                Log.d(TAG, "Attempt ${retryCount + 1}: Total provas in database: ${allProvas.size}")

                upcomingProvas = allProvas
                    .filter { it.data >= startOfToday && it.data <= twoMonthsFromNow }
                    .sortedBy { it.data }

                retryCount++
            }

            Log.d(TAG, "Found ${upcomingProvas.size} upcoming provas (from today onwards)")

            // Add unique provas with aggressive deduplication
            val seenNames = mutableSetOf<String>()
            for (prova in upcomingProvas) {
                val normalizedName = prova.nome.lowercase()
                    .replace(Regex("[0-9]+"), "")
                    .replace(Regex("(?i)\\b(edição|edicao|edition|etapa|stage)\\b"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .split(" ")
                    .filter { it.length > 3 }
                    .take(3)
                    .joinToString(" ")

                val isDuplicate = seenNames.any { existing ->
                    normalizedName.contains(existing) ||
                    existing.contains(normalizedName) ||
                    normalizedName == existing
                }

                if (!isDuplicate && normalizedName.isNotBlank()) {
                    seenNames.add(normalizedName)
                    val dateStr = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(prova.data)
                    Log.d(TAG, "  Adding prova: ${prova.nome} ($dateStr)")
                    allRaces.add(
                        UnifiedRace(
                            name = prova.nome,
                            date = prova.data,
                            query = buildProvaSearchQuery(prova),
                            displayTitle = "🇵🇹 ${prova.nome}",
                            source = prova.source,
                            isProva = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get provas: ${e.message}", e)
        }

        // 2. Get WorldTour races
        try {
            val races = raceRepository.getUpcomingRaces().first()
                .filter { it.startDate >= startOfToday && it.startDate <= twoMonthsFromNow }
                .sortedBy { it.startDate }

            Log.d(TAG, "Found ${races.size} upcoming WorldTour races")
            for (race in races.take(5)) {
                val dateStr = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(race.startDate)
                Log.d(TAG, "  WorldTour: ${race.name} ($dateStr)")
                allRaces.add(
                    UnifiedRace(
                        name = race.name,
                        date = race.startDate,
                        query = "${race.name} cycling highlights 2026",
                        displayTitle = "🌍 ${race.name}",
                        source = race.category ?: "WorldTour",
                        isProva = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get WorldTour races: ${e.message}")
        }

        // Sort all races by date and take next races (extras in case some have no videos)
        val nextRaces = allRaces
            .sortedBy { it.date }
            .distinctBy { it.name.lowercase().take(15) }
            .take(MAX_RACES + 2) // Extra buffer

        Log.d(TAG, "Selected ${nextRaces.size} races for video search:")
        for ((index, race) in nextRaces.withIndex()) {
            val type = if (race.isProva) "Prova" else "WorldTour"
            val dateStr = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(race.date)
            Log.d(TAG, "  Race ${index + 1} ($type): ${race.name} ($dateStr) -> Query: ${race.query}")
            raceTopics.add(
                SearchTopic(
                    query = race.query,
                    displayTitle = race.displayTitle,
                    description = if (race.isProva) "Próxima prova" else "WorldTour",
                    source = race.source,
                    timestamp = race.date,
                    priority = index + 1
                )
            )
        }

        // 3. Add news topics (up to 6 for 6 videos) with DIVERSITY
        try {
            val recentNews = newsRepository.getLatestNews(30).first()
                .filter { it.publishedAt >= oneWeekAgo }
                .sortedByDescending { it.publishedAt }

            Log.d(TAG, "Found ${recentNews.size} recent news articles")

            // Track used queries AND used cyclists/topics for diversity
            val usedQueries = mutableSetOf<String>()
            val usedCyclistTopics = mutableSetOf<String>() // Track cyclist names to avoid duplicates

            // Max 1 video per cyclist to ensure diversity
            val maxVideosPerCyclist = 1
            val cyclistVideoCount = mutableMapOf<String, Int>()

            for (article in recentNews) {
                if (newsTopics.size >= MAX_NEWS_VIDEOS) break

                val (searchQuery, cyclistKey) = extractSearchTermsFromNewsWithKey(article)

                // Check if we've already used this cyclist too many times
                if (cyclistKey.isNotBlank()) {
                    val currentCount = cyclistVideoCount.getOrDefault(cyclistKey, 0)
                    if (currentCount >= maxVideosPerCyclist) {
                        Log.d(TAG, "Skipping news about $cyclistKey - already have $currentCount video(s)")
                        continue
                    }
                    cyclistVideoCount[cyclistKey] = currentCount + 1
                }

                if (searchQuery.isNotBlank() && searchQuery.lowercase() !in usedQueries) {
                    usedQueries.add(searchQuery.lowercase())
                    Log.d(TAG, "News topic: ${article.title.take(40)} -> Query: $searchQuery (cyclist: ${cyclistKey.ifBlank { "none" }})")
                    newsTopics.add(
                        SearchTopic(
                            query = searchQuery,
                            displayTitle = "📰 ${article.title.take(35)}${if (article.title.length > 35) "..." else ""}",
                            description = article.source,
                            source = article.source,
                            timestamp = article.publishedAt,
                            priority = 100 + newsTopics.size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get news: ${e.message}")
        }

        // 4. Add diverse fallback topics if needed (varied topics to ensure diversity)
        if (newsTopics.size < MAX_NEWS_VIDEOS) {
            val fallbackNewsTopics = listOf(
                // Mix of cyclists, races, teams, and general content
                SearchTopic("ciclismo Portugal 2026", "🇵🇹 Ciclismo Portugal", "Portugal", "Portugal", now, 200),
                SearchTopic("GCN Racing cycling highlights", "🚴 GCN Racing", "Análises", "GCN", now, 201),
                SearchTopic("Paris Roubaix highlights 2026", "🏁 Paris-Roubaix", "Monument", "Cycling", now, 202),
                SearchTopic("Tadej Pogacar cycling 2026", "🏆 Tadej Pogačar", "Campeão Mundial", "Pro Cycling", now, 203),
                SearchTopic("Mathieu van der Poel classics 2026", "🏆 Van der Poel", "Clássicas", "Pro Cycling", now, 204),
                SearchTopic("cycling women peloton 2026", "🚴‍♀️ Women's Cycling", "Feminino", "Women", now, 205),
                SearchTopic("Remco Evenepoel cycling 2026", "🏆 Remco Evenepoel", "Bélgica", "Pro Cycling", now, 206),
                SearchTopic("BTT mountain bike race 2026", "🚵 Mountain Bike", "BTT/MTB", "BTT", now, 207),
                SearchTopic("cycling sprint finish 2026", "🏁 Sprint Finishes", "Highlights", "Cycling", now, 208)
            )

            // Track what cyclists we already have in news topics
            val usedCyclistsInFallback = newsTopics.map { it.query.lowercase() }.toMutableSet()

            for (topic in fallbackNewsTopics) {
                if (newsTopics.size >= MAX_NEWS_VIDEOS) break
                // Skip if we already have something similar
                if (topic.query.lowercase() !in usedCyclistsInFallback) {
                    usedCyclistsInFallback.add(topic.query.lowercase())
                    newsTopics.add(topic)
                }
            }
        }

        Log.d(TAG, "Final topics - Races: ${raceTopics.size}, News: ${newsTopics.size}")
        return Pair(raceTopics, newsTopics)
    }

    /**
     * Build YouTube search query for a local Portuguese race.
     * Keep it simple and short for better search results.
     */
    private fun buildProvaSearchQuery(prova: Prova): String {
        // Extract just the main race name (first 3-4 words)
        val nameWords = prova.nome
            .replace(Regex("[0-9]+"), "") // Remove numbers
            .replace(Regex("\\s+"), " ")  // Normalize spaces
            .trim()
            .split(" ")
            .filter { it.length > 2 }     // Remove short words
            .take(4)                       // Take first 4 meaningful words
            .joinToString(" ")

        // Build simple query
        val queryParts = mutableListOf<String>()

        if (nameWords.isNotBlank()) {
            queryParts.add(nameWords)
        }

        // Add type for context
        when {
            prova.tipo.contains("BTT", ignoreCase = true) -> queryParts.add("BTT ciclismo")
            prova.tipo.contains("Gravel", ignoreCase = true) -> queryParts.add("gravel cycling")
            else -> queryParts.add("ciclismo")
        }

        return queryParts.joinToString(" ")
    }

    /**
     * Extract search terms from a news article.
     * Returns a Pair of (searchQuery, cyclistKey) where cyclistKey is used for diversity tracking.
     */
    private fun extractSearchTermsFromNewsWithKey(article: NewsArticle): Pair<String, String> {
        val title = article.title.lowercase()

        // Extract cyclist names from title - use same key for diversity tracking
        val cyclistNames = listOf(
            "pogacar" to Pair("Tadej Pogacar", "pogacar"),
            "vingegaard" to Pair("Jonas Vingegaard", "vingegaard"),
            "evenepoel" to Pair("Remco Evenepoel", "evenepoel"),
            "van aert" to Pair("Wout van Aert", "vanaert"),
            "van der poel" to Pair("Mathieu van der Poel", "vanderpoel"),
            "almeida" to Pair("Joao Almeida", "almeida"),
            "oliveira" to Pair("Nelson Oliveira", "oliveira"),
            "rui costa" to Pair("Rui Costa", "ruicosta"),
            "roglič" to Pair("Primoz Roglic", "roglic"),
            "roglic" to Pair("Primoz Roglic", "roglic"),
            "cavendish" to Pair("Mark Cavendish", "cavendish"),
            "philipsen" to Pair("Jasper Philipsen", "philipsen"),
            "pidcock" to Pair("Tom Pidcock", "pidcock"),
            "bernal" to Pair("Egan Bernal", "bernal"),
            "mas" to Pair("Enric Mas cycling", "mas"),
            "wout" to Pair("Wout van Aert", "vanaert"),
            "mathieu" to Pair("Mathieu van der Poel", "vanderpoel"),
            "remco" to Pair("Remco Evenepoel", "evenepoel"),
            "tadej" to Pair("Tadej Pogacar", "pogacar"),
            "jonas" to Pair("Jonas Vingegaard", "vingegaard"),
            "primoz" to Pair("Primoz Roglic", "roglic")
        )

        // Extract race names from title
        val raceNames = listOf(
            "tour de france" to Pair("Tour de France highlights", "tdf"),
            "giro d'italia" to Pair("Giro d'Italia highlights", "giro"),
            "giro" to Pair("Giro d'Italia highlights", "giro"),
            "vuelta" to Pair("Vuelta a Espana highlights", "vuelta"),
            "volta a portugal" to Pair("Volta a Portugal ciclismo", "voltaportugal"),
            "paris-roubaix" to Pair("Paris Roubaix highlights", "roubaix"),
            "paris roubaix" to Pair("Paris Roubaix highlights", "roubaix"),
            "milano-sanremo" to Pair("Milano Sanremo highlights", "sanremo"),
            "liege-bastogne" to Pair("Liege Bastogne Liege highlights", "liege"),
            "tour of flanders" to Pair("Tour of Flanders highlights", "flanders"),
            "ronde van vlaanderen" to Pair("Tour of Flanders highlights", "flanders"),
            "strade bianche" to Pair("Strade Bianche highlights", "stradebianche"),
            "amstel gold" to Pair("Amstel Gold Race highlights", "amstel"),
            "fleche wallonne" to Pair("Fleche Wallonne highlights", "fleche"),
            "clasica san sebastian" to Pair("Clasica San Sebastian highlights", "sansebastian"),
            "world championship" to Pair("cycling world championship", "worldchamp"),
            "campeonato do mundo" to Pair("cycling world championship", "worldchamp")
        )

        // Check for cyclist names
        for ((keyword, queryAndKey) in cyclistNames) {
            if (title.contains(keyword)) {
                return Pair("${queryAndKey.first} cycling 2026", queryAndKey.second)
            }
        }

        // Check for race names
        for ((keyword, queryAndKey) in raceNames) {
            if (title.contains(keyword)) {
                return Pair("${queryAndKey.first} 2026", queryAndKey.second)
            }
        }

        // Check for cycling categories - use category as key
        if (title.contains("btt") || title.contains("mtb") || title.contains("mountain bike")) {
            return Pair("mountain bike race highlights 2026", "btt")
        }
        if (title.contains("gravel")) {
            return Pair("gravel cycling race 2026", "gravel")
        }
        if (title.contains("ciclismo") || title.contains("cycling") || title.contains("pelotão")) {
            return Pair("pro cycling highlights 2026", "general")
        }

        return Pair("", "")
    }

    /**
     * Extract search terms from a news article (legacy, returns only query).
     */
    private fun extractSearchTermsFromNews(article: NewsArticle): String {
        return extractSearchTermsFromNewsWithKey(article).first
    }

    /**
     * Get curated list of cycling videos (fallback - uses search URLs that open in YouTube app).
     * These always work because they're search queries, not specific video IDs.
     */
    fun getCuratedVideos(): List<CyclingVideo> {
        val searchBase = "https://www.youtube.com/results?search_query="
        return listOf(
            CyclingVideo(
                id = "search_gcn",
                title = "🚴 GCN Racing",
                description = "Análises e highlights",
                thumbnailUrl = "https://i.imgur.com/DqLpKMx.jpg",
                videoUrl = "${searchBase}gcn+racing+cycling+highlights+2025",
                channelName = "GCN",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            ),
            CyclingVideo(
                id = "search_volta",
                title = "🇵🇹 Volta a Portugal",
                description = "Ciclismo Português",
                thumbnailUrl = "https://i.imgur.com/kP8rHqN.jpg",
                videoUrl = "${searchBase}volta+a+portugal+ciclismo+2025",
                channelName = "Portugal",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            ),
            CyclingVideo(
                id = "search_tdf",
                title = "🇫🇷 Tour de France",
                description = "Grand Tour Highlights",
                thumbnailUrl = "https://i.imgur.com/YJlqVvL.jpg",
                videoUrl = "${searchBase}tour+de+france+highlights+2025",
                channelName = "WorldTour",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            ),
            CyclingVideo(
                id = "search_giro",
                title = "🇮🇹 Giro d'Italia",
                description = "Grand Tour Highlights",
                thumbnailUrl = "https://i.imgur.com/n7QZXHK.jpg",
                videoUrl = "${searchBase}giro+d'italia+highlights+2025",
                channelName = "WorldTour",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            ),
            CyclingVideo(
                id = "search_vuelta",
                title = "🇪🇸 La Vuelta",
                description = "Grand Tour Highlights",
                thumbnailUrl = "https://i.imgur.com/8KqPxbM.jpg",
                videoUrl = "${searchBase}la+vuelta+highlights+2025",
                channelName = "WorldTour",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            ),
            CyclingVideo(
                id = "search_pogacar",
                title = "🏆 Tadej Pogačar",
                description = "Campeão Mundial",
                thumbnailUrl = "https://i.imgur.com/vJKqLpM.jpg",
                videoUrl = "${searchBase}pogacar+cycling+2025",
                channelName = "Pro Cycling",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            ),
            CyclingVideo(
                id = "search_evenepoel",
                title = "🏆 Remco Evenepoel",
                description = "Campeão Olímpico",
                thumbnailUrl = "https://i.imgur.com/WxqLpKM.jpg",
                videoUrl = "${searchBase}evenepoel+cycling+2025",
                channelName = "Pro Cycling",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            ),
            CyclingVideo(
                id = "search_btt",
                title = "🇵🇹 BTT Portugal",
                description = "Mountain Bike",
                thumbnailUrl = "https://i.imgur.com/BTTmtbPT.jpg",
                videoUrl = "${searchBase}btt+portugal+mountain+bike+2025",
                channelName = "Portugal",
                durationSeconds = 0,
                source = VideoSource.YOUTUBE
            )
        )
    }

    /**
     * Force refresh videos (clears cache).
     */
    suspend fun refreshVideos(): List<CyclingVideo> {
        cachedVideos = null
        lastFetchTime = 0
        return getVideosForCurrentContent()
    }

    private data class SearchTopic(
        val query: String,
        val displayTitle: String,
        val description: String,
        val source: String,
        val timestamp: Long,
        val priority: Int
    )
}
