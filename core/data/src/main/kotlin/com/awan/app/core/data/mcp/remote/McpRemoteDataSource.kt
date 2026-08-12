package com.awan.app.core.data.mcp.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.mcp.ApiKeyResponseDto
import com.awan.app.core.network.dto.mcp.ApiKeySummaryDto

internal interface McpRemoteDataSource {
    suspend fun getApiKeys(): Result<List<ApiKeySummaryDto>>
    suspend fun createApiKey(name: String): Result<ApiKeyResponseDto>
    suspend fun revokeApiKey(id: String): Result<Unit>
}
