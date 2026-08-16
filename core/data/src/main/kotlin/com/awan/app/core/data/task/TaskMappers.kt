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
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ProposedTaskDto
import com.awan.app.core.network.dto.task.ScheduledSessionResponse
import com.awan.app.core.network.dto.task.SessionDraftDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskProposalResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.task.TasksWithSessionsResponse
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

internal fun TaskInfoResponse.toTaskModel(): Task = Task(
    id = id,
    title = title,
    description = description,
    estimatedDurationMinutes = estimatedDuration ?: 0,
    status = status.toTaskStatus(),
    mandatory = mandatory ?: true,
    estimatedPoints = estimatedPoints ?: 0,
    allowTaskSplitting = allowTaskSplitting ?: false,
    goalId = goalId,
    dependsOnTaskIds = dependsOnTaskIds.orEmpty(),
    category = category?.toModel(),
    completedAt = completedAt,
)

internal fun com.awan.app.core.database.model.TaskEntity.toTaskModel(
    dependsOnTaskIds: List<String> = emptyList(),
    category: com.awan.app.core.model.Category? = null,
): Task = Task(
    id = id,
    title = title,
    description = description,
    estimatedDurationMinutes = estimatedDuration,
    status = status.toTaskStatus(),
    mandatory = mandatory,
    estimatedPoints = estimatedPoints,
    allowTaskSplitting = allowTaskSplitting,
    goalId = goalId,
    dependsOnTaskIds = dependsOnTaskIds,
    category = category,
    completedAt = completedAt,
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

internal fun TasksWithSessionsResponse.toModel(): List<Task> = tasks.map { it.task.toTaskModel() }

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

internal fun TaskInfoResponse.toEntity(
    goalId: String? = this.goalId,
    categoryId: String? = this.category?.id,
    expiryTime: Long = 0L
): com.awan.app.core.database.model.TaskEntity = com.awan.app.core.database.model.TaskEntity(
    id = id,
    title = title,
    description = description,
    estimatedDuration = estimatedDuration ?: 0,
    status = status ?: "SCHEDULED",
    mandatory = mandatory ?: false,
    estimatedPoints = estimatedPoints ?: 0,
    allowTaskSplitting = allowTaskSplitting ?: false,
    goalId = goalId,
    categoryId = categoryId,
    completedAt = completedAt,
    expiryTime = expiryTime,
)

internal fun TaskInfoResponse.toDependencyEntities(): List<com.awan.app.core.database.model.TaskDependencyEntity> {
    return dependsOnTaskIds?.map { prerequisiteId ->
        com.awan.app.core.database.model.TaskDependencyEntity(
            taskId = id,
            dependsOnTaskId = prerequisiteId
        )
    } ?: emptyList()
}


internal fun com.awan.app.core.network.dto.session.SessionDto.toEntity(
    taskId: String,
    date: String,
    expiryTime: Long = 0L
): com.awan.app.core.database.model.SessionEntity {
    val sessionDate = if (start.length >= 10) start.substring(0, 10) else date
    val startTime = if (start.length >= 19) start.substring(11, 19) else "00:00:00"
    val endTime = if (end.length >= 19) end.substring(11, 19) else "00:00:00"
    return com.awan.app.core.database.model.SessionEntity(
        id = id,
        taskId = this.taskId ?: taskId,
        zoneId = zoneId,
        date = sessionDate,
        startTime = startTime,
        endTime = endTime,
        status = status ?: "SCHEDULED",
        locked = locked,
        expiryTime = expiryTime,
    )
}
