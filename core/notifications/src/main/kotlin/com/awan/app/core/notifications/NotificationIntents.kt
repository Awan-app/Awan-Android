package com.awan.app.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.notifications.model.NotificationAction
import com.awan.app.core.notifications.receiver.NotificationActionReceiver
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Builds the intents behind notification taps and buttons.
 *
 * Every [PendingIntent] needs its own request code, or Android reuses the first one it made and
 * every button on every session's notification ends up doing the same thing.
 */
object NotificationIntents {

    const val EXTRA_SESSION_ID = "com.awan.app.extra.SESSION_ID"
    const val EXTRA_SESSION_DATE = "com.awan.app.extra.SESSION_DATE"
    const val EXTRA_ACTION = "com.awan.app.extra.NOTIFICATION_ACTION"
    const val EXTRA_START_ISO = "com.awan.app.extra.START_ISO"
    const val EXTRA_END_ISO = "com.awan.app.extra.END_ISO"
    const val EXTRA_NOW_ISO = "com.awan.app.extra.NOW_ISO"
    const val EXTRA_NOTIFICATION_ID = "com.awan.app.extra.NOTIFICATION_ID"

    val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /** Opens the app on the session's day with its detail sheet showing. */
    fun openSession(context: Context, session: UpcomingSession): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_SESSION_ID, session.id)
                putExtra(EXTRA_SESSION_DATE, session.start.toLocalDate().toString())
            }
            ?: return null

        return PendingIntent.getActivity(
            context,
            requestCode(session.id, OPEN_TAG),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * [now] is captured when the notification is built rather than read when the work finally runs:
     * a "Stop Here" tapped offline may not reach the server for hours, and it must still end the
     * session at the moment the user pressed it.
     */
    fun action(
        context: Context,
        session: UpcomingSession,
        action: NotificationAction,
        notificationId: Int,
        now: LocalDateTime,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_ACTION, action.name)
            putExtra(EXTRA_START_ISO, session.start.format(ISO))
            putExtra(EXTRA_END_ISO, session.end.format(ISO))
            putExtra(EXTRA_NOW_ISO, now.format(ISO))
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode(session.id, action.name),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val OPEN_TAG = "open"

    private fun requestCode(sessionId: String, tag: String): Int = "$tag:$sessionId".hashCode()
}
