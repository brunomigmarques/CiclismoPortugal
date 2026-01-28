package com.ciclismo.portugal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.ciclismo.portugal.worker.NewsSyncWorker
import com.ciclismo.portugal.workers.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CiclismoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        setupWorkManager()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Events sync worker
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // News sync worker - sync every 6 hours
        val newsSyncRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NewsSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            newsSyncRequest
        )
    }
}
