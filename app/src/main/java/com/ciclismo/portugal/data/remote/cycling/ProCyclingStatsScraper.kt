package com.ciclismo.portugal.data.remote.cycling

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProCyclingStatsScraper @Inject constructor() : CyclingDataSource {

    companion object {
        private const val BASE_URL = "https://www.procyclingstats.com"
        private const val TIMEOUT = 30000L

        // Country name to ISO code mapping
        private val COUNTRY_CODES = mapOf(
            "Slovenia" to "SI", "Belgium" to "BE", "France" to "FR", "Italy" to "IT",
            "Spain" to "ES", "Portugal" to "PT", "Netherlands" to "NL", "Germany" to "DE",
            "United Kingdom" to "GB", "Great Britain" to "GB", "Denmark" to "DK",
            "Norway" to "NO", "Sweden" to "SE", "Switzerland" to "CH", "Austria" to "AT",
            "Poland" to "PL", "Czech Republic" to "CZ", "Czechia" to "CZ",
            "Australia" to "AU", "New Zealand" to "NZ", "United States" to "US", "USA" to "US",
            "Canada" to "CA", "Colombia" to "CO", "Ecuador" to "EC", "Argentina" to "AR",
            "Ireland" to "IE", "Luxembourg" to "LU", "Kazakhstan" to "KZ", "Russia" to "RU",
            "Ukraine" to "UA", "Slovakia" to "SK", "Croatia" to "HR", "Serbia" to "RS",
            "South Africa" to "ZA", "Eritrea" to "ER", "Ethiopia" to "ET", "Rwanda" to "RW",
            "Japan" to "JP", "China" to "CN", "Israel" to "IL", "Finland" to "FI"
        )

        // User agents rotativos - mais realistas
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15"
        )
    }

    // Cookie jar para manter sessão
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore.getOrPut(url.host) { mutableListOf() }.apply {
                clear()
                addAll(cookies)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var currentUserAgentIndex = 0
    private var sessionInitialized = false
    private var lastRequestTime = 0L
    private val minRequestInterval = 1000L // Mínimo 1 segundo entre pedidos

    private fun getNextUserAgent(): String {
        val ua = USER_AGENTS[currentUserAgentIndex]
        currentUserAgentIndex = (currentUserAgentIndex + 1) % USER_AGENTS.size
        return ua
    }

    /**
     * Initialize session by visiting homepage to get cookies
     * This mimics normal browser behavior and helps bypass some protections
     */
    private suspend fun initializeSession() {
        if (sessionInitialized) return

        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("PCS_SCRAPER", "Initializing session by visiting homepage...")

                val userAgent = getNextUserAgent()
                val request = Request.Builder()
                    .url(BASE_URL)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val code = response.code
                response.close()

                android.util.Log.d("PCS_SCRAPER", "Homepage response: $code, cookies: ${cookieStore.size}")
                sessionInitialized = code == 200

                // Small delay after initialization
                delay(500)
            } catch (e: Exception) {
                android.util.Log.e("PCS_SCRAPER", "Failed to initialize session: ${e.message}")
                // Continue anyway
            }
        }
    }

    /**
     * Ensure minimum delay between requests to avoid rate limiting
     */
    private suspend fun throttleRequest() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < minRequestInterval && lastRequestTime > 0) {
            delay(minRequestInterval - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    /**
     * Get cyclists from all WorldTour teams for 2026
     */
    override suspend fun getTopCyclists(limit: Int): Result<List<CyclistDto>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    /**
     * Get cyclists from a specific team page URL
     * Extracts: name, nationality, age, ranking, and speciality
     */
    override suspend fun getCyclistsFromTeamUrl(teamUrl: String, teamName: String): Result<List<CyclistDto>> = withContext(Dispatchers.IO) {
        try {
            // Initialize session first (gets cookies)
            initializeSession()
            throttleRequest()

            val cyclists = mutableListOf<CyclistDto>()

            // Clean URL and try variations
            val baseUrl = teamUrl.trim()
                .removeSuffix("/")
                .removeSuffix("/overview")
                .removeSuffix("/start")
                .removeSuffix("/overview/start")

            android.util.Log.d("PCS_SCRAPER", "Base team URL: $baseUrl")

            // Try different URL variations that PCS uses
            val urlsToTry = listOf(
                baseUrl,
                "$baseUrl/overview",
                "$baseUrl/overview/start"
            )

            var doc: Document? = null
            var successUrl: String? = null

            for (url in urlsToTry) {
                try {
                    android.util.Log.d("PCS_SCRAPER", "Trying URL: $url")
                    doc = fetchDocument(url)
                    if (doc.body()?.text()?.length ?: 0 > 500) {
                        successUrl = url
                        android.util.Log.d("PCS_SCRAPER", "Success with URL: $url")
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PCS_SCRAPER", "Failed URL $url: ${e.message}")
                    continue
                }
            }

            if (doc == null) {
                return@withContext Result.failure(Exception("Não foi possível aceder à página. O site pode estar a bloquear pedidos."))
            }

            // Extract team ID from URL
            val teamId = baseUrl.substringAfterLast("/team/").substringBefore("?").substringBefore("/").substringBefore("#")

            android.util.Log.d("PCS_SCRAPER", "Page title: ${doc.title()}")
            android.util.Log.d("PCS_SCRAPER", "Page HTML length: ${doc.html().length}")

            // Try multiple CSS selectors that PCS might use
            val selectorsToTry = listOf(
                "ul.list.fs14 li a[href*=rider]" to "ul.list.fs14 li a[href*=rider]",
                "ul.list li a[href*=rider]" to "ul.list li a[href*=rider]",
                "div.ridersCont a[href*=rider]" to "div.ridersCont a[href*=rider]",
                "div.riders a[href*=rider]" to "div.riders a[href*=rider]",
                "table.basic tbody tr a[href*=rider]" to "table.basic tbody tr a[href*=rider]",
                "table tbody tr td a[href*=rider]" to "table tbody tr td a[href*=rider]",
                ".riderlist a[href*=rider]" to ".riderlist a[href*=rider]",
                "a[href*=/rider/]" to "a[href*=/rider/] (generic)"
            )

            var riderElements = org.jsoup.select.Elements()

            for ((selector, description) in selectorsToTry) {
                val elements = doc.select(selector)
                android.util.Log.d("PCS_SCRAPER", "Selector '$description': ${elements.size} elements")
                if (elements.isNotEmpty() && riderElements.isEmpty()) {
                    riderElements = elements
                    android.util.Log.d("PCS_SCRAPER", "Using selector: $description")
                }
            }

            // Log page structure for debugging
            android.util.Log.d("PCS_SCRAPER", "Page has ${doc.select("ul").size} <ul> elements")
            android.util.Log.d("PCS_SCRAPER", "Page has ${doc.select("table").size} <table> elements")
            android.util.Log.d("PCS_SCRAPER", "Page has ${doc.select("a[href*=rider]").size} rider links total")

            // Process unique riders (avoid duplicates from multiple links to same rider)
            val processedIds = mutableSetOf<String>()

            for (element in riderElements) {
                try {
                    val href = element.attr("href")
                    if (!href.contains("rider")) continue

                    val riderId = href.substringAfterLast("/").substringBefore("?")
                    if (riderId.isBlank() || riderId in processedIds) continue

                    processedIds.add(riderId)

                    val riderName = element.text().trim()
                    if (riderName.isBlank() || riderName.length < 3) continue

                    // Skip non-name links (like "more info", "statistics", etc.)
                    if (riderName.lowercase().contains("more") ||
                        riderName.lowercase().contains("statistic") ||
                        riderName.lowercase().contains("overview")) continue

                    // Parse name (format: "LASTNAME Firstname")
                    val nameParts = riderName.split(" ").filter { it.isNotBlank() }
                    val (firstName, lastName) = parseRiderName(nameParts)

                    // Try to get nationality from nearby flag element
                    val parentRow = element.parents().find { it.tagName() == "tr" || it.tagName() == "li" }
                    val flagElement = parentRow?.select("span.flag, span[class*=flag]")?.firstOrNull()
                        ?: element.parent()?.select("span.flag")?.firstOrNull()
                    val nationality = extractNationality(flagElement)

                    cyclists.add(
                        CyclistDto(
                            id = riderId,
                            firstName = firstName,
                            lastName = lastName,
                            teamId = teamId,
                            teamName = teamName,
                            nationality = nationality,
                            photoUrl = null,
                            uciRanking = null,
                            points = 0,
                            age = null,
                            speciality = null
                        )
                    )

                    android.util.Log.d("PCS_SCRAPER", "Found cyclist: $firstName $lastName ($riderId)")

                } catch (e: Exception) {
                    android.util.Log.e("PCS_SCRAPER", "Error parsing rider element: ${e.message}")
                    continue
                }
            }

            android.util.Log.d("PCS_SCRAPER", "Total cyclists found from team page: ${cyclists.size}")

            if (cyclists.isEmpty()) {
                // Log some page content for debugging
                val bodyText = doc.body()?.text()?.take(800) ?: "No body"
                android.util.Log.d("PCS_SCRAPER", "Page body preview: $bodyText")

                // Log raw HTML structure
                val htmlPreview = doc.html().take(1500)
                android.util.Log.d("PCS_SCRAPER", "HTML preview: $htmlPreview")

                // Check if it's a consent/captcha page
                val hasConsent = doc.html().contains("consent", ignoreCase = true) || doc.html().contains("cookie", ignoreCase = true)
                val hasCaptcha = doc.html().contains("captcha", ignoreCase = true) || doc.html().contains("cloudflare", ignoreCase = true)
                android.util.Log.d("PCS_SCRAPER", "Has consent/cookie: $hasConsent, Has captcha/cloudflare: $hasCaptcha")

                return@withContext Result.failure(Exception("Nenhum ciclista encontrado na página. Verifica se o URL está correto."))
            }

            // Fetch details for each cyclist to get speciality, age, ranking
            val enrichedCyclists = mutableListOf<CyclistDto>()
            for ((index, cyclist) in cyclists.withIndex()) {
                try {
                    // Add delay to avoid rate limiting
                    if (index > 0) delay(300)

                    android.util.Log.d("PCS_SCRAPER", "Fetching details for ${cyclist.firstName} ${cyclist.lastName} (${index + 1}/${cyclists.size})")

                    val detailResult = getCyclistDetails(cyclist.id)
                    detailResult.fold(
                        onSuccess = { details ->
                            // Copy nationality from details if valid (not XX)
                            val finalNationality = if (details.nationality != "XX" && details.nationality.length == 2) {
                                details.nationality
                            } else {
                                cyclist.nationality
                            }

                            android.util.Log.d("PCS_SCRAPER", "Enriched ${cyclist.firstName} ${cyclist.lastName}: nat=$finalNationality, photo=${details.photoUrl?.take(50)}")

                            enrichedCyclists.add(
                                cyclist.copy(
                                    nationality = finalNationality,
                                    photoUrl = details.photoUrl,
                                    uciRanking = details.uciRanking,
                                    points = details.points,
                                    age = details.age,
                                    speciality = details.speciality,
                                    profileUrl = details.profileUrl
                                )
                            )
                        },
                        onFailure = {
                            android.util.Log.e("PCS_SCRAPER", "Failed to get details for ${cyclist.id}: ${it.message}")
                            enrichedCyclists.add(cyclist)
                        }
                    )
                } catch (e: Exception) {
                    enrichedCyclists.add(cyclist)
                }
            }

            android.util.Log.d("PCS_SCRAPER", "Enriched ${enrichedCyclists.size} cyclists from $teamName")
            Result.success(enrichedCyclists)

        } catch (e: Exception) {
            android.util.Log.e("PCS_SCRAPER", "Error scraping team $teamName: ${e.message}", e)
            Result.failure(Exception("Erro ao obter ciclistas de $teamName: ${e.message}"))
        }
    }

    /**
     * Get detailed information for a single cyclist
     */
    override suspend fun getCyclistDetails(cyclistId: String): Result<CyclistDto> = withContext(Dispatchers.IO) {
        try {
            // Initialize session if needed
            initializeSession()
            throttleRequest()

            val url = "$BASE_URL/rider/$cyclistId"
            val doc = fetchDocument(url)

            // Get name from h1 or title
            val h1Text = doc.select("h1").firstOrNull()?.text()?.trim() ?: ""
            val fullName = if (h1Text.isNotBlank()) h1Text else doc.title().substringBefore("-").trim()

            val nameParts = fullName.split(" ", limit = 2)
            val lastName = nameParts.getOrNull(0)?.uppercase() ?: fullName
            val firstName = nameParts.getOrNull(1) ?: ""

            // Get team from info section
            val team = doc.select("a[href*=team]").firstOrNull()?.text()?.trim() ?: "N/A"

            // Get nationality from flag - try multiple selectors
            var nationality = "XX"

            // Debug: log all elements with "flag" in class
            val allFlagElements = doc.select("[class*=flag]")
            android.util.Log.d("PCS_SCRAPER", "Found ${allFlagElements.size} elements with 'flag' in class")
            allFlagElements.take(5).forEach { el ->
                android.util.Log.d("PCS_SCRAPER", "Flag element: tag=${el.tagName()}, class='${el.attr("class")}', src='${el.attr("src")}'")
            }

            val flagSelectors = listOf(
                "span.flag",
                "div.rdr-info-cont span.flag",
                "div.rider-info span.flag",
                "span[class*=flag]",
                "img[src*=flags]",
                "a.flag",
                "div.info span.flag",
                "div.left span.flag",
                "b.flag"
            )
            for (selector in flagSelectors) {
                val flagElement = doc.select(selector).firstOrNull()
                if (flagElement != null) {
                    android.util.Log.d("PCS_SCRAPER", "Trying selector '$selector': found element with class='${flagElement.attr("class")}'")
                    nationality = extractNationality(flagElement)
                    if (nationality != "XX" && nationality.length >= 2) {
                        android.util.Log.d("PCS_SCRAPER", "Found nationality: $nationality using $selector")
                        break
                    }
                }
            }

            // Also try to get nationality from text (e.g., "Slovenia" in info)
            if (nationality == "XX") {
                val infoContainer = doc.select("div.rdr-info-cont, div.rider-info, div.info, div.left").text()
                android.util.Log.d("PCS_SCRAPER", "Info container text (first 200 chars): ${infoContainer.take(200)}")
                val countryMatch = COUNTRY_CODES.entries.find { (name, _) ->
                    infoContainer.contains(name, ignoreCase = true)
                }
                if (countryMatch != null) {
                    nationality = countryMatch.value
                    android.util.Log.d("PCS_SCRAPER", "Found nationality from text: $nationality")
                }
            }

            android.util.Log.d("PCS_SCRAPER", "Final nationality for $cyclistId: $nationality")

            // Get photo URL - try multiple selectors with priority
            var photoUrl: String? = null

            // Debug: log all img elements
            val allImages = doc.select("img")
            android.util.Log.d("PCS_SCRAPER", "Found ${allImages.size} img elements on page")
            allImages.take(10).forEach { img ->
                val src = img.attr("src").ifBlank { img.attr("data-src") }
                android.util.Log.d("PCS_SCRAPER", "Img: class='${img.attr("class")}', src='${src.take(80)}'")
            }

            val photoSelectors = listOf(
                "img.rdr-img-cont",
                "div.rdr-img-cont img",
                "img.rdr-img",
                "div.rider-image img",
                "div.rider-photo img",
                "img[src*=/riders/]",
                "img[src*=rider]",
                "div.left img[src*=.jpg]",
                "div.left img[src*=.png]",
                "div.left img",
                "img[width='100']",
                "img[width='200']"
            )
            for (selector in photoSelectors) {
                val imgElement = doc.select(selector).firstOrNull()
                if (imgElement != null) {
                    var src = imgElement.attr("src")
                    if (src.isBlank()) src = imgElement.attr("data-src")
                    android.util.Log.d("PCS_SCRAPER", "Trying photo selector '$selector': src='${src.take(80)}'")
                    if (src.isNotBlank() && !src.contains("nophoto") && !src.contains("blank") && !src.contains("logo") && !src.contains("icon")) {
                        if (src.startsWith("/")) {
                            photoUrl = "$BASE_URL$src"
                        } else if (src.startsWith("http")) {
                            photoUrl = src
                        }
                        android.util.Log.d("PCS_SCRAPER", "Found photo URL: $photoUrl using $selector")
                        break
                    }
                }
            }

            android.util.Log.d("PCS_SCRAPER", "Final photo URL for $cyclistId: $photoUrl")

            // Get speciality - try multiple selectors
            var speciality: String? = null
            // Try red text in rider info
            speciality = doc.select("div.rdr-info-cont span.red, span.rider-info span.red").text().trim().takeIf { it.isNotBlank() }
            // Try from specific class
            if (speciality.isNullOrBlank()) {
                speciality = doc.select("span.speciality, div.speciality").text().trim().takeIf { it.isNotBlank() }
            }

            // Get UCI ranking
            var uciRanking: Int? = null
            // Look for ranking in rider info
            val rankingElements = doc.select("div.rdr-rankings li, ul.rdr-rankings li, div.ranking")
            for (rankElement in rankingElements) {
                val text = rankElement.text()
                if (text.contains("UCI", ignoreCase = true) || text.contains("World", ignoreCase = true)) {
                    val rankValue = rankElement.select("div.rnk, span.rnk, strong").text()
                        .filter { it.isDigit() }
                        .toIntOrNull()
                    if (rankValue != null) {
                        uciRanking = rankValue
                        break
                    }
                }
            }

            // Get points
            val pointsText = doc.select("div.pnt span.nr, span.points-value").firstOrNull()?.text()
            val points = pointsText?.replace(",", "")?.replace(".", "")?.filter { it.isDigit() }?.toIntOrNull() ?: 0

            // Get age from birthdate info (usually shows like "(29)")
            var age: Int? = null
            val infoText = doc.select("div.rdr-info-cont, div.rider-info").text()
            val ageMatch = Regex("\\((\\d{2})\\)").find(infoText)
            age = ageMatch?.groupValues?.get(1)?.toIntOrNull()

            // Alternative: look for birth year and calculate
            if (age == null) {
                val birthMatch = Regex("(19|20)\\d{2}").find(infoText)
                birthMatch?.value?.toIntOrNull()?.let { birthYear ->
                    age = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - birthYear
                }
            }

            Result.success(
                CyclistDto(
                    id = cyclistId,
                    firstName = firstName,
                    lastName = lastName,
                    teamId = team.lowercase().replace(" ", "-").replace("|", "").replace("--", "-"),
                    teamName = team,
                    nationality = nationality,
                    photoUrl = photoUrl,
                    uciRanking = uciRanking,
                    points = points,
                    age = age,
                    speciality = speciality,
                    profileUrl = "$BASE_URL/rider/$cyclistId"
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("PCS_SCRAPER", "Error fetching cyclist details for $cyclistId: ${e.message}")
            Result.failure(Exception("Erro ao obter detalhes do ciclista: ${e.message}"))
        }
    }

    /**
     * Get cyclist details from a full URL
     */
    override suspend fun getCyclistFromUrl(url: String, teamName: String): Result<CyclistDto> = withContext(Dispatchers.IO) {
        try {
            // Initialize session if needed
            initializeSession()
            throttleRequest()

            // Extract cyclist ID from URL
            // URL format: https://www.procyclingstats.com/rider/tadej-pogacar
            val cleanUrl = url.trim()
                .removeSuffix("/")
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")

            val cyclistId = cleanUrl.substringAfter("rider/").substringBefore("/").substringBefore("?")

            if (cyclistId.isBlank()) {
                return@withContext Result.failure(Exception("URL inválido. Formato esperado: .../rider/nome-ciclista"))
            }

            android.util.Log.d("PCS_SCRAPER", "Extracting cyclist from URL: $url -> ID: $cyclistId")

            // Use existing method to get details
            val result = getCyclistDetails(cyclistId)

            // If team name provided, update it
            result.map { dto ->
                if (teamName.isNotBlank()) {
                    dto.copy(
                        teamId = teamName.lowercase().replace(" ", "-"),
                        teamName = teamName
                    )
                } else {
                    dto
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PCS_SCRAPER", "Error fetching cyclist from URL $url: ${e.message}")
            Result.failure(Exception("Erro ao obter ciclista: ${e.message}"))
        }
    }

    override suspend fun getUpcomingRaces(): Result<List<RaceDto>> = withContext(Dispatchers.IO) {
        try {
            initializeSession()
            throttleRequest()

            val races = mutableListOf<RaceDto>()
            val doc = fetchDocument("$BASE_URL/races.php")

            val raceRows = doc.select("table.basic tbody tr")

            for (row in raceRows) {
                try {
                    val dateCell = row.select("td").getOrNull(0)?.text()?.trim() ?: continue
                    val nameCell = row.select("td a").firstOrNull() ?: continue
                    val categoryCell = row.select("td").getOrNull(3)?.text()?.trim() ?: ""
                    val countryCell = row.select("td span.flag").firstOrNull()
                        ?.attr("class")
                        ?.replace("flag ", "")
                        ?.uppercase() ?: "XX"

                    val raceLink = nameCell.attr("href")
                    val raceId = raceLink.substringAfterLast("/")
                    val raceName = nameCell.text().trim()

                    val isStageRace = dateCell.contains("-")
                    val stages = if (isStageRace) {
                        when {
                            raceName.contains("Tour de France", true) -> 21
                            raceName.contains("Giro", true) -> 21
                            raceName.contains("Vuelta", true) -> 21
                            raceName.contains("Tour", true) -> 7
                            else -> 5
                        }
                    } else 1

                    races.add(
                        RaceDto(
                            id = raceId,
                            name = raceName,
                            startDate = System.currentTimeMillis(),
                            endDate = if (isStageRace) System.currentTimeMillis() else null,
                            country = countryCell,
                            category = categoryCell,
                            stages = stages,
                            profileUrl = "$BASE_URL$raceLink"
                        )
                    )
                } catch (e: Exception) {
                    continue
                }
            }

            Result.success(races)
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao obter corridas: ${e.message}"))
        }
    }

    override suspend fun getRaceResults(raceId: String): Result<List<RaceResultDto>> = withContext(Dispatchers.IO) {
        try {
            initializeSession()
            throttleRequest()

            val results = mutableListOf<RaceResultDto>()
            val doc = fetchDocument("$BASE_URL/race/$raceId/result")

            val rows = doc.select("table.results tbody tr")

            for (row in rows) {
                try {
                    val positionCell = row.select("td").getOrNull(0)?.text()?.trim() ?: continue
                    val position = positionCell.toIntOrNull() ?: continue

                    val riderCell = row.select("td a[href*=rider]").firstOrNull() ?: continue
                    val riderLink = riderCell.attr("href")
                    val riderId = riderLink.substringAfterLast("/")
                    val riderName = riderCell.text().trim()

                    val timeCell = row.select("td").getOrNull(5)?.text()?.trim()
                    val points = calculatePoints(position)

                    results.add(
                        RaceResultDto(
                            raceId = raceId,
                            cyclistId = riderId,
                            cyclistName = riderName,
                            position = position,
                            stageNumber = null,
                            time = timeCell,
                            points = points
                        )
                    )
                } catch (e: Exception) {
                    continue
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao obter resultados: ${e.message}"))
        }
    }

    override suspend fun searchCyclists(query: String): Result<List<CyclistDto>> = withContext(Dispatchers.IO) {
        try {
            initializeSession()
            throttleRequest()

            val cyclists = mutableListOf<CyclistDto>()
            val doc = fetchDocument("$BASE_URL/search.php?term=$query&searchType=rider")

            val rows = doc.select("ul.list li a")

            for (row in rows.take(20)) {
                try {
                    val link = row.attr("href")
                    if (!link.contains("rider")) continue

                    val riderId = link.substringAfterLast("/")
                    val riderName = row.text().trim()

                    val nameParts = riderName.split(" ", limit = 2)
                    val lastName = nameParts.getOrNull(0)?.uppercase() ?: riderName
                    val firstName = nameParts.getOrNull(1) ?: ""

                    cyclists.add(
                        CyclistDto(
                            id = riderId,
                            firstName = firstName,
                            lastName = lastName,
                            teamId = "",
                            teamName = "",
                            nationality = "XX",
                            photoUrl = null,
                            uciRanking = null,
                            points = 0,
                            age = null,
                            speciality = null
                        )
                    )
                } catch (e: Exception) {
                    continue
                }
            }

            Result.success(cyclists)
        } catch (e: Exception) {
            Result.failure(Exception("Erro na pesquisa: ${e.message}"))
        }
    }

    /**
     * Fetch document using OkHttp with sophisticated browser emulation
     * Similar approach to what procyclingstats Python library does
     */
    private suspend fun fetchDocument(url: String, retryCount: Int = 0): Document = withContext(Dispatchers.IO) {
        android.util.Log.d("PCS_SCRAPER", "Fetching URL: $url (attempt ${retryCount + 1})")

        try {
            val userAgent = getNextUserAgent()

            // Build request with comprehensive browser headers
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9,pt;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Cache-Control", "max-age=0")
                .header("DNT", "1")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val statusCode = response.code

            android.util.Log.d("PCS_SCRAPER", "Response status: $statusCode for $url")

            if (statusCode == 403) {
                response.close()
                // Retry with different user agent after delay
                if (retryCount < 3) {
                    android.util.Log.w("PCS_SCRAPER", "Got 403, retrying with different UA in 2s...")
                    delay(2000 + (retryCount * 1000L))
                    return@withContext fetchDocument(url, retryCount + 1)
                }
                android.util.Log.e("PCS_SCRAPER", "Access denied (403) after ${retryCount + 1} attempts for $url")
                throw Exception("Acesso negado pelo site (403). O site está a bloquear scraping.")
            }

            if (statusCode == 404) {
                response.close()
                android.util.Log.e("PCS_SCRAPER", "Page not found (404) for $url")
                throw Exception("Página não encontrada (404). Verifica se o URL está correto.")
            }

            if (statusCode == 429) {
                response.close()
                // Rate limited - wait longer
                if (retryCount < 3) {
                    android.util.Log.w("PCS_SCRAPER", "Rate limited (429), waiting 5s...")
                    delay(5000 + (retryCount * 2000L))
                    return@withContext fetchDocument(url, retryCount + 1)
                }
                throw Exception("Demasiados pedidos (429). Espera alguns minutos e tenta novamente.")
            }

            if (statusCode != 200) {
                response.close()
                throw Exception("Erro HTTP $statusCode")
            }

            val html = response.body?.string() ?: throw Exception("Resposta vazia")
            response.close()

            val doc = Jsoup.parse(html, url)
            android.util.Log.d("PCS_SCRAPER", "Page loaded successfully, body length: ${doc.body()?.text()?.length ?: 0}")

            doc
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("PCS_SCRAPER", "Timeout fetching $url")
            if (retryCount < 2) {
                delay(1500)
                return@withContext fetchDocument(url, retryCount + 1)
            }
            throw Exception("Timeout ao aceder ao site. Verifica a conexão à internet.")
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("PCS_SCRAPER", "Unknown host: ${e.message}")
            throw Exception("Não foi possível resolver o endereço. Verifica a conexão à internet.")
        } catch (e: java.io.IOException) {
            android.util.Log.e("PCS_SCRAPER", "IO Error: ${e.message}")
            if (retryCount < 2) {
                delay(1500)
                return@withContext fetchDocument(url, retryCount + 1)
            }
            throw Exception("Erro de rede: ${e.message}")
        }
    }

    private fun parseRiderName(nameParts: List<String>): Pair<String, String> {
        if (nameParts.isEmpty()) return "" to ""

        return if (nameParts.size >= 2) {
            // Check if first part is uppercase (LASTNAME format)
            if (nameParts[0].all { it.isUpperCase() || !it.isLetter() }) {
                nameParts.drop(1).joinToString(" ") to nameParts[0].uppercase()
            } else {
                nameParts.dropLast(1).joinToString(" ") to nameParts.last().uppercase()
            }
        } else {
            nameParts.first() to ""
        }
    }

    private fun extractNationality(flagElement: org.jsoup.nodes.Element?): String {
        return flagElement?.let { element ->
            // Try class attribute (e.g., "flag si", "flag-si", "flag si ")
            val classAttr = element.attr("class")
            var nationality = classAttr
                .replace("flag", "")
                .replace("-", " ")
                .trim()
                .split(" ")
                .firstOrNull { it.length == 2 }
                ?.uppercase()

            if (nationality != null && nationality.length == 2) {
                return@let nationality
            }

            // Try from src attribute for img elements (e.g., "/img/flags/si.png")
            val src = element.attr("src")
            if (src.isNotBlank()) {
                nationality = src
                    .substringAfterLast("/")
                    .substringBefore(".")
                    .uppercase()
                    .take(2)
                if (nationality.length == 2) {
                    return@let nationality
                }
            }

            // Try title or alt attribute
            val title = element.attr("title").ifBlank { element.attr("alt") }
            if (title.isNotBlank()) {
                val countryMatch = COUNTRY_CODES.entries.find { (name, _) ->
                    title.contains(name, ignoreCase = true)
                }
                if (countryMatch != null) {
                    return@let countryMatch.value
                }
            }

            "XX"
        } ?: "XX"
    }

    private fun calculatePoints(position: Int): Int {
        return when (position) {
            1 -> 100
            2 -> 70
            3 -> 50
            4 -> 40
            5 -> 35
            6 -> 30
            7 -> 25
            8 -> 20
            9 -> 15
            10 -> 10
            in 11..20 -> 5
            in 21..30 -> 2
            else -> 0
        }
    }
}
