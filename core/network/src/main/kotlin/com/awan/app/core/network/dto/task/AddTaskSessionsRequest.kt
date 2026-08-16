package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddTaskSessionsRequest(
    @SerialName("sessions") val sessions: List<AddSessionItemDto>,
)

@Serializable
data class AddSessionItemDto(
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("status") val status: String = "SCHEDULED",
    @SerialName("zoneId") val zoneId: String? = null,
)
