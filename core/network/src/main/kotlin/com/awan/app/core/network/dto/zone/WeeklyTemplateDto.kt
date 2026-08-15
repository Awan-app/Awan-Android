package com.awan.app.core.network.dto.zone

import com.awan.app.core.network.dto.category.CategoryDto
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyTemplateDto(
    val id: String,
    val name: String,
    val daysOfWeek: List<String>,
    val zones: List<ZoneDto>,
    val category: CategoryDto? = null
)
