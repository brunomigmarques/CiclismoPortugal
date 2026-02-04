package com.ciclismo.portugal.data.remote.scraper

import android.util.Log
import com.ciclismo.portugal.domain.model.Prova
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic scraper that attempts to extract race information from any cycling race website.
 * Used by admin to manually add new races by providing a URL.
 */
@Singleton
class GenericRaceScraper @Inject constructor() {

    companion object {
        private const val TAG = "GenericRaceScraper"
    }

    /**
     * Extracts race info from a URL.
     * Returns a ProvaEntity with extracted data that admin can review/edit before saving.
     */
    suspend fun scrapeFromUrl(url: String): Result<ScrapedRaceData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Scraping URL: $url")

            val document: Document = Jsoup.connect(url)
                .timeout(20000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .followRedirects(true)
                .get()

            Log.d(TAG, "Connected. Title: ${document.title()}")

            val parsedUrl = URL(url)
            val domain = parsedUrl.host.removePrefix("www.")

            val scrapedData = ScrapedRaceData(
                nome = extractRaceName(document, domain),
                data = extractEventDate(document),
                local = extractLocation(document),
                tipo = detectRaceType(document, url),
                distancias = extractDistances(document),
                preco = extractPrice(document),
                organizador = extractOrganizer(document, domain),
                descricao = extractDescription(document),
                urlInscricao = findRegistrationUrl(document, url),
                imageUrl = extractMainImage(document, url),
                sourceUrl = url
            )

            Log.d(TAG, "Scraped: ${scrapedData.nome}")
            Result.success(scrapedData)

        } catch (e: Exception) {
            Log.e(TAG, "Error scraping $url: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractRaceName(document: Document, domain: String): String {
        // Try various approaches
        val candidates = mutableListOf<String>()

        // 1. Open Graph title
        document.select("meta[property=og:title]").firstOrNull()?.attr("content")
            ?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }

        // 2. Page title (cleaned)
        document.title()
            ?.replace(" - ", "|")
            ?.replace(" | ", "|")
            ?.split("|")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.length > 3 }
            ?.let { candidates.add(it) }

        // 3. H1 heading
        document.select("h1").firstOrNull()?.text()
            ?.takeIf { it.isNotBlank() && it.length < 100 }
            ?.let { candidates.add(it) }

