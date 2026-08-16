package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.mcp.usecase.GetMcpConnectionDetailsUseCase
import com.awan.feature.profile.impl.helpers.ProfileErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(McpSettingsState())
    val uiState: StateFlow<McpSettingsState> = _uiState.asStateFlow()

    private val _events = Channel<McpSettingsEvent>(Channel.BUFFERED)
    val events: Flow<McpSettingsEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun onAction(action: McpSettingsAction) {
        when (action) {
            McpSettingsAction.Refresh -> loadData()
            McpSettingsAction.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getMcpConnectionDetailsUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                connectionDetails = result.data,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }
                    is Result.Error -> {
                        val uiError = ProfileErrorMapper.mapToUiText(result.error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = uiError,
                            )
                        }
                        _events.send(McpSettingsEvent.Error(uiError))
                    }
                    Result.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                }
            }
        }
    }
}
