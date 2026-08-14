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
    const val EXTRA_SESSION_TITLE = "com.awan.app.extra.SESSION_TITLE"
    const val EXTRA_SESSION_DATE = "com.awan.app.extra.SESSION_DATE"
    const val EXTRA_ACTION = "com.awan.app.extra.NOTIFICATION_ACTION"
    const val EXTRA_START_ISO = "com.awan.app.extra.START_ISO"
    const val EXTRA_END_ISO = "com.awan.app.extra.END_ISO"
    const val EXTRA_NOTIFICATION_ID = "com.awan.app.extra.NOTIFICATION_ID"

    /** Set only by the snooze picker's buttons; absent means "use the stored length". */
    const val EXTRA_SNOOZE_MINUTES = "com.awan.app.extra.SNOOZE_MINUTES"

    /** [EXTRA_SNOOZE_MINUTES] when the intent does not carry one. */
    const val NO_SNOOZE_MINUTES = 0

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
     * Deliberately carries no timestamp.
     *
     * It used to carry the moment the notification was *built*, which "Complete now" then used as the
     * session's end — so a session was cut back to whenever it was last redrawn rather than to the
     * moment the button was pressed. The receiver runs at press time and stamps its own clock, which
     * is the thing the worker actually needs and survives the tap being queued offline for hours.
     */
    fun action(
        context: Context,
        session: UpcomingSession,
        action: NotificationAction,
        notificationId: Int,
        snoozeMinutes: Int = NO_SNOOZE_MINUTES,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, session.id)
            // Carried so a button that re-posts the notification — the snooze picker — can keep the
            // title the user is looking at without a Room read inside a broadcast receiver.
            putExtra(EXTRA_SESSION_TITLE, session.title)
            putExtra(EXTRA_ACTION, action.name)
            putExtra(EXTRA_START_ISO, session.start.format(ISO))
            putExtra(EXTRA_END_ISO, session.end.format(ISO))
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }

        return PendingIntent.getBroadcast(
            context,
            // The length is part of the code: the picker's three buttons are all SNOOZE on the same
            // session, so without it they share one PendingIntent and every one of them snoozes by
            // whatever the last button built.
            requestCode(session.id, "${action.name}:$snoozeMinutes"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val OPEN_TAG = "open"

    private fun requestCode(sessionId: String, tag: String): Int = "$tag:$sessionId".hashCode()
}
