package com.awan.app.core.network.dto.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatedMcpTokenResponseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("rawToken") val rawToken: String,
    @SerialName("maskedToken") val maskedToken: String,
    @SerialName("createdAt") val createdAt: String,
)
