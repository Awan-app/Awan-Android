package com.awan.app.core.network.dto.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplateOverrideDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("dateOfDay") val dateOfDay: String,
    @SerialName("zones") val zones: List<ZoneDto> = emptyList(),
)
