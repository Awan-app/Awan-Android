package com.awan.app.core.domain.mcp.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import kotlinx.coroutines.flow.Flow

interface McpRepository {
    fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>>
}
