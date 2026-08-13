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
}
