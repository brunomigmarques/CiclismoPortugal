package com.ciclismo.portugal.data.remote.scraper.news

import com.ciclismo.portugal.data.local.entity.NewsArticleEntity

interface BaseNewsScraper {
    val sourceName: String
    suspend fun scrapeNews(): Result<List<NewsArticleEntity>>
}
