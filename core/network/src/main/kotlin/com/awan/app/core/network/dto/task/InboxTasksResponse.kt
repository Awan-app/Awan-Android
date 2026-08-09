package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InboxTasksResponse(
    @SerialName("tasks") val tasks: List<TaskWithSessionsDto> = emptyList(),
)
