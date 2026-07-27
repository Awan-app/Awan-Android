package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionCreateDto(
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("status") val status: String = "SCHEDULED",
)

@Serializable
data class CreateTaskWithSessionsRequest(
    @SerialName("task") val task: CreateTaskRequest,
    @SerialName("sessions") val sessions: List<SessionCreateDto>,
)
