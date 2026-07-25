package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyTemplateDto(
    val id: String,
    val name: String,
    val daysOfWeek: List<String>,
    val zones: List<ZoneDto>
)
