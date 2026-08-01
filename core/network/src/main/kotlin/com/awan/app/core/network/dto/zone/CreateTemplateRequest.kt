package com.awan.app.core.network.dto.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTemplateRequest(
    @SerialName("name") val name: String,
    @SerialName("daysOfWeek") val daysOfWeek: List<String>,
    @SerialName("zones") val zones: List<ZoneDto> = emptyList()
)
