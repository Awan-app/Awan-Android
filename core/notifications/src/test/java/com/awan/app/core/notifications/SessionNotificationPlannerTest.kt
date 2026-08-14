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
    }

    @Test
    fun `a running session's live event is due even though its next tick is in the future`() {
        // The regression that hid the live notification entirely: `at` is the next redraw, always a
        // tick ahead while the session runs, so judging it like the other events by `at <= now`
        // meant it was never due and the notification never appeared.
        val session = session(start = now.minusMinutes(10), end = now.plusMinutes(50))

        val live = SessionNotificationPlanner.plan(listOf(session), preferences, now)
            .filterIsInstance<SessionNotificationEvent.Live>()
            .single()

        assertTrue(live.at.isAfter(now))
        assertTrue(SessionNotificationPlanner.isDue(live, now))
    }

    @Test
    fun `a live event becomes due the moment the session starts`() {
        val session = session(start = now, end = now.plusMinutes(60))

        val live = SessionNotificationPlanner.plan(listOf(session), preferences, now)
            .filterIsInstance<SessionNotificationEvent.Live>()
            .single()

        assertTrue(SessionNotificationPlanner.isDue(live, now))
    }

    @Test
    fun `a live event is not due before the session starts`() {
        val session = session(start = now.plusMinutes(30), end = now.plusMinutes(90))

        val live = SessionNotificationPlanner.plan(listOf(session), preferences, now)
            .filterIsInstance<SessionNotificationEvent.Live>()
            .single()

        assertFalse(SessionNotificationPlanner.isDue(live, now))
        assertEquals(session.start, live.at)
    }

    @Test
    fun `a live event for a session that already ended is not due`() {
        val session = session(start = now.minusHours(2), end = now.minusMinutes(1))
        val live = SessionNotificationEvent.Live(now.minusMinutes(1), session)

        assertFalse(SessionNotificationPlanner.isDue(live, now))
    }

    @Test
    fun `a session that already ended plans no live tick`() {
        // Otherwise the tick sits a minute in the future forever, is never due, and the scheduler
        // burns an exact alarm every minute on a session that is over.
        val session = session(start = now.minusHours(2), end = now.minusMinutes(1))

        val plan = SessionNotificationPlanner.plan(listOf(session), preferences, now)

        assertTrue(plan.none { it is SessionNotificationEvent.Live })
    }

    @Test
    fun `the next alarm is never set for a moment that has already passed`() {
        // A plan legitimately holds past events. Picking one as the next alarm would fire it at
        // once, find it still not due, and reschedule it forever.
        val sessions = listOf(
            session(id = "over", start = now.minusHours(3), end = now.minusHours(2)),
            session(id = "running", start = now.minusMinutes(5), end = now.plusMinutes(25)),
            session(id = "soon", start = now.plusMinutes(3), end = now.plusMinutes(60)),
            session(id = "later", start = now.plusHours(6), end = now.plusHours(7)),
        )
        val plan = SessionNotificationPlanner.plan(sessions, preferences, now)

        // Guard the premise: this plan really does contain the stranded past events.
        assertTrue(plan.any { !SessionNotificationPlanner.isDue(it, now) && !it.at.isAfter(now) })

        listOfNotNull(
            SessionNotificationPlanner.nextUserEventAfter(plan, now),
            SessionNotificationPlanner.nextTickAfter(plan, now),
        ).forEach { next ->
            assertTrue("next alarm is in the past: $next", next.at.isAfter(now))
        }
    }

    @Test
    fun `a running session's ticks never displace the next reminder`() {
        // Why the two alarms are separate: a single slot always held the running session's next
        // tick, because a tick is a minute away and the reminder is not, so the reminder never
        // reached the OS and one dropped tick lost it entirely.
        val running = session(id = "running", start = now.minusMinutes(5), end = now.plusMinutes(55))
        val later = session(id = "later", start = now.plusHours(2), end = now.plusHours(3))
        // End notifications off, so the running session's own end cannot be the next user event and
        // the reminder is unambiguously what the user slot has to hold.
        val prefs = preferences.copy(sessionEndEnabled = false)
        val plan = SessionNotificationPlanner.plan(listOf(running, later), prefs, now)

        val userEvent = SessionNotificationPlanner.nextUserEventAfter(plan, now)
        val tick = SessionNotificationPlanner.nextTickAfter(plan, now)

        // The tick is sooner, so it would have won a shared slot.
        assertEquals(now.plusMinutes(1), tick!!.at)
        assertTrue(tick.at.isBefore(userEvent!!.at))
        // The reminder is scheduled in its own right regardless.
        assertTrue(userEvent is SessionNotificationEvent.Reminder)
        assertEquals(later.start.minusMinutes(prefs.reminderLeadMinutes.toLong()), userEvent.at)
    }

    @Test
    fun `a session's own start is a user event, not a tick`() {
        val soon = session(start = now.plusMinutes(30), end = now.plusMinutes(90))
        val plan = SessionNotificationPlanner.plan(listOf(soon), preferences, now)
        val live = plan.filterIsInstance<SessionNotificationEvent.Live>().single()

        assertFalse(SessionNotificationPlanner.isMidSessionTick(live))
    }

    @Test
    fun `there is no next alarm when nothing remains`() {
        val over = session(id = "over", start = now.minusHours(3), end = now.minusHours(2))
        val plan = SessionNotificationPlanner.plan(listOf(over), preferences, now)

        assertEquals(null, SessionNotificationPlanner.nextUserEventAfter(plan, now))
        assertEquals(null, SessionNotificationPlanner.nextTickAfter(plan, now))
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
