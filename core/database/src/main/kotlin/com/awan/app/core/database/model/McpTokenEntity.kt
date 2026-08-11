package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mcp_tokens")
data class McpTokenEntity(
    @PrimaryKey val id: String,
    val name: String,
    val maskedToken: String,
    val createdAt: String,
    val lastUsedAt: String? = null,
)
