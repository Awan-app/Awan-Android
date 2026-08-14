package com.awan.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.awan.app.core.common.di.ApplicationScope
import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.domain.notifications.usecase.GetUpcomingSessionsUseCase
import com.awan.app.core.domain.notifications.usecase.IsScheduleKnownUseCase
import com.awan.app.core.domain.profile.usecase.ObserveProfileUseCase
import com.awan.app.core.notifications.model.AwanNotificationEvent
import com.awan.app.core.notifications.model.NotificationDayContext
import com.awan.app.core.notifications.model.SessionNotificationEvent
import com.awan.app.core.notifications.receiver.SessionAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The whole local-notification engine.
 *
 * Holds **two** exact alarms — the next moment the user is waiting for, and the next progress redraw
 * for a session already running — and rebuilds everything from Room each time it runs. They are
 * separate because a running session redraws every minute, which would otherwise monopolise a single
 * slot and leave the next reminder unregistered with the OS until the session ended.
 *
 * Nothing about which notifications exist is persisted, because it cannot be:
 * `SessionDao.replaceSessionsForDates` deletes and rewrites entire date ranges on every sync, so any
 * per-session bookkeeping would be destroyed underneath us.
 *
 * [rescheduleAll] is therefore idempotent and self-healing, and is the only entry point. It runs on
 * an alarm firing, on boot, on app foreground, on a preference change, on a timezone change, and on
 * every write to the sessions table.
 */
