package com.awan.app.core.data.mcp.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.McpApiService
import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto
import com.awan.app.core.network.error.safeApiCall
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

internal class McpRemoteDataSourceImpl @Inject constructor(
    private val apiService: McpApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : McpRemoteDataSource {

    override suspend fun getConnectionDetails(): Result<McpConnectionDetailsDto> =
        safeApiCall(ioDispatcher, json) { apiService.getConnectionDetails() }
}
