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
 * Scraper for 114 Gravel Race - UCI Gravel World Series.
 * Website: https://114gravelrace.com/
 *
 * Cross-border event: Elvas (Portugal) to Badajoz (Spain)
 * Part of UCI Gravel World Series qualifier events.
 */
@Singleton
class GravelRace114Scraper @Inject constructor() : BaseScraper {

    override val sourceName: String = "114 Gravel Race"
    private val baseUrl = "https://114gravelrace.com"

    override suspend fun scrapeProvas(): Result<List<ProvaEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("GravelRace114Scraper", "Starting scrape from: $baseUrl")
            val provas = mutableListOf<ProvaEntity>()

            val document: Document = Jsoup.connect(baseUrl)
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("GravelRace114Scraper", "Connected successfully. Title: ${document.title()}")

            // Extract event date from the page
            val eventDate = extractEventDate(document)

            if (eventDate == 0L) {
                Log.w("GravelRace114Scraper", "Could not extract event date, using known date")
                // Fallback to known date: March 28, 2026
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))
                calendar.set(2026, Calendar.MARCH, 28, 8, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            val finalDate = if (eventDate != 0L) eventDate else {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))
                calendar.set(2026, Calendar.MARCH, 28, 8, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }

            // Only add if event is in the future
            val today = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (finalDate < today) {
                Log.d("GravelRace114Scraper", "Event is in the past, skipping")
                return@withContext Result.success(emptyList())
            }

            // Extract image URL
            val imageUrl = extractImageUrl(document)

            // Extract distances from page
            val distances = extractDistances(document)

            // Extract price info
            val priceInfo = extractPriceInfo(document)

            // Create the main event
            val prova = ProvaEntity(
                id = 0,
                nome = "114 Gravel Race - UCI Gravel World Series",
                data = finalDate,
                local = "Elvas → Badajoz",
                tipo = "Gravel",
                distancias = distances,
                preco = priceInfo,
                prazoInscricao = calculateDeadline(finalDate),
                organizador = "114 Gravel Race",
                descricao = "Prova transfronteiriça de gravel entre Portugal e Espanha. " +
                        "Parte integrante do UCI Gravel World Series, qualificando ciclistas " +
                        "para o Campeonato do Mundo de Gravel UCI. " +
                        "Partida em Elvas (Portugal), chegada em Badajoz (Espanha). " +
                        "Percursos por estradas de terra e caminhos rurais alentejanos e extremenhos.",
                urlInscricao = "$baseUrl/inscricoes/",
                source = sourceName,
                imageUrl = imageUrl
            )

            provas.add(prova)
            Log.d("GravelRace114Scraper", "✓ Added: ${prova.nome} - ${prova.data}")

            Result.success(provas)

        } catch (e: Exception) {
            Log.e("GravelRace114Scraper", "Error scraping: ${e.message}", e)
            Result.success(emptyList())
        }
    }

    private fun extractEventDate(document: Document): Long {
        try {
            val monthsMap = mapOf(
                "janeiro" to 0, "january" to 0,
                "fevereiro" to 1, "february" to 1,
                "março" to 2, "marco" to 2, "march" to 2,
                "abril" to 3, "april" to 3,
                "maio" to 4, "may" to 4,
                "junho" to 5, "june" to 5,
                "julho" to 6, "july" to 6,
                "agosto" to 7, "august" to 7,
                "setembro" to 8, "september" to 8,
                "outubro" to 9, "october" to 9,
                "novembro" to 10, "november" to 10,
                "dezembro" to 11, "december" to 11
            )

            val pageText = document.body().text().lowercase()

            // Pattern: "28 de março 2026" or "March 28, 2026" or "28/03/2026"
            val patterns = listOf(
                "(\\d{1,2})\\s+(?:de\\s+)?(janeiro|fevereiro|março|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\\s+(\\d{4})".toRegex(RegexOption.IGNORE_CASE),
                "(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(\\d{4})".toRegex(RegexOption.IGNORE_CASE),
                "(\\d{1,2})/(\\d{1,2})/(\\d{4})".toRegex()
            )

            for (pattern in patterns) {
                val match = pattern.find(pageText)
                if (match != null) {
                    try {
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))

                        when (pattern.pattern) {
                            patterns[0].pattern -> {
                                val day = match.groupValues[1].toInt()
                                val month = monthsMap[match.groupValues[2].lowercase()] ?: continue
                                val year = match.groupValues[3].toInt()
                                if (year in 2024..2030) {
                                    calendar.set(year, month, day, 8, 0, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    Log.d("GravelRace114Scraper", "Extracted date (PT): $day/${month + 1}/$year")
                                    return calendar.timeInMillis
                                }
                            }
                            patterns[1].pattern -> {
                                val month = monthsMap[match.groupValues[1].lowercase()] ?: continue
                                val day = match.groupValues[2].toInt()
                                val year = match.groupValues[3].toInt()
                                if (year in 2024..2030) {
                                    calendar.set(year, month, day, 8, 0, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    Log.d("GravelRace114Scraper", "Extracted date (EN): $day/${month + 1}/$year")
                                    return calendar.timeInMillis
                                }
                            }
                            patterns[2].pattern -> {
                                val day = match.groupValues[1].toInt()
                                val month = match.groupValues[2].toInt() - 1
                                val year = match.groupValues[3].toInt()
                                if (year in 2024..2030) {
                                    calendar.set(year, month, day, 8, 0, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    Log.d("GravelRace114Scraper", "Extracted date (numeric): $day/${month + 1}/$year")
                                    return calendar.timeInMillis
                                }
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }

            Log.w("GravelRace114Scraper", "Could not find date in page content")
            return 0L

        } catch (e: Exception) {
            Log.e("GravelRace114Scraper", "Error extracting date: ${e.message}")
            return 0L
        }
    }

    private fun extractImageUrl(document: Document): String? {
        try {
            val imageSelectors = listOf(
                "meta[property=og:image]",
                ".hero img",
                ".banner img",
                "header img",
                ".featured-image img",
                "img[src*=gravel]",
                "img[src*=114]",
                "img[src*=race]"
            )

            // Try meta tag first (og:image)
            val ogImage = document.select("meta[property=og:image]").first()
            if (ogImage != null) {
                val content = ogImage.attr("content")
                if (content.isNotBlank()) {
                    Log.d("GravelRace114Scraper", "Found og:image: $content")
                    return content
                }
            }

            // Try other selectors
            for (selector in imageSelectors) {
                if (selector.startsWith("meta")) continue
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
                        Log.d("GravelRace114Scraper", "Found image: $fullUrl")
                        return fullUrl
                    }
                }
            }

            // Fallback: try to find any large image
            val allImages = document.select("img")
            for (img in allImages) {
                val width = img.attr("width").toIntOrNull() ?: 0
                val height = img.attr("height").toIntOrNull() ?: 0
                val src = img.attr("src")
                if ((width >= 400 || height >= 300) && src.isNotBlank() &&
                    !src.contains("logo", ignoreCase = true) &&
                    !src.contains("icon", ignoreCase = true)) {
                    return if (src.startsWith("http")) src else "$baseUrl$src"
                }
            }

            return null
        } catch (e: Exception) {
            Log.e("GravelRace114Scraper", "Error extracting image: ${e.message}")
            return null
        }
    }

    private fun extractDistances(document: Document): String {
        try {
            val pageText = document.body().text().lowercase()
            val distances = mutableListOf<String>()

            // Look for distance patterns like "114 km", "80km", etc.
            val distancePattern = "(\\d+)\\s*km".toRegex(RegexOption.IGNORE_CASE)
            val matches = distancePattern.findAll(pageText)

            val foundDistances = matches.map { it.groupValues[1].toInt() }
                .filter { it in 30..250 } // Filter reasonable cycling distances
                .distinct()
                .sortedDescending()
                .toList()

            if (foundDistances.isNotEmpty()) {
                foundDistances.forEach { distances.add("${it}km") }
            }

            return if (distances.isNotEmpty()) {
                distances.joinToString(" | ")
            } else {
                // Fallback based on race name (114 km)
                "114km"
            }
        } catch (e: Exception) {
            return "114km"
        }
    }

    private fun extractPriceInfo(document: Document): String {
        try {
            val pageText = document.body().text()

            // Look for price patterns like "€50", "50€", "EUR 50"
            val pricePatterns = listOf(
                "€\\s*(\\d+)".toRegex(),
                "(\\d+)\\s*€".toRegex(),
                "EUR\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
            )

            for (pattern in pricePatterns) {
                val match = pattern.find(pageText)
                if (match != null) {
                    val price = match.groupValues[1]
                    return "€$price"
                }
            }

            return "Consultar site"
        } catch (e: Exception) {
            return "Consultar site"
        }
    }

    private fun calculateDeadline(provaDate: Long): Long {
        if (provaDate == 0L) return 0L
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))
        calendar.timeInMillis = provaDate
        calendar.add(Calendar.DAY_OF_MONTH, -14) // Registration usually closes 2 weeks before
        return calendar.timeInMillis
    }
}
