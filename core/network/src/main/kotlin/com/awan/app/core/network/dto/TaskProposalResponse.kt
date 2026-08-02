package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared response shape for `POST v1/ai/task-create` and `POST v1/ai/image-to-tasks`. Nothing here
 * is persisted — every [ProposedTaskDto.draft] is exactly the body to POST to
 * `v1/tasks/with-sessions` (or the bulk equivalent) once the user confirms.
 */
@Serializable
data class TaskProposalResponse(
    /** The vision model's raw read of an uploaded image. Always null for a typed note. */
    @SerialName("sourceSummary") val sourceSummary: String? = null,
    @SerialName("tasks") val tasks: List<ProposedTaskDto> = emptyList(),
)

@Serializable
data class ProposedTaskDto(
    @SerialName("draft") val draft: CreateTaskWithSessionsRequest,
    /** Awan's own scheduling suggestion, grounded in real calendar availability. */
    @SerialName("aiProposedSessions") val aiProposedSessions: List<SessionDraftDto> = emptyList(),
    @SerialName("reason") val reason: String? = null,
)
