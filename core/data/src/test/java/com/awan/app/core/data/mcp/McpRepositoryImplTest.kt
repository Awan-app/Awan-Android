package com.awan.app.core.data.mcp

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.mcp.repository.McpRepositoryImpl
import com.awan.app.core.database.dao.McpTokenDao
import com.awan.app.core.database.model.McpTokenEntity
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.network.api.McpApiService
import com.awan.app.core.network.dto.mcp.CreateMcpTokenRequestDto
import com.awan.app.core.network.dto.mcp.CreatedMcpTokenResponseDto
import com.awan.app.core.network.dto.mcp.McpConnectionDetailsDto
import com.awan.app.core.network.dto.mcp.McpTokenResponseDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMcpApiService : McpApiService {
    var connectionDetailsDto = McpConnectionDetailsDto(
        mcpUrl = "https://mcp.awan.com",
        clientId = "client-123",
    )
    var tokensList = mutableListOf<McpTokenResponseDto>()
    var createdTokenResponse = CreatedMcpTokenResponseDto(
        id = "token-1",
        name = "Default Token",
        rawToken = "raw-secret-123",
        maskedToken = "mcp_...123",
        createdAt = "2026-08-11T00:00:00Z",
    )
    var shouldFailWithException: Exception? = null
    var lastCreatedName: String? = null
    var lastDeletedId: String? = null
    var lastRegeneratedId: String? = null

    override suspend fun getConnectionDetails(): McpConnectionDetailsDto {
        shouldFailWithException?.let { throw it }
        return connectionDetailsDto
    }

    override suspend fun getTokens(): List<McpTokenResponseDto> {
        shouldFailWithException?.let { throw it }
        return tokensList
    }

    override suspend fun createToken(request: CreateMcpTokenRequestDto): CreatedMcpTokenResponseDto {
        shouldFailWithException?.let { throw it }
        lastCreatedName = request.name
        return createdTokenResponse.copy(name = request.name)
    }

    override suspend fun deleteToken(id: String) {
        shouldFailWithException?.let { throw it }
        lastDeletedId = id
    }

    override suspend fun regenerateToken(id: String): CreatedMcpTokenResponseDto {
        shouldFailWithException?.let { throw it }
        lastRegeneratedId = id
        return createdTokenResponse
    }
}

private class FakeMcpTokenDao : McpTokenDao {
    private val tokensState = MutableStateFlow<List<McpTokenEntity>>(emptyList())
    val storedTokens: List<McpTokenEntity> get() = tokensState.value
    var shouldFailOnUpsert: Boolean = false
    var replaceCount: Int = 0

    override fun getMcpTokens(): Flow<List<McpTokenEntity>> = tokensState

    override suspend fun upsertMcpTokens(tokens: List<McpTokenEntity>) {
        if (shouldFailOnUpsert) {
            throw IllegalStateException("Database write error")
        }
        val current = tokensState.value.toMutableList()
        tokens.forEach { newToken ->
            current.removeAll { it.id == newToken.id }
            current.add(newToken)
        }
        tokensState.value = current
    }

    override suspend fun deleteMcpToken(id: String) {
        tokensState.value = tokensState.value.filterNot { it.id == id }
    }

    override suspend fun clearAll() {
        tokensState.value = emptyList()
    }

