package com.awan.app.core.data.task

import com.awan.app.core.data.category.toModel
import com.awan.app.core.model.ProposedSession
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposal
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskSession
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.ProposedTaskDto
import com.awan.app.core.network.dto.ScheduledSessionResponse
import com.awan.app.core.network.dto.SessionDraftDto
import com.awan.app.core.network.dto.SessionDto
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskProposalResponse
import com.awan.app.core.network.dto.TaskScheduleResponse
import com.awan.app.core.network.dto.TaskWithSessionsDto
import com.awan.app.core.network.dto.TasksWithSessionsResponse
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

internal fun TaskWithSessionsDraft.toRequest(): CreateTaskWithSessionsRequest = task.toRequest(sessions)

internal fun SessionDraft.toDto(): SessionDraftDto = SessionDraftDto(
    start = start.format(ApiDateTime),
    end = end.format(ApiDateTime),
    zoneId = zoneId,
)

internal fun TaskInfoResponse.toModel(): Task = Task(
    id = id,
    title = title,
    description = description,
    estimatedDurationMinutes = estimatedDuration,
    status = status.toTaskStatus(),
    mandatory = mandatory ?: true,
    estimatedPoints = estimatedPoints ?: 0,
    allowTaskSplitting = allowTaskSplitting ?: false,
    goalId = goalId,
    dependsOnTaskIds = dependsOnTaskIds.orEmpty(),
    category = category?.toModel(),
)

internal fun TaskProposalResponse.toModel(): TaskProposals = TaskProposals(
    sourceSummary = sourceSummary,
    tasks = tasks.map { it.toModel() },
)

/**
 * Merges the two backend session channels into one list: [ProposedTaskDto.draft]'s own `sessions`
 * are timing the source explicitly stated, [ProposedTaskDto.aiProposedSessions] is Awan's own
 * availability-grounded suggestion. Both are user-editable once merged; [ProposedSession.isAiSuggested]
 * is the only thing that tells them apart downstream.
 *
 * The two channels can name the same slot — the backend echoes a stated time back as its suggestion.
 * One task cannot occupy one start twice, so the stated one wins and the echo is dropped.
 */
internal fun ProposedTaskDto.toModel(): TaskProposal {
    val task = draft.task
    val stated = draft.sessions.mapNotNull { it.toProposedSession(isAiSuggested = false) }
    val suggested = aiProposedSessions.mapNotNull { it.toProposedSession(isAiSuggested = true) }
    return TaskProposal(
        draft = TaskDraft(
            title = task.title,
            description = task.description,
            mandatory = task.mandatory ?: true,
            durationMinutes = task.estimatedDuration,
            categoryId = task.categoryId,
            estimatedPoints = task.estimatedPoints ?: 0,
            allowTaskSplitting = task.allowTaskSplitting ?: false,
            goalId = task.goalId,
        ),
        sessions = (stated + suggested).distinctBy { it.start },
        reason = reason,
    )
}

/** A session whose times don't parse is dropped rather than crashing the whole proposal. */
internal fun SessionDraftDto.toProposedSession(isAiSuggested: Boolean): ProposedSession? {
    val parsedStart = start.toLocalDateTimeOrNull() ?: return null
    val parsedEnd = end.toLocalDateTimeOrNull() ?: return null
    return ProposedSession(
        start = parsedStart,
        end = parsedEnd,
        zoneId = zoneId,
        isAiSuggested = isAiSuggested,
    )
}

internal fun TasksWithSessionsResponse.toModel(): List<Task> = tasks.map { it.task.toModel() }

/**
 * An empty `scheduledSessions` with nothing in `unscheduledTasks` still means nothing was placed, so
 * it gets a reason of its own rather than passing for a successful schedule.
 */
internal fun TaskScheduleResponse.toModel(): TaskSchedule {
    val sessions = scheduledSessions.orEmpty().mapNotNull { it.toModel() }
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

internal fun ScheduledSessionResponse.toModel(): TaskSession? {
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

internal fun TaskWithSessionsDto.toModel(): TaskWithSessions = TaskWithSessions(
    task = task.toModel(),
    sessions = sessions.mapNotNull { it.toModel() },
)

/** A session whose times don't parse is dropped rather than crashing the whole create. */
internal fun SessionDto.toModel(): TaskSession? {
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
