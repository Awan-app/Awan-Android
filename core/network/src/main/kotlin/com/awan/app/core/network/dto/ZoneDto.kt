package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `startTime`/`endTime` are `LocalTime` strings — `HH:mm:ss`. */
@Serializable
data class ZoneDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    @SerialName("color") val color: String? = null,
    @SerialName("templateId") val templateId: String? = null,
    @SerialName("templateOverrideId") val templateOverrideId: String? = null,
    @SerialName("category") val category: CategoryDto? = null,
)
