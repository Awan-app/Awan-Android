package com.awan.app.core.network.dto.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class McpConnectionDetailsDto(
    @SerialName("mcpUrl")
    val mcpUrl: String,
    @SerialName("clientId")
    val clientId: String,
)
