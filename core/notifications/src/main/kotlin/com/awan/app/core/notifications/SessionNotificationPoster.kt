package com.awan.app.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.awan.app.core.designsystem.AwanToastManager
import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.notifications.model.NotificationAction
import com.awan.app.core.notifications.model.SessionNotificationEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts every notification this app shows, and cancels the ones the schedule no longer
 * justifies.
 */
@Singleton
class SessionNotificationPoster @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channels: AwanNotificationChannels,
    private val timeFormatter: SessionTimeFormatter,
) {

    private val manager = NotificationManagerCompat.from(context)

    fun post(event: SessionNotificationEvent, now: LocalDateTime) {
        channels.ensureCreated()
        when (event) {
            is SessionNotificationEvent.Reminder -> postReminder(event.session, now)
            is SessionNotificationEvent.Live -> postLive(event.session, now)
            is SessionNotificationEvent.Ended -> postEnded(event.session, now)
        }
    }

    fun cancel(notificationId: Int) = manager.cancel(notificationId)

    fun cancelAllFor(sessionId: String) = NotificationIds.allFor(sessionId).forEach(::cancel)

    /**
     * Ids currently showing in the tray. The scheduler diffs this against the plan, which is how a
     * deleted or rescheduled session loses its notification without anything being persisted.
     */
    fun postedIds(): Set<Int> = runCatching {
        manager.activeNotifications.map { it.id }.toSet()
    }.getOrElse {
        // Some OEM builds throw here rather than returning empty. Nothing to reconcile against is
        // recoverable; crashing an alarm receiver is not.
        Log.w(TAG, "Could not read active notifications", it)
        emptySet()
    }

    private fun postReminder(session: UpcomingSession, now: LocalDateTime) {
        val minutes = Duration.between(now, session.start).toMinutes().coerceAtLeast(0).toInt()
        val text = context.resources.getQuantityString(
            R.plurals.notifications_reminder_text,
            minutes,
            minutes,
            timeFormatter.timeRange(context, session),
        )
        val id = NotificationIds.reminder(session.id)

        notify(
            id = id,
            builder = baseBuilder(AwanNotificationChannels.SESSION_REMINDERS, session)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(snoozeAction(session, id, now))
                .addAction(rescheduleAction(session)),
            toastTitle = session.title,
            toastMessage = text,
        )
    }

    /**
     * A plain ongoing notification. Phase 4 promotes it to a Live Update with a progress bar; the
     * countdown here already comes from the system chronometer.
     */
    private fun postLive(session: UpcomingSession, now: LocalDateTime) {
        val builder = baseBuilder(AwanNotificationChannels.SESSION_LIVE, session)
            .setContentText(timeFormatter.timeRange(context, session))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(session.end.toEpochMillis())
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // No toast: this redraws every minute, and an in-app banner every minute is unusable.
        notify(id = NotificationIds.live(session.id), builder = builder)
    }

    private fun postEnded(session: UpcomingSession, now: LocalDateTime) {
        val title = context.getString(R.string.notifications_session_ended_title, session.title)
        val text = context.getString(
            R.string.notifications_session_ended_text,
            timeFormatter.timeRange(context, session),
        )
        val id = NotificationIds.ended(session.id)

        notify(
            id = id,
            builder = baseBuilder(AwanNotificationChannels.SESSION_END, session)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(
                    action(
                        iconRes = R.drawable.ic_notification_check,
                        labelRes = R.string.notifications_action_complete,
                        session = session,
                        type = NotificationAction.COMPLETE,
                        notificationId = id,
                        now = now,
                    )
                )
                .addAction(rescheduleAction(session)),
            toastTitle = title,
            toastMessage = text,
        )
    }

    private fun snoozeAction(session: UpcomingSession, notificationId: Int, now: LocalDateTime) =
        action(
            iconRes = R.drawable.ic_notification_snooze,
            labelRes = R.string.notifications_action_snooze,
            session = session,
            type = NotificationAction.SNOOZE,
            notificationId = notificationId,
            now = now,
        )

    /**
     * Rescheduling needs a time picker, so this opens the session's detail sheet rather than trying
     * to guess a new slot from a notification button.
     */
    private fun rescheduleAction(session: UpcomingSession) = NotificationCompat.Action.Builder(
        R.drawable.ic_notification_reschedule,
        context.getString(R.string.notifications_action_reschedule),
        NotificationIntents.openSession(context, session),
    ).build()

    private fun action(
        iconRes: Int,
        labelRes: Int,
        session: UpcomingSession,
        type: NotificationAction,
        notificationId: Int,
        now: LocalDateTime,
    ) = NotificationCompat.Action.Builder(
        iconRes,
        context.getString(labelRes),
        NotificationIntents.action(context, session, type, notificationId, now),
    ).build()

    /** Posts a payload that arrived over FCM — rewards and the daily wheel. */
    fun postRemote(title: String, body: String) {
        channels.ensureCreated()
        notify(
            id = REMOTE_NOTIFICATION_ID,
            builder = NotificationCompat.Builder(context, AwanNotificationChannels.REWARDS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchIntent()),
            toastTitle = title,
            toastMessage = body,
        )
    }

    private fun baseBuilder(channelId: String, session: UpcomingSession) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(session.title)
            .setContentIntent(NotificationIntents.openSession(context, session))

    /**
     * Posts, and mirrors into the in-app banner when the app is open so a notification is not
     * something the user only sees by pulling down the shade over their own screen.
     */
    private fun notify(
        id: Int,
        builder: NotificationCompat.Builder,
        toastTitle: String? = null,
        toastMessage: String? = null,
    ) {
        if (!manager.areNotificationsEnabled()) {
            Log.i(TAG, "Notifications are disabled; skipping $id")
            return
        }
        try {
            manager.notify(id, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check above and here.
            Log.w(TAG, "Missing POST_NOTIFICATIONS; dropped notification $id", e)
        }

        if (toastMessage != null && isAppInForeground()) {
            AwanToastManager.showToast(title = toastTitle, message = toastMessage)
        }
    }

    private fun launchIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
            ?: return null
        return PendingIntent.getActivity(
            context,
            LAUNCH_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun isAppInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        const val TAG = "AwanNotifications"
        const val LAUNCH_REQUEST_CODE = 2001

        /** Remote payloads share one id: the newest reward replaces the last rather than stacking. */
        const val REMOTE_NOTIFICATION_ID = 9001
    }
}
