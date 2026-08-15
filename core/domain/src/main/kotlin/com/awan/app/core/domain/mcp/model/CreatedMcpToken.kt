package com.awan.app.core.domain.mcp.model

data class CreatedMcpToken(
    val id: String,
    val name: String,
    val rawToken: String,
    val maskedToken: String,
    val createdAt: String,
)
