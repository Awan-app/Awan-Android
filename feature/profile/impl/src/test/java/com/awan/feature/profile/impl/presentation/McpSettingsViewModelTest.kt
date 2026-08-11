package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.model.McpToken
import com.awan.app.core.domain.mcp.repository.McpRepository
import com.awan.app.core.domain.mcp.usecase.CreateMcpTokenUseCase
import com.awan.app.core.domain.mcp.usecase.DeleteMcpTokenUseCase
import com.awan.app.core.domain.mcp.usecase.GetMcpConnectionDetailsUseCase
import com.awan.app.core.domain.mcp.usecase.GetMcpTokensUseCase
import com.awan.app.core.domain.mcp.usecase.RegenerateMcpTokenUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class McpSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeRepository: FakeMcpRepository
    private lateinit var viewModel: McpSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMcpRepository()
        viewModel = McpSettingsViewModel(
            getMcpConnectionDetailsUseCase = GetMcpConnectionDetailsUseCase(fakeRepository),
            getMcpTokensUseCase = GetMcpTokensUseCase(fakeRepository),
            createMcpTokenUseCase = CreateMcpTokenUseCase(fakeRepository),
            deleteMcpTokenUseCase = DeleteMcpTokenUseCase(fakeRepository),
            regenerateMcpTokenUseCase = RegenerateMcpTokenUseCase(fakeRepository),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state loads connection details and tokens`() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertNotNull(state.connectionDetails)
        assertEquals("https://mcp.awan.app/v1", state.connectionDetails?.mcpUrl)
        assertEquals(1, state.tokens.size)
        assertEquals("Claude Desktop", state.tokens.first().name)
    }

    @Test
    fun `CreateToken action creates token, sets createdToken, and emits TokenCreated event`() = runTest(testDispatcher) {
        val events = mutableListOf<McpSettingsEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.onAction(McpSettingsAction.CreateToken("Cursor"))

        val state = viewModel.uiState.value
        assertNotNull(state.createdToken)
        assertEquals("Cursor", state.createdToken?.name)
        assertEquals("raw_secret_cursor_key", state.createdToken?.rawToken)
        assertEquals(1, events.size)
        assert(events.first() is McpSettingsEvent.TokenCreated)

        job.cancel()
    }

    @Test
    fun `DeleteToken action removes token from state and emits TokenDeleted event`() = runTest(testDispatcher) {
        val events = mutableListOf<McpSettingsEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.onAction(McpSettingsAction.DeleteToken("token-1"))

        val state = viewModel.uiState.value
        assertEquals(0, state.tokens.size)
        assertEquals(1, events.size)
        assert(events.first() is McpSettingsEvent.TokenDeleted)

        job.cancel()
    }

    @Test
    fun `RegenerateToken action sets new createdToken and emits TokenRegenerated event`() = runTest(testDispatcher) {
        val events = mutableListOf<McpSettingsEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.onAction(McpSettingsAction.RegenerateToken("token-1"))

        val state = viewModel.uiState.value
        assertNotNull(state.createdToken)
        assertEquals("raw_regenerated_token-1", state.createdToken?.rawToken)
        assertEquals(1, events.size)
        assert(events.first() is McpSettingsEvent.TokenRegenerated)

        job.cancel()
    }

    @Test
    fun `DismissCreatedModal clears createdToken in state`() = runTest(testDispatcher) {
        viewModel.onAction(McpSettingsAction.CreateToken("Test"))
        assertNotNull(viewModel.uiState.value.createdToken)

        viewModel.onAction(McpSettingsAction.DismissCreatedModal)
        assertNull(viewModel.uiState.value.createdToken)
    }

    private class FakeMcpRepository : McpRepository {
        private val tokensList = mutableListOf(
            McpToken("token-1", "Claude Desktop", "••••••••abcd", "2026-08-11T00:00:00Z")
        )
        private val tokensFlow = MutableStateFlow<Result<List<McpToken>>>(Result.Success(tokensList))

        override fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>> {
            return flowOf(Result.Success(McpConnectionDetails("https://mcp.awan.app/v1", "awan-android-client")))
        }

        override fun getMcpTokens(): Flow<Result<List<McpToken>>> = tokensFlow

        override suspend fun createMcpToken(name: String): Result<CreatedMcpToken> {
            val created = CreatedMcpToken(
                id = "token-${System.currentTimeMillis()}",
                name = name,
                rawToken = "raw_secret_${name.lowercase()}_key",
                maskedToken = "••••••••secret",
                createdAt = "2026-08-11T00:00:00Z"
            )
            tokensList.add(McpToken(created.id, created.name, created.maskedToken, created.createdAt))
            tokensFlow.value = Result.Success(tokensList.toList())
            return Result.Success(created)
        }

        override suspend fun deleteMcpToken(id: String): Result<Unit> {
            tokensList.removeAll { it.id == id }
            tokensFlow.value = Result.Success(tokensList.toList())
            return Result.Success(Unit)
        }

        override suspend fun regenerateMcpToken(id: String): Result<CreatedMcpToken> {
            val created = CreatedMcpToken(
                id = id,
                name = "Regenerated",
                rawToken = "raw_regenerated_$id",
                maskedToken = "••••••••regen",
                createdAt = "2026-08-11T00:00:00Z"
            )
            return Result.Success(created)
        }
    }
}
