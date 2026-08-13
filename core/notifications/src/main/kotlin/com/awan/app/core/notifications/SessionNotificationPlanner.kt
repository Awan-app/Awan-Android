package com.awan.app.core.notifications

import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.notifications.model.SessionNotificationEvent
import java.time.Duration
import java.time.LocalDateTime

/**
 * Turns the cached schedule into the full list of notification events, past and future.
 *
 * Pure and Android-free so the timing rules — which are the part that actually goes wrong — can be
 * tested without a device, an alarm, or a clock.
 *
 * The scheduler posts the events that are already [due], and sets a single alarm for the first one
 * that is not.
 */
object SessionNotificationPlanner {

    /** How long after its moment an event is still worth firing. */
    val REMINDER_GRACE: Duration = Duration.ofMinutes(5)
    val ENDED_GRACE: Duration = Duration.ofMinutes(30)

    /** Cadence of the live notification's progress bar. */
    val LIVE_TICK: Duration = Duration.ofMinutes(1)

    fun plan(
        sessions: List<UpcomingSession>,
        preferences: NotificationPreferences,
        now: LocalDateTime,
    ): List<SessionNotificationEvent> =
        sessions.flatMap { eventsFor(it, preferences, now) }.sortedBy { it.at }

    /**
     * True when [event] should be showing right now. Past events outside their grace window are
     * deliberately not replayed — after a reboot at noon, this morning's reminders are noise.
     */
    fun isDue(event: SessionNotificationEvent, now: LocalDateTime): Boolean {
        if (event.at.isAfter(now)) return false
        return when (event) {
            is SessionNotificationEvent.Reminder ->
                // Never fire a reminder for a session that has already started.
                now.isBefore(event.session.start) && withinGrace(event.at, now, REMINDER_GRACE)
            is SessionNotificationEvent.Live -> now.isBefore(event.session.end)
            is SessionNotificationEvent.Ended -> withinGrace(event.at, now, ENDED_GRACE)
        }
    }

    private fun withinGrace(at: LocalDateTime, now: LocalDateTime, grace: Duration): Boolean =
        Duration.between(at, now) <= grace

    private fun eventsFor(
        session: UpcomingSession,
        preferences: NotificationPreferences,
        now: LocalDateTime,
    ): List<SessionNotificationEvent> {
        // A session the user already closed out, or the engine cancelled, has nothing left to say.
        if (session.status == SessionStatus.COMPLETED || session.status == SessionStatus.CANCELLED) {
            return emptyList()
        }

        val events = mutableListOf<SessionNotificationEvent>()

        if (preferences.sessionRemindersEnabled) {
            val remindAt = session.start.minusMinutes(preferences.reminderLeadMinutes.toLong())
            // A lead time longer than the gap to the session would put the reminder in the past on
            // a session created moments before it starts; there is nothing useful to fire then.
            if (remindAt.isBefore(session.start)) {
                events += SessionNotificationEvent.Reminder(remindAt, session)
            }
        }

        if (preferences.sessionLiveActivityEnabled) {
            events += SessionNotificationEvent.Live(nextLiveMoment(session, now), session)
        }

        if (preferences.sessionEndEnabled) {
            events += SessionNotificationEvent.Ended(session.end, session)
        }

        return events
    }

    /**
     * The next moment the live notification needs redrawing: the session's start if it has not begun,
     * otherwise one tick from now. Ticks are computed off [now] rather than off the start time so a
     * long-running session does not accumulate a queue of missed ticks to catch up on.
     */
    private fun nextLiveMoment(session: UpcomingSession, now: LocalDateTime): LocalDateTime =
        if (now.isBefore(session.start)) session.start else now.plus(LIVE_TICK)
}
