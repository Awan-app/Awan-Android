package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTemplateRequest(
    val name: String,
    val daysOfWeek: List<String>
)
