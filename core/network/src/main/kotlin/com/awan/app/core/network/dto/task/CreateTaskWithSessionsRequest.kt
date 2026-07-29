package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskWithSessionsRequest(
    @SerialName("task") val task: CreateTaskRequest,
    @SerialName("sessions") val sessions: List<SessionDraftDto> = emptyList(),
)

/** `start`/`end` are `LocalDateTime` strings — `YYYY-MM-DDTHH:mm:ss`, no offset. */
@Serializable
data class SessionDraftDto(
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("status") val status: String? = null,
)
