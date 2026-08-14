package com.awan.app.core.notifications

import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.notifications.model.AwanNotificationEvent
import com.awan.app.core.notifications.model.DayNotificationEvent
import com.awan.app.core.notifications.model.NotificationDayContext
import com.awan.app.core.notifications.model.SessionNotificationEvent
import com.awan.app.core.notifications.model.SessionWindow
import java.time.Duration
import java.time.LocalDateTime

/**
 * Turns the cached schedule into the full list of notification events, past and future.
 *
 * Pure and Android-free so the timing rules — which are the part that actually goes wrong — can be
 * tested without a device, an alarm, or a clock.
 *
 * The scheduler posts the events that are already [isDue], and sets alarms for the ones that are not.
 *
 * Conditions that depend on more than one session — "nothing finished today", "the day is empty" —
 * are decided here, at plan time, not in [isDue]. The plan is rebuilt on every session write, so a
 * condition that stops holding simply stops producing its event, and the scheduler's reconciliation
 * cancels whatever was already showing.
 */
object SessionNotificationPlanner {

    /** How long after its moment an event is still worth firing. */
    val REMINDER_GRACE: Duration = Duration.ofMinutes(5)
    val ENDED_GRACE: Duration = Duration.ofMinutes(30)
    val FOLLOW_UP_GRACE: Duration = Duration.ofMinutes(60)
    val STREAK_RISK_GRACE: Duration = Duration.ofMinutes(45)
    val DAILY_BRIEF_GRACE: Duration = Duration.ofMinutes(30)

    /** How long before the end of the day the streak warning lands. */
    val STREAK_RISK_LEAD: Duration = Duration.ofHours(2)

    /** Long enough after waking that the morning brief is not the alarm clock. */
    val BRIEF_AFTER_WAKE: Duration = Duration.ofHours(1)

    /**
     * Cadence of the live notification's progress bar.
     *
     * ponytail: setExactAndAllowWhileIdle is throttled to roughly once every 9 minutes once the app
     * is idle in Doze, so with the screen off the bar advances in jumps rather than every minute.
     * The countdown itself is drawn by the system chronometer and stays exact regardless. Upgrade
     * path if the bar must be smooth in Doze: a foreground service for the session's duration.
     */
    val LIVE_TICK: Duration = Duration.ofMinutes(1)

    fun plan(
        sessions: List<UpcomingSession>,
        preferences: NotificationPreferences,
        now: LocalDateTime,
        day: NotificationDayContext,
        dismissedLive: Map<String, SessionWindow> = emptyMap(),
    ): List<AwanNotificationEvent> {
        val perSession = sessions.flatMap { eventsFor(it, preferences, now, dismissedLive) }
        return (collapseFollowUps(perSession, now) + dayEventsFor(sessions, preferences, now, day))
            .sortedBy { it.at }
    }

    /**
     * A redraw of an already-running session's progress bar, as opposed to a moment the user is
     * waiting for. Cosmetic: missing one costs a stale bar for a minute and nothing else.
     */
    fun isMidSessionTick(event: AwanNotificationEvent): Boolean =
        event is SessionNotificationEvent.Live && event.at.isAfter(event.session.start)

    /**
     * The next moment that matters to the user — a reminder, a session starting, a session ending.
     *
     * Kept apart from [nextTickAfter] because these two cannot share one alarm slot. A running
     * session produces a tick every minute, and a tick is always sooner than the next reminder, so a
     * single slot spends the whole session holding ticks and the reminder is never registered with
     * the OS at all. It then exists only as a link in a chain: drop one tick — process killed, OEM
     * defers the alarm — and the reminder is simply never scheduled.
     *
     * Strictly in the future, never merely "not due". A plan legitimately contains events whose
     * moment has passed without being due — this morning's reminders, after a reboot at noon. Taking
     * the first non-due event instead would set an alarm for a time already gone, which fires at
     * once, is still not due, and reschedules itself forever.
     */
    fun nextUserEventAfter(
        plan: List<AwanNotificationEvent>,
        now: LocalDateTime,
    ): AwanNotificationEvent? =
        plan.filter { it.at.isAfter(now) && !isMidSessionTick(it) }.minByOrNull { it.at }

