package com.awan.app.core.network.dto.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTemplateOverrideRequest(
    @SerialName("name") val name: String,
    @SerialName("dateOfDay") val dateOfDay: String,
    @SerialName("zones") val zones: List<CreateZoneRequest> = emptyList(),
)
