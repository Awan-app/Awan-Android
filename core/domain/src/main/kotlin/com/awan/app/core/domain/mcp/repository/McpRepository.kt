package com.awan.app.core.domain.mcp.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.model.McpToken
import kotlinx.coroutines.flow.Flow

interface McpRepository {
    fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>>
    fun getMcpTokens(): Flow<Result<List<McpToken>>>
    suspend fun createMcpToken(name: String): Result<CreatedMcpToken>
    suspend fun deleteMcpToken(id: String): Result<Unit>
    suspend fun regenerateMcpToken(id: String): Result<CreatedMcpToken>
}
