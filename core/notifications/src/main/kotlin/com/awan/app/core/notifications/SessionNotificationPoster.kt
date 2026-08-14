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
import com.awan.app.core.notifications.model.AwanNotificationEvent
import com.awan.app.core.notifications.model.DayNotificationEvent
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

    fun post(event: AwanNotificationEvent, now: LocalDateTime) {
        channels.ensureCreated()
        when (event) {
            is SessionNotificationEvent.Reminder -> postReminder(event.session, now)
            is SessionNotificationEvent.Live -> postLive(event.session, now)
            is SessionNotificationEvent.Ended -> postEnded(event.session, now)
            is SessionNotificationEvent.FollowUp -> postFollowUp(event.session, now)
            is DayNotificationEvent.StreakRisk -> postStreakRisk(event)
            is DayNotificationEvent.DailyBrief -> postDailyBrief(event)
        }
    }

    fun cancel(notificationId: Int) = manager.cancel(SESSION_TAG, notificationId)

    /**
     * Ids of the session notifications currently showing. The scheduler diffs this against the plan,
     * which is how a deleted or rescheduled session loses its notification without anything being
     * persisted.
     *
     * Matching on the tag rather than on a list of known session ids is what makes deletion work: a
     * session removed from Room contributes no id to compare against, so an id-based filter would
     * leave its notification stranded on screen forever. The tag also keeps the reward notification
     * from being swept up as collateral.
     */
    fun postedSessionIds(): Set<Int> = runCatching {
        manager.activeNotifications.filter { it.tag == SESSION_TAG }.map { it.id }.toSet()
    }.getOrElse {
        // Some OEM builds throw here rather than returning empty. Having nothing to reconcile
        // against is recoverable; crashing an alarm receiver is not.
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
     * The running-session "live activity".
     *
     * On Android 16+ `setRequestPromotedOngoing` plus [NotificationCompat.ProgressStyle] make this a
     * Live Update — a chip in the status bar and a card on the lock screen, which is the platform's
     * counterpart to a Dynamic Island Live Activity. `NotificationCompat` no-ops those calls below
     * API 36, where it stays an ordinary ongoing notification with the same content.
     *
     * The countdown comes from the system chronometer, not from the tick: that keeps the remaining
     * time exact even when Doze throttles the alarm chain and no tick arrives for several minutes.
     * Ticks only advance the progress bar.
     */
    private fun postLive(session: UpcomingSession, now: LocalDateTime) {
        // Seconds, not minutes: a bar scaled in minutes can only ever move in whole-minute steps, so
        // it sat still between ticks however often the notification was redrawn.
        val totalSeconds = Duration.between(session.start, session.end).seconds
            .coerceAtLeast(1)
            .toInt()
        val elapsedSeconds = Duration.between(session.start, now).seconds
            .coerceIn(0, totalSeconds.toLong())
            .toInt()
        val remainingMinutes = Duration.ofSeconds((totalSeconds - elapsedSeconds).toLong())
            .toMinutes()
            .toInt()
        val id = NotificationIds.live(session.id)

        val builder = baseBuilder(AwanNotificationChannels.SESSION_LIVE, session)
            .setContentText(timeFormatter.timeRange(context, session))
            .setOngoing(true)
            // Without this the channel re-alerts on every one-minute redraw.
            .setOnlyAlertOnce(true)
            .setWhen(session.end.toEpochMillis())
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShortCriticalText(
                context.getString(R.string.notifications_live_short_remaining, remainingMinutes)
            )
            .setRequestPromotedOngoing(true)
            .setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgress(elapsedSeconds)
                    .addProgressSegment(NotificationCompat.ProgressStyle.Segment(totalSeconds))
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                action(
                    iconRes = R.drawable.ic_notification_check,
                    labelRes = R.string.notifications_action_stop_here,
                    session = session,
                    type = NotificationAction.STOP_HERE,
                    notificationId = id,
                    now = now,
                )
            )

        // No toast: this redraws every minute, and an in-app banner every minute is unusable.
        notify(id = id, builder = builder)
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

    /**
     * The softer second ask, well after the session ended and nobody answered the first one.
     *
     * Same actions as [postEnded], different tone: by now the user has moved on, so this asks how it
     * went rather than announcing that the time is up.
     */
    private fun postFollowUp(session: UpcomingSession, now: LocalDateTime) {
        val title = context.getString(R.string.notifications_follow_up_title, session.title)
        val text = context.getString(
            R.string.notifications_follow_up_text,
            timeFormatter.timeRange(context, session),
        )
        val id = NotificationIds.followUp(session.id)

        notify(
            id = id,
            builder = baseBuilder(AwanNotificationChannels.SESSION_END, session)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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

    /**
     * Nothing finished today and the day is nearly over.
     *
     * Says "finish one thing", never "your streak is N": the streak belongs to the server and this
     * device only knows what it has seen completed locally.
     */
    private fun postStreakRisk(event: DayNotificationEvent.StreakRisk) {
        val title = context.getString(R.string.notifications_streak_risk_title)
        val text = context.getString(R.string.notifications_streak_risk_text)

        notify(
            id = NotificationIds.streakRisk(event.date),
            builder = dayBuilder(title, text),
            toastTitle = title,
            toastMessage = text,
        )
    }

    private fun postDailyBrief(event: DayNotificationEvent.DailyBrief) {
        val title = context.getString(R.string.notifications_daily_brief_title)
        val text = if (event.plannedCount == 0 || event.firstTitle == null) {
            context.getString(R.string.notifications_daily_brief_empty_text)
        } else {
            context.resources.getQuantityString(
                R.plurals.notifications_daily_brief_text,
                event.plannedCount,
                event.plannedCount,
                event.firstTitle,
                event.firstStart?.let { timeFormatter.time(context, it) }.orEmpty(),
            )
        }

        notify(
            id = NotificationIds.dailyBrief(event.date, event.slot),
            builder = dayBuilder(title, text),
            toastTitle = title,
            toastMessage = text,
        )
    }

    /** Shape shared by the notifications that are about the day rather than about one session. */
    private fun dayBuilder(title: String, text: String) =
        NotificationCompat.Builder(context, AwanNotificationChannels.NUDGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(launchIntent())

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
            tag = REMOTE_TAG,
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
        tag: String = SESSION_TAG,
        toastTitle: String? = null,
        toastMessage: String? = null,
    ) {
        if (!manager.areNotificationsEnabled()) {
            Log.i(TAG, "Notifications are disabled; skipping $id")
            return
        }
        try {
            manager.notify(tag, id, builder.build())
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

        /** Marks the notifications the scheduler owns and may cancel during reconciliation. */
        const val SESSION_TAG = "awan-session"

        /** Deliberately a different tag, so reconciliation never cancels a reward notification. */
        const val REMOTE_TAG = "awan-remote"

        /** Remote payloads share one id: the newest reward replaces the last rather than stacking. */
        const val REMOTE_NOTIFICATION_ID = 9001
    }
}
