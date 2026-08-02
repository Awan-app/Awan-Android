package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class UpdateZonesRequest(
    val zones: List<ZoneDto>
)
