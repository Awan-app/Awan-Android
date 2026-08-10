package com.awan.app.core.network.dto.zone

import com.awan.app.core.network.dto.category.CategoryDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `startTime`/`endTime` are `LocalTime` strings — `HH:mm:ss`.
 *
 * [category] is the read side (the server nests it on every `ZoneResponse`); [categoryId] is the
 * write side and is required by the backend on every request body that contains a zone. Both live on
 * one type, and since the app's Json sets `explicitNulls = true`, a write posts `"category": null`
 * alongside the id — which the backend accepts. Splitting the type is only worth it if that changes.
 */
@Serializable
data class ZoneDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    @SerialName("color") val color: String? = null,
    @SerialName("templateId") val templateId: String? = null,
    @SerialName("templateOverrideId") val templateOverrideId: String? = null,
    @SerialName("category") val category: CategoryDto? = null,
    @SerialName("categoryId") val categoryId: String? = null,
)
