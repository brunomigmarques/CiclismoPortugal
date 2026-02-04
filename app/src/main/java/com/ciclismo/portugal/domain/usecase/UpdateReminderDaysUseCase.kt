package com.ciclismo.portugal.domain.usecase

import com.ciclismo.portugal.domain.repository.ProvaRepository
import javax.inject.Inject

class UpdateReminderDaysUseCase @Inject constructor(
    private val repository: ProvaRepository
) {
    suspend operator fun invoke(provaId: Long, reminderDays: Int) {
        repository.updateReminderDays(provaId, reminderDays)
    }
}
