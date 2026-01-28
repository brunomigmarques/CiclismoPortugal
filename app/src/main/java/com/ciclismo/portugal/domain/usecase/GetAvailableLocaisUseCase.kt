package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.repository.ProvaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAvailableLocaisUseCase @Inject constructor(
    private val repository: ProvaRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.getAvailableLocais()
}
