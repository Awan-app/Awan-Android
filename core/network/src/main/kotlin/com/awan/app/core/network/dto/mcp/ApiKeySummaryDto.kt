package com.awan.app.core.network.dto.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiKeySummaryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("keyPrefix") val keyPrefix: String,
    @SerialName("createdAt") val createdAt: String,
)
