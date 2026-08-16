package com.awan.app.core.data.mcp.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto

internal interface McpRemoteDataSource {
    suspend fun getConnectionDetails(): Result<McpConnectionDetailsDto>
}
