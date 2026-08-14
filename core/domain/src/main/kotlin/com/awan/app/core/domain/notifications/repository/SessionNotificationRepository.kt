package com.awan.app.core.domain.notifications.repository

import com.awan.app.core.domain.notifications.model.UpcomingSession
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Reads the cached schedule for the notification scheduler. Local only — the scheduler must work
 * with the radio off, which is the whole reason this feature exists.
 */
interface SessionNotificationRepository {

    /** Re-emits on every write to the sessions table, which is what keeps alarms in step. */
    fun observeUpcoming(startDate: LocalDate, endDate: LocalDate): Flow<List<UpcomingSession>>

    /** One-shot read for the alarm receiver, which has no scope to collect a flow in. */
    suspend fun getUpcoming(startDate: LocalDate, endDate: LocalDate): List<UpcomingSession>

    /**
     * Whether the schedule for [date] has actually been fetched.
     *
     * Without this an empty day and a day that has never synced look identical, and a fresh install
     * gets told to go plan a day the app simply has not loaded yet.
     */
    suspend fun isScheduleKnown(date: LocalDate): Boolean

    /**
     * The same answer as a flow, so the scheduler re-plans the moment it changes.
     *
     * Needed on its own because a day with no sessions produces no session emission when it syncs:
     * the sessions table is written with the same nothing it already held, and the day notifications
     * gated on this flag would stay unplanned until the next app foreground.
     */
    fun observeScheduleKnown(date: LocalDate): Flow<Boolean>
}
