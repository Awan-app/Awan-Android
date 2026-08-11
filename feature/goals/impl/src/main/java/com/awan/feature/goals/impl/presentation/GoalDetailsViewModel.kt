package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.usecase.DeleteGoalUseCase
import com.awan.app.core.domain.goal.usecase.GetGoalUseCase
import com.awan.app.core.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GoalDetailsEvent {
    data object NavigateBack : GoalDetailsEvent
    data class OpenAddTask(val goalId: String) : GoalDetailsEvent
}

@HiltViewModel
class GoalDetailsViewModel @Inject constructor(
    private val getGoalUseCase: GetGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalDetailsState())
    val state: StateFlow<GoalDetailsState> = _state.asStateFlow()

    private val _events = Channel<GoalDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadGoal(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getGoalUseCase(id)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, goal = result.data) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = "Failed to load goal") }
                }
                Result.Loading -> {}
            }
        }
    }

    fun onAction(action: GoalDetailsAction) {
        when (action) {
            GoalDetailsAction.Retry -> {
                _state.value.goal?.id?.let { loadGoal(it) }
            }

            GoalDetailsAction.Back -> {
                viewModelScope.launch {
                    _events.send(GoalDetailsEvent.NavigateBack)
                }
            }

            GoalDetailsAction.DeleteClicked -> deleteGoal()

            GoalDetailsAction.AddTaskClicked -> {
                _state.value.goal?.id?.let {
                    viewModelScope.launch {
                        _events.send(GoalDetailsEvent.OpenAddTask(it))
                    }
                }
            }
        }
    }

    private fun deleteGoal() {
        val goalId = _state.value.goal?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (deleteGoalUseCase(goalId)) {
                is Result.Success -> {
                    _events.send(GoalDetailsEvent.NavigateBack)
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = "Failed to delete goal") }
                }
                Result.Loading -> {}
            }
        }
    }
}
