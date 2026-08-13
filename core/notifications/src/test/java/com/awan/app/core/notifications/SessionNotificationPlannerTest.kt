package com.awan.app.core.notifications

import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.notifications.model.SessionNotificationEvent
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionNotificationPlannerTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 13, 9, 0)
    private val preferences = NotificationPreferences()

    private fun session(
        id: String = "s1",
        start: LocalDateTime = now.plusMinutes(30),
        end: LocalDateTime = now.plusMinutes(90),
        status: SessionStatus = SessionStatus.SCHEDULED,
    ) = UpcomingSession(
        id = id,
        taskId = "t1",
        title = "Deep Work",
        start = start,
        end = end,
        status = status,
        zoneId = "z1",
    )

    @Test
    fun `reminder is planned at the configured lead time before the start`() {
        val session = session(start = now.plusMinutes(30))

        val reminder = SessionNotificationPlanner
            .plan(listOf(session), preferences.copy(reminderLeadMinutes = 15), now)
            .filterIsInstance<SessionNotificationEvent.Reminder>()
            .single()

        assertEquals(session.start.minusMinutes(15), reminder.at)
    }

    @Test
    fun `a completed session plans nothing`() {
        val plan = SessionNotificationPlanner.plan(
            listOf(session(status = SessionStatus.COMPLETED)),
            preferences,
            now,
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `a cancelled session plans nothing`() {
        val plan = SessionNotificationPlanner.plan(
            listOf(session(status = SessionStatus.CANCELLED)),
            preferences,
            now,
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `each preference removes only its own events`() {
        val only = { prefs: NotificationPreferences ->
            SessionNotificationPlanner.plan(listOf(session()), prefs, now)
        }

        assertTrue(
            only(preferences.copy(sessionRemindersEnabled = false))
                .none { it is SessionNotificationEvent.Reminder }
        )
        assertTrue(
            only(preferences.copy(sessionRemindersEnabled = false))
                .any { it is SessionNotificationEvent.Live }
        )
        assertTrue(
            only(preferences.copy(sessionLiveActivityEnabled = false))
                .none { it is SessionNotificationEvent.Live }
        )
        assertTrue(
            only(preferences.copy(sessionEndEnabled = false))
                .none { it is SessionNotificationEvent.Ended }
        )
        assertTrue(
            only(
                preferences.copy(
                    sessionRemindersEnabled = false,
                    sessionLiveActivityEnabled = false,
                    sessionEndEnabled = false,
                )
            ).isEmpty()
        )
    }

    @Test
    fun `a stale reminder outside the grace window is not replayed`() {
        // The device rebooted long after the reminder's moment passed.
        val session = session(start = now.plusMinutes(1))
        val reminder = SessionNotificationEvent.Reminder(now.minusHours(3), session)

        assertFalse(SessionNotificationPlanner.isDue(reminder, now))
    }

    @Test
    fun `a reminder just inside the grace window is still due`() {
        val session = session(start = now.plusMinutes(8))
        val reminder = SessionNotificationEvent.Reminder(now.minusMinutes(2), session)

        assertTrue(SessionNotificationPlanner.isDue(reminder, now))
    }

    @Test
    fun `a reminder for a session that already started is not due`() {
        val session = session(start = now.minusMinutes(1), end = now.plusMinutes(59))
        val reminder = SessionNotificationEvent.Reminder(now.minusMinutes(2), session)

        assertFalse(SessionNotificationPlanner.isDue(reminder, now))
    }

    @Test
    fun `a session spanning now ticks one minute out`() {
        val session = session(start = now.minusMinutes(10), end = now.plusMinutes(50))

        val live = SessionNotificationPlanner.plan(listOf(session), preferences, now)
            .filterIsInstance<SessionNotificationEvent.Live>()
            .single()

        assertEquals(now.plusMinutes(1), live.at)
        assertTrue(SessionNotificationPlanner.isDue(live.copy(at = now), now))
    }

    @Test
    fun `a live event for a session that already ended is not due`() {
        val session = session(start = now.minusHours(2), end = now.minusMinutes(1))
        val live = SessionNotificationEvent.Live(now.minusMinutes(1), session)

        assertFalse(SessionNotificationPlanner.isDue(live, now))
    }

    @Test
    fun `a live event before the session starts waits for the start`() {
        val session = session(start = now.plusMinutes(30))

        val live = SessionNotificationPlanner.plan(listOf(session), preferences, now)
            .filterIsInstance<SessionNotificationEvent.Live>()
            .single()

        assertEquals(session.start, live.at)
    }

    @Test
    fun `an ended event is due only within its grace window`() {
        val session = session(start = now.minusHours(2), end = now.minusMinutes(5))

        assertTrue(
            SessionNotificationPlanner.isDue(
                SessionNotificationEvent.Ended(session.end, session),
                now,
            )
        )
        assertFalse(
            SessionNotificationPlanner.isDue(
                SessionNotificationEvent.Ended(now.minusHours(4), session),
                now,
            )
        )
    }

    @Test
    fun `the plan is ordered so the scheduler can take the first future event`() {
        val early = session(id = "early", start = now.plusMinutes(20), end = now.plusMinutes(50))
        val late = session(id = "late", start = now.plusHours(4), end = now.plusHours(5))

        val plan = SessionNotificationPlanner.plan(listOf(late, early), preferences, now)

        assertEquals(plan.map { it.at }.sorted(), plan.map { it.at })
    }

    @Test
    fun `no reminder is planned when the lead time is zero minutes away from the start`() {
        val session = session(start = now.plusMinutes(30))

        val plan = SessionNotificationPlanner.plan(
            listOf(session),
            preferences.copy(reminderLeadMinutes = 0),
            now,
        )

        assertTrue(plan.none { it is SessionNotificationEvent.Reminder })
    }
}
