package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for `POST v1/tasks/with-sessions/bulk`. Capped at 50 entries server-side, atomic. */
@Serializable
data class BulkCreateTasksWithSessionsRequest(
    @SerialName("tasks") val tasks: List<CreateTaskWithSessionsRequest>,
)

@Serializable
data class TasksWithSessionsResponse(
    @SerialName("tasks") val tasks: List<TaskWithSessionsDto> = emptyList(),
)
