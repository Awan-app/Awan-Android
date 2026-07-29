package com.awan.app.core.data.task

import com.awan.app.core.data.category.toModel
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskSession
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduledSessionResponse
import com.awan.app.core.network.dto.task.SessionDraftDto
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** The backend speaks `LocalDateTime` with no offset, so no zone conversion happens here. */
private val ApiDateTime: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

internal fun TaskDraft.toRequest(): CreateTaskRequest = CreateTaskRequest(
    title = title.trim(),
    description = description?.takeIf { it.isNotBlank() },
    estimatedDuration = durationMinutes,
    mandatory = mandatory,
    estimatedPoints = estimatedPoints,
    allowTaskSplitting = allowTaskSplitting,
    categoryId = categoryId,
    goalId = goalId,
)

internal fun TaskDraft.toRequest(sessions: List<SessionDraft>): CreateTaskWithSessionsRequest =
    CreateTaskWithSessionsRequest(
        task = toRequest(),
        sessions = sessions.map { it.toDto() },
    )

internal fun SessionDraft.toDto(): SessionDraftDto = SessionDraftDto(
    start = start.format(ApiDateTime),
    end = end.format(ApiDateTime),
    zoneId = zoneId,
)

internal fun TaskInfoResponse.toTaskModel(): Task = Task(
    id = id,
    title = title,
    description = description,
    estimatedDurationMinutes = estimatedDuration ?: 0,
    status = status.toTaskStatus(),
    mandatory = mandatory ?: false,
    estimatedPoints = estimatedPoints ?: 0,
    allowTaskSplitting = allowTaskSplitting ?: false,
    goalId = goalId,
    dependsOnTaskIds = dependsOnTaskIds.orEmpty(),
    category = category?.toModel(),
)

/**
 * An empty `scheduledSessions` with nothing in `unscheduledTasks` still means nothing was placed, so
 * it gets a reason of its own rather than passing for a successful schedule.
 */
internal fun TaskScheduleResponse.toScheduleModel(): TaskSchedule {
    val sessions = scheduledSessions.orEmpty().mapNotNull { it.toSessionModel() }
    val refusal = unscheduledTasks.orEmpty().firstOrNull()
    return TaskSchedule(
        sessions = sessions,
        unscheduledReason = when {
            refusal != null -> refusal.message ?: refusal.reason ?: UNKNOWN_REFUSAL
            sessions.isEmpty() -> UNKNOWN_REFUSAL
            else -> null
        },
    )
}

private const val UNKNOWN_REFUSAL = "UNSCHEDULED"

internal fun ScheduledSessionResponse.toSessionModel(): TaskSession? {
    val parsedStart = start?.toLocalDateTimeOrNull() ?: return null
    val parsedEnd = end?.toLocalDateTimeOrNull() ?: return null
    return TaskSession(
        id = sessionId.orEmpty(),
        start = parsedStart,
        end = parsedEnd,
        status = SessionStatus.SCHEDULED,
        locked = false,
        zoneId = zoneId,
    )
}

internal fun TaskWithSessionsDto.toWithSessionsModel(): TaskWithSessions = TaskWithSessions(
    task = task.toTaskModel(),
    sessions = sessions.mapNotNull { it.toSessionModel() },
)

/** A session whose times don't parse is dropped rather than crashing the whole create. */
internal fun SessionDto.toSessionModel(): TaskSession? {
    val parsedStart = start.toLocalDateTimeOrNull() ?: return null
    val parsedEnd = end.toLocalDateTimeOrNull() ?: return null
    return TaskSession(
        id = id,
        start = parsedStart,
        end = parsedEnd,
        status = status.toSessionStatus(),
        locked = locked,
        zoneId = zoneId,
    )
}

private fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { LocalDateTime.parse(this, ApiDateTime) }.getOrNull()

private fun String?.toTaskStatus(): TaskStatus =
    runCatching { TaskStatus.valueOf(orEmpty()) }.getOrDefault(TaskStatus.UNKNOWN)

private fun String?.toSessionStatus(): SessionStatus =
    runCatching { SessionStatus.valueOf(orEmpty()) }.getOrDefault(SessionStatus.UNKNOWN)
