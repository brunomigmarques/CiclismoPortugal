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
class JNNewsScraper @Inject constructor() : BaseNewsScraper {

    override val sourceName: String = "Jornal de Notícias"
    private val baseUrl = "https://www.jn.pt"

    override suspend fun scrapeNews(): Result<List<NewsArticleEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("JNNewsScraper", "Starting scrape from: $baseUrl/topico/ciclismo")
            val articles = mutableListOf<NewsArticleEntity>()

            val document: Document = Jsoup.connect("$baseUrl/topico/ciclismo")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("JNNewsScraper", "Connected successfully")

            // Selecionar artigos
            val newsElements = document.select("article, .item, .news-item, [class*=article], [class*=story], .t-card").take(20)

            Log.d("JNNewsScraper", "Found ${newsElements.size} potential news articles")

            for ((index, element) in newsElements.withIndex()) {
                try {
                    // Extrair link - procurar link válido de notícia
                    val linkElement = element.select("a[href]").firstOrNull { link ->
                        val href = link.attr("abs:href")
                        href.contains("jn.pt") && (href.contains("/desporto/") || href.contains("/ciclismo") || href.contains("/topico/"))
                    } ?: element.select("a[href]").firstOrNull()

                    if (linkElement == null) continue

                    val url = linkElement.attr("abs:href")

                    if (url.isBlank() || !url.contains("jn.pt")) {
                        Log.d("JNNewsScraper", "Skipping invalid URL: $url")
                        continue
                    }

                    // Validar se é URL de notícia válida (não é homepage ou página de categoria)
                    if (url.endsWith("/ciclismo") || url.endsWith("/desporto") || url.endsWith(".pt/") || url.endsWith(".pt")) {
                        Log.d("JNNewsScraper", "Skipping category URL: $url")
                        continue
                    }

                    // Extrair título
                    val title = element.select("h1, h2, h3, h4, .title, .titulo, .t-card__title, .headline").text().trim()
                        .ifBlank { linkElement.attr("title").trim() }
                        .ifBlank { linkElement.text().trim() }

                    if (title.isBlank() || title.length < 10) {
                        Log.d("JNNewsScraper", "Skipping - title too short: $title")
                        continue
                    }

                    // Extrair sumário
                    val summary = element.select("p, .lead, .summary, .excerpt, .t-card__text, .description").text().trim()
                        .ifBlank { title }

                    // Extrair imagem
                    val imageUrl = element.select("img, picture img").firstOrNull()?.let { img ->
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
                    Log.d("JNNewsScraper", "✓ Scraped: $title -> $url")

                } catch (e: Exception) {
                    Log.e("JNNewsScraper", "Error parsing article $index: ${e.message}")
                    continue
                }
            }

            Log.d("JNNewsScraper", "Successfully scraped ${articles.size} articles")
            Result.success(articles)

        } catch (e: Exception) {
            Log.e("JNNewsScraper", "Error scraping JN: ${e.message}", e)
            Result.success(emptyList())
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
