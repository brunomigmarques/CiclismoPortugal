package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.domain.repository.ProvaRepository
import javax.inject.Inject

class GetProvaByIdUseCase @Inject constructor(
    private val repository: ProvaRepository
) {
    suspend operator fun invoke(id: Long): Prova? = repository.getProvaById(id)
}
