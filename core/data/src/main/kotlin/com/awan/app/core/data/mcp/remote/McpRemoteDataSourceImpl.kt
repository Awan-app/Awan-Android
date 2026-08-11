package com.awan.app.core.data.mcp.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.McpApiService
import com.awan.app.core.network.dto.mcp.ApiKeyResponseDto
import com.awan.app.core.network.dto.mcp.ApiKeySummaryDto
import com.awan.app.core.network.dto.mcp.CreateApiKeyRequestDto
import com.awan.app.core.network.error.safeApiCall
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

internal class McpRemoteDataSourceImpl @Inject constructor(
    private val apiService: McpApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : McpRemoteDataSource {

    override suspend fun getApiKeys(): Result<List<ApiKeySummaryDto>> =
        safeApiCall(ioDispatcher, json) { apiService.getApiKeys() }

    override suspend fun createApiKey(name: String): Result<ApiKeyResponseDto> =
        safeApiCall(ioDispatcher, json) { apiService.createApiKey(CreateApiKeyRequestDto(name)) }

    override suspend fun revokeApiKey(id: String): Result<Unit> =
        safeApiCall(ioDispatcher, json) { apiService.revokeApiKey(id) }
}
