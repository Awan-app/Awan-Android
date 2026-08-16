package com.awan.app.core.domain.mcp.usecase

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
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

    @Before
    fun setUp() {
        fakeRepository = FakeMcpRepository()
        getMcpConnectionDetailsUseCase = GetMcpConnectionDetailsUseCase(fakeRepository)
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
    fun `GetMcpConnectionDetailsUseCase propagates error from repository`() = runTest {
        fakeRepository.shouldReturnError = true
        val result = getMcpConnectionDetailsUseCase().first()
        assertTrue(result is Result.Error)
        assertEquals(AppError.Network, (result as Result.Error).error)
    }

    private class FakeMcpRepository : McpRepository {
        var shouldReturnError = false

        override fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>> {
            if (shouldReturnError) {
                return flowOf(Result.Error(AppError.Network))
            }
            return flowOf(
                Result.Success(
                    McpConnectionDetails(
                        mcpUrl = "https://api.awan.app/mcp",
                        clientId = "client-123",
                    ),
                ),
            )
        }
    }
}
