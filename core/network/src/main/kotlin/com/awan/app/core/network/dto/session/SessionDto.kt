package com.awan.app.core.network.dto.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("status") val status: String = "SCHEDULED",
    @SerialName("locked") val locked: Boolean = false,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("taskId") val taskId: String? = null
)
