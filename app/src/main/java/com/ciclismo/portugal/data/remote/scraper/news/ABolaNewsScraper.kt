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
class ABolaNewsScraper @Inject constructor() : BaseNewsScraper {

    override val sourceName: String = "A Bola"
    private val baseUrl = "https://www.abola.pt"

    override suspend fun scrapeNews(): Result<List<NewsArticleEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("ABolaNewsScraper", "Starting scrape from: $baseUrl/modalidades/ciclismo")
            val articles = mutableListOf<NewsArticleEntity>()

            val document: Document = Jsoup.connect("$baseUrl/modalidades/ciclismo")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("ABolaNewsScraper", "Connected successfully")

            // Selecionar artigos
            val newsElements = document.select("article, .noticia, .news, [class*=article]").take(20)

            Log.d("ABolaNewsScraper", "Found ${newsElements.size} potential news articles")

            for ((index, element) in newsElements.withIndex()) {
                try {
                    // Extrair link
                    val linkElement = element.select("a[href]").first() ?: continue
                    val url = linkElement.attr("abs:href")

                    if (url.isBlank() || !url.contains("abola.pt")) continue

                    // Extrair título
                    val title = element.select("h2, h3, h4, .title, .titulo").text().trim()
                        .ifBlank { linkElement.text().trim() }

                    if (title.isBlank() || title.length < 10) continue

                    // Extrair sumário
                    val summary = element.select("p, .resume, .sumario, .texto").text().trim()
                        .ifBlank { title }

                    // Extrair imagem
                    val imageUrl = element.select("img").first()?.let { img ->
                        img.attr("data-src").ifBlank { img.attr("src") }
                    }?.let { if (it.startsWith("http")) it else "$baseUrl$it" }

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
                    Log.d("ABolaNewsScraper", "✓ Scraped: $title")

                } catch (e: Exception) {
                    Log.e("ABolaNewsScraper", "Error parsing article $index: ${e.message}")
                    continue
                }
            }

            Log.d("ABolaNewsScraper", "Successfully scraped ${articles.size} articles")
            Result.success(articles)

        } catch (e: Exception) {
            Log.e("ABolaNewsScraper", "Error scraping A Bola: ${e.message}", e)
            Result.success(emptyList())
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
