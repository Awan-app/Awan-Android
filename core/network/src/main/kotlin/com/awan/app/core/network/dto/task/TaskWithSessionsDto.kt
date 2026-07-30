package com.awan.app.core.network.dto.task


import com.awan.app.core.network.dto.session.SessionDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskWithSessionsDto(
    @SerialName("task") val task: TaskInfoResponse,
    @SerialName("sessions") val sessions: List<SessionDto> = emptyList(),
)