        // 4. Logo alt text
        document.select("img[alt*=logo], .logo img, #logo img").firstOrNull()?.attr("alt")
            ?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }

        // Return the best candidate (prefer OG title or H1)
        return candidates.firstOrNull() ?: domain.replace(".", " ").replaceFirstChar { it.uppercase() }
    }

    private fun extractEventDate(document: Document): Long {
        val monthsMap = mapOf(
            "janeiro" to 0, "january" to 0, "jan" to 0,
            "fevereiro" to 1, "february" to 1, "feb" to 1,
            "março" to 2, "marco" to 2, "march" to 2, "mar" to 2,
            "abril" to 3, "april" to 3, "apr" to 3,
            "maio" to 4, "may" to 4,
            "junho" to 5, "june" to 5, "jun" to 5,
            "julho" to 6, "july" to 6, "jul" to 6,
            "agosto" to 7, "august" to 7, "aug" to 7,
            "setembro" to 8, "september" to 8, "sep" to 8, "sept" to 8,
            "outubro" to 9, "october" to 9, "oct" to 9,
            "novembro" to 10, "november" to 10, "nov" to 10,
            "dezembro" to 11, "december" to 11, "dec" to 11
        )

        val pageText = document.body().text().lowercase()

        // Pattern variations
        val patterns = listOf(
            // "28 de março de 2026" or "28 março 2026"
            "(\\d{1,2})\\s+(?:de\\s+)?(janeiro|fevereiro|março|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\\s+(?:de\\s+)?(\\d{4})".toRegex(RegexOption.IGNORE_CASE),
            // "March 28, 2026" or "March 28th 2026"
            "(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(\\d{4})".toRegex(RegexOption.IGNORE_CASE),
            // "28 Mar 2026"
            "(\\d{1,2})\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\\s+(\\d{4})".toRegex(RegexOption.IGNORE_CASE),
            // "2026-03-28" ISO format
            "(\\d{4})-(\\d{2})-(\\d{2})".toRegex(),
            // "28/03/2026" or "28-03-2026"
            "(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})".toRegex()
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(pageText)
            if (match != null) {
                try {
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))

                    when (index) {
                        0 -> { // Portuguese format
                            val day = match.groupValues[1].toInt()
                            val month = monthsMap[match.groupValues[2].lowercase()] ?: continue
                            val year = match.groupValues[3].toInt()
                            if (year in 2024..2030) {
                                calendar.set(year, month, day, 8, 0, 0)
                                return calendar.timeInMillis
                            }
                        }
                        1 -> { // English format "March 28, 2026"
                            val month = monthsMap[match.groupValues[1].lowercase()] ?: continue
                            val day = match.groupValues[2].toInt()
                            val year = match.groupValues[3].toInt()
                            if (year in 2024..2030) {
                                calendar.set(year, month, day, 8, 0, 0)
                                return calendar.timeInMillis
                            }
                        }
                        2 -> { // Short month "28 Mar 2026"
                            val day = match.groupValues[1].toInt()
                            val month = monthsMap[match.groupValues[2].lowercase()] ?: continue
                            val year = match.groupValues[3].toInt()
                            if (year in 2024..2030) {
                                calendar.set(year, month, day, 8, 0, 0)
                                return calendar.timeInMillis
                            }
                        }
                        3 -> { // ISO format "2026-03-28"
                            val year = match.groupValues[1].toInt()
                            val month = match.groupValues[2].toInt() - 1
                            val day = match.groupValues[3].toInt()
                            if (year in 2024..2030) {
                                calendar.set(year, month, day, 8, 0, 0)
                                return calendar.timeInMillis
                            }
                        }
                        4 -> { // Numeric "28/03/2026"
                            val day = match.groupValues[1].toInt()
                            val month = match.groupValues[2].toInt() - 1
                            val year = match.groupValues[3].toInt()
                            if (year in 2024..2030 && day in 1..31 && month in 0..11) {
                                calendar.set(year, month, day, 8, 0, 0)
                                return calendar.timeInMillis
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }

        return 0L
    }

    private fun extractLocation(document: Document): String {
        // Try meta location
        document.select("meta[name=geo.placename]").firstOrNull()?.attr("content")
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // Look for common location patterns in text
        val pageText = document.body().text()

        // Portuguese cities/regions commonly mentioned
        val portugueseLocations = listOf(
            "Lisboa", "Porto", "Sintra", "Cascais", "Setúbal", "Leiria", "Coimbra",
            "Aveiro", "Braga", "Guimarães", "Viseu", "Guarda", "Beja", "Évora",
            "Faro", "Portimão", "Albufeira", "Elvas", "Badajoz", "Serra da Estrela",
            "Alentejo", "Algarve", "Douro", "Minho", "Trás-os-Montes"
        )

        for (location in portugueseLocations) {
            if (pageText.contains(location, ignoreCase = true)) {
                return location
            }
        }

        // Try address-like elements
        document.select("[class*=location], [class*=address], address").firstOrNull()?.text()
            ?.takeIf { it.isNotBlank() && it.length < 100 }
            ?.let { return it }

        return "Portugal"
    }

    private fun detectRaceType(document: Document, url: String): String {
        val textLower = (document.body().text() + " " + url).lowercase()

        return when {
            textLower.contains("gravel") -> "Gravel"
            textLower.contains("btt") || textLower.contains("mountain bike") ||
                    textLower.contains("mtb") || textLower.contains("xco") ||
                    textLower.contains("xc") -> "BTT"
            textLower.contains("estrada") || textLower.contains("road") ||
                    textLower.contains("granfondo") || textLower.contains("fondo") -> "Estrada"
            textLower.contains("cicloturismo") || textLower.contains("passeio") -> "Cicloturismo"
            textLower.contains("cx") || textLower.contains("ciclocross") -> "Ciclocrosse"
            else -> "Estrada" // Default
        }
    }

    private fun extractDistances(document: Document): String {
        val pageText = document.body().text()
        val distances = mutableSetOf<Int>()

        // Find all distance patterns
        val distancePattern = "(\\d+)\\s*(?:km|quilómetros|quilometros|kilometers)".toRegex(RegexOption.IGNORE_CASE)
        distancePattern.findAll(pageText).forEach {
            val km = it.groupValues[1].toIntOrNull()
            if (km != null && km in 15..400) {
                distances.add(km)
            }
        }

        return if (distances.isNotEmpty()) {
            distances.sortedDescending().joinToString(" | ") { "${it}km" }
        } else {
            "Consultar site"
        }
    }

    private fun extractPrice(document: Document): String {
        val pageText = document.body().text()

        val pricePatterns = listOf(
            "€\\s*(\\d+(?:[.,]\\d{2})?)".toRegex(),
            "(\\d+(?:[.,]\\d{2})?)\\s*€".toRegex(),
            "EUR\\s*(\\d+(?:[.,]\\d{2})?)".toRegex(RegexOption.IGNORE_CASE),
            "(\\d+(?:[.,]\\d{2})?)\\s*euros?".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in pricePatterns) {
            val match = pattern.find(pageText)
            if (match != null) {
                val price = match.groupValues[1].replace(",", ".")
                return "€$price"
            }
        }

        return "Consultar site"
    }

    private fun extractOrganizer(document: Document, domain: String): String {
        // Try meta author
        document.select("meta[name=author]").firstOrNull()?.attr("content")
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // Try footer/copyright
        document.select("footer, .footer, [class*=copyright]").firstOrNull()?.text()
            ?.takeIf { it.length < 50 }
            ?.let { return it }

        // Fallback to domain
        return domain.split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Organizador"
    }

    private fun extractDescription(document: Document): String {
        // Try meta description
        val metaDesc = document.select("meta[name=description], meta[property=og:description]")
            .firstOrNull()?.attr("content")

        if (!metaDesc.isNullOrBlank() && metaDesc.length > 20) {
            return metaDesc.take(500)
        }

        // Try first paragraph
        val firstP = document.select("article p, .content p, main p, .description, .about").firstOrNull()?.text()
        if (!firstP.isNullOrBlank() && firstP.length > 20) {
            return firstP.take(500)
        }

        return ""
    }

    private fun findRegistrationUrl(document: Document, baseUrl: String): String {
        val registrationKeywords = listOf(
            "inscricao", "inscricoes", "inscrever", "register", "registration",
            "signup", "sign-up", "participar", "entrada", "bilhete"
        )

        for (keyword in registrationKeywords) {
            val link = document.select("a[href*=$keyword]").firstOrNull()
            if (link != null) {
                val href = link.attr("href")
                return when {
                    href.startsWith("http") -> href
                    href.startsWith("//") -> "https:$href"
                    href.startsWith("/") -> baseUrl.substringBefore("/", baseUrl) + href
                    else -> "$baseUrl/$href"
                }
            }
        }

        return baseUrl
    }

    private fun extractMainImage(document: Document, baseUrl: String): String? {
        // Try OG image first (usually best quality)
        document.select("meta[property=og:image]").firstOrNull()?.attr("content")
            ?.takeIf { it.isNotBlank() }?.let { return normalizeUrl(it, baseUrl) }

        // Try Twitter image
        document.select("meta[name=twitter:image]").firstOrNull()?.attr("content")
            ?.takeIf { it.isNotBlank() }?.let { return normalizeUrl(it, baseUrl) }

        // Try hero/banner image
        val heroSelectors = listOf(
            ".hero img", ".banner img", "header img", ".featured-image img",
            ".main-image img", "[class*=hero] img", "[class*=banner] img"
        )

        for (selector in heroSelectors) {
            document.select(selector).firstOrNull()?.let { img ->
                val src = img.attr("data-src").ifBlank { img.attr("src") }
                if (src.isNotBlank()) {
                    return normalizeUrl(src, baseUrl)
                }
            }
        }

        // Fallback: find largest image
        val largeImage = document.select("img")
            .filter { img ->
                val width = img.attr("width").toIntOrNull() ?: 0
                val height = img.attr("height").toIntOrNull() ?: 0
                val src = img.attr("src")
                (width >= 300 || height >= 200) &&
                        !src.contains("logo", ignoreCase = true) &&
                        !src.contains("icon", ignoreCase = true) &&
                        !src.contains("avatar", ignoreCase = true)
            }
            .firstOrNull()

        largeImage?.let {
            val src = it.attr("data-src").ifBlank { it.attr("src") }
            if (src.isNotBlank()) {
                return normalizeUrl(src, baseUrl)
            }
        }

        return null
    }

    private fun normalizeUrl(url: String, baseUrl: String): String {
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> {
                val parsedBase = URL(baseUrl)
                "${parsedBase.protocol}://${parsedBase.host}$url"
            }
            else -> "$baseUrl/$url"
        }
    }
}

/**
 * Data class holding scraped race information for admin review.
 */
data class ScrapedRaceData(
    val nome: String,
    val data: Long,
    val local: String,
    val tipo: String,
    val distancias: String,
    val preco: String,
    val organizador: String,
    val descricao: String,
    val urlInscricao: String,
    val imageUrl: String?,
    val sourceUrl: String
) {
    fun toDomain(): Prova {
        return Prova(
            id = 0,
            nome = nome,
            data = data,
            local = local,
            tipo = tipo,
            distancias = distancias,
            preco = preco,
            prazoInscricao = if (data > 0) data - (14 * 24 * 60 * 60 * 1000L) else null,
            organizador = organizador,
            descricao = descricao,
            urlInscricao = urlInscricao,
            source = "Manual - $sourceUrl",
            imageUrl = imageUrl
        )
    }
}
