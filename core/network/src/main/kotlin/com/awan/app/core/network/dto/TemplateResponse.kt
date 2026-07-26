package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplateResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("daysOfWeek") val daysOfWeek: List<String>? = emptyList(),
    @SerialName("zones") val zones: List<ZoneResponse>? = emptyList(),
    @SerialName("category") val category: CategoryResponse? = null,
)

@Serializable
data class ZoneResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("templateId") val templateId: String? = null,
    @SerialName("templateOverrideId") val templateOverrideId: String? = null,
)

@Serializable
data class CategoryResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
)
