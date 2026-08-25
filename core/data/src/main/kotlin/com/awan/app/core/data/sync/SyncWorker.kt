package com.awan.app.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncCoordinator: OfflineSyncCoordinator,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)
        val success = syncCoordinator.syncAll(forceRefresh = forceRefresh)
        return if (success) Result.success() else Result.retry()
    }

    companion object {
        const val PERIODIC_SYNC_WORK_NAME = "PeriodicSyncWork"
        const val IMMEDIATE_SYNC_WORK_NAME = "ImmediateSyncWork"
        const val SCHEDULE_INVALIDATION_SYNC_WORK_NAME = "ScheduleInvalidationSyncWork"
        private const val KEY_FORCE_REFRESH = "force_refresh"

        fun schedulePeriodicSync(context: Context) {
            val constraints = networkConstraints()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(Duration.ofHours(6))
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueueImmediateSync(context: Context) {
            val request = oneTimeRequest(forceRefresh = false)
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        /**
         * Reconciles a server-side schedule mutation (MCP, another client, etc.). This has its own
         * unique-work slot so an ordinary immediate sync cannot replace the forced invalidation while
         * the device is waiting for connectivity. KEEP also collapses repeated invalidations into the
         * already-pending forced refresh.
         */
        fun enqueueScheduleInvalidationSync(context: Context) {
            val request = oneTimeRequest(forceRefresh = true)
            WorkManager.getInstance(context).enqueueUniqueWork(
                SCHEDULE_INVALIDATION_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        private fun oneTimeRequest(forceRefresh: Boolean) =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints())
                .setInputData(workDataOf(KEY_FORCE_REFRESH to forceRefresh))
                .build()

        private fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
