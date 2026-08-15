package com.awan.app.core.domain.mcp.model

data class McpToken(
    val id: String,
    val name: String,
    val maskedToken: String,
    val createdAt: String,
    val lastUsedAt: String? = null,
)
