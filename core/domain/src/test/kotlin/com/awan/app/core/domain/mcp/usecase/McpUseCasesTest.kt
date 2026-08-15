package com.awan.app.core.domain.mcp.usecase

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.app.core.domain.mcp.repository.McpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class McpUseCasesTest {

    private lateinit var fakeRepository: FakeMcpRepository
    private lateinit var getMcpConnectionDetailsUseCase: GetMcpConnectionDetailsUseCase
    private lateinit var getMcpTokensUseCase: GetMcpTokensUseCase
    private lateinit var createMcpTokenUseCase: CreateMcpTokenUseCase
    private lateinit var deleteMcpTokenUseCase: DeleteMcpTokenUseCase
    private lateinit var regenerateMcpTokenUseCase: RegenerateMcpTokenUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeMcpRepository()
        getMcpConnectionDetailsUseCase = GetMcpConnectionDetailsUseCase(fakeRepository)
        getMcpTokensUseCase = GetMcpTokensUseCase(fakeRepository)
        createMcpTokenUseCase = CreateMcpTokenUseCase(fakeRepository)
        deleteMcpTokenUseCase = DeleteMcpTokenUseCase(fakeRepository)
        regenerateMcpTokenUseCase = RegenerateMcpTokenUseCase(fakeRepository)
    }

    @Test
    fun `GetMcpConnectionDetailsUseCase returns connection details from repository`() = runTest {
        val result = getMcpConnectionDetailsUseCase().first()
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("https://api.awan.app/mcp", data.mcpUrl)
        assertEquals("client-123", data.clientId)
    }

    @Test
    fun `GetMcpTokensUseCase returns list of tokens from repository`() = runTest {
        val result = getMcpTokensUseCase().first()
        assertTrue(result is Result.Success)
        val tokens = (result as Result.Success).data
        assertEquals(1, tokens.size)
        assertEquals("Claude Desktop", tokens.first().name)
    }

    @Test
    fun `CreateMcpTokenUseCase succeeds and returns created token`() = runTest {
        val result = createMcpTokenUseCase("Cursor")
        assertTrue(result is Result.Success)
        val created = (result as Result.Success).data
        assertEquals("Cursor", created.name)
        assertEquals("raw_secret_token_123", created.rawToken)
    }

    @Test
    fun `CreateMcpTokenUseCase propagates repository error`() = runTest {
        fakeRepository.shouldReturnError = true
        val result = createMcpTokenUseCase("Error Token")
        assertTrue(result is Result.Error)
        assertEquals(AppError.Network, (result as Result.Error).error)
    }

    @Test
    fun `DeleteMcpTokenUseCase succeeds when deleting valid token`() = runTest {
        val result = deleteMcpTokenUseCase("token-1")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `RegenerateMcpTokenUseCase succeeds and returns new created token`() = runTest {
        val result = regenerateMcpTokenUseCase("token-1")
        assertTrue(result is Result.Success)
        val created = (result as Result.Success).data
        assertEquals("token-1", created.id)
        assertEquals("raw_regenerated_token_456", created.rawToken)
    }

    private class FakeMcpRepository : McpRepository {
        var shouldReturnError = false

        override fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>> {
            return flowOf(
                Result.Success(
                    McpConnectionDetails(
                        mcpUrl = "https://api.awan.app/mcp",
                        clientId = "client-123",
                    ),
                ),
            )
        }

        override fun getMcpTokens(): Flow<Result<List<McpToken>>> {
            return flowOf(
                Result.Success(
                    listOf(
                        McpToken(
                            id = "token-1",
                            name = "Claude Desktop",
                            maskedToken = "••••••••abc123",
                            createdAt = "2026-08-11T00:00:00Z",
                        ),
                    ),
                ),
            )
        }

        override suspend fun createMcpToken(name: String): Result<CreatedMcpToken> {
            if (shouldReturnError) return Result.Error(AppError.Network)
            return Result.Success(
                CreatedMcpToken(
                    id = "token-new",
                    name = name,
                    rawToken = "raw_secret_token_123",
                    maskedToken = "••••••••token_123",
                    createdAt = "2026-08-11T00:00:00Z",
                ),
            )
        }

        override suspend fun deleteMcpToken(id: String): Result<Unit> {
            if (shouldReturnError) return Result.Error(AppError.Network)
            return Result.Success(Unit)
        }

        override suspend fun regenerateMcpToken(id: String): Result<CreatedMcpToken> {
            if (shouldReturnError) return Result.Error(AppError.Network)
            return Result.Success(
                CreatedMcpToken(
                    id = id,
                    name = "Regenerated Token",
                    rawToken = "raw_regenerated_token_456",
                    maskedToken = "••••••••token_456",
                    createdAt = "2026-08-11T00:00:00Z",
                ),
            )
        }
    }
}
