package com.ciclismo.portugal.data.remote.scraper.news

import android.util.Log
import com.ciclismo.portugal.data.local.entity.NewsArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OJogoNewsScraper @Inject constructor() : BaseNewsScraper {

    override val sourceName: String = "O Jogo"
    private val baseUrl = "https://www.ojogo.pt"

    override suspend fun scrapeNews(): Result<List<NewsArticleEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("OJogoNewsScraper", "Starting scrape from: $baseUrl/modalidades/ciclismo")
            val articles = mutableListOf<NewsArticleEntity>()

            val document: Document = Jsoup.connect("$baseUrl/modalidades/ciclismo")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("OJogoNewsScraper", "Connected successfully")

            // Selecionar artigos
            val newsElements = document.select("article, .noticia, .news-item, [class*=article], [class*=story]").take(20)

            Log.d("OJogoNewsScraper", "Found ${newsElements.size} potential news articles")

            for ((index, element) in newsElements.withIndex()) {
                try {
                    // Extrair link - procurar em vários lugares
                    val linkElement = element.select("a[href]").firstOrNull { link ->
                        val href = link.attr("abs:href")
                        href.contains("ojogo.pt") && (href.contains("/noticias/") || href.contains("/ciclismo"))
                    } ?: element.select("a[href]").firstOrNull()

                    if (linkElement == null) continue

                    val url = linkElement.attr("abs:href")

                    if (url.isBlank() || !url.contains("ojogo.pt")) {
                        Log.d("OJogoNewsScraper", "Skipping invalid URL: $url")
                        continue
                    }

                    // Validar se é URL de notícia válida
                    if (!url.contains("/noticias/") && !url.contains("/ciclismo") && !url.contains("/modalidades/")) {
                        Log.d("OJogoNewsScraper", "Skipping non-news URL: $url")
                        continue
                    }

                    // Extrair título
                    val title = element.select("h1, h2, h3, h4, .title, .titulo, .headline").text().trim()
                        .ifBlank { linkElement.text().trim() }

                    if (title.isBlank() || title.length < 10) {
                        Log.d("OJogoNewsScraper", "Skipping - title too short: $title")
                        continue
                    }

                    // Extrair sumário
                    val summary = element.select("p, .lead, .summary, .excerpt, .description").text().trim()
                        .ifBlank { title }

                    // Extrair imagem
                    val imageUrl = element.select("img").firstOrNull()?.let { img ->
                        val src = img.attr("data-src").ifBlank {
                            img.attr("data-original").ifBlank {
                                img.attr("src")
                            }
                        }
                        if (src.startsWith("http")) src else if (src.startsWith("/")) "$baseUrl$src" else null
                    }

                    // Gerar hash único baseado apenas no URL (mais confiável)
                    val contentHash = generateHash(url)

                    // Data aproximada (agora)
                    val publishedAt = System.currentTimeMillis()

                    val article = NewsArticleEntity(
                        id = 0,
                        title = title,
                        summary = summary.take(300),
                        url = url,
                        imageUrl = imageUrl,
                        source = sourceName,
                        publishedAt = publishedAt,
                        contentHash = contentHash
                    )

                    articles.add(article)
                    Log.d("OJogoNewsScraper", "✓ Scraped: $title -> $url")

                } catch (e: Exception) {
                    Log.e("OJogoNewsScraper", "Error parsing article $index: ${e.message}")
                    continue
                }
            }

            Log.d("OJogoNewsScraper", "Successfully scraped ${articles.size} articles")
            Result.success(articles)

        } catch (e: Exception) {
            Log.e("OJogoNewsScraper", "Error scraping O Jogo: ${e.message}", e)
            Result.success(emptyList())
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
