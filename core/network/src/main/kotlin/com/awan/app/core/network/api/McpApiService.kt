package com.awan.app.core.network.api

import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto
import retrofit2.http.GET

interface McpApiService {
    @GET("v1/mcp/settings/connection-details")
    suspend fun getConnectionDetails(): McpConnectionDetailsDto
}

