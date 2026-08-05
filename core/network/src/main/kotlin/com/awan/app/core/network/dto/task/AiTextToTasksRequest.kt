package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Free-form note handed to `POST v1/ai/task-create`, max 4000 chars server-side. */
@Serializable
data class AiTextToTasksRequest(
    @SerialName("text") val text: String,
)
