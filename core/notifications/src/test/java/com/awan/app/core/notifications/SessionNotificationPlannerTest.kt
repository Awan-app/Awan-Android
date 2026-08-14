package com.awan.app.core.notifications

import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.notifications.model.DayNotificationEvent
import com.awan.app.core.notifications.model.NotificationDayContext
import com.awan.app.core.notifications.model.SessionNotificationEvent
import com.awan.app.core.notifications.model.SessionWindow
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionNotificationPlannerTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 13, 9, 0)
    private val preferences = NotificationPreferences()

    /**
     * The default for the session-only tests: with the schedule unknown, no day-scoped event is
     * planned, so each test sees exactly the events belonging to the sessions it passed in.
     */
    private val dayUnknown = NotificationDayContext(
        today = now.toLocalDate(),
        wake = LocalTime.of(7, 0),
        dayEnd = LocalTime.of(23, 0),
        scheduleKnown = false,
    )

    private val dayKnown = dayUnknown.copy(scheduleKnown = true)

    private fun plan(
        sessions: List<UpcomingSession>,
        prefs: NotificationPreferences = preferences,
        at: LocalDateTime = now,
        day: NotificationDayContext = dayUnknown,
        dismissedLive: Map<String, SessionWindow> = emptyMap(),
    ) = SessionNotificationPlanner.plan(sessions, prefs, at, day, dismissedLive)

    private fun session(
        id: String = "s1",
        start: LocalDateTime = now.plusMinutes(30),
        end: LocalDateTime = now.plusMinutes(90),
        status: SessionStatus = SessionStatus.SCHEDULED,
        title: String = "Deep Work",
    ) = UpcomingSession(
        id = id,
        taskId = "t1",
        title = title,
        start = start,
        end = end,
        status = status,
        zoneId = "z1",
    )

    @Test
    fun `reminder is planned at the configured lead time before the start`() {
        val session = session(start = now.plusMinutes(30))

        val reminder = plan(listOf(session), preferences.copy(reminderLeadMinutes = 15))
            .filterIsInstance<SessionNotificationEvent.Reminder>()
            .single()

        assertEquals(session.start.minusMinutes(15), reminder.at)
    }

    @Test
    fun `a completed session plans nothing`() {
        assertTrue(plan(listOf(session(status = SessionStatus.COMPLETED))).isEmpty())
    }

    @Test
    fun `a cancelled session plans nothing`() {
        assertTrue(plan(listOf(session(status = SessionStatus.CANCELLED))).isEmpty())
    }

    @Test
    fun `each preference removes only its own events`() {
        val only = { prefs: NotificationPreferences -> plan(listOf(session()), prefs) }

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
            only(preferences.copy(sessionFollowUpEnabled = false))
                .none { it is SessionNotificationEvent.FollowUp }
        )
        assertTrue(
            only(
                preferences.copy(
                    sessionRemindersEnabled = false,
                    sessionLiveActivityEnabled = false,
                    sessionEndEnabled = false,
                    sessionFollowUpEnabled = false,
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

        val live = plan(listOf(session)).filterIsInstance<SessionNotificationEvent.Live>().single()

        assertEquals(now.plusMinutes(1), live.at)
    }

    @Test
    fun `a running session's live event is due even though its next tick is in the future`() {
        // The regression that hid the live notification entirely: `at` is the next redraw, always a
        // tick ahead while the session runs, so judging it like the other events by `at <= now`
        // meant it was never due and the notification never appeared.
        val session = session(start = now.minusMinutes(10), end = now.plusMinutes(50))

        val live = plan(listOf(session)).filterIsInstance<SessionNotificationEvent.Live>().single()

        assertTrue(live.at.isAfter(now))
        assertTrue(SessionNotificationPlanner.isDue(live, now))
    }

    @Test
    fun `a live event becomes due the moment the session starts`() {
        val session = session(start = now, end = now.plusMinutes(60))

        val live = plan(listOf(session)).filterIsInstance<SessionNotificationEvent.Live>().single()

        assertTrue(SessionNotificationPlanner.isDue(live, now))
    }

    @Test
    fun `a live event is not due before the session starts`() {
        val session = session(start = now.plusMinutes(30), end = now.plusMinutes(90))

        val live = plan(listOf(session)).filterIsInstance<SessionNotificationEvent.Live>().single()

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

        assertTrue(plan(listOf(session)).none { it is SessionNotificationEvent.Live })
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
        val plan = plan(sessions, day = dayKnown)

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
        // End and follow-up notifications off, so the running session's own end cannot be the next
        // user event and the reminder is unambiguously what the user slot has to hold.
        val prefs = preferences.copy(sessionEndEnabled = false, sessionFollowUpEnabled = false)
        val plan = plan(listOf(running, later), prefs)

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
        val live = plan(listOf(soon)).filterIsInstance<SessionNotificationEvent.Live>().single()

        assertFalse(SessionNotificationPlanner.isMidSessionTick(live))
    }

    @Test
    fun `there is no next alarm when nothing remains`() {
        val over = session(id = "over", start = now.minusHours(3), end = now.minusHours(2))
        // Follow-ups off: an ended session legitimately has one in the future, which is an alarm.
        val plan = plan(listOf(over), preferences.copy(sessionFollowUpEnabled = false))

        assertEquals(null, SessionNotificationPlanner.nextUserEventAfter(plan, now))
        assertEquals(null, SessionNotificationPlanner.nextTickAfter(plan, now))
    }

    @Test
    fun `a live event before the session starts waits for the start`() {
        val session = session(start = now.plusMinutes(30))

        val live = plan(listOf(session)).filterIsInstance<SessionNotificationEvent.Live>().single()

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

        val plan = plan(listOf(late, early), day = dayKnown)

        assertEquals(plan.map { it.at }.sorted(), plan.map { it.at })
    }

    @Test
    fun `no reminder is planned when the lead time is zero minutes away from the start`() {
        val session = session(start = now.plusMinutes(30))

        val plan = plan(listOf(session), preferences.copy(reminderLeadMinutes = 0))

        assertTrue(plan.none { it is SessionNotificationEvent.Reminder })
    }

    // --- Follow-up -----------------------------------------------------------------------------

    @Test
    fun `a follow-up is planned the configured delay after the session ends`() {
        val session = session(start = now.plusMinutes(30), end = now.plusMinutes(90))

        val followUp = plan(listOf(session), preferences.copy(followUpDelayMinutes = 45))
            .filterIsInstance<SessionNotificationEvent.FollowUp>()
            .single()

        assertEquals(session.end.plusMinutes(45), followUp.at)
    }

    @Test
    fun `every timing default is one of the options offered for it`() {
        // A stored value outside the choice row's options renders as nothing selected, which reads
        // as a broken screen rather than as a default.
        assertTrue(
            NotificationPreferences.DEFAULT_REMINDER_LEAD_MINUTES in
                NotificationPreferences.REMINDER_LEAD_CHOICES
        )
        assertTrue(
            NotificationPreferences.DEFAULT_SNOOZE_MINUTES in NotificationPreferences.SNOOZE_CHOICES
        )
        assertTrue(
            NotificationPreferences.DEFAULT_FOLLOW_UP_MINUTES in
                NotificationPreferences.FOLLOW_UP_CHOICES
        )
    }

    @Test
    fun `the snooze lengths offered on the notification are real lengths`() {
        // They become the picker's buttons and then a number of minutes to move a session by, so a
        // sentinel among them would shift a session backwards by one minute.
        assertTrue(NotificationPreferences.SNOOZE_LENGTH_CHOICES.all { it > 0 })
        assertEquals(
            NotificationPreferences.SNOOZE_CHOICES.size - 1,
            NotificationPreferences.SNOOZE_LENGTH_CHOICES.size,
        )
        assertEquals(NotificationPreferences.SNOOZE_ASK, NotificationPreferences.DEFAULT_SNOOZE_MINUTES)
    }

    @Test
    fun `minutes until a session are rounded, not truncated`() {
        // An alarm is delivered at or after its moment, never before, so the time left at post time
        // is always a shade under the lead the user chose — and flooring turned every "in 5 minutes"
        // reminder into "in 4 minutes".
        assertEquals(5, SessionNotificationPlanner.roundedMinutes(Duration.ofSeconds(299)))
        assertEquals(5, SessionNotificationPlanner.roundedMinutes(Duration.ofMinutes(5)))
        assertEquals(4, SessionNotificationPlanner.roundedMinutes(Duration.ofSeconds(269)))
        assertEquals(0, SessionNotificationPlanner.roundedMinutes(Duration.ofSeconds(-30)))
    }

    @Test
    fun `minutes remaining are rounded up`() {
        // This sits beside the system chronometer, which counts real seconds: anything that floors
        // reads 0m for the last full minute while the countdown plainly disagrees.
        assertEquals(1, SessionNotificationPlanner.ceilMinutes(Duration.ofSeconds(10)))
        assertEquals(2, SessionNotificationPlanner.ceilMinutes(Duration.ofSeconds(61)))
        assertEquals(0, SessionNotificationPlanner.ceilMinutes(Duration.ZERO))
        assertEquals(0, SessionNotificationPlanner.ceilMinutes(Duration.ofSeconds(-90)))
    }

    @Test
    fun `an event's grace is the same window it is judged due within`() {
        // The delivery record expires on this, so a grace that disagrees with isDue either re-posts
        // a notification the user dismissed or blocks one that was never delivered.
        val session = session(start = now.plusMinutes(10), end = now.plusMinutes(70))
        val events = listOf(
            SessionNotificationEvent.Reminder(now, session),
            SessionNotificationEvent.Ended(now, session),
            SessionNotificationEvent.FollowUp(now, session),
            DayNotificationEvent.StreakRisk(now, now.toLocalDate()),
        )

        events.forEach { event ->
            val grace = SessionNotificationPlanner.graceFor(event)
            assertTrue(SessionNotificationPlanner.isDue(event, now.plus(grace)))
            assertFalse(SessionNotificationPlanner.isDue(event, now.plus(grace).plusSeconds(1)))
        }
    }

    @Test
    fun `a follow-up is due only within its grace window`() {
        val session = session(start = now.minusHours(4), end = now.minusHours(3))

        assertTrue(
            SessionNotificationPlanner.isDue(
                SessionNotificationEvent.FollowUp(now.minusMinutes(30), session),
                now,
            )
        )
        assertFalse(
            SessionNotificationPlanner.isDue(
                SessionNotificationEvent.FollowUp(now.minusHours(5), session),
                now,
            )
        )
    }

    @Test
    fun `a day of missed sessions produces one follow-up, not one per session`() {
        // Three notifications saying the same thing is how an app gets muted.
        val sessions = listOf(
            session(id = "a", start = now.minusHours(6), end = now.minusHours(5)),
            session(id = "b", start = now.minusHours(5), end = now.minusHours(4)),
            session(id = "c", start = now.minusHours(4), end = now.minusHours(3)),
        )

        val followUps = plan(sessions).filterIsInstance<SessionNotificationEvent.FollowUp>()

        assertEquals(1, followUps.size)
        // The most recent one, since that is the session still worth asking about.
        assertEquals("c", followUps.single().session.id)
    }

    @Test
    fun `future follow-ups are all kept so each still gets an alarm`() {
        // Collapsing these too would leave the earlier sessions with no alarm at all, so their
        // follow-ups would never be reached.
        val sessions = listOf(
            session(id = "a", start = now.plusHours(1), end = now.plusHours(2)),
            session(id = "b", start = now.plusHours(3), end = now.plusHours(4)),
        )

        val followUps = plan(sessions).filterIsInstance<SessionNotificationEvent.FollowUp>()

        assertEquals(2, followUps.size)
    }

    @Test
    fun `a completed session gets no follow-up`() {
        val session = session(start = now.minusHours(4), end = now.minusHours(3), status = SessionStatus.COMPLETED)

        assertTrue(plan(listOf(session)).none { it is SessionNotificationEvent.FollowUp })
    }

    @Test
    fun `a session the backend marked missed still gets its follow-up`() {
        val session = session(
            start = now.minusHours(4),
            end = now.minusHours(3),
            status = SessionStatus.MISSED,
        )

        assertTrue(plan(listOf(session)).any { it is SessionNotificationEvent.FollowUp })
    }

    // --- Live dismissal ------------------------------------------------------------------------

    @Test
    fun `a dismissed running session plans no live event`() {
        val session = session(start = now.minusMinutes(10), end = now.plusMinutes(50))

        val plan = plan(
            listOf(session),
            dismissedLive = mapOf(session.id to SessionWindow(session.start, session.end)),
        )

        assertTrue(plan.none { it is SessionNotificationEvent.Live })
    }

    @Test
    fun `moving a dismissed session brings its live notification back`() {
        // The dismissal is scoped to the session as it was. A rescheduled block is a new thing to be
        // told about, and silently swallowing its live update would be the worse failure.
        val session = session(start = now.minusMinutes(10), end = now.plusMinutes(50))
        val staleWindow = SessionWindow(session.start.plusHours(2), session.end.plusHours(2))

        val plan = plan(listOf(session), dismissedLive = mapOf(session.id to staleWindow))

        assertTrue(plan.any { it is SessionNotificationEvent.Live })
    }

    @Test
    fun `dismissing one session does not silence another`() {
        val dismissed = session(id = "a", start = now.minusMinutes(10), end = now.plusMinutes(20))
        val other = session(id = "b", start = now.minusMinutes(5), end = now.plusMinutes(25))

        val live = plan(
            listOf(dismissed, other),
            dismissedLive = mapOf("a" to SessionWindow(dismissed.start, dismissed.end)),
        ).filterIsInstance<SessionNotificationEvent.Live>()

        assertEquals(listOf("b"), live.map { it.session.id })
    }

    // --- Day events ----------------------------------------------------------------------------

    @Test
    fun `no day events are planned for a schedule that has never been fetched`() {
        // Otherwise a cold install is told to go plan a day the app simply has not loaded yet.
        val plan = plan(emptyList(), day = dayUnknown)

        assertTrue(plan.none { it is DayNotificationEvent })
    }

    @Test
    fun `the streak warning lands two hours before the day ends`() {
        val streak = plan(emptyList(), day = dayKnown)
            .filterIsInstance<DayNotificationEvent.StreakRisk>()
            .single()

        assertEquals(now.toLocalDate().atTime(21, 0), streak.at)
    }

    @Test
    fun `an overnight day puts the streak warning tonight, not this morning`() {
        // Asleep at 01:00 means the day ends on the following date. Anchoring it to today would put
        // the alarm hours in the past, where it fires at once, is never due, and never fires again.
        val overnight = dayKnown.copy(wake = LocalTime.of(7, 0), dayEnd = LocalTime.of(1, 0))

        val streak = plan(emptyList(), day = overnight)
            .filterIsInstance<DayNotificationEvent.StreakRisk>()
            .single()

        assertEquals(now.toLocalDate().atTime(23, 0), streak.at)
        assertTrue(streak.at.isAfter(now))
    }

    @Test
    fun `finishing anything today clears the streak warning`() {
        val done = session(
            id = "done",
            start = now.minusHours(2),
            end = now.minusHours(1),
            status = SessionStatus.COMPLETED,
        )

        val plan = plan(listOf(done), day = dayKnown)

        assertTrue(plan.none { it is DayNotificationEvent.StreakRisk })
    }

    @Test
    fun `a session merely running does not clear the streak warning`() {
        // It used to. A condition applied here decides whether the event exists at all, and an event
        // that does not exist gets no alarm — so a block running at 21:00 threw the whole day's
        // warning away with nothing left to bring it back inside its grace window. Running is not
        // finished, so the warning still holds.
        val running = session(start = now.minusMinutes(5), end = now.plusMinutes(55))

        val plan = plan(listOf(running), day = dayKnown)

        assertTrue(plan.any { it is DayNotificationEvent.StreakRisk })
    }

    @Test
    fun `the streak warning is due only within its grace window`() {
        val event = DayNotificationEvent.StreakRisk(now.minusMinutes(30), now.toLocalDate())
        val stale = DayNotificationEvent.StreakRisk(now.minusHours(3), now.toLocalDate())

        assertTrue(SessionNotificationPlanner.isDue(event, now))
        assertFalse(SessionNotificationPlanner.isDue(stale, now))
    }

    @Test
    fun `the morning brief lands an hour after waking and counts the day`() {
        val first = session(id = "a", start = now.plusHours(1), end = now.plusHours(2), title = "Design review")
        val second = session(id = "b", start = now.plusHours(3), end = now.plusHours(4))

        val brief = plan(listOf(second, first), day = dayKnown)
            .filterIsInstance<DayNotificationEvent.DailyBrief>()
            .single { it.slot == DayNotificationEvent.DailyBrief.Slot.MORNING }

        assertEquals(now.toLocalDate().atTime(8, 0), brief.at)
        assertEquals(2, brief.plannedCount)
        assertEquals("Design review", brief.firstTitle)
        assertEquals(first.start, brief.firstStart)
    }

    @Test
    fun `the brief names the next session ahead, not one that is already over`() {
        // Caught on a device: a block scheduled before the user woke up was announced as "first",
        // so the morning brief was describing something already finished.
        val overnight = session(
            id = "early",
            start = now.toLocalDate().atTime(4, 12),
            end = now.toLocalDate().atTime(5, 12),
            title = "Gym",
        )
        val ahead = session(
            id = "ahead",
            start = now.toLocalDate().atTime(10, 0),
            end = now.toLocalDate().atTime(11, 0),
            title = "Design review",
        )

        val brief = plan(listOf(overnight, ahead), day = dayKnown)
            .filterIsInstance<DayNotificationEvent.DailyBrief>()
            .single { it.slot == DayNotificationEvent.DailyBrief.Slot.MORNING }

        assertEquals("Design review", brief.firstTitle)
        // The count is still the whole day's plan, which is what "3 blocks today" means.
        assertEquals(2, brief.plannedCount)
    }

    @Test
    fun `a brief with nothing left ahead falls back to the day's first session`() {
        val done = session(
            id = "early",
            start = now.toLocalDate().atTime(4, 12),
            end = now.toLocalDate().atTime(5, 12),
            title = "Gym",
        )

        val brief = plan(listOf(done), day = dayKnown)
            .filterIsInstance<DayNotificationEvent.DailyBrief>()
            .single()

        assertEquals("Gym", brief.firstTitle)
    }

    @Test
    fun `an empty day is nudged again at midday`() {
        val briefs = plan(emptyList(), day = dayKnown)
            .filterIsInstance<DayNotificationEvent.DailyBrief>()

        assertEquals(
            listOf(
                DayNotificationEvent.DailyBrief.Slot.MORNING,
                DayNotificationEvent.DailyBrief.Slot.MIDDAY,
            ),
            briefs.map { it.slot },
        )
        assertEquals(0, briefs.first().plannedCount)
        // Halfway between waking at 07:00 and the day ending at 23:00.
        assertEquals(now.toLocalDate().atTime(15, 0), briefs.last().at)
    }

    @Test
    fun `a day with something planned is not nudged again at midday`() {
        val briefs = plan(listOf(session()), day = dayKnown)
            .filterIsInstance<DayNotificationEvent.DailyBrief>()

        assertEquals(listOf(DayNotificationEvent.DailyBrief.Slot.MORNING), briefs.map { it.slot })
    }

    @Test
    fun `the two brief slots are separate notifications`() {
        // Sharing an id would make the scheduler read the midday nudge as a re-post of the morning
        // one that is already on screen, and skip it.
        val date = now.toLocalDate()

        assertTrue(
            NotificationIds.dailyBrief(date, DayNotificationEvent.DailyBrief.Slot.MORNING) !=
                NotificationIds.dailyBrief(date, DayNotificationEvent.DailyBrief.Slot.MIDDAY)
        )
    }

    @Test
    fun `turning off a day notification removes only its own events`() {
        assertTrue(
            plan(emptyList(), preferences.copy(streakReminderEnabled = false), day = dayKnown)
                .none { it is DayNotificationEvent.StreakRisk }
        )
        assertTrue(
            plan(emptyList(), preferences.copy(streakReminderEnabled = false), day = dayKnown)
                .any { it is DayNotificationEvent.DailyBrief }
        )
        assertTrue(
            plan(emptyList(), preferences.copy(dailyBriefEnabled = false), day = dayKnown)
                .none { it is DayNotificationEvent.DailyBrief }
        )
        assertTrue(
            plan(
                emptyList(),
                preferences.copy(streakReminderEnabled = false, dailyBriefEnabled = false),
                day = dayKnown,
            ).isEmpty()
        )
    }
}
