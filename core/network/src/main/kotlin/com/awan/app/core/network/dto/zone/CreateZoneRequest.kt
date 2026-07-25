package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class CreateZoneRequest(
    val name: String,
    val startTime: String,
    val endTime: String,
    val color: String? = null
)
