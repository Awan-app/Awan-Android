package com.awan.app.core.data.mcp.mapper

import com.awan.app.core.database.model.McpTokenEntity
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.app.core.network.dto.mcp.ApiKeyResponseDto
import com.awan.app.core.network.dto.mcp.ApiKeySummaryDto

fun ApiKeySummaryDto.toEntity(): McpTokenEntity = McpTokenEntity(
    id = id,
    name = name,
    maskedToken = keyPrefix,
    createdAt = createdAt,
    lastUsedAt = null,
)

fun ApiKeyResponseDto.toDomain(): CreatedMcpToken = CreatedMcpToken(
    id = id,
    name = name,
    rawToken = keyValue,
    maskedToken = if (keyValue.length >= 12) keyValue.take(12) + "..." else keyValue,
    createdAt = createdAt,
)

fun ApiKeyResponseDto.toEntity(): McpTokenEntity = McpTokenEntity(
    id = id,
    name = name,
    maskedToken = if (keyValue.length >= 12) keyValue.take(12) + "..." else keyValue,
    createdAt = createdAt,
    lastUsedAt = null,
)

fun McpTokenEntity.toDomain(): McpToken = McpToken(
    id = id,
    name = name,
    maskedToken = maskedToken,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)

