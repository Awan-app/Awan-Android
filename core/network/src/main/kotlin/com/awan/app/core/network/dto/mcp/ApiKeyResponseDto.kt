package com.awan.app.core.network.dto.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiKeyResponseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("keyValue") val keyValue: String,
    @SerialName("createdAt") val createdAt: String,
)
