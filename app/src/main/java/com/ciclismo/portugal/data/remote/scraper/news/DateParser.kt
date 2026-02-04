package com.ciclismo.portugal.data.remote.scraper.news

import android.util.Log
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for parsing Portuguese article dates from HTML elements.
 */
object DateParser {
    private const val TAG = "DateParser"

    private val portugueseMonths = mapOf(
        "janeiro" to 0, "jan" to 0,
        "fevereiro" to 1, "fev" to 1,
        "março" to 2, "marco" to 2, "mar" to 2,
        "abril" to 3, "abr" to 3,
        "maio" to 4, "mai" to 4,
        "junho" to 5, "jun" to 5,
        "julho" to 6, "jul" to 6,
        "agosto" to 7, "ago" to 7,
        "setembro" to 8, "set" to 8,
        "outubro" to 9, "out" to 9,
        "novembro" to 10, "nov" to 10,
        "dezembro" to 11, "dez" to 11
    )

    /**
     * Try to extract publication date from an article element.
     * Looks for time tags, date classes, meta tags, and text patterns.
     */
    fun extractDate(element: Element): Long? {
        // 1. Try time element with datetime attribute
        element.select("time[datetime]").firstOrNull()?.let { time ->
            val datetime = time.attr("datetime")
            parseIsoDate(datetime)?.let { return it }
        }

        // 2. Try common date class selectors
        val dateSelectors = listOf(
            ".date", ".data", ".time", ".timestamp",
            "[class*=date]", "[class*=time]",
            ".published", ".pub-date", ".post-date"
        )
        for (selector in dateSelectors) {
            element.select(selector).firstOrNull()?.let { dateEl ->
                // Check datetime attribute first
                dateEl.attr("datetime").takeIf { it.isNotBlank() }?.let { dt ->
                    parseIsoDate(dt)?.let { return it }
                }
                // Try to parse text content
                parsePortugueseDate(dateEl.text())?.let { return it }
            }
        }

        // 3. Try to find date pattern in element text
        val elementText = element.text()
        parsePortugueseDateFromText(elementText)?.let { return it }

        return null
    }

    /**
     * Try to extract date from article page URL.
     * Portuguese news sites use various URL patterns:
     * - /2026/02/01/article-title (JN, Record, etc.)
     * - /2026-02-01-article-title
     * - /noticia-YYYYMMDD-titulo
     * - /titulo-artigo-20260201
     */
    fun extractDateFromUrl(url: String): Long? {
        // Pattern 1: /YYYY/MM/DD/ (most common - JN, Record, etc.)
        val pattern1 = Regex("""/(\d{4})/(\d{2})/(\d{2})/""")
        pattern1.find(url)?.let { match ->
            parseYearMonthDay(match.groupValues[1], match.groupValues[2], match.groupValues[3])?.let { return it }
        }

        // Pattern 2: -YYYY-MM-DD- or /YYYY-MM-DD/
        val pattern2 = Regex("""[-/](20\d{2})-(\d{2})-(\d{2})[-/]""")
        pattern2.find(url)?.let { match ->
            parseYearMonthDay(match.groupValues[1], match.groupValues[2], match.groupValues[3])?.let { return it }
        }

        // Pattern 3: YYYYMMDD in URL (compact format, 8 digits starting with 202x)
        val pattern3 = Regex("""[-/_](202\d)(\d{2})(\d{2})[-/_.]""")
        pattern3.find(url)?.let { match ->
            parseYearMonthDay(match.groupValues[1], match.groupValues[2], match.groupValues[3])?.let { return it }
        }

        // Pattern 4: -DDMMYYYY- (European format)
        val pattern4 = Regex("""[-/](\d{2})(\d{2})(202\d)[-/]""")
        pattern4.find(url)?.let { match ->
            val day = match.groupValues[1]
            val month = match.groupValues[2]
            val year = match.groupValues[3]
            // Only accept if day is valid (01-31)
            if (day.toIntOrNull() in 1..31) {
                parseYearMonthDay(year, month, day)?.let { return it }
            }
        }

        // Pattern 5: titulo-YYYYMMDD at end of URL (before extension or end)
        val pattern5 = Regex("""-(202\d)(\d{2})(\d{2})(?:[./]|$)""")
        pattern5.find(url)?.let { match ->
            parseYearMonthDay(match.groupValues[1], match.groupValues[2], match.groupValues[3])?.let { return it }
        }

        Log.d(TAG, "Could not extract date from URL: $url")
        return null
    }

