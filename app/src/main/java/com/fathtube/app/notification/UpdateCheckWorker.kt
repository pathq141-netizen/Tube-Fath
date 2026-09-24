package com.fathtube.app.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Worker that checks for application updates in the background.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "update_check_work"
        private const val TAG = "UpdateCheckWorker"
        private const val COOLDOWN_HOURS = 12L

        suspend fun schedulePeriodicCheck(
            context: Context,
            reschedule: Boolean = false,
        ) {
            cancelScheduledChecks(context)
        }

        fun cancelScheduledChecks(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled scheduled update checks")
        }
    }

    override suspend fun doWork(): Result {
        cancelScheduledChecks(applicationContext)
        return Result.success()
    }

    private fun isForcedCheck(): Boolean = inputData.getBoolean("force", false)
}
