package com.awan.app.core.data.mcp.mapper

import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto

fun McpConnectionDetailsDto.toDomain(): McpConnectionDetails = McpConnectionDetails(
    mcpUrl = mcpUrl,
    clientId = clientId,
)

