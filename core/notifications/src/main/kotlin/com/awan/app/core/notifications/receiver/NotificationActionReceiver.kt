package com.awan.app.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awan.app.core.notifications.LiveNotificationDismissals
import com.awan.app.core.notifications.NotificationIntents
import com.awan.app.core.notifications.SessionNotificationPoster
import com.awan.app.core.notifications.SessionNotificationScheduler
import com.awan.app.core.notifications.model.NotificationAction
import com.awan.app.core.notifications.model.SessionWindow
import com.awan.app.core.notifications.work.NotificationActionWorker
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    lateinit var dismissals: LiveNotificationDismissals

    @Inject
    lateinit var scheduler: SessionNotificationScheduler

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
        if (notificationId != -1) poster.cancel(notificationId)

        val startIso = intent.getStringExtra(NotificationIntents.EXTRA_START_ISO)
        val endIso = intent.getStringExtra(NotificationIntents.EXTRA_END_ISO)

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
            nowIso = intent.getStringExtra(NotificationIntents.EXTRA_NOW_ISO),
        )
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

        dismissals.dismiss(sessionId, window)
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
