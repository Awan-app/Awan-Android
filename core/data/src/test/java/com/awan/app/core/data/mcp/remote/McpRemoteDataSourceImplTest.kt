package com.awan.app.core.data.mcp.remote

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.McpApiService
import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMcpApiService : McpApiService {
    var details = McpConnectionDetailsDto(
        mcpUrl = "https://awanproduction.up.railway.app/mcp",
        clientId = "awan-mcp",
    )
    var getConnectionDetailsError: Throwable? = null

    override suspend fun getConnectionDetails(): McpConnectionDetailsDto {
        getConnectionDetailsError?.let { throw it }
        return details
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class McpRemoteDataSourceImplTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `getConnectionDetails returns the transport payload on success`() = runTest(dispatcher) {
        val api = FakeMcpApiService()

        val result = McpRemoteDataSourceImpl(api, Json, dispatcher).getConnectionDetails()

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("https://awanproduction.up.railway.app/mcp", data.mcpUrl)
        assertEquals("awan-mcp", data.clientId)
    }

    @Test
    fun `getConnectionDetails maps transport failures to AppError`() = runTest(dispatcher) {
        val api = FakeMcpApiService().apply { getConnectionDetailsError = IOException("offline") }

        val result = McpRemoteDataSourceImpl(api, Json, dispatcher).getConnectionDetails()

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network, (result as Result.Error).error)
    }
}
