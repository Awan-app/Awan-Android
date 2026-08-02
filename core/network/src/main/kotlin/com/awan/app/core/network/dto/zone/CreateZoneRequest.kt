package com.awan.app.core.network.dto.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateZoneRequest(
    @SerialName("name") val name: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    @SerialName("color") val color: String? = "#2E8BFF",
    @SerialName("categoryId") val categoryId: String? = null,
)
