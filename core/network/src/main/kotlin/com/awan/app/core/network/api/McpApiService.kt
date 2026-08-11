package com.awan.app.core.network.api

import com.awan.app.core.network.dto.mcp.CreateMcpTokenRequestDto
import com.awan.app.core.network.dto.mcp.CreatedMcpTokenResponseDto
import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto
import com.awan.app.core.network.dto.mcp.McpTokenResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface McpApiService {
    @GET("v1/mcp/settings/connection-details")
    suspend fun getConnectionDetails(): McpConnectionDetailsDto

    @GET("v1/mcp/tokens")
    suspend fun getTokens(): List<McpTokenResponseDto>

    @POST("v1/mcp/tokens")
    suspend fun createToken(@Body request: CreateMcpTokenRequestDto): CreatedMcpTokenResponseDto

    @DELETE("v1/mcp/tokens/{id}")
    suspend fun deleteToken(@Path("id") id: String)

    @POST("v1/mcp/tokens/{id}/regenerate")
    suspend fun regenerateToken(@Path("id") id: String): CreatedMcpTokenResponseDto
}
