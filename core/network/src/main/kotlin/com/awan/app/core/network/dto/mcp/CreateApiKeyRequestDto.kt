package com.awan.app.core.network.dto.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateApiKeyRequestDto(
    @SerialName("name") val name: String,
)
