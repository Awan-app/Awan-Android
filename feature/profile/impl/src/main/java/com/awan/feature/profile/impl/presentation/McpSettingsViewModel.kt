package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.usecase.CreateMcpTokenUseCase
import com.awan.app.core.domain.mcp.usecase.DeleteMcpTokenUseCase
import com.awan.app.core.domain.mcp.usecase.GetMcpConnectionDetailsUseCase
import com.awan.app.core.domain.mcp.usecase.GetMcpTokensUseCase
import com.awan.app.core.domain.mcp.usecase.RegenerateMcpTokenUseCase
import com.awan.feature.profile.impl.helpers.ProfileErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class McpSettingsViewModel @Inject constructor(
    private val getMcpConnectionDetailsUseCase: GetMcpConnectionDetailsUseCase,
    private val getMcpTokensUseCase: GetMcpTokensUseCase,
    private val createMcpTokenUseCase: CreateMcpTokenUseCase,
    private val deleteMcpTokenUseCase: DeleteMcpTokenUseCase,
    private val regenerateMcpTokenUseCase: RegenerateMcpTokenUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(McpSettingsState())
    val uiState: StateFlow<McpSettingsState> = _uiState.asStateFlow()

    private val _events = Channel<McpSettingsEvent>(Channel.BUFFERED)
    val events: Flow<McpSettingsEvent> = _events.receiveAsFlow()

    init {
        loadData()
    }

    fun onAction(action: McpSettingsAction) {
        when (action) {
            is McpSettingsAction.CreateToken -> createToken(action.name)
            is McpSettingsAction.DeleteToken -> deleteToken(action.id)
            is McpSettingsAction.RegenerateToken -> regenerateToken(action.id)
            McpSettingsAction.DismissCreatedModal -> _uiState.update { it.copy(createdToken = null) }
            McpSettingsAction.DismissError -> _uiState.update { it.copy(error = null) }
            McpSettingsAction.Refresh -> loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getMcpConnectionDetailsUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(connectionDetails = result.data, isLoading = false) }
                    }
                    is Result.Error -> {
                        val uiError = ProfileErrorMapper.mapToUiText(result.error)
                        _uiState.update { it.copy(error = uiError, isLoading = false) }
                    }
                    Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }

        viewModelScope.launch {
            getMcpTokensUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(tokens = result.data) }
                    }
                    is Result.Error -> {
                        val uiError = ProfileErrorMapper.mapToUiText(result.error)
                        _uiState.update { it.copy(error = uiError) }
                    }
                    Result.Loading -> Unit
                }
            }
        }
    }

    private fun createToken(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            when (val result = createMcpTokenUseCase(name)) {
                is Result.Success -> {
                    val created = result.data
                    _uiState.update { state ->
                        state.copy(
                            isCreating = false,
                            createdToken = created,
                        )
                    }
                    _events.send(McpSettingsEvent.TokenCreated(created))
                }
                is Result.Error -> {
                    val uiError = ProfileErrorMapper.mapToUiText(result.error)
                    _uiState.update { it.copy(isCreating = false, error = uiError) }
                    _events.send(McpSettingsEvent.Error(uiError))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun deleteToken(id: String) {
        viewModelScope.launch {
            when (val result = deleteMcpTokenUseCase(id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(tokens = state.tokens.filterNot { it.id == id })
                    }
                    _events.send(McpSettingsEvent.TokenDeleted)
                }
                is Result.Error -> {
                    val uiError = ProfileErrorMapper.mapToUiText(result.error)
                    _uiState.update { it.copy(error = uiError) }
                    _events.send(McpSettingsEvent.Error(uiError))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun regenerateToken(id: String) {
        viewModelScope.launch {
            when (val result = regenerateMcpTokenUseCase(id)) {
                is Result.Success -> {
                    val regenerated = result.data
                    _uiState.update { state ->
                        state.copy(createdToken = regenerated)
                    }
                    _events.send(McpSettingsEvent.TokenRegenerated(regenerated))
                }
                is Result.Error -> {
                    val uiError = ProfileErrorMapper.mapToUiText(result.error)
                    _uiState.update { it.copy(error = uiError) }
                    _events.send(McpSettingsEvent.Error(uiError))
                }
                Result.Loading -> Unit
            }
        }
    }
}
