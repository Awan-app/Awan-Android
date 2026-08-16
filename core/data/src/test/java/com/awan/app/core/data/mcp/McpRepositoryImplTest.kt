package com.awan.app.core.data.mcp

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.mcp.remote.McpRemoteDataSource
import com.awan.app.core.data.mcp.repository.McpRepositoryImpl
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMcpRemoteDataSource : McpRemoteDataSource {
    var details = McpConnectionDetailsDto(
        mcpUrl = "https://awanproduction.up.railway.app/mcp",
        clientId = "awan-mcp",
    )
    var getConnectionDetailsResult: Result<McpConnectionDetailsDto>? = null

    override suspend fun getConnectionDetails(): Result<McpConnectionDetailsDto> =
        getConnectionDetailsResult ?: Result.Success(details)
}

@OptIn(ExperimentalCoroutinesApi::class)
class McpRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val onlineMonitor = object : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(true)
        override fun isCurrentlyOnline(): Boolean = true
    }

    private val offlineMonitor = object : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(false)
        override fun isCurrentlyOnline(): Boolean = false
    }

    private fun buildRepository(
        remoteDataSource: McpRemoteDataSource = FakeMcpRemoteDataSource(),
        monitor: NetworkConnectivityMonitor = onlineMonitor,
    ) = McpRepositoryImpl(
        remoteDataSource = remoteDataSource,
        connectivityMonitor = monitor,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `getMcpConnectionDetails returns success with mapped domain model when online`() = runTest(testDispatcher) {
        val remoteDataSource = FakeMcpRemoteDataSource()
        val repository = buildRepository(remoteDataSource = remoteDataSource, monitor = onlineMonitor)

        val emissions = repository.getMcpConnectionDetails().toList()

        assertTrue(emissions.first() is Result.Loading)
        val finalResult = emissions.last()
        assertTrue(finalResult is Result.Success)
        val data = (finalResult as Result.Success).data
        assertEquals("https://awanproduction.up.railway.app/mcp", data.mcpUrl)
        assertEquals("awan-mcp", data.clientId)
    }

    @Test
    fun `getMcpConnectionDetails returns network error when offline`() = runTest(testDispatcher) {
        val repository = buildRepository(monitor = offlineMonitor)

        val result = repository.getMcpConnectionDetails().first()

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network, (result as Result.Error).error)
    }

    @Test
    fun `getMcpConnectionDetails propagates remote data source errors`() = runTest(testDispatcher) {
        val remoteDataSource = FakeMcpRemoteDataSource().apply {
            getConnectionDetailsResult = Result.Error(AppError.Server(500))
        }
        val repository = buildRepository(remoteDataSource = remoteDataSource, monitor = onlineMonitor)

        val emissions = repository.getMcpConnectionDetails().toList()

        val finalResult = emissions.last()
        assertTrue(finalResult is Result.Error)
        assertEquals(AppError.Server(500), (finalResult as Result.Error).error)
    }
}
