package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.usecase.GetInboxTasksUseCase
import com.awan.app.core.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val getInboxTasksUseCase: GetInboxTasksUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state.asStateFlow()
    
    private val _events = Channel<InboxEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    private var fetchJob: Job? = null

    init {
        loadInboxTasks()
    }

    fun onAction(action: InboxAction) {
        when (action) {
            is InboxAction.SearchQueryChanged -> updateState { it.copy(searchQuery = action.query) }

            is InboxAction.StatusFilterToggled -> updateState { current ->
                val filters = current.activeStatusFilters.toMutableSet()
                if (!filters.add(action.filter)) filters.remove(action.filter)
                current.copy(activeStatusFilters = filters)
            }

            InboxAction.FilterClicked -> updateState { it.copy(showFilterSheet = true) }

            InboxAction.FilterDismissed -> updateState { it.copy(showFilterSheet = false) }

            InboxAction.RetryClicked -> loadInboxTasks()
            
            InboxAction.BackClicked -> {
                viewModelScope.launch {
                    _events.send(InboxEvent.NavigateBack)
                }
            }
        }
    }

    private fun updateState(block: (InboxUiState) -> InboxUiState) {
        _state.update { current ->
            val next = block(current)
            next.copy(visibleTasks = filterTasks(next))
        }
    }

    private fun filterTasks(state: InboxUiState): List<InboxTaskUiModel> {
        var result = state.allTasks

        if (state.activeStatusFilters.isNotEmpty()) {
            result = result.filter { it.displayStatus in state.activeStatusFilters }
        }

        val q = state.searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            result = result.filter { task ->
                task.title.lowercase().contains(q) ||
                    task.description?.lowercase()?.contains(q) == true
            }
        }
        return result
    }

    private fun loadInboxTasks() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true, isError = false) }
            when (val result = getInboxTasksUseCase()) {
                is Result.Success -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            isError = false,
                            allTasks = result.data.map { task -> task.toUiModel() },
                        )
                    }
                }
                is Result.Error -> updateState { it.copy(isLoading = false, isError = true) }
                Result.Loading -> { /* Handled above */ }
            }
        }
    }

    private fun Task.toUiModel(): InboxTaskUiModel {
        return InboxTaskUiModel(
            id = id,
            title = title,
            description = description,
            displayStatus = deriveDisplayStatus(),
        )
    }
}
