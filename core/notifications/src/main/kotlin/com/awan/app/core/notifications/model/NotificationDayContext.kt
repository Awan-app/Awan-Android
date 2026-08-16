package com.awan.app.core.notifications.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * What the planner needs to know about the day itself, as opposed to the sessions in it.
 *
 * All of it is read from Room — the engine has to work with the radio off, so nothing here may come
 * from the network.
 */
data class NotificationDayContext(
    val today: LocalDate,
    /** From the synced profile, or [DEFAULT_WAKE] when no profile has reached this device yet. */
    val wake: LocalTime,
    val dayEnd: LocalTime,
    /**
     * False when the schedule for [today] has never been fetched. An empty day and an unfetched day
     * are indistinguishable from the sessions table, and only one of them is worth a notification.
     */
    val scheduleKnown: Boolean,
) {
    companion object {
        /** Matches `DayBounds.Default`: onboarding never writes the profile row locally. */
        val DEFAULT_WAKE: LocalTime = LocalTime.of(7, 0)
        val DEFAULT_DAY_END: LocalTime = LocalTime.of(23, 0)
    }
}
