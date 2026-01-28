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
class FPCScraper @Inject constructor() : BaseScraper {

    override val sourceName: String = "FPC"
    private val baseUrl = "https://www.fpciclismo.pt"

    override suspend fun scrapeProvas(): Result<List<ProvaEntity>> = withContext(Dispatchers.IO) {
        try {
            val provas = mutableListOf<ProvaEntity>()

            // Scrape do calendário
            val document: Document = Jsoup.connect("$baseUrl/calendario")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("FPCScraper", "Connected to FPC website successfully")

            // Seleciona todas as linhas da tabela de provas
            val rows = document.select("div.row.eventos-row")

            Log.d("FPCScraper", "Found ${rows.size} event rows")

            for (row in rows) {
                try {
                    // Extrai data
                    val dateText = row.select("div.col-md-1, div[class*=col]").first()?.text()?.trim() ?: continue
                    val dateRange = parseDate(dateText)

                    // Extrai nome da prova
                    val nome = row.select("strong, b").first()?.text()?.trim() ?: continue

                    if (nome.isBlank() || dateRange.first == 0L) continue

                    // Extrai informações das colunas
                    val cols = row.select("div[class*=col]")

                    val local = cols.getOrNull(2)?.text()?.trim()?.takeIf { it.isNotBlank() } ?: "Portugal"
                    val tipo = cols.getOrNull(3)?.text()?.trim()?.takeIf { it.isNotBlank() } ?: "Estrada"
                    val organizador = cols.getOrNull(4)?.text()?.trim()?.takeIf { it.isNotBlank() } ?: sourceName

                    // Extrai URL de inscrição se disponível
                    val urlInscricao = row.select("a[href*=prova-inscrever], a[href*=inscri]").attr("abs:href")
                        .takeIf { it.isNotBlank() && isValidUrl(it) }

                    // Extract image URL
                    val imageUrl = row.select("img").firstOrNull()?.let { img ->
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

                    // Apenas adiciona se tiver URL válida
                    if (urlInscricao != null) {
                        val prova = ProvaEntity(
                            id = 0,
                            nome = nome,
                            data = dateRange.first,
                            local = local,
                            tipo = normalizeTipo(tipo),
                            distancias = "Várias categorias",
                            preco = "Consultar inscrição",
                            prazoInscricao = calculateDeadline(dateRange.first),
                            organizador = organizador,
                            descricao = "Prova oficial da Federação Portuguesa de Ciclismo.",
                            urlInscricao = urlInscricao,
                            source = sourceName,
                            imageUrl = imageUrl
                        )

                        provas.add(prova)
                        Log.d("FPCScraper", "Scraped prova: $nome")
                    } else {
                        Log.d("FPCScraper", "Skipped prova without valid URL: $nome")
                    }

                } catch (e: Exception) {
                    Log.e("FPCScraper", "Error parsing row: ${e.message}")
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

            if (futureProvas.isEmpty()) {
                Log.w("FPCScraper", "No future events found")
                return@withContext Result.success(emptyList())
            }

            Log.d("FPCScraper", "Successfully scraped ${futureProvas.size} future events")
            Result.success(futureProvas)

        } catch (e: Exception) {
            Log.e("FPCScraper", "Error scraping FPC: ${e.message}", e)
            Result.success(emptyList())
        }
    }

    private fun parseDate(dateText: String): Pair<Long, Long> {
        try {
            // Formato esperado: "04-01-2026" ou "04-01-2026 a 04-01-2026" ou "04/01/2026"
            val cleanText = dateText.replace(" a ", " ").replace(" à ", " ").trim()
            val dates = cleanText.split(" ", "/", "-")

            if (dates.size < 3) return Pair(0L, 0L)

            val day = dates[0].toIntOrNull() ?: return Pair(0L, 0L)
            val month = dates[1].toIntOrNull() ?: return Pair(0L, 0L)
            val year = dates[2].toIntOrNull() ?: Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon")).get(Calendar.YEAR)

            val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Lisbon"))
            calendar.set(year, month - 1, day, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            return Pair(calendar.timeInMillis, calendar.timeInMillis)
        } catch (e: Exception) {
            Log.e("FPCScraper", "Error parsing date: $dateText", e)
            return Pair(0L, 0L)
        }
    }

    private fun normalizeTipo(tipo: String): String {
        return when {
            tipo.contains("BTT", ignoreCase = true) || tipo.contains("XCO", ignoreCase = true) -> "BTT"
            tipo.contains("Estrada", ignoreCase = true) -> "Estrada"
            tipo.contains("Pista", ignoreCase = true) -> "Pista"
            tipo.contains("Ciclocross", ignoreCase = true) || tipo.contains("CX", ignoreCase = true) -> "Ciclocross"
            tipo.contains("Gran Fondo", ignoreCase = true) || tipo.contains("GF", ignoreCase = true) -> "Gran Fondo"
            else -> tipo.take(20) // Limita o tamanho
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

            // Rejeita URLs com domínios inválidos/inventados
            if (url.contains("www.fpc.pt") || url.contains("fpc.pt/") && !url.contains("fpciclismo.pt")) {
                Log.d("FPCScraper", "Rejected invalid domain URL: $url")
                return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}
