package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplateDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("daysOfWeek") val daysOfWeek: List<String> = emptyList(), // e.g. ["MONDAY", "TUESDAY"]
    @SerialName("zones") val zones: List<ZoneDto> = emptyList(),
)