    private fun parseYearMonthDay(yearStr: String, monthStr: String, dayStr: String): Long? {
        return try {
            val year = yearStr.toInt()
            val month = monthStr.toInt() - 1
            val day = dayStr.toInt()

            if (year in 2020..2030 && month in 0..11 && day in 1..31) {
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, 12, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse date: $yearStr-$monthStr-$dayStr")
            null
        }
    }

    /**
     * Parse ISO 8601 date format (e.g., 2026-02-01T10:30:00Z)
     */
    fun parseIsoDate(dateString: String): Long? {
        if (dateString.isBlank()) return null

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        for (format in formats) {
            try {
                format.isLenient = false
                return format.parse(dateString.trim())?.time
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }

    /**
     * Parse Portuguese date format (e.g., "1 de Fevereiro de 2026", "01/02/2026")
     */
    fun parsePortugueseDate(dateString: String): Long? {
        if (dateString.isBlank()) return null

        val cleanedDate = dateString.trim().lowercase()

        // Try numeric formats first
        val numericFormats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT")),
            SimpleDateFormat("dd-MM-yyyy", Locale("pt", "PT")),
            SimpleDateFormat("dd.MM.yyyy", Locale("pt", "PT")),
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT")),
            SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "PT"))
        )

        for (format in numericFormats) {
            try {
                format.isLenient = false
                return format.parse(dateString.trim())?.time
            } catch (e: Exception) {
                // Try next format
            }
        }

        // Try Portuguese text format: "1 de Fevereiro de 2026"
        val textPattern = Regex("""(\d{1,2})\s*(?:de\s+)?(\w+)\s*(?:de\s+)?(\d{4})""")
        textPattern.find(cleanedDate)?.let { match ->
            try {
                val day = match.groupValues[1].toInt()
                val monthName = match.groupValues[2].lowercase()
                val year = match.groupValues[3].toInt()

                val month = portugueseMonths[monthName]
                if (month != null && year in 2020..2030 && day in 1..31) {
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, day, 12, 0, 0)
                    return calendar.timeInMillis
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to parse Portuguese date: $dateString")
            }
        }

        return null
    }

    /**
     * Try to find and parse a date from free text.
     */
    private fun parsePortugueseDateFromText(text: String): Long? {
        if (text.isBlank()) return null

        val cleanText = text.lowercase()

        // Look for patterns like "1 de Fevereiro de 2026" or "01/02/2026"
        val patterns = listOf(
            Regex("""(\d{1,2})\s*de\s*(\w+)\s*de\s*(\d{4})"""),
            Regex("""(\d{1,2})/(\d{1,2})/(\d{4})"""),
            Regex("""(\d{1,2})-(\d{1,2})-(\d{4})""")
        )

        for (pattern in patterns) {
            pattern.find(cleanText)?.let { match ->
                when (pattern.pattern) {
                    patterns[0].pattern -> {
                        try {
                            val day = match.groupValues[1].toInt()
                            val monthName = match.groupValues[2]
                            val year = match.groupValues[3].toInt()

                            val month = portugueseMonths[monthName]
                            if (month != null && year in 2020..2030 && day in 1..31) {
                                val calendar = Calendar.getInstance()
                                calendar.set(year, month, day, 12, 0, 0)
                                return calendar.timeInMillis
                            }
                        } catch (e: Exception) {
                            // Continue
                        }
                    }
                    else -> {
                        try {
                            val day = match.groupValues[1].toInt()
                            val month = match.groupValues[2].toInt() - 1
                            val year = match.groupValues[3].toInt()

                            if (year in 2020..2030 && month in 0..11 && day in 1..31) {
                                val calendar = Calendar.getInstance()
                                calendar.set(year, month, day, 12, 0, 0)
                                return calendar.timeInMillis
                            }
                        } catch (e: Exception) {
                            // Continue
                        }
                    }
                }
            }
        }

        return null
    }
}
