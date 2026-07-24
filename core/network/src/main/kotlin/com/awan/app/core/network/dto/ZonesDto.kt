package com.awan.app.core.network.dto

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

@Serializable
data class WeeklyTemplateDto(
    val id: String,
    val name: String,
    val daysOfWeek: List<String>,
    val zones: List<ZoneDto>
)

@Serializable
data class TemplateOverrideDto(
    val id: String,
    val dateOfDay: String,
    val zones: List<ZoneDto>
)

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val daysOfWeek: List<String>,
    val zones: List<ZoneDto>
)

@Serializable
data class UpdateZonesRequest(
    val zones: List<ZoneDto>
)

@Serializable
data class CreateOverrideRequest(
    val dateOfDay: String,
    val zones: List<ZoneDto>
)
