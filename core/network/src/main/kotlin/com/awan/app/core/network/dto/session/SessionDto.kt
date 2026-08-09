package com.awan.app.core.network.dto.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("status") val status: String? = null,
    @SerialName("locked") val locked: Boolean = false,
    /** Set the first time the session was completed — the payout only ever happens then. */
    @SerialName("firstCompletedAt") val firstCompletedAt: String? = null,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("taskId") val taskId: String? = null
)
