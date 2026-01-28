package com.ciclismo.portugal.data.remote.scraper

import android.util.Log
import com.ciclismo.portugal.data.local.entity.ProvaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CabreiraScraper @Inject constructor() : BaseScraper {

    override val sourceName: String = "Cabreira Solutions"
    private val baseUrl = "https://cabreirasolutions.com"

    override suspend fun scrapeProvas(): Result<List<ProvaEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("CabreiraScraper", "Starting scrape from: $baseUrl/eventos/")
            val provas = mutableListOf<ProvaEntity>()

            val document: Document = Jsoup.connect("$baseUrl/eventos/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("CabreiraScraper", "Connected to Cabreira website successfully")
            Log.d("CabreiraScraper", "Page title: ${document.title()}")

            // Try multiple selectors to find events
            val selectors = listOf(
                "a[href*=/evento/]",
                "a[href*=evento]",
                ".evento a",
                ".event a",
                "article a"
            )

            var eventElements = org.jsoup.select.Elements()
            for (selector in selectors) {
                eventElements = document.select(selector)
                if (eventElements.isNotEmpty()) {
                    Log.d("CabreiraScraper", "Found ${eventElements.size} elements using selector: $selector")
                    break
                }
            }

            if (eventElements.isEmpty()) {
                Log.w("CabreiraScraper", "No event links found with any selector")
                // Log page structure for debugging
                Log.d("CabreiraScraper", "Page HTML preview: ${document.body().html().take(500)}")
                return@withContext Result.success(emptyList())
            }

            Log.d("CabreiraScraper", "Processing ${eventElements.size} event elements")

            for ((index, element) in eventElements.withIndex().take(20)) {
                try {
                    Log.d("CabreiraScraper", "Processing event $index")

                    val eventUrl = element.attr("abs:href")
                    Log.d("CabreiraScraper", "Event URL: $eventUrl")

                    if (eventUrl.isBlank()) {
                        Log.d("CabreiraScraper", "Skipping - blank URL")
                        continue
                    }

                    // Get all text from the element and its parents
                    val elementText = element.text()
                    val parentText = element.parent()?.text() ?: ""
                    val grandParentText = element.parent()?.parent()?.text() ?: ""

                    val allText = "$elementText $parentText $grandParentText"
                    Log.d("CabreiraScraper", "Element text: $elementText")
                    Log.d("CabreiraScraper", "All text: ${allText.take(200)}")

                    // Try to extract date and location
                    val (data, local) = parseDateAndLocation(allText)

                    if (data == 0L) {
                        Log.d("CabreiraScraper", "Skipping - no valid date found")
                        continue
                    }

                    Log.d("CabreiraScraper", "Parsed date: $data, location: $local")

                    // Extract event name
                    val img = element.select("img").first()
                    val imgAlt = img?.attr("alt") ?: ""
                    val imgTitle = img?.attr("title") ?: ""
                    val nome = extractEventName(eventUrl, imgAlt, imgTitle, elementText)

                    Log.d("CabreiraScraper", "Event name: $nome")

                    if (nome.isBlank() || !isValidUrl(eventUrl)) {
                        Log.d("CabreiraScraper", "Skipping - invalid name or URL")
                        continue
                    }

                    // Extract image URL
                    val imageUrl = img?.let { imgElement ->
                        val src = imgElement.attr("data-src").ifBlank {
                            imgElement.attr("data-lazy").ifBlank {
                                imgElement.attr("data-original").ifBlank {
                                    imgElement.attr("src")
                                }
                            }
                        }
                        when {
                            src.startsWith("http") -> src
                            src.startsWith("//") -> "https:$src"
                            src.startsWith("/") -> "$baseUrl$src"
                            else -> null
                        }
                    }

                    Log.d("CabreiraScraper", "Event image URL: $imageUrl")

                    val prova = ProvaEntity(
                        id = 0,
                        nome = nome,
                        data = data,
                        local = local.ifBlank { "Portugal" },
                        tipo = detectTipo(nome, allText),
                        distancias = "Ver detalhes",
                        preco = "Consultar organizador",
                        prazoInscricao = calculateDeadline(data),
                        organizador = sourceName,
                        descricao = "Evento organizado por Cabreira Solutions. Consulte o site para mais detalhes.",
                        urlInscricao = eventUrl,
                        source = sourceName,
                        imageUrl = imageUrl
                    )

                    provas.add(prova)
                    Log.d("CabreiraScraper", "✓ Successfully added event: $nome")

                } catch (e: Exception) {
                    Log.e("CabreiraScraper", "Error parsing event ${index}: ${e.message}", e)
                    continue
                }
            }

            // Filter future events
            val today = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val futureProvas = provas.filter { it.data >= today }

            Log.d("CabreiraScraper", "Total scraped: ${provas.size}, Future events: ${futureProvas.size}")

            if (futureProvas.isEmpty()) {
                Log.w("CabreiraScraper", "No future events found")
            } else {
                futureProvas.forEach {
                    Log.d("CabreiraScraper", "Final event: ${it.nome} - ${it.data}")
                }
            }

            Result.success(futureProvas)

        } catch (e: Exception) {
            Log.e("CabreiraScraper", "Error scraping Cabreira: ${e.message}", e)
            e.printStackTrace()
            Result.success(emptyList())
        }
    }

    private fun parseDateAndLocation(text: String): Pair<Long, String> {
        try {
            Log.d("CabreiraScraper", "Parsing date from: ${text.take(200)}")

            val monthsMap = mapOf(
                "JANEIRO" to 1, "FEVEREIRO" to 2, "MARÇO" to 3, "ABRIL" to 4,
                "MAIO" to 5, "JUNHO" to 6, "JULHO" to 7, "AGOSTO" to 8,
                "SETEMBRO" to 9, "OUTUBRO" to 10, "NOVEMBRO" to 11, "DEZEMBRO" to 12,
                "JAN" to 1, "FEV" to 2, "MAR" to 3, "ABR" to 4,
                "MAI" to 5, "JUN" to 6, "JUL" to 7, "AGO" to 8,
                "SET" to 9, "OUT" to 10, "NOV" to 11, "DEZ" to 12
            )

            // Try multiple date formats
            val patterns = listOf(
                // "14 de FEVEREIRO de 2026 Arcos de Valdevez"
                "(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})\\s+(.+)",
                // "14 FEVEREIRO 2026 Arcos de Valdevez"
                "(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+(.+)",
                // "FEVEREIRO 14, 2026 Arcos de Valdevez"
                "(\\w+)\\s+(\\d{1,2}),?\\s+(\\d{4})\\s+(.+)",
                // "14/02/2026 Arcos de Valdevez"
                "(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+(.+)",
                // Just "14 de FEVEREIRO de 2026" without location
                "(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})",
                // "2026-02-14 Arcos de Valdevez"
                "(\\d{4})-(\\d{1,2})-(\\d{1,2})\\s+(.+)"
            )

            for (pattern in patterns) {
                val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(text)

                if (match != null) {
                    Log.d("CabreiraScraper", "Matched pattern: $pattern")
                    Log.d("CabreiraScraper", "Match groups: ${match.groupValues}")

                    try {
                        var day = 0
                        var month = 0
                        var year = 0
                        var local = ""

                        when (patterns.indexOf(pattern)) {
                            0, 1 -> { // Standard format with month name
                                day = match.groupValues[1].toInt()
                                val monthStr = match.groupValues[2].uppercase()
                                year = match.groupValues[3].toInt()
                                local = if (match.groupValues.size > 4) match.groupValues[4].trim() else ""
                                month = monthsMap[monthStr] ?: continue
                            }
                            2 -> { // Month first format
                                val monthStr = match.groupValues[1].uppercase()
                                day = match.groupValues[2].toInt()
                                year = match.groupValues[3].toInt()
                                local = if (match.groupValues.size > 4) match.groupValues[4].trim() else ""
                                month = monthsMap[monthStr] ?: continue
                            }
                            3 -> { // DD/MM/YYYY format
                                day = match.groupValues[1].toInt()
                                month = match.groupValues[2].toInt()
                                year = match.groupValues[3].toInt()
                                local = if (match.groupValues.size > 4) match.groupValues[4].trim() else ""
                            }
                            4 -> { // Standard format without location
                                day = match.groupValues[1].toInt()
                                val monthStr = match.groupValues[2].uppercase()
                                year = match.groupValues[3].toInt()
                                month = monthsMap[monthStr] ?: continue
                                local = ""
                            }
                            5 -> { // YYYY-MM-DD format
                                year = match.groupValues[1].toInt()
                                month = match.groupValues[2].toInt()
                                day = match.groupValues[3].toInt()
                                local = if (match.groupValues.size > 4) match.groupValues[4].trim() else ""
                            }
                        }

                        // Validate date
                        if (day > 0 && day <= 31 && month > 0 && month <= 12 && year >= 2026 && year <= 2030) {
                            val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon"))
                            calendar.set(year, month - 1, day, 0, 0, 0)
                            calendar.set(Calendar.MILLISECOND, 0)

                            // Clean location
                            local = local.split("\\s{2,}".toRegex()).firstOrNull()?.trim() ?: local
                            if (local.length > 50) local = local.take(50)

                            Log.d("CabreiraScraper", "Parsed: $day/$month/$year - $local")
                            return Pair(calendar.timeInMillis, local)
                        }
                    } catch (e: Exception) {
                        Log.e("CabreiraScraper", "Error processing match: ${e.message}")
                        continue
                    }
                }
            }

            Log.w("CabreiraScraper", "No date pattern matched")
            return Pair(0L, "")
        } catch (e: Exception) {
            Log.e("CabreiraScraper", "Error parsing date and location: $text", e)
            return Pair(0L, "")
        }
    }

    private fun extractEventName(url: String, imgAlt: String, imgTitle: String, elementText: String): String {
        // Try image alt text first
        if (imgAlt.isNotBlank() && imgAlt.length > 3 && !imgAlt.contains("logo", ignoreCase = true)) {
            Log.d("CabreiraScraper", "Using image alt: $imgAlt")
            return imgAlt.trim()
        }

        // Try image title
        if (imgTitle.isNotBlank() && imgTitle.length > 3 && !imgTitle.contains("logo", ignoreCase = true)) {
            Log.d("CabreiraScraper", "Using image title: $imgTitle")
            return imgTitle.trim()
        }

        // Try element text (first meaningful line)
        val cleanedText = elementText.trim().lines().firstOrNull {
            it.trim().length > 5 && !it.contains("\\d{4}".toRegex())
        }
        if (cleanedText != null && cleanedText.isNotBlank()) {
            Log.d("CabreiraScraper", "Using element text: $cleanedText")
            return cleanedText.trim()
        }

        // Extract from URL as fallback
        val slug = url.substringAfterLast("/evento/")
            .substringBefore("/")
            .substringBefore("?")
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        Log.d("CabreiraScraper", "Using URL slug: $slug")
        return slug
    }

    private fun detectTipo(nome: String, text: String): String {
        val content = "$nome $text".lowercase()
        return when {
            content.contains("btt") || content.contains("mtb") -> "BTT"
            content.contains("gravel") -> "Gravel"
            content.contains("estrada") || content.contains("road") -> "Estrada"
            content.contains("gran fondo") || content.contains("granfondo") -> "Gran Fondo"
            content.contains("ngps") -> "NGPS"
            else -> "Estrada"
        }
    }

    private fun calculateDeadline(provaDate: Long): Long {
        if (provaDate == 0L) return 0L
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon"))
        calendar.timeInMillis = provaDate
        calendar.add(Calendar.DAY_OF_MONTH, -5)
        return calendar.timeInMillis
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            // Rejeita URLs vazias ou que não começam com http/https
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false
            }

            // Rejeita URLs com domínios inválidos (fpc.pt sem "ciclismo")
            if (url.contains("www.fpc.pt") || (url.contains("fpc.pt/") && !url.contains("fpciclismo.pt"))) {
                Log.d("CabreiraScraper", "Rejected invalid domain URL: $url")
                return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}
