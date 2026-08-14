package com.awan.app.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.notifications.NotificationIntents
import com.awan.app.core.notifications.NotificationRecords
import com.awan.app.core.notifications.SessionNotificationPoster
import com.awan.app.core.notifications.SessionNotificationScheduler
import com.awan.app.core.notifications.model.NotificationAction
import com.awan.app.core.notifications.model.SessionWindow
import com.awan.app.core.notifications.work.NotificationActionWorker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles a notification button press.
 *
 * Does no network work itself — it dismisses the notification so the press feels immediate, then
 * hands the endpoint call to [NotificationActionWorker], which owns the retry and the offline queue.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var poster: SessionNotificationPoster

    @Inject
    lateinit var records: NotificationRecords

    @Inject
    lateinit var scheduler: SessionNotificationScheduler

    @Inject
    lateinit var getNotificationPreferences: GetNotificationPreferencesUseCase

    @Inject
    lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(NotificationIntents.EXTRA_SESSION_ID)
        val action = NotificationAction.fromNameOrNull(
            intent.getStringExtra(NotificationIntents.EXTRA_ACTION)
        )

        if (sessionId == null || action == null) {
            Log.w(TAG, "Ignoring malformed notification action intent")
            return
        }

        val notificationId = intent.getIntExtra(NotificationIntents.EXTRA_NOTIFICATION_ID, -1)
        val startIso = intent.getStringExtra(NotificationIntents.EXTRA_START_ISO)
        val endIso = intent.getStringExtra(NotificationIntents.EXTRA_END_ISO)
        val snoozeMinutes = intent.getIntExtra(
            NotificationIntents.EXTRA_SNOOZE_MINUTES,
            NotificationIntents.NO_SNOOZE_MINUTES,
        )

        // The one press that must not dismiss anything yet: a snooze with no length is a question,
        // and the answer replaces this very notification.
        if (action == NotificationAction.SNOOZE && snoozeMinutes == NotificationIntents.NO_SNOOZE_MINUTES) {
            handleSnoozePress(context, intent, sessionId, notificationId, startIso, endIso)
            return
        }

        if (notificationId != -1) poster.cancel(notificationId)

        // Nothing to send anywhere: the session is untouched, only its notification is gone. The
        // record is what keeps the next reschedule from putting it straight back.
        if (action == NotificationAction.DISMISS_LIVE) {
            rememberDismissal(sessionId, startIso, endIso)
            // Cancelling above is not enough on its own. While the app is alive the scheduler drives
            // the live notification from a coroutine every few seconds, and that loop is only
            // stopped by a reschedule finding the session no longer needs one.
            retimeAsync()
            return
        }

        NotificationActionWorker.enqueue(
            context = context,
            sessionId = sessionId,
            action = action,
            startIso = startIso,
            endIso = endIso,
            // Stamped here, not when the notification was built: "Complete now" ends the
            // session at this instant, and the receiver is the only place that runs at press time.
            nowIso = LocalDateTime.now(clock).format(NotificationIntents.ISO),
            snoozeMinutes = snoozeMinutes,
        )
    }

    /**
     * Snoozes by the stored length, or asks for one.
     *
     * The preference is read here rather than baked into the button at post time, because a
     * preference changed after the notification was posted should still be the one that applies.
     * Reading DataStore is a suspend call, hence [goAsync].
     */
    private fun handleSnoozePress(
        context: Context,
        intent: Intent,
        sessionId: String,
        notificationId: Int,
        startIso: String?,
        endIso: String?,
    ) {
        val session = parseSession(intent, sessionId, startIso, endIso)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                val stored = getNotificationPreferences().first().snoozeMinutes
                // Without a readable window there is nothing to re-post and nothing to move, so
                // falling through to the worker at least fails somewhere that can log it.
                if (session != null && stored == NotificationPreferences.SNOOZE_ASK) {
                    poster.postSnoozeChoice(session)
                } else {
                    if (notificationId != -1) poster.cancel(notificationId)
                    NotificationActionWorker.enqueue(
                        context = context,
                        sessionId = sessionId,
                        action = NotificationAction.SNOOZE,
                        startIso = startIso,
                        endIso = endIso,
                        nowIso = LocalDateTime.now(clock).format(NotificationIntents.ISO),
                        snoozeMinutes = NotificationIntents.NO_SNOOZE_MINUTES,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle a snooze press", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Enough of the session to re-post its notification, rebuilt from the intent that was already
     * carrying all of it — a Room read is too slow for a receiver's own window.
     */
    private fun parseSession(
        intent: Intent,
        sessionId: String,
        startIso: String?,
        endIso: String?,
    ): UpcomingSession? = runCatching {
        UpcomingSession(
            id = sessionId,
            taskId = "",
            title = intent.getStringExtra(NotificationIntents.EXTRA_SESSION_TITLE).orEmpty(),
            start = LocalDateTime.parse(startIso, NotificationIntents.ISO),
            end = LocalDateTime.parse(endIso, NotificationIntents.ISO),
            status = SessionStatus.SCHEDULED,
            zoneId = null,
        )
    }.getOrElse {
        Log.w(TAG, "Snooze press without a readable session window", it)
        null
    }

    private fun rememberDismissal(sessionId: String, startIso: String?, endIso: String?) {
        val window = runCatching {
            SessionWindow(
                start = LocalDateTime.parse(startIso, NotificationIntents.ISO),
                end = LocalDateTime.parse(endIso, NotificationIntents.ISO),
            )
        }.getOrElse {
            // Without a window the dismissal cannot be scoped, and an unscoped one would silence
            // the session even after it moves. Better to let the notification return.
            Log.w(TAG, "Dismissal without a readable session window; not recording it", it)
            return
        }

        records.dismiss(sessionId, window)
    }

    /** Same shape as [SessionAlarmReceiver]: a Room read is too slow for a receiver's own window. */
    private fun retimeAsync() {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                scheduler.rescheduleAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retime after a notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AwanNotifications"
    }
}
