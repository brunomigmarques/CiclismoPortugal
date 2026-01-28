package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.repository.ProvaRepository
import javax.inject.Inject

class SyncProvasUseCase @Inject constructor(
    private val repository: ProvaRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.syncProvas()
}