@Singleton
class SessionNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getUpcomingSessions: GetUpcomingSessionsUseCase,
    private val getNotificationPreferences: GetNotificationPreferencesUseCase,
    private val isScheduleKnown: IsScheduleKnownUseCase,
    private val observeProfile: ObserveProfileUseCase,
    private val poster: SessionNotificationPoster,
    private val dismissals: LiveNotificationDismissals,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Room emissions, the foreground catch-up and an alarm can land at once; without this they
    // interleave and the loser's alarm overwrites the winner's.
    private val mutex = Mutex()

    private var liveTicker: Job? = null

    suspend fun rescheduleAll() = withContext(ioDispatcher) {
        mutex.withLock {
            val now = LocalDateTime.now(clock)
            val preferences = getNotificationPreferences().first()
            // Always at least a full day of lookahead, so the last alarm of today can still find
            // tomorrow's first session.
            val today = now.toLocalDate()
            val sessions = getUpcomingSessions(today, today.plusDays(LOOKAHEAD_DAYS))

            val plan = SessionNotificationPlanner.plan(
                sessions = sessions,
                preferences = preferences,
                now = now,
                day = dayContext(today),
                // Reading also prunes: this runs on every alarm, every session write and every app
                // foreground, so a dismissal outlives its session by one reschedule at most.
                dismissedLive = dismissals.current(now),
            )

            val due = plan.filter { SessionNotificationPlanner.isDue(it, now) }

            // Snapshot before posting, so "already showing" means showing when we arrived.
            val alreadyShowing = poster.postedSessionIds()
            due.forEach { event ->
                // An event stays due for its whole grace window, and this runs on every session
                // write, every preference change and every app foreground. Re-posting the same id
                // updates the notification in place, which on an alerting channel means it buzzes
                // and peeks again for something the user is already looking at.
                //
                // The live notification is the exception: redrawing it is the entire point, and its
                // channel is silent and only-alert-once, so it costs nothing.
                val isRedraw = event is SessionNotificationEvent.Live
                if (isRedraw || NotificationIds.forEvent(event) !in alreadyShowing) {
                    poster.post(event, now)
                }
            }
            cancelStale(due, alreadyShowing)
            // Two slots, not one. See SessionNotificationPlanner.nextUserEventAfter: sharing a slot
            // means a running session's per-minute ticks keep the next reminder from ever reaching
            // the OS, so one dropped tick loses the reminder entirely.
            schedule(
                requestCode = USER_ALARM_REQUEST_CODE,
                next = SessionNotificationPlanner.nextUserEventAfter(plan, now),
                now = now,
            )
            schedule(
                requestCode = TICK_ALARM_REQUEST_CODE,
                next = SessionNotificationPlanner.nextTickAfter(plan, now),
                now = now,
            )
            driveLiveWhileInProcess(due.filterIsInstance<SessionNotificationEvent.Live>().firstOrNull())
        }
    }

    /**
     * When the user's day starts and ends, and whether today's schedule has actually been fetched.
     *
     * The profile flow is Room-backed, so this stays correct with the radio off. A device that has
     * never synced a profile — onboarding writes it to the backend, not to Room — has no wake or
     * sleep time at all, and `CalendarLocalDataSourceImpl` can write an empty string, so both are
     * treated as absent and fall back to the same defaults `DayBounds` uses.
     */
    private suspend fun dayContext(today: LocalDate): NotificationDayContext {
        val preferences = runCatching { observeProfile().firstOrNull()?.preferences }
            .onFailure { Log.w(TAG, "Could not read the profile; using default day bounds", it) }
            .getOrNull()

        return NotificationDayContext(
            today = today,
            wake = preferences?.wakeupTime.toLocalTimeOrNull()
                ?: NotificationDayContext.DEFAULT_WAKE,
            dayEnd = preferences?.sleepTime.toLocalTimeOrNull()
                ?: NotificationDayContext.DEFAULT_DAY_END,
            scheduleKnown = isScheduleKnown(today),
        )
    }

    private fun String?.toLocalTimeOrNull(): LocalTime? =
        this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    /**
     * Redraws the running-session notification from inside the process, between alarm ticks.
     *
     * The alarm chain is what keeps the notification correct when nothing of ours is running, but it
     * is coarse and the OS is free to deliver it late — which is what left the progress bar lagging
     * the countdown beside it. Whenever the process is alive, which is most of the time the user is
     * actually looking at the notification, this drives it instead.
     *
     * Cancelled and restarted on every reschedule, so it always holds current session data, and it
     * stops itself at the end of the session rather than running for as long as the process does.
     */
    private fun driveLiveWhileInProcess(live: SessionNotificationEvent.Live?) {
        liveTicker?.cancel()
        if (live == null) return

        liveTicker = scope.launch {
            while (isActive) {
                delay(IN_PROCESS_TICK_MS)
                val now = LocalDateTime.now(clock)
                if (!now.isBefore(live.session.end)) break
                poster.post(live, now)
            }
        }
    }

    /**
     * Drops anything showing that the plan no longer justifies.
     *
     * This is what makes "delete a session and its reminder disappears" true: the session is gone
     * from Room, so it produces no event, so its notification id is not in [due] and gets cancelled.
     * Same for a session that was completed, cancelled, or moved out of its window.
     *
     * Deliberately driven by what is on screen rather than by the sessions Room still knows about —
     * a deleted session contributes nothing to compare against, so anything keyed off the surviving
     * rows would strand exactly the notification this is meant to clear.
     */
    private fun cancelStale(due: List<AwanNotificationEvent>, posted: Set<Int>) {
        val shouldBeShowing = due.map { NotificationIds.forEvent(it) }.toSet()
        posted.filterNot { it in shouldBeShowing }.forEach(poster::cancel)
    }

    private fun schedule(requestCode: Int, next: AwanNotificationEvent?, now: LocalDateTime) {
        val pendingIntent = alarmPendingIntent(requestCode)
        if (next == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerAt = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            // An event in the past that was not due (outside its grace window) must not schedule an
            // alarm for a moment that has gone; nudge it forward so the chain keeps moving.
            .coerceAtLeast(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + MIN_DELAY_MS)

        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // USE_EXACT_ALARM makes this unreachable on API 33+, but the permission can still be
            // absent on 31–32, and a late notification beats none.
            Log.w(TAG, "Exact alarms unavailable; falling back to inexact")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmPendingIntent(requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, SessionAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "AwanNotifications"
        /** Reminders, session starts and session ends — the moments the user is waiting for. */
        const val USER_ALARM_REQUEST_CODE = 3001

        /** Progress redraws for a session already running. Losing one costs a stale bar. */
        const val TICK_ALARM_REQUEST_CODE = 3002
        const val LOOKAHEAD_DAYS = 1L
        const val MIN_DELAY_MS = 1_000L

        /**
         * Fine enough that the bar visibly tracks the countdown, coarse enough that it is a handful
         * of notification updates a minute rather than a redraw loop.
         */
        const val IN_PROCESS_TICK_MS = 5_000L
    }
}
