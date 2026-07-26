package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias ZoneResponse = ZoneDto
typealias CategoryResponse = CategoryDto

@Serializable
data class TemplateResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("daysOfWeek") val daysOfWeek: List<String>? = emptyList(),
    @SerialName("zones") val zones: List<ZoneResponse>? = emptyList(),
    @SerialName("category") val category: CategoryResponse? = null,
)
