package com.awan.app.core.data.sync

import java.time.LocalDate

/**
 * The one slice of [OfflineSyncCoordinator] a repository needs: pull a date range into Room.
 *
 * Separate from the coordinator so a repository depends on one method rather than on its fifteen
 * data sources — otherwise every repository test has to build the whole sync graph.
 */
interface ScheduleSynchronizer {

    /** Replaces the sessions cached for [startDate]..[endDate]. Returns false if nothing was written. */
    suspend fun syncScheduleRange(
        startDate: LocalDate,
        endDate: LocalDate,
        forceRefresh: Boolean = false,
    ): Boolean
}
