package com.awan.app.core.network.dto

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

@Serializable
data class CreateTemplateOverrideRequest(
    @SerialName("name") val name: String,
    @SerialName("dateOfDay") val dateOfDay: String,
    @SerialName("zones") val zones: List<CreateZoneRequest> = emptyList(),
)

@Serializable
data class TemplateOverrideResponseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("dateOfDay") val dateOfDay: String,
    @SerialName("zones") val zones: List<ZoneDto> = emptyList(),
)
