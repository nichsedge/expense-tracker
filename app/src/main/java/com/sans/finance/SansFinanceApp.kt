package com.sans.finance

import android.app.Application
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SansFinanceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var localeManager: com.sans.finance.data.util.LocaleManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleSync()
        scheduleDailyRecap()
        rescheduleBackupWork(this, localeManager)
    }

    private fun scheduleDailyRecap() {
        val currentCal = java.util.Calendar.getInstance()
        val targetCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 21)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (targetCal.before(currentCal)) {
            targetCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelayMillis = targetCal.timeInMillis - currentCal.timeInMillis

        val recapRequest =
            PeriodicWorkRequestBuilder<com.sans.finance.data.worker.SpendingRecapWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySpendingRecap",
            ExistingPeriodicWorkPolicy.KEEP,
            recapRequest
        )
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRatesRequest =
            PeriodicWorkRequestBuilder<com.sans.finance.data.worker.SyncExchangeRatesWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncExchangeRates",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRatesRequest
        )
    }

    companion object {
        fun rescheduleBackupWork(
            context: Context,
            localeManager: com.sans.finance.data.util.LocaleManager
        ) {
            val frequency = localeManager.getBackupFrequency()
            val wifiOnly = localeManager.isBackupWifiOnly()
            val requiresCharging = localeManager.isBackupRequiresCharging()

            val workManager = WorkManager.getInstance(context)

            if (frequency == "OFF" || frequency == "MANUAL") {
                workManager.cancelUniqueWork("CloudSyncAndBackup")
                return
            }

            val intervalDays = when (frequency) {
                "DAILY" -> 1L
                "WEEKLY" -> 7L
                "MONTHLY" -> 30L
                else -> 7L
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(requiresCharging)
                .build()

            val cloudSyncRequest =
                PeriodicWorkRequestBuilder<com.sans.finance.data.worker.CloudSyncAndBackupWorker>(
                    intervalDays, TimeUnit.DAYS
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                "CloudSyncAndBackup",
                ExistingPeriodicWorkPolicy.UPDATE,
                cloudSyncRequest
            )
        }
    }
}
