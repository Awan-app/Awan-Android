package com.awan.app.core.scheduling.valueobjects

import com.awan.app.core.scheduling.conflicts.ScheduleNudge
import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.entities.Goal
import com.awan.app.core.scheduling.entities.Session
import com.awan.app.core.scheduling.entities.Zone
import com.awan.app.core.scheduling.errors.SchedulingException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class LocalTime(val hour: Int, val minute: Int) : Comparable<LocalTime> {
    init {
        if (hour !in 0..23 || minute !in 0..59) {
            throw SchedulingException.InvalidLocalTime(hour, minute)
        }
    }

    val minutesSinceMidnight: Int
        get() = (hour * 60) + minute

    override fun compareTo(other: LocalTime): Int {
        return this.minutesSinceMidnight.compareTo(other.minutesSinceMidnight)
    }
}

data class ScheduleWorkspace(
    val zones: List<Zone>,
    val goals: List<Goal>,
    val tasks: List<AwanTask>,
    val sessions: List<Session>
)

data class ScheduleOperationResult(
    val workspace: ScheduleWorkspace,
    val nudge: ScheduleNudge?
)

data class SchedulingConfiguration(
    val minimumSessionMinutes: Int,
    val futureSearchDayLimit: Int
) {
    init {
        if (minimumSessionMinutes <= 0 || futureSearchDayLimit <= 0) {
            throw SchedulingException.InvalidConfiguration
        }
    }

    companion object {
        val standard = SchedulingConfiguration(
            minimumSessionMinutes = 15,
            futureSearchDayLimit = 14
        )
    }
}

data class SessionDraft(
    val taskID: UUID,
    val zoneID: UUID?,
    val timeRange: TimeRange
)

sealed class SchedulingIssueReason {
    object ZoneRequiredForAutomaticScheduling : SchedulingIssueReason()
    object InsufficientZoneTime : SchedulingIssueReason()
    data class DependencyUnavailable(val dependencyIDs: Set<UUID>) : SchedulingIssueReason()
}

enum class ResolutionKind {
    SPLIT_WITHIN_TODAY,
    CONTINUE_PAST_ZONE,
    SPLIT_ACROSS_DAYS,
    SCHEDULE_NEXT_AVAILABLE_DAY
}

sealed class ResolutionConsequence {
    data class SplitsTask(val sessionCount: Int) : ResolutionConsequence()
    data class ExtendsZone(val minutes: Int) : ResolutionConsequence()
    object UsesFutureDay : ResolutionConsequence()
}

data class ResolutionCandidate(
    val id: ID,
    val kind: ResolutionKind,
    val sessionDrafts: List<SessionDraft>,
    val consequences: List<ResolutionConsequence>,
    val requiresUserApproval: Boolean = true
) {
    data class ID(val taskID: UUID, val kind: ResolutionKind)

    constructor(
        taskID: UUID,
        kind: ResolutionKind,
        sessionDrafts: List<SessionDraft>,
        consequences: List<ResolutionConsequence>,
        requiresUserApproval: Boolean = true
    ) : this(
        id = ID(taskID, kind),
        kind = kind,
        sessionDrafts = sessionDrafts,
        consequences = consequences,
        requiresUserApproval = requiresUserApproval
    )
}

data class SchedulingIssue(
    val taskID: UUID,
    val reason: SchedulingIssueReason,
    val requiredMinutes: Int,
    val availableMinutes: Int,
    val resolutionCandidates: List<ResolutionCandidate>
) {
    val id: UUID get() = taskID
}

data class SchedulingResult(
    val todaySessionDrafts: List<SessionDraft>,
    val sessionUpdates: List<Session> = emptyList(),
    val issues: List<SchedulingIssue>
)

data class SchedulingSnapshot(
    val planningDay: Instant,
    val now: Instant,
    val timeZone: ZoneId,
    val zones: List<Zone>,
    val goals: List<Goal>,
    val tasks: List<AwanTask>,
    val sessions: List<Session>,
    val unavailableTime: List<TimeRange> = emptyList(),
    val configuration: SchedulingConfiguration = SchedulingConfiguration.standard
)

data class TaskDuration(val minutes: Int) {
    init {
        if (minutes <= 0) {
            throw SchedulingException.InvalidDuration(minutes)
        }
    }
}

data class TimeRange(val start: Instant, val end: Instant) {
    init {
        if (!start.isBefore(end)) {
            throw SchedulingException.InvalidTimeRange
        }
    }

    val durationMinutes: Int
        get() = Duration.between(start, end).toMinutes().toInt()

    fun overlaps(other: TimeRange): Boolean {
        return start.isBefore(other.end) && other.start.isBefore(end)
    }
}

data class ZoneColor(val hex: String) {
    val value: String

    init {
        val normalized = if (hex.startsWith("#")) hex.substring(1) else hex
        val validChars = "0123456789ABCDEFabcdef"
        if (normalized.length != 6 || normalized.any { it !in validChars }) {
            throw SchedulingException.InvalidColorHex(hex)
        }
        value = "#" + normalized.uppercase()
    }
}
