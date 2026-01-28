package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.NewsArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsArticleDao {

    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC LIMIT :limit")
    fun getLatestNews(limit: Int = 50): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE id = :id")
    suspend fun getById(id: Long): NewsArticleEntity?

    @Query("SELECT * FROM news_articles WHERE contentHash = :hash")
    suspend fun getByContentHash(hash: String): NewsArticleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(article: NewsArticleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<NewsArticleEntity>): List<Long>

    @Delete
    suspend fun delete(article: NewsArticleEntity)

    @Query("DELETE FROM news_articles WHERE publishedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM news_articles")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getCount(): Int
}
