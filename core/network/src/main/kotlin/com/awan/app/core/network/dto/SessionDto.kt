package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("start") val start: String,          // "YYYY-MM-DDTHH:mm:ss"
    @SerialName("end") val end: String,              // "YYYY-MM-DDTHH:mm:ss"
    @SerialName("status") val status: String = "SCHEDULED",
    @SerialName("locked") val locked: Boolean = false,
    @SerialName("zoneId") val zoneId: String? = null,
)
