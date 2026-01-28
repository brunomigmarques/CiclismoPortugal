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
class BikeServiceScraper @Inject constructor() : BaseScraper {

    override val sourceName: String = "BikeService"
    private val baseUrl = "https://bikeservice.pt"

    override suspend fun scrapeProvas(): Result<List<ProvaEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("BikeServiceScraper", "Starting scrape from: $baseUrl/pt/")
            val provas = mutableListOf<ProvaEntity>()

            val document: Document = Jsoup.connect("$baseUrl/pt/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("BikeServiceScraper", "Connected to BikeService website successfully")
            Log.d("BikeServiceScraper", "Page title: ${document.title()}")

            // Seleciona todos os links de eventos
            val eventLinks = document.select("a[href*=/event/]")

            Log.d("BikeServiceScraper", "Found ${eventLinks.size} event links")

            val uniqueEvents = mutableSetOf<String>()

            for ((index, link) in eventLinks.withIndex()) {
                try {
                    Log.d("BikeServiceScraper", "Processing event $index")

                    val eventUrl = link.attr("abs:href")
                    Log.d("BikeServiceScraper", "Event URL: $eventUrl")

                    if (eventUrl.isBlank() || uniqueEvents.contains(eventUrl)) {
                        Log.d("BikeServiceScraper", "Skipping - blank or duplicate URL")
                        continue
                    }

                    uniqueEvents.add(eventUrl)

                    // Extrai informações do card do evento
                    val parent = link.parent()?.parent() ?: continue
                    val grandParent = parent.parent() ?: parent

                    // Nome do evento
                    val nome = link.text().trim().takeIf { it.isNotBlank() }
                        ?: extractEventNameFromUrl(eventUrl)

                    Log.d("BikeServiceScraper", "Event name: $nome")

                    // Procura por informações de data e local em vários lugares
                    val elementText = link.text()
                    val parentText = parent.text()
                    val grandParentText = grandParent.text()
                    val infoText = "$elementText $parentText $grandParentText"

                    Log.d("BikeServiceScraper", "Parsing date from: ${infoText.take(200)}")

                    // Tenta encontrar data no formato "15 março 2026"
                    val (data, locationFromDate) = parseDate(infoText)

                    if (data == 0L) {
                        Log.d("BikeServiceScraper", "Skipping - no valid date found")
                        continue
                    }

                    Log.d("BikeServiceScraper", "Parsed date: $data")

                    // Tenta encontrar local
                    val local = locationFromDate.ifBlank { extractLocation(infoText, nome) }

                    if (nome.isBlank() || !isValidUrl(eventUrl)) {
                        Log.d("BikeServiceScraper", "Skipping - invalid name or URL")
                        continue
                    }

                    val tipo = detectTipo(nome, infoText)

                    // Extract image URL
                    val imageUrl = grandParent.select("img").firstOrNull()?.let { img ->
                        val src = img.attr("data-src").ifBlank {
                            img.attr("data-lazy").ifBlank {
                                img.attr("data-original").ifBlank {
                                    img.attr("src")
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

                    Log.d("BikeServiceScraper", "Event image URL: $imageUrl")

                    val prova = ProvaEntity(
                        id = 0,
                        nome = nome,
                        data = data,
                        local = local.ifBlank { "Portugal" },
                        tipo = tipo,
                        distancias = "Ver detalhes no site",
                        preco = "Consultar organizador",
                        prazoInscricao = calculateDeadline(data),
                        organizador = sourceName,
                        descricao = "Evento organizado pela BikeService. Visite o site para mais informações e inscrições.",
                        urlInscricao = eventUrl,
                        source = sourceName,
                        imageUrl = imageUrl
                    )

                    provas.add(prova)
                    Log.d("BikeServiceScraper", "✓ Successfully added event: $nome - $data")

                } catch (e: Exception) {
                    Log.e("BikeServiceScraper", "Error parsing event ${index}: ${e.message}", e)
                    continue
                }
            }

            // Filtra apenas eventos futuros
            val today = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val futureProvas = provas.filter { it.data >= today }

            Log.d("BikeServiceScraper", "Total scraped: ${provas.size}, Future events: ${futureProvas.size}")

            if (futureProvas.isEmpty()) {
                Log.w("BikeServiceScraper", "No future events found")
            } else {
                futureProvas.forEach {
                    Log.d("BikeServiceScraper", "Final event: ${it.nome} - ${it.data}")
                }
            }

            Result.success(futureProvas)

        } catch (e: Exception) {
            Log.e("BikeServiceScraper", "Error scraping BikeService: ${e.message}", e)
            e.printStackTrace()
            Result.success(emptyList())
        }
    }

    private fun parseDate(text: String): Pair<Long, String> {
        try {
            val monthsMap = mapOf(
                "janeiro" to 1, "fevereiro" to 2, "março" to 3, "abril" to 4,
                "maio" to 5, "junho" to 6, "julho" to 7, "agosto" to 8,
                "setembro" to 9, "outubro" to 10, "novembro" to 11, "dezembro" to 12,
                "jan" to 1, "fev" to 2, "mar" to 3, "abr" to 4,
                "mai" to 5, "jun" to 6, "jul" to 7, "ago" to 8,
                "set" to 9, "out" to 10, "nov" to 11, "dez" to 12
            )

            // Try multiple date formats (similar to CabreiraScraper)
            val patterns = listOf(
                // "15 de março de 2026 Porto"
                "(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})\\s+(.+)",
                // "15 março 2026 Porto"
                "(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+(.+)",
                // "março 15, 2026 Porto"
                "(\\w+)\\s+(\\d{1,2}),?\\s+(\\d{4})\\s+(.+)",
                // "15/03/2026 Porto"
                "(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+(.+)",
                // Just "15 de março de 2026" without location
                "(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})",
                // Just "15 março 2026" without location
                "(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})",
                // "2026-03-15 Porto"
                "(\\d{4})-(\\d{1,2})-(\\d{1,2})\\s+(.+)",
                // Just "2026-03-15"
                "(\\d{4})-(\\d{1,2})-(\\d{1,2})"
            )

            for (pattern in patterns) {
                val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(text)

                if (match != null) {
                    Log.d("BikeServiceScraper", "Matched pattern: $pattern")
                    Log.d("BikeServiceScraper", "Match groups: ${match.groupValues}")

                    try {
                        var day = 0
                        var month = 0
                        var year = 0
                        var local = ""

                        when (patterns.indexOf(pattern)) {
                            0, 1 -> { // Standard format with month name
                                day = match.groupValues[1].toInt()
                                val monthStr = match.groupValues[2].lowercase()
                                year = match.groupValues[3].toInt()
                                local = if (match.groupValues.size > 4) match.groupValues[4].trim() else ""
                                month = monthsMap[monthStr] ?: continue
                            }
                            2 -> { // Month first format
                                val monthStr = match.groupValues[1].lowercase()
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
                            4, 5 -> { // Standard format without location
                                day = match.groupValues[1].toInt()
                                val monthStr = match.groupValues[2].lowercase()
                                year = match.groupValues[3].toInt()
                                month = monthsMap[monthStr] ?: continue
                                local = ""
                            }
                            6 -> { // YYYY-MM-DD with location
                                year = match.groupValues[1].toInt()
                                month = match.groupValues[2].toInt()
                                day = match.groupValues[3].toInt()
                                local = if (match.groupValues.size > 4) match.groupValues[4].trim() else ""
                            }
                            7 -> { // YYYY-MM-DD without location
                                year = match.groupValues[1].toInt()
                                month = match.groupValues[2].toInt()
                                day = match.groupValues[3].toInt()
                                local = ""
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

                            Log.d("BikeServiceScraper", "Parsed: $day/$month/$year - $local")
                            return Pair(calendar.timeInMillis, local)
                        }
                    } catch (e: Exception) {
                        Log.e("BikeServiceScraper", "Error processing match: ${e.message}")
                        continue
                    }
                }
            }

            Log.w("BikeServiceScraper", "No date pattern matched")
            return Pair(0L, "")
        } catch (e: Exception) {
            Log.e("BikeServiceScraper", "Error parsing date: $text", e)
            return Pair(0L, "")
        }
    }

    private fun extractLocation(text: String, eventName: String): String {
        // Locais comuns baseados nos nomes dos eventos
        return when {
            eventName.contains("Viana", ignoreCase = true) -> "Viana do Castelo"
            eventName.contains("Douro", ignoreCase = true) -> "Vale do Douro"
            eventName.contains("Porto", ignoreCase = true) -> "Porto"
            eventName.contains("Lisboa", ignoreCase = true) -> "Lisboa"
            eventName.contains("Algarve", ignoreCase = true) -> "Algarve"
            text.contains("Portugal", ignoreCase = true) -> "Portugal"
            else -> "Portugal"
        }
    }

    private fun extractEventNameFromUrl(url: String): String {
        return url.substringAfterLast("/event/")
            .substringBefore("/")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun detectTipo(nome: String, text: String): String {
        val content = "$nome $text".lowercase()
        return when {
            content.contains("granfondo") || content.contains("gran fondo") -> "Gran Fondo"
            content.contains("btt") || content.contains("mtb") -> "BTT"
            content.contains("gravel") -> "Gravel"
            content.contains("estrada") || content.contains("road") -> "Estrada"
            content.contains("atletismo") -> "Atletismo"
            else -> "Gran Fondo" // BikeService é principalmente GranFondos
        }
    }

    private fun calculateDeadline(provaDate: Long): Long {
        if (provaDate == 0L) return 0L
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon"))
        calendar.timeInMillis = provaDate
        calendar.add(Calendar.DAY_OF_MONTH, -7)
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
                Log.d("BikeServiceScraper", "Rejected invalid domain URL: $url")
                return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}
