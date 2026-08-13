package com.awan.app.core.notifications

import android.util.Log
import com.awan.app.core.common.di.ApplicationScope
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
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
            ) { sessions, preferences -> sessions to preferences }
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
