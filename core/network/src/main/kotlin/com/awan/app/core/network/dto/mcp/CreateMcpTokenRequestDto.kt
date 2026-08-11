package com.awan.app.core.network.dto.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateMcpTokenRequestDto(
    @SerialName("name") val name: String,
)
