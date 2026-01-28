package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.repository.ProvaRepository
import javax.inject.Inject

class AddToCalendarUseCase @Inject constructor(
    private val repository: ProvaRepository
) {
    suspend operator fun invoke(provaId: Long, add: Boolean) {
        repository.addToCalendar(provaId, add)
    }
}
