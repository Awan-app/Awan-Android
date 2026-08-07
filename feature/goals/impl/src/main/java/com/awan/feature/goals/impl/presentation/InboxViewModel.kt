package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.usecase.GetInboxTasksUseCase
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.TaskWithSessions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val DateLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val TimeLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val getInboxTasksUseCase: GetInboxTasksUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state.asStateFlow()

    init {
        loadInboxTasks()
    }

    fun onAction(action: InboxAction) {
        when (action) {
            is InboxAction.SearchQueryChanged -> _state.update { it.copy(searchQuery = action.query) }

            is InboxAction.StatusFilterToggled -> _state.update { current ->
                val filters = current.activeStatusFilters.toMutableSet()
                if (!filters.add(action.filter)) filters.remove(action.filter)
                current.copy(activeStatusFilters = filters)
            }

            is InboxAction.SessionFilterToggled -> _state.update { current ->
                val filters = current.activeSessionFilters.toMutableSet()
                if (!filters.add(action.filter)) filters.remove(action.filter)
                current.copy(activeSessionFilters = filters)
            }

            is InboxAction.TaskExpandToggled -> _state.update { current ->
                val newId = if (current.expandedTaskId == action.taskId) null else action.taskId
                current.copy(expandedTaskId = newId)
            }

            InboxAction.RetryClicked -> loadInboxTasks()
        }
    }

    private fun loadInboxTasks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false) }
            when (val result = getInboxTasksUseCase()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isError = false,
                            allTasks = result.data.map { tws -> tws.toUiModel() },
                        )
                    }
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, isError = true) }
                Result.Loading -> { /* Handled above */ }
            }
        }
    }

    private fun TaskWithSessions.toUiModel(): InboxTaskUiModel {
        val now = LocalDateTime.now()
        return InboxTaskUiModel(
            id = task.id,
            title = task.title,
            description = task.description,
            displayStatus = deriveDisplayStatus(),
            sessions = sessions.mapNotNull { session ->
                val isScheduled = session.status == SessionStatus.SCHEDULED
                InboxSessionUiModel(
                    id = session.id,
                    dateLabel = session.start.format(DateLabel),
                    timeRange = "${session.start.format(TimeLabel)} – ${session.end.format(TimeLabel)}",
                    statusLabel = when (session.status) {
                        SessionStatus.SCHEDULED -> "Scheduled"
                        SessionStatus.COMPLETED -> "Completed"
                        SessionStatus.CANCELLED -> "Cancelled"
                        SessionStatus.MISSED -> "Missed"
                        else -> session.status.name.lowercase()
                            .replaceFirstChar { it.uppercase() }
                    },
                    isActiveNow = isScheduled && !now.isBefore(session.start) && !now.isAfter(session.end),
                    isMissed = isScheduled && now.isAfter(session.end),
                )
            },
        )
    }
}
