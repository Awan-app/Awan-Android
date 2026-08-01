package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class CreateOverrideRequest(
    val name: String? = null,
    val dateOfDay: String,
    val zones: List<ZoneDto> = emptyList()
)
