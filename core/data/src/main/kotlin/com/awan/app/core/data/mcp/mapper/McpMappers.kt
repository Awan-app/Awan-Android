package com.awan.app.core.data.mcp.mapper

import com.awan.app.core.database.model.McpTokenEntity
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.app.core.network.dto.mcp.CreatedMcpTokenResponseDto
import com.awan.app.core.network.dto.mcp.McpTokenResponseDto

fun McpTokenResponseDto.toEntity(): McpTokenEntity = McpTokenEntity(
    id = id,
    name = name,
    maskedToken = maskedToken,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)

fun CreatedMcpTokenResponseDto.toEntity(): McpTokenEntity = McpTokenEntity(
    id = id,
    name = name,
    maskedToken = maskedToken,
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
