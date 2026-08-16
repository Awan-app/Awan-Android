package com.awan.app.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.notifications.SessionNotificationPoster
import com.awan.app.core.notifications.model.AwanNotificationEvent
import com.awan.app.core.notifications.model.DayNotificationEvent
import com.awan.app.core.notifications.model.SessionNotificationEvent
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase

/**
 * Fires any notification immediately, without waiting for its moment.
 *
 * Everything this engine does is time-gated, and the emulator images used here cannot have their
 * clock moved (`adb root` and `su` are both unavailable), so the end-of-day nudge and the morning
 * brief were previously untestable on a device at all.
 *
 * Debug source set only — it is not compiled into a release build, which is also why it can be
 * exported without being a way in.
 *
 * ```
 * adb shell am broadcast -a com.awan.app.DEBUG_NOTIFICATION --es kind streak
 * ```
 *
 * Kinds: `reminder`, `live`, `ended`, `followup`, `streak`, `brief`.
 */
@AndroidEntryPoint
class DebugNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var poster: SessionNotificationPoster

    @Inject
    lateinit var getNotificationPreferences: GetNotificationPreferencesUseCase

    @Inject
    lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND).orEmpty()
        val now = LocalDateTime.now(clock)
        val event = eventFor(kind, now) ?: run {
            Log.w(TAG, "Unknown debug notification kind '$kind'")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                poster.post(event, now, getNotificationPreferences().first())
                Log.d(TAG, "Posted debug notification '$kind'")
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Built against a session that does not exist, on purpose: its buttons would move or complete a
     * real session, and a test notification must not be able to.
     */
    private fun eventFor(kind: String, now: LocalDateTime): AwanNotificationEvent? {
        val session = UpcomingSession(
            id = DEBUG_SESSION_ID,
            taskId = DEBUG_SESSION_ID,
            title = "Debug session",
            start = now.plusMinutes(NotificationPreferences.DEFAULT_REMINDER_LEAD_MINUTES.toLong()),
            end = now.plusMinutes(SESSION_LENGTH_MINUTES),
            status = SessionStatus.SCHEDULED,
            zoneId = null,
        )

        return when (kind) {
            "reminder" -> SessionNotificationEvent.Reminder(now, session)
            "live" -> SessionNotificationEvent.Live(
                at = now,
                session = session.copy(start = now.minusMinutes(SESSION_LENGTH_MINUTES / 2)),
            )
            "ended" -> SessionNotificationEvent.Ended(now, session.copy(end = now))
            "followup" -> SessionNotificationEvent.FollowUp(now, session.copy(end = now))
            "streak" -> DayNotificationEvent.StreakRisk(now, now.toLocalDate())
            "brief" -> DayNotificationEvent.DailyBrief(
                at = now,
                date = now.toLocalDate(),
                slot = DayNotificationEvent.DailyBrief.Slot.MORNING,
                plannedCount = 1,
                firstTitle = session.title,
                firstStart = session.start,
            )
            else -> null
        }
    }

    private companion object {
        const val TAG = "AwanNotifications"
        const val EXTRA_KIND = "kind"
        const val DEBUG_SESSION_ID = "debug-session"
        const val SESSION_LENGTH_MINUTES = 60L
    }
}
