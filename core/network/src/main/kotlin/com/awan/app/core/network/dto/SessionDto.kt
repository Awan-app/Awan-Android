package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("status") val status: String? = null,
    @SerialName("locked") val locked: Boolean = false,
    @SerialName("zoneId") val zoneId: String? = null,
)

@Serializable
data class TaskWithSessionsResponse(
    @SerialName("task") val task: TaskInfoResponse,
    @SerialName("sessions") val sessions: List<SessionDto> = emptyList(),
)