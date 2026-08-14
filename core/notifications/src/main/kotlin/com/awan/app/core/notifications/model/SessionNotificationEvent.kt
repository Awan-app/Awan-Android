package com.awan.app.core.notifications.model

import com.awan.app.core.domain.notifications.model.UpcomingSession
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Something the scheduler has to do at a point in time.
 *
 * [at] is local, not absolute, for the same reason session times are: the plan is recomputed against
 * the current zone every time it is built, so a timezone change re-times everything for free.
 */
sealed interface AwanNotificationEvent {
    val at: LocalDateTime
}

/** An event about one session. */
sealed interface SessionNotificationEvent : AwanNotificationEvent {

    val session: UpcomingSession

    /** Fires [NotificationPreferences.reminderLeadMinutes] before the session starts. */
    data class Reminder(
        override val at: LocalDateTime,
        override val session: UpcomingSession,
    ) : SessionNotificationEvent

    /**
     * Posts or refreshes the ongoing live notification. Emitted repeatedly while a session is
     * running — the scheduler folds the next tick into the same single alarm rather than keeping a
     * second one alive.
     */
    data class Live(
        override val at: LocalDateTime,
        override val session: UpcomingSession,
    ) : SessionNotificationEvent

    /** Fires when the session's end time passes and it is still not completed. */
    data class Ended(
        override val at: LocalDateTime,
        override val session: UpcomingSession,
    ) : SessionNotificationEvent

    /**
     * The gentler second ask, an hour or so after a session ended and nobody said how it went.
     *
     * Distinct from [Ended] on purpose: that one lands the moment the time is up, when the user may
     * still be finishing. This one arrives once they have moved on, and is the last thing the app
     * says about that session.
     */
    data class FollowUp(
        override val at: LocalDateTime,
        override val session: UpcomingSession,
    ) : SessionNotificationEvent
}

/** An event about the day as a whole rather than about any one session. */
sealed interface DayNotificationEvent : AwanNotificationEvent {

    val date: LocalDate

    /**
     * The day is nearly over and nothing has been completed.
     *
     * Deliberately phrased around finishing something today, never around a streak count: the streak
     * is server-owned and the device cannot know whether today already counts.
     */
    data class StreakRisk(
        override val at: LocalDateTime,
        override val date: LocalDate,
    ) : DayNotificationEvent

    /**
     * The morning summary — or, on an empty day, the nudge to go plan one.
     *
     * One event with two bodies, because it is one thing the app says about the day ahead. The
     * [Slot.MIDDAY] repeat is only planned while the day is still empty.
     */
    data class DailyBrief(
        override val at: LocalDateTime,
        override val date: LocalDate,
        val slot: Slot,
        val plannedCount: Int,
        val firstTitle: String?,
        val firstStart: LocalDateTime?,
    ) : DayNotificationEvent {

        /** Separate ids, so the midday nudge is not swallowed as a re-post of the morning one. */
        enum class Slot { MORNING, MIDDAY }
    }
}
