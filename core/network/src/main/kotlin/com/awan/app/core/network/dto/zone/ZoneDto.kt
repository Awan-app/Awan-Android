package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class ZoneDto(
    val id: String? = null,
    val name: String,
    val startTime: String,
    val endTime: String,
    val color: String,
    val templateId: String? = null,
    val templateOverrideId: String? = null
)
