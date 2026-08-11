package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.goal.usecase.DeleteGoalUseCase
import com.awan.app.core.domain.goal.usecase.GetGoalUseCase
import com.awan.app.core.domain.goal.usecase.UpdateGoalUseCase
import com.awan.app.core.model.Goal
import com.awan.feature.goals.impl.R
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
class GoalDetailsViewModel @Inject constructor(
    private val getGoalUseCase: GetGoalUseCase,
    private val updateGoalUseCase: UpdateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle.get<String>("goalId") ?: savedStateHandle.get<String>("id")
    private var currentGoalId: String? = goalId

    private val _state = MutableStateFlow(GoalDetailsState())
    val state: StateFlow<GoalDetailsState> = _state.asStateFlow()

    private val _events = Channel<GoalDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        currentGoalId?.let { loadGoal(it) }
    }

    fun loadGoal(id: String) {
        currentGoalId = id
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getGoalUseCase(id)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, goal = result.data) }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.toUiText()
                        )
                    }
                }

                Result.Loading -> {}
            }
        }
    }

    fun onAction(action: GoalDetailsAction) {
        when (action) {
            GoalDetailsAction.Retry -> {
                currentGoalId?.let { loadGoal(it) }
            }

            GoalDetailsAction.Back -> {
                viewModelScope.launch {
                    _events.send(GoalDetailsEvent.NavigateBack)
                }
            }

            GoalDetailsAction.DeleteClicked -> deleteGoal()
            GoalDetailsAction.EditClicked -> _state.update { it.copy(showEditSheet = true) }
            GoalDetailsAction.EditDismissed -> _state.update { it.copy(showEditSheet = false) }
            is GoalDetailsAction.GoalUpdated -> updateGoal(action)
        }
    }

    private fun deleteGoal() {
        val goalId = _state.value.goal?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (val result = deleteGoalUseCase(goalId)) {
                is Result.Success -> {
                    _events.send(GoalDetailsEvent.NavigateBack)
                }
                is Result.Error -> {
                    _state.update { it.copy(isDeleting = false, error = result.error.toUiText()) }
                }
                Result.Loading -> {}
            }
        }
    }

    private fun updateGoal(action: GoalDetailsAction.GoalUpdated) {
        val goalId = _state.value.goal?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true) }
            val result = updateGoalUseCase(
                goalId = goalId,
                title = action.title,
                description = action.description,
                status = action.status,
                targetDate = action.targetDate,
            )
            when (result) {
                is Result.Success -> {
                    _state.update { 
                        it.copy(
                            isUpdating = false, 
                            showEditSheet = false, 
                            goal = result.data 
                        ) 
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isUpdating = false, error = result.error.toUiText()) }
                }
                Result.Loading -> {}
            }
        }
    }
}
