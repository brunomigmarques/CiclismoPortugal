package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.repository.ProvaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchProvasUseCase @Inject constructor(
    private val repository: ProvaRepository
) {
    operator fun invoke(query: String): Flow<List<Prova>> = repository.searchProvas(query)
}
