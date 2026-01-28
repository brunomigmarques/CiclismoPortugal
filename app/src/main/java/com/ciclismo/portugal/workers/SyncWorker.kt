package com.ciclismo.portugal.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ciclismo.portugal.data.local.dao.ProvaDao
import com.ciclismo.portugal.domain.usecase.SyncProvasUseCase
import com.ciclismo.portugal.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncProvasUseCase: SyncProvasUseCase,
    private val provaDao: ProvaDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val countBefore = provaDao.getCount()

            val result = syncProvasUseCase()

            if (result.isSuccess) {
                val countAfter = provaDao.getCount()
                val newProvas = countAfter - countBefore

                if (newProvas > 0) {
                    notificationHelper.showNewProvaNotification(
                        provaName = "Novas provas",
                        provaCount = newProvas
                    )
                }

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SyncProvasWork"
    }
}