    override suspend fun replaceMcpTokens(tokens: List<McpTokenEntity>) {
        replaceCount++
        clearAll()
        upsertMcpTokens(tokens)
    }
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
        apiService: McpApiService = FakeMcpApiService(),
        tokenDao: McpTokenDao = FakeMcpTokenDao(),
        monitor: NetworkConnectivityMonitor = onlineMonitor,
    ) = McpRepositoryImpl(
        mcpApiService = apiService,
        mcpTokenDao = tokenDao,
        connectivityMonitor = monitor,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `getMcpConnectionDetails returns success when online`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService()
        val repository = buildRepository(apiService = apiService, monitor = onlineMonitor)

        val result = repository.getMcpConnectionDetails().first()

        assertTrue(result is Result.Success)
        val details = (result as Result.Success).data
        assertEquals("https://mcp.awan.com", details.mcpUrl)
        assertEquals("client-123", details.clientId)
    }

    @Test
    fun `getMcpConnectionDetails returns network error when offline`() = runTest(testDispatcher) {
        val repository = buildRepository(monitor = offlineMonitor)

        val result = repository.getMcpConnectionDetails().first()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Network)
    }

    @Test
    fun `getMcpTokens fetches remote and updates Room atomically when online`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService().apply {
            tokensList.add(
                McpTokenResponseDto(
                    id = "token-1",
                    name = "Claude Desktop",
                    maskedToken = "mcp_...123",
                    createdAt = "2026-08-11T00:00:00Z",
                )
            )
        }
        val tokenDao = FakeMcpTokenDao()
        val repository = buildRepository(apiService = apiService, tokenDao = tokenDao, monitor = onlineMonitor)

        val result = repository.getMcpTokens().first()

        assertTrue(result is Result.Success)
        val tokens = (result as Result.Success).data
        assertEquals(1, tokens.size)
        assertEquals("Claude Desktop", tokens.first().name)
        assertEquals(1, tokenDao.storedTokens.size)
        assertEquals("Claude Desktop", tokenDao.storedTokens.first().name)
        assertEquals(1, tokenDao.replaceCount)
    }

    @Test
    fun `getMcpTokens falls back to Room cached tokens when offline`() = runTest(testDispatcher) {
        val tokenDao = FakeMcpTokenDao().apply {
            upsertMcpTokens(
                listOf(
                    McpTokenEntity(
                        id = "token-cached",
                        name = "Cached Token",
                        maskedToken = "mcp_...cached",
                        createdAt = "2026-08-10T00:00:00Z",
                    )
                )
            )
        }
        val repository = buildRepository(tokenDao = tokenDao, monitor = offlineMonitor)

        val result = repository.getMcpTokens().first()

        assertTrue(result is Result.Success)
        val tokens = (result as Result.Success).data
        assertEquals(1, tokens.size)
        assertEquals("Cached Token", tokens.first().name)
    }

    @Test
    fun `createMcpToken succeeds when online and upserts entity into Room`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService()
        val tokenDao = FakeMcpTokenDao()
        val repository = buildRepository(apiService = apiService, tokenDao = tokenDao, monitor = onlineMonitor)

        val result = repository.createMcpToken("Claude Desktop")

        assertTrue(result is Result.Success)
        val createdToken = (result as Result.Success).data
        assertEquals("Claude Desktop", createdToken.name)
        assertEquals("raw-secret-123", createdToken.rawToken)
        assertEquals("Claude Desktop", apiService.lastCreatedName)
        assertEquals(1, tokenDao.storedTokens.size)
        assertEquals("token-1", tokenDao.storedTokens.first().id)
    }

    @Test
    fun `createMcpToken preserves raw token success even if Room write fails`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService()
        val tokenDao = FakeMcpTokenDao().apply { shouldFailOnUpsert = true }
        val repository = buildRepository(apiService = apiService, tokenDao = tokenDao, monitor = onlineMonitor)

        val result = repository.createMcpToken("Claude Desktop")

        assertTrue(result is Result.Success)
        val createdToken = (result as Result.Success).data
        assertEquals("Claude Desktop", createdToken.name)
        assertEquals("raw-secret-123", createdToken.rawToken)
    }

    @Test
    fun `createMcpToken returns network error when offline`() = runTest(testDispatcher) {
        val repository = buildRepository(monitor = offlineMonitor)

        val result = repository.createMcpToken("Claude Desktop")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Network)
    }

    @Test
    fun `deleteMcpToken deletes token from network and Room`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService()
        val tokenDao = FakeMcpTokenDao().apply {
            upsertMcpTokens(
                listOf(
                    McpTokenEntity("token-1", "Test", "mcp_...123", "2026-08-11T00:00:00Z")
                )
            )
        }
        val repository = buildRepository(apiService = apiService, tokenDao = tokenDao, monitor = onlineMonitor)

        val result = repository.deleteMcpToken("token-1")

        assertTrue(result is Result.Success)
        assertEquals("token-1", apiService.lastDeletedId)
        assertTrue(tokenDao.storedTokens.none { it.id == "token-1" })
    }

    @Test
    fun `deleteMcpToken returns network error when offline`() = runTest(testDispatcher) {
        val repository = buildRepository(monitor = offlineMonitor)

        val result = repository.deleteMcpToken("token-1")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Network)
    }

    @Test
    fun `regenerateMcpToken updates token in network and Room`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService().apply {
            createdTokenResponse = CreatedMcpTokenResponseDto(
                id = "token-1",
                name = "Claude Desktop",
                rawToken = "new-raw-secret",
                maskedToken = "mcp_...new",
                createdAt = "2026-08-11T01:00:00Z",
            )
        }
        val tokenDao = FakeMcpTokenDao().apply {
            upsertMcpTokens(
                listOf(
                    McpTokenEntity("token-1", "Claude Desktop", "mcp_...old", "2026-08-11T00:00:00Z")
                )
            )
        }
        val repository = buildRepository(apiService = apiService, tokenDao = tokenDao, monitor = onlineMonitor)

        val result = repository.regenerateMcpToken("token-1")

        assertTrue(result is Result.Success)
        val createdToken = (result as Result.Success).data
        assertEquals("new-raw-secret", createdToken.rawToken)
        assertEquals("token-1", apiService.lastRegeneratedId)
        assertEquals("mcp_...new", tokenDao.storedTokens.first().maskedToken)
    }

    @Test
    fun `regenerateMcpToken preserves raw token success even if Room write fails`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService().apply {
            createdTokenResponse = CreatedMcpTokenResponseDto(
                id = "token-1",
                name = "Claude Desktop",
                rawToken = "new-raw-secret",
                maskedToken = "mcp_...new",
                createdAt = "2026-08-11T01:00:00Z",
            )
        }
        val tokenDao = FakeMcpTokenDao().apply { shouldFailOnUpsert = true }
        val repository = buildRepository(apiService = apiService, tokenDao = tokenDao, monitor = onlineMonitor)

        val result = repository.regenerateMcpToken("token-1")

        assertTrue(result is Result.Success)
        val createdToken = (result as Result.Success).data
        assertEquals("new-raw-secret", createdToken.rawToken)
    }
}
