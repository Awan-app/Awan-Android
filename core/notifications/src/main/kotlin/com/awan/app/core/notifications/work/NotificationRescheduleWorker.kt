package com.awan.app.core.notifications.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.awan.app.core.notifications.SessionNotificationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Rebuilds the alarm chain from a context that cannot wait on it — boot, a package replace, or a
 * clock change. A receiver's `goAsync` window is too short to be relied on for a Room read on a
 * device that has just finished booting.
 */
@HiltWorker
class NotificationRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: SessionNotificationScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        scheduler.rescheduleAll()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "NotificationRescheduleWork"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NotificationRescheduleWorker>().build(),
            )
        }
    }
}
