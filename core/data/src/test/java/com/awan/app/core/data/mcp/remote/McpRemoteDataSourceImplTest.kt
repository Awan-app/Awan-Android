package com.awan.app.core.data.mcp.remote

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.McpApiService
import com.awan.app.core.network.dto.mcp.ApiKeyResponseDto
import com.awan.app.core.network.dto.mcp.ApiKeySummaryDto
import com.awan.app.core.network.dto.mcp.CreateApiKeyRequestDto
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMcpApiService : McpApiService {
    var apiKeys = emptyList<ApiKeySummaryDto>()
    var getApiKeysError: Throwable? = null

    override suspend fun getApiKeys(): List<ApiKeySummaryDto> {
        getApiKeysError?.let { throw it }
        return apiKeys
    }

    override suspend fun createApiKey(request: CreateApiKeyRequestDto): ApiKeyResponseDto =
        error("Not used by this test")

    override suspend fun revokeApiKey(keyId: String) = error("Not used by this test")
}

@OptIn(ExperimentalCoroutinesApi::class)
class McpRemoteDataSourceImplTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `getApiKeys returns the transport payload on success`() = runTest(dispatcher) {
        val api = FakeMcpApiService().apply {
            apiKeys = listOf(
                ApiKeySummaryDto(
                    id = "key-1",
                    name = "Claude Desktop",
                    keyPrefix = "mcp_...123",
                    createdAt = "2026-08-11T00:00:00Z",
                ),
            )
        }

        val result = McpRemoteDataSourceImpl(api, Json, dispatcher).getApiKeys()

        assertTrue(result is Result.Success)
        assertEquals("key-1", (result as Result.Success).data.single().id)
    }

    @Test
    fun `getApiKeys maps transport failures to AppError`() = runTest(dispatcher) {
        val api = FakeMcpApiService().apply { getApiKeysError = IOException("offline") }

        val result = McpRemoteDataSourceImpl(api, Json, dispatcher).getApiKeys()

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network, (result as Result.Error).error)
    }
}
