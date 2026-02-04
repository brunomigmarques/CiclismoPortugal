package com.ciclismo.portugal.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ciclismo.portugal.data.remote.video.CyclingVideosRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker that runs every 6 hours to sync videos to Firestore.
 *
 * This ensures Firestore always has fresh video IDs based on:
 * - Upcoming races from calendar (provas + WorldTour)
 * - Recent news articles
 *
 * All users load videos from Firestore, so only this worker
 * needs to make YouTube API calls, saving quota.
 */
@HiltWorker
class VideoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val videosRepository: CyclingVideosRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting video sync to Firestore...")

            val result = videosRepository.syncVideosToFirestore()

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                if (count > 0) {
                    Log.d(TAG, "Successfully synced $count videos to Firestore")
                } else {
                    Log.d(TAG, "Cache still valid, no sync needed")
                }
                Result.success()
            } else {
                Log.w(TAG, "Sync failed: ${result.exceptionOrNull()?.message}")
                // Don't retry on failure - will try again in 6 hours
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during video sync", e)
            // Retry on unexpected errors
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached, giving up")
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "VideoSyncWorker"
        const val WORK_NAME = "VideoSyncWork"
    }
}
