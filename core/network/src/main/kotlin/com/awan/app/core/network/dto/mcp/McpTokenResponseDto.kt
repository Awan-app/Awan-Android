package com.awan.app.core.network.dto.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class McpTokenResponseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("maskedToken") val maskedToken: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("lastUsedAt") val lastUsedAt: String? = null,
)