    /** The next progress redraw, on its own alarm so it cannot displace a user-facing one. */
    fun nextTickAfter(
        plan: List<AwanNotificationEvent>,
        now: LocalDateTime,
    ): AwanNotificationEvent? =
        plan.filter { it.at.isAfter(now) && isMidSessionTick(it) }.minByOrNull { it.at }

    /**
     * True when [event] should be showing right now. Past events outside their grace window are
     * deliberately not replayed — after a reboot at noon, this morning's reminders are noise.
     */
    fun isDue(event: AwanNotificationEvent, now: LocalDateTime): Boolean = when (event) {
        // Judged on the session running, not on `at`. For a live event `at` is the *next redraw*,
        // which is always a tick in the future while the session runs — requiring `at <= now` like
        // the others would mean the live notification could never be shown at all.
        is SessionNotificationEvent.Live ->
            !now.isBefore(event.session.start) && now.isBefore(event.session.end)

        is SessionNotificationEvent.Reminder ->
            !event.at.isAfter(now) &&
                // Never remind about a session that has already started.
                now.isBefore(event.session.start) &&
                withinGrace(event.at, now, REMINDER_GRACE)

        is SessionNotificationEvent.Ended ->
            !event.at.isAfter(now) && withinGrace(event.at, now, ENDED_GRACE)

        is SessionNotificationEvent.FollowUp ->
            !event.at.isAfter(now) && withinGrace(event.at, now, FOLLOW_UP_GRACE)

        // The conditions these two depend on — nothing finished today, the day being empty — are
        // applied when the plan is built. Reaching here at all means they held.
        is DayNotificationEvent.StreakRisk ->
            !event.at.isAfter(now) && withinGrace(event.at, now, STREAK_RISK_GRACE)

        is DayNotificationEvent.DailyBrief ->
            !event.at.isAfter(now) && withinGrace(event.at, now, DAILY_BRIEF_GRACE)
    }

    private fun withinGrace(at: LocalDateTime, now: LocalDateTime, grace: Duration): Boolean =
        Duration.between(at, now) <= grace

    private fun eventsFor(
        session: UpcomingSession,
        preferences: NotificationPreferences,
        now: LocalDateTime,
        dismissedLive: Map<String, SessionWindow>,
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

        // Only while there is still session left to show. Without the end check a finished session
        // keeps producing a tick a minute into the future, and since that tick is never due, the
        // scheduler burns an exact alarm every minute on a session that is over.
        if (
            preferences.sessionLiveActivityEnabled &&
            now.isBefore(session.end) &&
            !isLiveDismissed(session, dismissedLive)
        ) {
            events += SessionNotificationEvent.Live(nextLiveMoment(session, now), session)
        }

        if (preferences.sessionEndEnabled) {
            events += SessionNotificationEvent.Ended(session.end, session)
        }

        if (preferences.sessionFollowUpEnabled) {
            val followUpAt = session.end.plusMinutes(preferences.followUpDelayMinutes.toLong())
            events += SessionNotificationEvent.FollowUp(followUpAt, session)
        }

        return events
    }

    /**
     * The user pressed "Stop", and the session has not moved since.
     *
     * Scoped to the window rather than the id: a session that was moved, snoozed or rescheduled is a
     * new thing to be told about, so the dismissal stops applying and the live notification returns.
     */
    private fun isLiveDismissed(
        session: UpcomingSession,
        dismissedLive: Map<String, SessionWindow>,
    ): Boolean = dismissedLive[session.id] == SessionWindow(session.start, session.end)

    /**
     * Keeps at most one already-due follow-up: the most recent one.
     *
     * A day that got away from someone produces a follow-up per unanswered session, and three
     * notifications saying the same thing is how an app gets muted. Future ones are all kept — each
     * still needs its alarm — and this runs again when they come due.
     */
    private fun collapseFollowUps(
        events: List<SessionNotificationEvent>,
        now: LocalDateTime,
    ): List<SessionNotificationEvent> {
        val (followUps, rest) = events.partition { it is SessionNotificationEvent.FollowUp }
        val (past, future) = followUps.partition { !it.at.isAfter(now) }
        return rest + future + listOfNotNull(past.maxByOrNull { it.at })
    }

