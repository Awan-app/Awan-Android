package com.awan.app.core.scheduling.conflicts

import com.awan.app.core.scheduling.entities.Zone
import com.awan.app.core.scheduling.valueobjects.SchedulingIssue
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class TaskZoneChange(
    val previousZoneID: UUID?
)

sealed class ScheduleNudge {
    data class Overlap(
        val firstSessionID: UUID,
        val secondSessionID: UUID
    ) : ScheduleNudge()

    data class SchedulingIssueNudge(
        val issue: SchedulingIssue
    ) : ScheduleNudge()

    data class MissedDependencyChain(
        val goalID: UUID,
        val missedTaskID: UUID,
        val successorTaskID: UUID
    ) : ScheduleNudge()

    data class ZoneReconfigured(
        val zoneID: UUID,
        val previousZone: Zone,
        val affectedSessionIDs: List<UUID>
    ) : ScheduleNudge()

    data class FixedSessionOverAllocation(
        val taskID: UUID,
        val pendingZoneChange: TaskZoneChange?,
        val sessionIDs: List<UUID>,
        val scheduledMinutes: Int,
        val taskMinutes: Int,
        val canTrim: Boolean,
        val selectedDay: Instant,
        val timeZone: ZoneId
    ) : ScheduleNudge()

    data class FixedSessionsOutsideTaskZone(
        val taskID: UUID,
        val previousZoneID: UUID?,
        val zoneID: UUID,
        val sessionIDs: List<UUID>,
        val selectedDay: Instant,
        val timeZone: ZoneId
    ) : ScheduleNudge()
}
