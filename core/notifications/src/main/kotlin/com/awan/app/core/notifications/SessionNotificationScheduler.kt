package com.awan.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.domain.notifications.usecase.GetUpcomingSessionsUseCase
import com.awan.app.core.notifications.model.SessionNotificationEvent
import com.awan.app.core.notifications.receiver.SessionAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The whole local-notification engine.
 *
 * Holds **one** exact alarm at a time — whatever has to happen next — and rebuilds everything from
 * Room each time it runs. Nothing about which notifications exist is persisted, because it cannot be:
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
    private val poster: SessionNotificationPoster,
    private val clock: Clock,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Room emissions, the foreground catch-up and an alarm can land at once; without this they
    // interleave and the loser's alarm overwrites the winner's.
    private val mutex = Mutex()

    suspend fun rescheduleAll() = withContext(ioDispatcher) {
        mutex.withLock {
            val now = LocalDateTime.now(clock)
            val preferences = getNotificationPreferences().first()
            // Always at least a full day of lookahead, so the last alarm of today can still find
            // tomorrow's first session.
            val today = now.toLocalDate()
            val sessions = getUpcomingSessions(today, today.plusDays(LOOKAHEAD_DAYS))

            val plan = SessionNotificationPlanner.plan(sessions, preferences, now)
            val (due, upcoming) = plan.partition { SessionNotificationPlanner.isDue(it, now) }

            due.forEach { poster.post(it, now) }
            cancelStale(due, sessions.map { it.id }.toSet())
            scheduleNext(upcoming.firstOrNull(), now)
        }
    }

    /**
     * Drops anything showing that the plan no longer produced.
     *
     * This is what makes "delete a session and its reminder disappears" true: the session is gone
     * from Room, so it is gone from the plan, so its id is not in [due], so the posted notification
     * is cancelled. Same for a session that was completed, cancelled, or moved out of its window.
     */
    private fun cancelStale(due: List<SessionNotificationEvent>, knownSessionIds: Set<String>) {
        val shouldBeShowing = due.map { NotificationIds.forEvent(it) }.toSet()
        val posted = poster.postedIds()
        // Only ids this engine owns, so an FCM reward notification is never collateral.
        val ownedIds = knownSessionIds.flatMap { NotificationIds.allFor(it) }.toSet() + shouldBeShowing
        posted.filter { it in ownedIds && it !in shouldBeShowing }.forEach(poster::cancel)
    }

    private fun scheduleNext(next: SessionNotificationEvent?, now: LocalDateTime) {
        val pendingIntent = alarmPendingIntent()
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

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, SessionAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "AwanNotifications"
        const val ALARM_REQUEST_CODE = 3001
        const val LOOKAHEAD_DAYS = 1L
        const val MIN_DELAY_MS = 1_000L
    }
}