    private fun dayEventsFor(
        sessions: List<UpcomingSession>,
        preferences: NotificationPreferences,
        now: LocalDateTime,
        day: NotificationDayContext,
    ): List<DayNotificationEvent> {
        // Nothing to say about a day the app has never loaded. Without this a cold install is told
        // its day is empty before the first sync has had a chance to fill it.
        if (!day.scheduleKnown) return emptyList()

        val today = sessions.filter { it.start.toLocalDate() == day.today }
        val events = mutableListOf<DayNotificationEvent>()

        if (preferences.streakReminderEnabled && isStreakAtRisk(today, now)) {
            events += DayNotificationEvent.StreakRisk(
                at = dayEndMoment(day).minus(STREAK_RISK_LEAD),
                date = day.today,
            )
        }

        if (preferences.dailyBriefEnabled) {
            val wakeMoment = day.today.atTime(day.wake)
            fun brief(at: LocalDateTime, slot: DayNotificationEvent.DailyBrief.Slot) {
                // "First" means the next one still ahead of the user, not the earliest on the
                // clock: a block scheduled before they woke up is already over by the time the
                // brief lands, and naming it reads as a brief about yesterday.
                val next = today.filter { it.end.isAfter(at) }.minByOrNull { it.start }
                    ?: today.minByOrNull { it.start }
                events += DayNotificationEvent.DailyBrief(
                    at = at,
                    date = day.today,
                    slot = slot,
                    plannedCount = today.size,
                    firstTitle = next?.title,
                    firstStart = next?.start,
                )
            }

            val morningAt = wakeMoment.plus(BRIEF_AFTER_WAKE)
            brief(morningAt, DayNotificationEvent.DailyBrief.Slot.MORNING)

            // The second ask exists only for a day that is still empty — once anything is planned,
            // the morning summary has already said everything there is to say.
            val middayAt = wakeMoment.plusMinutes(
                Duration.between(wakeMoment, dayEndMoment(day)).toMinutes() / 2
            )
            if (today.isEmpty() && middayAt.isAfter(morningAt)) {
                brief(middayAt, DayNotificationEvent.DailyBrief.Slot.MIDDAY)
            }
        }

        return events
    }

    /**
     * Nothing finished today, and nothing running right now.
     *
     * "Nothing finished" is the local stand-in for the streak, which is server-owned and not cached
     * per-day. The running check keeps the warning off the screen while the live notification is
     * already there saying the opposite.
     */
    private fun isStreakAtRisk(today: List<UpcomingSession>, now: LocalDateTime): Boolean {
        val completedAny = today.any { it.status == SessionStatus.COMPLETED }
        val running = today.any { !now.isBefore(it.start) && now.isBefore(it.end) }
        return !completedAny && !running
    }

    /**
     * The moment the user's day is over.
     *
     * An overnight day — asleep at 01:00, awake at 07:00 — ends on the *following* date. Anchoring it
     * to today would place the whole thing in this morning, hours before the day began, so the
     * warning's alarm would sit permanently in the past and never fire.
     */
    private fun dayEndMoment(day: NotificationDayContext): LocalDateTime {
        val sameDay = day.today.atTime(day.dayEnd)
        return if (day.dayEnd > day.wake) sameDay else sameDay.plusDays(1)
    }

    /**
     * The next moment the live notification needs redrawing: the session's start if it has not begun,
     * otherwise the next point on a grid laid from the start.
     *
     * On the grid rather than [now] + one tick. An alarm always fires a little late, and scheduling
     * the next one relative to when the last one *ran* folds that lateness in again every minute —
     * after twenty ticks the bar was redrawing twenty seconds off the minute it was drawing. Anchored
     * to the start, a late tick is late once and the one after it is back on time.
     */
    private fun nextLiveMoment(session: UpcomingSession, now: LocalDateTime): LocalDateTime {
        if (now.isBefore(session.start)) return session.start
        val ticksElapsed = Duration.between(session.start, now).toMillis() / LIVE_TICK.toMillis()
        return session.start.plus(LIVE_TICK.multipliedBy(ticksElapsed + 1))
    }
}
