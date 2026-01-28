package com.ciclismo.portugal.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ciclismo.portugal.domain.repository.NewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NewsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val newsRepository: NewsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("NewsSyncWorker", "Starting news sync")

            // Sync news from all sources
            newsRepository.syncNews().getOrThrow()

            // Delete old news (older than 30 days)
            newsRepository.deleteOldNews(30)

            Log.d("NewsSyncWorker", "News sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("NewsSyncWorker", "Error syncing news: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "news_sync_worker"
    }
}
