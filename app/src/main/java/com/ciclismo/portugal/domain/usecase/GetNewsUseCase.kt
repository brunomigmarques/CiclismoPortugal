package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.model.NewsArticle
import com.ciclismo.portugal.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNewsUseCase @Inject constructor(
    private val repository: NewsRepository
) {
    operator fun invoke(limit: Int = 50): Flow<List<NewsArticle>> {
        return repository.getLatestNews(limit)
    }
}
