package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.repository.McpRepository
import com.awan.app.core.domain.mcp.usecase.GetMcpConnectionDetailsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state loads connection details successfully`() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertNotNull(state.connectionDetails)
        assertEquals("https://awanproduction.up.railway.app/mcp", state.connectionDetails?.mcpUrl)
        assertEquals("awan-mcp", state.connectionDetails?.clientId)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `initial state sets error and emits event when repository returns error`() = runTest(testDispatcher) {
        val errorRepo = FakeMcpRepository().apply { shouldReturnError = true }
        val events = mutableListOf<McpSettingsEvent>()

        val errorViewModel = McpSettingsViewModel(
            getMcpConnectionDetailsUseCase = GetMcpConnectionDetailsUseCase(errorRepo),
        )
        val job = launch { errorViewModel.events.toList(events) }

        val state = errorViewModel.uiState.value
        assertNull(state.connectionDetails)
        assertNotNull(state.error)
        assertEquals(1, events.size)
        assert(events.first() is McpSettingsEvent.Error)

        job.cancel()
    }

    @Test
    fun `Refresh action re-fetches connection details`() = runTest(testDispatcher) {
        fakeRepository.details = McpConnectionDetails(
            mcpUrl = "https://updated.railway.app/mcp",
            clientId = "awan-updated",
        )

        viewModel.onAction(McpSettingsAction.Refresh)

        val state = viewModel.uiState.value
        assertEquals("https://updated.railway.app/mcp", state.connectionDetails?.mcpUrl)
        assertEquals("awan-updated", state.connectionDetails?.clientId)
    }

    @Test
    fun `DismissError clears error in state`() = runTest(testDispatcher) {
        val errorRepo = FakeMcpRepository().apply { shouldReturnError = true }
        val errorViewModel = McpSettingsViewModel(
            getMcpConnectionDetailsUseCase = GetMcpConnectionDetailsUseCase(errorRepo),
        )

        assertNotNull(errorViewModel.uiState.value.error)
        errorViewModel.onAction(McpSettingsAction.DismissError)
        assertNull(errorViewModel.uiState.value.error)
    }

    private class FakeMcpRepository : McpRepository {
        var shouldReturnError = false
        var details = McpConnectionDetails(
            mcpUrl = "https://awanproduction.up.railway.app/mcp",
            clientId = "awan-mcp",
        )

        override fun getMcpConnectionDetails(): Flow<Result<McpConnectionDetails>> {
            if (shouldReturnError) {
                return flowOf(Result.Error(AppError.Network))
            }
            return flowOf(Result.Success(details))
        }
    }
}
