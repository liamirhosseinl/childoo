package com.childoo.lockkit

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.childoo.core.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Every 15 minutes, brings protection back if the system (or an aggressive battery manager) killed it.
 * Android may refuse a background start; the next run simply tries again.
 */
class ProtectionWatchdog(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = SettingsRepository(applicationContext).settings.first().protectionEnabled
        if (!enabled) {
            cancel(applicationContext)
            return Result.success()
        }
        if (!UsageMonitorService.isRunning) {
            try {
                LockKit.start(applicationContext)
            } catch (_: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException on Android 12+ when started from the background.
            } catch (_: SecurityException) {
            }
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "childoo-protection-watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ProtectionWatchdog>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
