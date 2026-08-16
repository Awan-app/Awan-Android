package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class UpdateZoneRequest(
    val name: String,
    val startTime: String,
    val endTime: String,
    val color: String? = null,
    val categoryId: String? = null,
)
