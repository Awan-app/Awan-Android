package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.usecase.GetInboxTasksUseCase
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.TaskWithSessions
import com.awan.feature.goals.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

            is InboxAction.SessionFilterToggled -> updateState { current ->
                val filters = current.activeSessionFilters.toMutableSet()
                if (!filters.add(action.filter)) filters.remove(action.filter)
                current.copy(activeSessionFilters = filters)
            }

            is InboxAction.TaskExpandToggled -> updateState { current ->
                val newId = if (current.expandedTaskId == action.taskId) null else action.taskId
                current.copy(expandedTaskId = newId)
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

        if (state.activeSessionFilters.isNotEmpty()) {
            result = result.filter { task ->
                task.sessions.any { session ->
                    (InboxSessionFilter.ActiveNow in state.activeSessionFilters && session.isActiveNow) ||
                        (InboxSessionFilter.Missed in state.activeSessionFilters && session.isMissed)
                }
            }
        }

        val q = state.searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            result = result.filter { task ->
                task.title.lowercase().contains(q) ||
                    task.description?.lowercase()?.contains(q) == true ||
                    task.sessions.any { s ->
                        s.dateLabel.lowercase().contains(q) ||
                            s.startTime.lowercase().contains(q) ||
                            s.endTime.lowercase().contains(q)
                    }
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
                    val now = LocalDateTime.now()
                    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    updateState {
                        it.copy(
                            isLoading = false,
                            isError = false,
                            allTasks = result.data.map { tws -> tws.toUiModel(now, dateFormatter, timeFormatter) },
                        )
                    }
                }
                is Result.Error -> updateState { it.copy(isLoading = false, isError = true) }
                Result.Loading -> { /* Handled above */ }
            }
        }
    }

    private fun TaskWithSessions.toUiModel(
        now: LocalDateTime,
        dateFormatter: DateTimeFormatter,
        timeFormatter: DateTimeFormatter
    ): InboxTaskUiModel {
        return InboxTaskUiModel(
            id = task.id,
            title = task.title,
            description = task.description,
            displayStatus = deriveDisplayStatus(),
            sessions = sessions.mapNotNull { session ->
                val isScheduled = session.status == SessionStatus.SCHEDULED
                val isBackendMissed = session.status == SessionStatus.MISSED
                
                InboxSessionUiModel(
                    id = session.id,
                    dateLabel = session.start.format(dateFormatter),
                    startTime = session.start.format(timeFormatter),
                    endTime = session.end.format(timeFormatter),
                    statusLabelRes = when (session.status) {
                        SessionStatus.SCHEDULED -> R.string.inbox_status_scheduled
                        SessionStatus.COMPLETED -> R.string.inbox_status_completed
                        SessionStatus.CANCELLED -> R.string.inbox_status_cancelled
                        SessionStatus.MISSED -> R.string.inbox_status_missed
                        else -> R.string.inbox_status_unknown
                    },
                    isActiveNow = isScheduled && !now.isBefore(session.start) && !now.isAfter(session.end),
                    isMissed = isBackendMissed || (isScheduled && now.isAfter(session.end)),
                )
            },
        )
    }
}
