package com.awan.app.core.notifications.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.awan.app.core.common.error.AppError
import com.awan.app.core.domain.home.usecase.CompleteSessionUseCase
import com.awan.app.core.domain.home.usecase.MoveSessionUseCase
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.notifications.SessionNotificationScheduler
import com.awan.app.core.notifications.model.NotificationAction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import com.awan.app.core.common.result.Result as DomainResult

/**
 * Runs the endpoint call behind a notification button.
 *
 * Going through WorkManager with a network constraint is the point: a "Mark complete" tapped in
 * airplane mode is queued and lands when connectivity returns, instead of failing silently deep in
 * the stack. This is the behaviour the whole migration off push exists to get.
 */
@HiltWorker
class NotificationActionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val moveSession: MoveSessionUseCase,
    private val completeSession: CompleteSessionUseCase,
    private val getNotificationPreferences: GetNotificationPreferencesUseCase,
    private val scheduler: SessionNotificationScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val action = NotificationAction.fromNameOrNull(inputData.getString(KEY_ACTION))
            ?: return Result.failure()

        val outcome = when (action) {
            NotificationAction.SNOOZE -> snooze(sessionId)
            NotificationAction.COMPLETE -> complete(sessionId)
            NotificationAction.COMPLETE_NOW -> completeNow(sessionId)
            // Handled entirely in the receiver: it touches nothing a worker could carry.
            NotificationAction.DISMISS_LIVE -> return Result.success()
        }

        return when (outcome) {
            is AppResult.Ok -> {
                // The mutation changed Room, but only this process's collector sees that; a worker
                // may be running with no Activity alive, so retime explicitly.
                scheduler.rescheduleAll()
                Result.success()
            }
            is AppResult.Retry -> Result.retry()
            is AppResult.Fail -> {
                Log.e(TAG, "Notification action $action failed permanently for $sessionId")
                Result.failure()
            }
        }
    }

    private suspend fun snooze(sessionId: String): AppResult {
        val start = inputData.getString(KEY_START_ISO)?.let(::parse) ?: return AppResult.Fail
        val end = inputData.getString(KEY_END_ISO)?.let(::parse) ?: return AppResult.Fail
        val minutes = getNotificationPreferences().first().snoozeMinutes.toLong()

        // Shifts both ends, so snoozing preserves the session's length rather than eating into it.
        return moveSession(
            sessionId = sessionId,
            startIso = start.plusMinutes(minutes).format(ISO),
            endIso = end.plusMinutes(minutes).format(ISO),
        ).toAppResult()
    }

    private suspend fun complete(sessionId: String): AppResult =
        completeSession(sessionId).toAppResult()

    /**
     * Ends the session at the tap moment, then completes it.
     *
     * The move has to land first: completing sets the session's outcome, and moving afterwards would
     * be editing an already-closed session. If the move fails the whole thing retries, so a partial
     * "moved but not completed" state resolves on the next attempt rather than sticking.
     */
    private suspend fun completeNow(sessionId: String): AppResult {
        val start = inputData.getString(KEY_START_ISO)?.let(::parse) ?: return AppResult.Fail
        val now = inputData.getString(KEY_NOW_ISO)?.let(::parse) ?: return AppResult.Fail

        // A session stopped within the same minute it began would otherwise get a zero or negative
        // length, which the backend rejects.
        val end = maxOf(now, start.plusMinutes(MIN_SESSION_MINUTES))

        return when (val moved = moveSession(sessionId, start.format(ISO), end.format(ISO)).toAppResult()) {
            is AppResult.Ok -> completeSession(sessionId).toAppResult()
            else -> moved
        }
    }

    private fun parse(raw: String): LocalDateTime? = runCatching { LocalDateTime.parse(raw, ISO) }.getOrNull()

    /**
     * Offline and server-side failures are retried; a malformed request would fail identically
     * forever, so it is not.
     */
    private fun <T> DomainResult<T>.toAppResult(): AppResult = when (this) {
        is DomainResult.Success -> AppResult.Ok
        is DomainResult.Loading -> AppResult.Retry
        is DomainResult.Error -> when (error) {
            is AppError.Validation, is AppError.NotFound -> AppResult.Fail
            else -> AppResult.Retry
        }
    }

    private sealed interface AppResult {
        data object Ok : AppResult
        data object Retry : AppResult
        data object Fail : AppResult
    }

    companion object {
        private const val TAG = "AwanNotifications"
        private const val KEY_SESSION_ID = "sessionId"
        private const val KEY_ACTION = "action"
        private const val KEY_START_ISO = "startIso"
        private const val KEY_END_ISO = "endIso"
        private const val KEY_NOW_ISO = "nowIso"
        private const val MIN_SESSION_MINUTES = 1L

        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun enqueue(
            context: Context,
            sessionId: String,
            action: NotificationAction,
            startIso: String?,
            endIso: String?,
            nowIso: String?,
        ) {
            val request = OneTimeWorkRequestBuilder<NotificationActionWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
                .setInputData(
                    Data.Builder()
                        .putString(KEY_SESSION_ID, sessionId)
                        .putString(KEY_ACTION, action.name)
                        .putString(KEY_START_ISO, startIso)
                        .putString(KEY_END_ISO, endIso)
                        .putString(KEY_NOW_ISO, nowIso)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                // Per session and action: a second tap on the same button replaces the first rather
                // than snoozing twice, but snoozing and completing never cancel each other.
                "notif-action-$sessionId-${action.name}",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
