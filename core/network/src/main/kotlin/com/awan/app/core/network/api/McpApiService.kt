package com.awan.app.core.network.api

import com.awan.app.core.network.dto.mcp.ApiKeyResponseDto
import com.awan.app.core.network.dto.mcp.ApiKeySummaryDto
import com.awan.app.core.network.dto.mcp.CreateApiKeyRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface McpApiService {
    @GET("v1/api-keys")
    suspend fun getApiKeys(): Response<List<ApiKeySummaryDto>>

    @POST("v1/api-keys")
    suspend fun createApiKey(@Body request: CreateApiKeyRequestDto): Response<ApiKeyResponseDto>

    @DELETE("v1/api-keys/{keyId}")
    suspend fun revokeApiKey(@Path("keyId") keyId: String): Response<Unit>
}

