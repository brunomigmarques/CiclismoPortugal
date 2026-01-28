package com.ciclismo.portugal.data.repository

import android.util.Log
import com.ciclismo.portugal.data.local.dao.NewsArticleDao
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.remote.scraper.news.BaseNewsScraper
import com.ciclismo.portugal.domain.model.NewsArticle
import com.ciclismo.portugal.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val newsDao: NewsArticleDao,
    private val scrapers: Set<@JvmSuppressWildcards BaseNewsScraper>
) : NewsRepository {

    override fun getLatestNews(limit: Int): Flow<List<NewsArticle>> {
        return newsDao.getLatestNews(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncNews(): Result<Unit> {
        return try {
            Log.d("NewsRepository", "Starting news sync with ${scrapers.size} scrapers")

            val allArticles = mutableListOf<com.ciclismo.portugal.data.local.entity.NewsArticleEntity>()

            scrapers.forEach { scraper ->
                Log.d("NewsRepository", "Scraping from: ${scraper.sourceName}")
                val result = scraper.scrapeNews()

                result.getOrNull()?.let { articles ->
                    Log.d("NewsRepository", "Got ${articles.size} articles from ${scraper.sourceName}")
                    allArticles.addAll(articles)
                }
            }

            // Remove duplicados por hash na lista atual (antes de inserir)
            val uniqueArticles = allArticles.distinctBy { it.contentHash }

            Log.d("NewsRepository", "Total scraped: ${allArticles.size}, Unique by hash: ${uniqueArticles.size}")

            // Inserir no banco - OnConflictStrategy.IGNORE irá ignorar duplicados automaticamente
            // baseado no índice único do contentHash
            val inserted = newsDao.insertAll(uniqueArticles)
            val insertedCount = inserted.count { it > 0 }

            Log.d("NewsRepository", "New articles inserted: $insertedCount / ${uniqueArticles.size}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error syncing news: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteOldNews(daysOld: Int) {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysOld.toLong())
        newsDao.deleteOlderThan(cutoffTime)
        Log.d("NewsRepository", "Deleted news older than $daysOld days")
    }
}
