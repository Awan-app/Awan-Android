package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.goal.usecase.DeleteGoalUseCase
import com.awan.app.core.domain.goal.usecase.GetGoalUseCase
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import com.awan.app.core.domain.goal.usecase.ObserveGoalUseCase
import com.awan.app.core.domain.goal.usecase.UpdateGoalUseCase
import com.awan.app.core.domain.task.usecase.CompleteTaskUseCase
import com.awan.app.core.domain.task.usecase.DeleteTaskUseCase
import com.awan.app.core.domain.task.usecase.MoveTaskUseCase
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.usecase.PublishRewardUseCase
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
import com.awan.app.core.model.TaskStatus
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
import javax.inject.Inject

@HiltViewModel
class GoalDetailsViewModel @Inject constructor(
    private val getGoalUseCase: GetGoalUseCase,
    private val observeGoalUseCase: ObserveGoalUseCase,
    private val updateGoalUseCase: UpdateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val moveTaskUseCase: MoveTaskUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val publishRewardUseCase: PublishRewardUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle.get<String>("goalId") ?: savedStateHandle.get<String>("id")
    private var currentGoalId: String? = goalId

    private val _state = MutableStateFlow(GoalDetailsState())
    val state: StateFlow<GoalDetailsState> = _state.asStateFlow()

    private val _events = Channel<GoalDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var observeJob: Job? = null

    init {
        currentGoalId?.let { loadGoal(it) }
    }

    private fun observeGoal(id: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeGoalUseCase(id).collect { goal ->
                _state.update { it.copy(goal = goal) }
            }
        }
    }

    fun loadGoal(id: String) {
        if (currentGoalId != id || observeJob == null) {
            observeGoal(id)
        }
        currentGoalId = id
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.goal == null, error = null) }
            when (val result = getGoalUseCase(id)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                }

                is Result.Error -> {
                    val errorUiText = result.error.toUiText()
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = if (currentState.goal == null) errorUiText else null
                        )
                    }
                    if (_state.value.goal != null) {
                        _events.send(GoalDetailsEvent.ShowError(errorUiText))
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
            is GoalDetailsAction.TaskChecked -> toggleTaskCompletion(action.taskId)
            GoalDetailsAction.AddTaskClicked -> _state.update { it.copy(showAddTaskSheet = true) }
            GoalDetailsAction.AddTaskDismissed -> _state.update { it.copy(showAddTaskSheet = false) }
            is GoalDetailsAction.DeleteTaskClicked -> deleteTask(action.taskId)
            is GoalDetailsAction.MoveTaskConfirmed -> moveTask(action.goalId)
            GoalDetailsAction.MoveTaskDismissed -> _state.update { it.copy(movingTaskId = null) }
        }
    }

    private fun loadAvailableGoals(taskId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingGoals = true, movingTaskId = taskId) }
            when (val result = getGoalsUseCase()) {
                is Result.Success -> {
                    val activeGoals = result.data.filter { 
                        it.status == GoalStatus.ACTIVE && it.id != currentGoalId 
                    }
                    _state.update { it.copy(availableGoals = activeGoals, isLoadingGoals = false) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoadingGoals = false) }
                    _events.send(GoalDetailsEvent.ShowError(result.error.toUiText()))
                }
                Result.Loading -> {}
            }
        }
    }

    private fun moveTask(targetGoalId: String) {
        val taskId = _state.value.movingTaskId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingGoals = true) }
            when (val result = moveTaskUseCase(taskId, targetGoalId)) {
                is Result.Success -> {
                    _state.update { currentState ->
                        val updatedGoal = currentState.goal?.let { goal ->
                            val newTasks = goal.tasks.filter { it.id != taskId }
                            goal.copy(tasks = newTasks)
                        }
                        currentState.copy(
                            goal = updatedGoal,
                            movingTaskId = null,
                            isLoadingGoals = false
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoadingGoals = false) }
                    _events.send(GoalDetailsEvent.ShowError(result.error.toUiText()))
                }
                Result.Loading -> {}
            }
        }
    }

    private fun toggleTaskCompletion(taskId: String) {
        val currentGoal = _state.value.goal ?: return
        val task = currentGoal.tasks.find { it.id == taskId } ?: return
        
        // If already completed, don't call complete endpoint again
        if (task.status == TaskStatus.COMPLETED) return
        
        // Avoid duplicate requests
        if (_state.value.completingTaskIds.contains(taskId)) return

        viewModelScope.launch {
            _state.update { it.copy(completingTaskIds = it.completingTaskIds + taskId) }
            
            when (val result = completeTaskUseCase(taskId)) {
                is Result.Success -> {
                    // Update the task in the current goal state
                    _state.update { currentState ->
                        val updatedGoal = currentState.goal?.let { goal ->
                            val newTasks = goal.tasks.map { 
                                if (it.id == taskId) result.data else it 
                            }
                            goal.copy(tasks = newTasks)
                        }
                        currentState.copy(
                            goal = updatedGoal,
                            completingTaskIds = currentState.completingTaskIds - taskId
                        )
                    }
                    
                    // Check if all tasks are completed to mark goal as ACHIEVED
                    checkGoalAchievement()
                }
                is Result.Error -> {
                    _state.update { it.copy(completingTaskIds = it.completingTaskIds - taskId) }
                    _events.send(GoalDetailsEvent.ShowError(result.error.toUiText()))
                }
                Result.Loading -> {}
            }
        }
    }

    private fun checkGoalAchievement() {
        val goal = _state.value.goal ?: return
        if (goal.status == GoalStatus.ACHIEVED) return
        if (goal.tasks.isEmpty()) return
        
        val allCompleted = goal.tasks.all { it.status == TaskStatus.COMPLETED }
        if (allCompleted) {
            viewModelScope.launch {
                val result = updateGoalUseCase(
                    goalId = goal.id,
                    status = GoalStatus.ACHIEVED.name
                )
                if (result is Result.Success) {
                    _state.update { it.copy(goal = result.data) }
                    publishRewardUseCase(
                        RewardEvent.GoalAchieved(
                            goalId = result.data.id,
                            title = result.data.title,
                            emoji = result.data.emoji
                        )
                    )
                }
            }
        }
    }

    private fun deleteTask(taskId: String) {
        viewModelScope.launch {
            when (val result = deleteTaskUseCase(taskId)) {
                is Result.Success -> {
                    _state.update { currentState ->
                        val updatedGoal = currentState.goal?.let { goal ->
                            val newTasks = goal.tasks.filter { it.id != taskId }
                            goal.copy(tasks = newTasks)
                        }
                        currentState.copy(goal = updatedGoal)
                    }
                    checkGoalAchievement()
                }
                is Result.Error -> {
                    _events.send(GoalDetailsEvent.ShowError(result.error.toUiText()))
                }
                Result.Loading -> {}
            }
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
        val goal = _state.value.goal ?: return
        val goalId = goal.id
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true) }

            val result = updateGoalUseCase(
                goalId = goalId,
                title = action.title.takeIf { it != goal.title },
                description = action.description.takeIf { it != goal.description },
                status = action.status.takeIf { it != goal.status.name },
                targetDate = action.targetDate.takeIf { it != goal.targetDate },
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
