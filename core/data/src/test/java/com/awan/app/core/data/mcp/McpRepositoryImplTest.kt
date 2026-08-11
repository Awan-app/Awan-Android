package com.awan.app.core.data.mcp

import com.awan.app.core.common.error.AppError
import com.awan.app.core.network.BuildConfig
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.mcp.repository.McpRepositoryImpl
import com.awan.app.core.database.dao.McpTokenDao
import com.awan.app.core.database.model.McpTokenEntity
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.network.api.McpApiService
import com.awan.app.core.network.dto.mcp.ApiKeyResponseDto
import com.awan.app.core.network.dto.mcp.ApiKeySummaryDto
import com.awan.app.core.network.dto.mcp.CreateApiKeyRequestDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

private class FakeMcpApiService : McpApiService {
    var apiKeysList = mutableListOf<ApiKeySummaryDto>()
    var createApiKeyResponse = ApiKeyResponseDto(
        id = "token-1",
        name = "Default Token",
        keyValue = "raw-secret-123",
        createdAt = "2026-08-11T00:00:00Z",
    )
    var shouldFailWithException: Exception? = null
    var lastCreatedName: String? = null
    var lastRevokedId: String? = null
    var httpErrorCode: Int? = null
    var revokeHttpErrorCode: Int? = null

    override suspend fun getApiKeys(): Response<List<ApiKeySummaryDto>> {
        shouldFailWithException?.let { throw it }
        httpErrorCode?.let {
            return Response.error(it, "Error".toResponseBody(null))
        }
        return Response.success(apiKeysList)
    }

    override suspend fun createApiKey(request: CreateApiKeyRequestDto): Response<ApiKeyResponseDto> {
        shouldFailWithException?.let { throw it }
        httpErrorCode?.let {
            return Response.error(it, "Error".toResponseBody(null))
        }
        lastCreatedName = request.name
        return Response.success(createApiKeyResponse.copy(name = request.name))
    }

    override suspend fun revokeApiKey(keyId: String): Response<Unit> {
        shouldFailWithException?.let { throw it }
        revokeHttpErrorCode?.let {
            return Response.error(it, "Error".toResponseBody(null))
        }
        httpErrorCode?.let {
            return Response.error(it, "Error".toResponseBody(null))
        }
        lastRevokedId = keyId
        return Response.success(Unit)
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
    fun `getMcpConnectionDetails returns static connection details`() = runTest(testDispatcher) {
        val repository = buildRepository(monitor = offlineMonitor)

        val result = repository.getMcpConnectionDetails().first()

        assertTrue(result is Result.Success)
        val details = (result as Result.Success).data
        assertEquals("${BuildConfig.AWAN_BASE_URL.trimEnd('/')}/v1/mcp", details.mcpUrl)
        assertEquals("awan-android-client", details.clientId)
    }

    @Test
    fun `getMcpTokens fetches remote api keys and updates Room atomically when online`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService().apply {
            apiKeysList.add(
                ApiKeySummaryDto(
                    id = "key-1",
                    name = "Claude Desktop",
                    keyPrefix = "mcp_...123",
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
        assertEquals("mcp_...123", tokenDao.storedTokens.first().maskedToken)
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
    fun `deleteMcpToken revokes token on network and deletes from Room`() = runTest(testDispatcher) {
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
        assertEquals("token-1", apiService.lastRevokedId)
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
    fun `regenerateMcpToken creates new token, revokes old token on network, and updates Room`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService().apply {
            createApiKeyResponse = ApiKeyResponseDto(
                id = "token-2",
                name = "Claude Desktop",
                keyValue = "new-raw-secret",
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
        assertEquals("Claude Desktop", apiService.lastCreatedName)
        assertEquals("token-1", apiService.lastRevokedId)
        assertTrue(tokenDao.storedTokens.none { it.id == "token-1" })
        assertEquals(1, tokenDao.storedTokens.size)
        assertEquals("token-2", tokenDao.storedTokens.first().id)
    }

    @Test
    fun `regenerateMcpToken keeps old Room token when revocation fails`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService().apply {
            createApiKeyResponse = ApiKeyResponseDto(
                id = "token-2",
                name = "Claude Desktop",
                keyValue = "new-raw-secret",
                createdAt = "2026-08-11T01:00:00Z",
            )
            revokeHttpErrorCode = 500
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

        assertTrue(result is Result.Error)
        assertEquals(1, tokenDao.storedTokens.size)
        assertEquals("token-1", tokenDao.storedTokens.first().id)
    }
    @Test
    fun `regenerateMcpToken preserves raw token success even if Room write fails`() = runTest(testDispatcher) {
        val apiService = FakeMcpApiService().apply {
            createApiKeyResponse = ApiKeyResponseDto(
                id = "token-2",
                name = "Claude Desktop",
                keyValue = "new-raw-secret",
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

