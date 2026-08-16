package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.usecase.DeleteGoalUseCase
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import com.awan.app.core.domain.goal.usecase.ObserveGoalsUseCase
import com.awan.app.core.domain.task.usecase.GetInboxTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val observeGoalsUseCase: ObserveGoalsUseCase,
    private val getInboxTasksUseCase: GetInboxTasksUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsState())
    val state: StateFlow<GoalsState> = _state.asStateFlow()

    private val _events = Channel<GoalsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeGoals()
        refreshGoals()
        loadInboxCount()
    }

    fun onAction(action: GoalsAction) {
        when (action) {
            is GoalsAction.SearchQueryChanged -> _state.update { it.copy(searchQuery = action.query) }
            GoalsAction.RetryClicked -> {
                refreshGoals()
                loadInboxCount()
            }
            is GoalsAction.GoalClicked -> {
                viewModelScope.launch {
                    _events.send(GoalsEvent.NavigateToGoalDetails(action.goalId))
                }
            }

            is GoalsAction.DeleteGoalClicked -> {
                _state.update { it.copy(deletingGoalId = action.goalId) }
            }

            GoalsAction.DeleteGoalConfirmed -> {
                val goalId = _state.value.deletingGoalId ?: return
                _state.update { it.copy(deletingGoalId = null, isLoading = true) }
                viewModelScope.launch {
                    when (val result = deleteGoalUseCase(goalId)) {
                        is Result.Success -> refreshGoals()
                        is Result.Error -> _state.update { it.copy(isLoading = false, isError = true) }
                        Result.Loading -> {}
                    }
                }
            }

            GoalsAction.DeleteGoalCancelled -> {
                _state.update { it.copy(deletingGoalId = null) }
            }

            GoalsAction.FilterClicked -> _state.update { 
                it.copy(
                    isFilterSheetOpen = true,
                    pendingFilters = it.appliedFilters
                ) 
            }
            GoalsAction.DismissFilterSheet -> _state.update { it.copy(isFilterSheetOpen = false) }

            is GoalsAction.PendingStatusFilterChanged -> _state.update { 
                it.copy(pendingFilters = it.pendingFilters.copy(status = action.status))
            }

            is GoalsAction.PendingTypeFilterChanged -> _state.update { 
                it.copy(pendingFilters = it.pendingFilters.copy(type = action.type))
            }
            
            GoalsAction.ApplyFiltersClicked -> _state.update { 
                it.copy(
                    appliedFilters = it.pendingFilters,
                    isFilterSheetOpen = false
                )
            }
            
            GoalsAction.ResetFiltersClicked -> _state.update { 
                it.copy(pendingFilters = GoalFilters())
            }

            GoalsAction.ClearFiltersClicked -> _state.update { 
                it.copy(
                    appliedFilters = GoalFilters(),
                    pendingFilters = GoalFilters()
                )
            }

            GoalsAction.AddGoalClicked -> {
                viewModelScope.launch {
                    _events.send(GoalsEvent.NavigateToAddGoal)
                }
            }
            GoalsAction.InboxClicked -> {
                viewModelScope.launch {
                    _events.send(GoalsEvent.NavigateToInbox)
                }
            }
        }
    }

    private fun observeGoals() {
        viewModelScope.launch {
            observeGoalsUseCase().collect { goals ->
                _state.update { it.copy(goals = goals) }
            }
        }
    }

    private fun loadInboxCount() {
        viewModelScope.launch {
            when (val result = getInboxTasksUseCase()) {
                is Result.Success -> {
                    _state.update { it.copy(inboxTaskCount = result.data.size) }
                }
                else -> { /* Ignore errors for inbox count in goals list for now */ }
            }
        }
    }

    private fun refreshGoals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false) }

            when (val result = getGoalsUseCase()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isError = false,
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, isError = true) }
                }
                Result.Loading -> {}
            }
        }
    }
}
