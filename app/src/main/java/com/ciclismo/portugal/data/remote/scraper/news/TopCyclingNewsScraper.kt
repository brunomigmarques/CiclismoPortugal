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

/**
 * Scraper for TopCycling.pt - Portuguese cycling news site
 * Covers road cycling, BTT, gravel, and more
 */
@Singleton
class TopCyclingNewsScraper @Inject constructor() : BaseNewsScraper {

    override val sourceName: String = "TopCycling"
    private val baseUrl = "https://www.topcycling.pt"

    override suspend fun scrapeNews(): Result<List<NewsArticleEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("TopCyclingScraper", "Starting scrape from: $baseUrl")
            val articles = mutableListOf<NewsArticleEntity>()

            val document: Document = Jsoup.connect(baseUrl)
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            Log.d("TopCyclingScraper", "Connected successfully")

            // Select main articles - try multiple selectors
            val newsElements = document.select(
                "article, .post, .entry, .news-item, [class*=article], [class*=post]"
            ).take(20)

            Log.d("TopCyclingScraper", "Found ${newsElements.size} potential news articles")

            for ((index, element) in newsElements.withIndex()) {
                try {
                    // Extract link
                    val linkElement = element.select("a[href]").first() ?: continue
                    val url = linkElement.attr("abs:href")

                    if (url.isBlank() || !url.contains("topcycling.pt")) continue
                    // Skip category/tag pages
                    if (url.contains("/categoria/") || url.contains("/tag/")) continue

                    // Extract title
                    val title = element.select("h2, h3, h4, .title, .entry-title, .post-title").text().trim()
                        .ifBlank { linkElement.text().trim() }

                    if (title.isBlank() || title.length < 10) continue

                    // Extract summary
                    val summary = element.select("p, .summary, .excerpt, .entry-content").text().trim()
                        .ifBlank { title }

                    // Extract image
                    val imageUrl = element.select("img").first()?.let { img ->
                        val src = img.attr("data-src").ifBlank {
                            img.attr("data-lazy-src").ifBlank {
                                img.attr("src")
                            }
                        }
                        when {
                            src.startsWith("http") -> src
                            src.startsWith("//") -> "https:$src"
                            src.startsWith("/") -> "$baseUrl$src"
                            else -> null
                        }
                    }

                    // Generate unique hash based on URL
                    val contentHash = generateHash(url)

                    // Try fast extraction first, then fetch page for meta tags if needed
                    // Default to 3 days ago if no date found (so it doesn't always appear as newest)
                    val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
                    val publishedAt = DateParser.extractDate(element)
                        ?: DateParser.extractDateFromUrl(url)
                        ?: fetchArticleDate(url)
                        ?: threeDaysAgo

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
                    Log.d("TopCyclingScraper", "✓ Scraped: $title (date: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(publishedAt)})")

                } catch (e: Exception) {
                    Log.e("TopCyclingScraper", "Error parsing article $index: ${e.message}")
                    continue
                }
            }

            Log.d("TopCyclingScraper", "Successfully scraped ${articles.size} articles")
            Result.success(articles)

        } catch (e: Exception) {
            Log.e("TopCyclingScraper", "Error scraping TopCycling: ${e.message}", e)
            Result.success(emptyList())
        }
    }

    /**
     * Fetch the article page and extract the real publication date from meta tags.
     */
    private fun fetchArticleDate(url: String): Long? {
        return try {
            val doc = Jsoup.connect(url)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .get()

            // Try meta tags first (most reliable)
            val metaSelectors = listOf(
                "meta[property=article:published_time]",
                "meta[name=article:published_time]",
                "meta[property=og:article:published_time]",
                "meta[name=pubdate]",
                "meta[name=publishdate]",
                "meta[name=date]",
                "meta[itemprop=datePublished]"
            )

            for (selector in metaSelectors) {
                doc.select(selector).firstOrNull()?.let { meta ->
                    val content = meta.attr("content")
                    DateParser.parseIsoDate(content)?.let { return it }
                }
            }

            // Try time elements
            doc.select("time[datetime]").firstOrNull()?.let { time ->
                DateParser.parseIsoDate(time.attr("datetime"))?.let { return it }
            }

            // Try JSON-LD
            doc.select("script[type=application/ld+json]").forEach { script ->
                val json = script.html()
                val dateMatch = Regex(""""datePublished"\s*:\s*"([^"]+)"""").find(json)
                dateMatch?.groupValues?.get(1)?.let { dateStr ->
                    DateParser.parseIsoDate(dateStr)?.let { return it }
                }
            }

            // Try visible date elements
            doc.select(".date, .data, .published, .article-date, [class*=date]").firstOrNull()?.let { dateEl ->
                DateParser.parsePortugueseDate(dateEl.text())?.let { return it }
            }

            null
        } catch (e: Exception) {
            Log.d("TopCyclingScraper", "Could not fetch date from $url: ${e.message}")
            null
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
