package com.ciclismo.portugal.data.remote.scraper

import android.util.Log
import com.ciclismo.portugal.data.local.entity.ProvaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scraper for Gran Fondo Serra da Estrela events.
 * Website: https://granfondoserradaestrela.com/
 *
 * Typically features:
 * - Granfondo (long distance ~174km)
 * - Mediofondo (medium distance ~97km)
 */
@Singleton
class GranFondoSerraEstrelaScraper @Inject constructor() : BaseScraper {

    override val sourceName: String = "Gran Fondo Serra da Estrela"
    private val baseUrl = "https://granfondoserradaestrela.com"

    override suspend fun scrapeProvas(): Result<List<ProvaEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("GFSerraEstrelaScraper", "Starting scrape from: $baseUrl")
            val provas = mutableListOf<ProvaEntity>()

            val document: Document = Jsoup.connect(baseUrl)
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("GFSerraEstrelaScraper", "Connected successfully. Title: ${document.title()}")

            // Extract event date from the page
            // Look for date patterns like "28 de Junho 2026"
            val eventDate = extractEventDate(document)

            if (eventDate == 0L) {
                Log.w("GFSerraEstrelaScraper", "Could not extract event date")
                return@withContext Result.success(emptyList())
            }

            // Only add if event is in the future
            val today = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (eventDate < today) {
                Log.d("GFSerraEstrelaScraper", "Event is in the past, skipping")
                return@withContext Result.success(emptyList())
            }

            // Extract image URL
            val imageUrl = extractImageUrl(document)

            // Extract distances from stage info
            val distances = extractDistances(document)

            // Create the main event
            val prova = ProvaEntity(
                id = 0,
                nome = "Gran Fondo Serra da Estrela",
                data = eventDate,
                local = "Serra da Estrela",
                tipo = "Estrada", // Road cycling event
                distancias = distances,
                preco = "Consultar site",
                prazoInscricao = calculateDeadline(eventDate),
                organizador = "Streamplan",
                descricao = "Evento de ciclismo de estrada na Serra da Estrela. " +
                        "Percursos Granfondo (174km, 4600m D+) e Mediofondo (97km, 2700m D+). " +
                        "Um dos eventos mais desafiantes de ciclismo em Portugal.",
                urlInscricao = baseUrl,
                source = sourceName,
                imageUrl = imageUrl
            )

            provas.add(prova)
            Log.d("GFSerraEstrelaScraper", "✓ Added: ${prova.nome} - ${prova.data}")

