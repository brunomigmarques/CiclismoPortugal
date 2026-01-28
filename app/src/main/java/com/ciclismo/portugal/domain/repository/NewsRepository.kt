package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.NewsArticle
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getLatestNews(limit: Int = 50): Flow<List<NewsArticle>>
    suspend fun syncNews(): Result<Unit>
    suspend fun deleteOldNews(daysOld: Int = 30)
}
