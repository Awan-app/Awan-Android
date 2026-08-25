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
        private const val KEY_FORCE_REFRESH = "force_refresh"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(Duration.ofHours(6))
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Enqueues a one-off reconciliation. Server invalidations must set [forceRefresh] so a
         * recently cached schedule cannot hide an MCP/other-device mutation behind its TTL.
         */
        fun enqueueImmediateSync(context: Context, forceRefresh: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_FORCE_REFRESH to forceRefresh))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
