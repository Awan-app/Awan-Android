package com.awan.app.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awan.app.core.notifications.NotificationIntents
import com.awan.app.core.notifications.SessionNotificationPoster
import com.awan.app.core.notifications.model.NotificationAction
import com.awan.app.core.notifications.work.NotificationActionWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

        NotificationActionWorker.enqueue(
            context = context,
            sessionId = sessionId,
            action = action,
            startIso = intent.getStringExtra(NotificationIntents.EXTRA_START_ISO),
            endIso = intent.getStringExtra(NotificationIntents.EXTRA_END_ISO),
            nowIso = intent.getStringExtra(NotificationIntents.EXTRA_NOW_ISO),
        )
    }

    private companion object {
        const val TAG = "AwanNotifications"
    }
}
