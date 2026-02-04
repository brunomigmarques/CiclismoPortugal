package com.ciclismo.portugal.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker that runs daily to check for races starting tomorrow.
 * Sends notifications to remind users to finalize their Fantasy teams
 * before the race day freeze kicks in.
 */
@HiltWorker
class RaceNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val raceRepository: RaceRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Checking for races starting tomorrow...")

            val racesTomorrow = raceRepository.getRacesStartingTomorrow()

            if (racesTomorrow.isNotEmpty()) {
                Log.d(TAG, "Found ${racesTomorrow.size} races starting tomorrow")

                // Send notification for each race
                racesTomorrow.forEach { race ->
                    notificationHelper.showRaceStartNotification(
                        raceName = race.name,
                        hoursUntilStart = 24 // Tomorrow
                    )
                    Log.d(TAG, "Sent notification for: ${race.name}")
                }
            } else {
                Log.d(TAG, "No races starting tomorrow")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for tomorrow's races", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RaceNotificationWorker"
        const val WORK_NAME = "RaceNotificationWork"
    }
}
