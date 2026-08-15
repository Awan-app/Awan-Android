package com.awan.app.core.notifications

import android.util.Log
import com.awan.app.core.common.di.ApplicationScope
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.domain.notifications.usecase.ObserveScheduleKnownUseCase
import com.awan.app.core.domain.notifications.usecase.ObserveUpcomingSessionsUseCase
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Keeps the alarm chain in step with the data.
 *
 * The alternative — calling the scheduler from each place that writes a session — means six call
 * sites today (two repositories, the task-creation paths, the AI path, and the sync coordinator) and
 * a seventh that someone forgets. Watching the table instead means every write, wherever it comes
 * from, retimes the notifications.
 */
@Singleton
class SessionNotificationStarter @Inject constructor(
    private val observeUpcomingSessions: ObserveUpcomingSessionsUseCase,
    private val getNotificationPreferences: GetNotificationPreferencesUseCase,
    private val observeScheduleKnown: ObserveScheduleKnownUseCase,
    private val scheduler: SessionNotificationScheduler,
    private val channels: AwanNotificationChannels,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {

    fun start() {
        // Up front, not on first post: until a channel exists the system notification settings page
        // is empty, so a user who opened it before their first session had nothing to configure.
        channels.ensureCreated()

        scope.launch {
            val today = LocalDate.now(clock)
            combine(
                observeUpcomingSessions(today, today.plusDays(OBSERVED_DAYS)),
                getNotificationPreferences(),
                // Watched separately from the sessions: a day that syncs empty writes the same
                // nothing back to the sessions table and emits no change, so the day notifications
                // gated on this flag would wait for the next app foreground to be planned at all.
                observeScheduleKnown(today),
            ) { sessions, preferences, scheduleKnown -> Triple(sessions, preferences, scheduleKnown) }
                .distinctUntilChanged()
                .catch { Log.e(TAG, "Session notification observation failed", it) }
                .collect { scheduler.rescheduleAll() }
        }
    }

    private companion object {
        const val TAG = "AwanNotifications"

        /**
         * Wider than the scheduler's own lookahead: the query window is fixed at process start, and
         * a process that stays alive past midnight must still see the following day's sessions.
         */
        const val OBSERVED_DAYS = 2L
    }
}