            Result.success(provas)

        } catch (e: Exception) {
            Log.e("GFSerraEstrelaScraper", "Error scraping: ${e.message}", e)
            Result.success(emptyList())
        }
    }

    private fun extractEventDate(document: Document): Long {
        try {
            // Try multiple approaches to find the date

            // 1. Look for date in slider/banner text
            val dateSelectors = listOf(
                ".sliderItemDateText",
                ".date",
                ".event-date",
                "[class*=date]",
                "h1", "h2", "h3"
            )

            val monthsMap = mapOf(
                "janeiro" to 0, "fevereiro" to 1, "março" to 2, "marco" to 2,
                "abril" to 3, "maio" to 4, "junho" to 5,
                "julho" to 6, "agosto" to 7, "setembro" to 8,
                "outubro" to 9, "novembro" to 10, "dezembro" to 11
            )

            // Get all text content
            val pageText = document.body().text().lowercase()

            // Pattern: "28 de junho 2026" or "28 junho 2026"
            val datePattern = "(\\d{1,2})\\s+(?:de\\s+)?(janeiro|fevereiro|março|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\\s+(\\d{4})".toRegex(RegexOption.IGNORE_CASE)

            val match = datePattern.find(pageText)
            if (match != null) {
                val day = match.groupValues[1].toInt()
                val monthName = match.groupValues[2].lowercase()
                val year = match.groupValues[3].toInt()

                val month = monthsMap[monthName]
                if (month != null && year >= 2024 && year <= 2030 && day in 1..31) {
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))
                    calendar.set(year, month, day, 8, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    Log.d("GFSerraEstrelaScraper", "Extracted date: $day/${month + 1}/$year")
                    return calendar.timeInMillis
                }
            }

            // 2. Try to find countdown script values
            val scripts = document.select("script")
            for (script in scripts) {
                val scriptContent = script.html()
                // Look for countdown initialization with year, month, day values
                val countdownPattern = "new Date\\s*\\(\\s*(\\d{4})\\s*,\\s*(\\d{1,2})\\s*,\\s*(\\d{1,2})".toRegex()
                val countdownMatch = countdownPattern.find(scriptContent)
                if (countdownMatch != null) {
                    val year = countdownMatch.groupValues[1].toInt()
                    val month = countdownMatch.groupValues[2].toInt() // JavaScript months are 0-indexed
                    val day = countdownMatch.groupValues[3].toInt()

                    if (year >= 2024 && year <= 2030) {
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))
                        calendar.set(year, month, day, 8, 0, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        Log.d("GFSerraEstrelaScraper", "Extracted date from countdown: $day/${month + 1}/$year")
                        return calendar.timeInMillis
                    }
                }
            }

            Log.w("GFSerraEstrelaScraper", "Could not find date in page content")
            return 0L

        } catch (e: Exception) {
            Log.e("GFSerraEstrelaScraper", "Error extracting date: ${e.message}")
            return 0L
        }
    }

    private fun extractImageUrl(document: Document): String? {
        try {
            // Look for event banner/header image
            val imageSelectors = listOf(
                ".sliderItem img",
                ".banner img",
                ".hero img",
                "header img",
                ".main-image img",
                "img[src*=granfondo]",
                "img[src*=serra]"
            )

            for (selector in imageSelectors) {
                val img = document.select(selector).first()
                if (img != null) {
                    val src = img.attr("data-src").ifBlank {
                        img.attr("data-lazy").ifBlank {
                            img.attr("src")
                        }
                    }

                    if (src.isNotBlank()) {
                        val fullUrl = when {
                            src.startsWith("http") -> src
                            src.startsWith("//") -> "https:$src"
                            src.startsWith("/") -> "$baseUrl$src"
                            else -> "$baseUrl/$src"
                        }
                        Log.d("GFSerraEstrelaScraper", "Found image: $fullUrl")
                        return fullUrl
                    }
                }
            }

            // Fallback: try to find any large image
            val allImages = document.select("img")
            for (img in allImages) {
                val width = img.attr("width").toIntOrNull() ?: 0
                val height = img.attr("height").toIntOrNull() ?: 0
                if (width >= 400 || height >= 300) {
                    val src = img.attr("src")
                    if (src.isNotBlank() && !src.contains("logo", ignoreCase = true)) {
                        return if (src.startsWith("http")) src else "$baseUrl$src"
                    }
                }
            }

            return null
        } catch (e: Exception) {
            Log.e("GFSerraEstrelaScraper", "Error extracting image: ${e.message}")
            return null
        }
    }

    private fun extractDistances(document: Document): String {
        try {
            val pageText = document.body().text()

            // Look for distance patterns like "174 km" and "97 km"
            val distances = mutableListOf<String>()

            // Granfondo
            if (pageText.contains("granfondo", ignoreCase = true) ||
                pageText.contains("174", ignoreCase = true)) {
                distances.add("Granfondo: 174km")
            }

            // Mediofondo
            if (pageText.contains("mediofondo", ignoreCase = true) ||
                pageText.contains("97", ignoreCase = true)) {
                distances.add("Mediofondo: 97km")
            }

            return if (distances.isNotEmpty()) {
                distances.joinToString(" | ")
            } else {
                "Granfondo: 174km | Mediofondo: 97km"
            }
        } catch (e: Exception) {
            return "Granfondo: 174km | Mediofondo: 97km"
        }
    }

    private fun calculateDeadline(provaDate: Long): Long {
        if (provaDate == 0L) return 0L
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))
        calendar.timeInMillis = provaDate
        calendar.add(Calendar.DAY_OF_MONTH, -7) // Registration usually closes 1 week before
        return calendar.timeInMillis
    }
}
