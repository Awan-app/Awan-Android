package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `v1/goals/inbox` answers with the inbox **goal**, whose `tasks[]` are bare task objects — not the
 * `{ task, sessions }` pairs the scheduled-task endpoints return. Inbox tasks are unscheduled, so
 * there are no sessions to carry. The goal's own fields are ignored (`ignoreUnknownKeys`).
 */
@Serializable
data class InboxTasksResponse(
    @SerialName("tasks") val tasks: List<TaskInfoResponse> = emptyList(),
)
