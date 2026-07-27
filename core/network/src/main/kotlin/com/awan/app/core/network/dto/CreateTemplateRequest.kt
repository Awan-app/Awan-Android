package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTemplateRequest(
    @SerialName("name") val name: String,
    @SerialName("daysOfWeek") val daysOfWeek: List<String>,
    @SerialName("zones") val zones: List<TemplateZoneRequest>,
)

@Serializable
data class TemplateZoneRequest(
    @SerialName("name") val name: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    @SerialName("color") val color: String? = null,
)
