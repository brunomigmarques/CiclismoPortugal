package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.repository.NewsRepository
import javax.inject.Inject

class SyncNewsUseCase @Inject constructor(
    private val repository: NewsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.syncNews()
    }
}
