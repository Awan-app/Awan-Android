package com.awan.app.core.notifications.model

import com.awan.app.core.domain.notifications.model.UpcomingSession
import java.time.LocalDateTime

/**
 * Something the scheduler has to do at a point in time.
 *
 * [at] is local, not absolute, for the same reason session times are: the plan is recomputed against
 * the current zone every time it is built, so a timezone change re-times everything for free.
 */
sealed interface SessionNotificationEvent {

    val at: LocalDateTime
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
}
