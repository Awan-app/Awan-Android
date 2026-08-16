@file:Suppress("NewApi")

package com.awan.feature.taskdetails.impl.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import com.awan.app.core.domain.home.usecase.DeleteSessionUseCase
import com.awan.app.core.domain.task.usecase.AddTaskDependencyUseCase
import com.awan.app.core.domain.task.usecase.AddTaskSessionsUseCase
import com.awan.app.core.domain.task.usecase.DeleteTaskUseCase
import com.awan.app.core.domain.task.usecase.GetTaskDependenciesUseCase
import com.awan.app.core.domain.task.usecase.GetTaskDependentsUseCase
import com.awan.app.core.domain.task.usecase.GetTasksByGoalUseCase
import com.awan.app.core.domain.task.usecase.GetTaskSessionsUseCase
import com.awan.app.core.domain.task.usecase.GetTaskUseCase
import com.awan.app.core.domain.task.usecase.MoveTaskUseCase
import com.awan.app.core.domain.task.usecase.RemoveTaskDependencyUseCase
import com.awan.app.core.domain.task.usecase.UpdateTaskUseCase
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskStatus
import com.awan.feature.taskdetails.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CONFLICT_ERROR_CODE = "TASK_HAS_DEPENDENTS"

@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    private val getTaskUseCase: GetTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val moveTaskUseCase: MoveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addDependencyUseCase: AddTaskDependencyUseCase,
    private val removeDependencyUseCase: RemoveTaskDependencyUseCase,
    private val getDependenciesUseCase: GetTaskDependenciesUseCase,
    private val getDependentsUseCase: GetTaskDependentsUseCase,
    private val getTaskSessionsUseCase: GetTaskSessionsUseCase,
    private val addTaskSessionsUseCase: AddTaskSessionsUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val getTasksByGoalUseCase: GetTasksByGoalUseCase,
) : ViewModel() {

    private var taskId: String = ""

    private val _uiState = MutableStateFlow(TaskDetailsUiState())
    val uiState: StateFlow<TaskDetailsUiState> = _uiState.asStateFlow()

    fun initTaskId(id: String) {
        if (id.isNotBlank() && taskId != id) {
            taskId = id
            loadAll()
        }
    }

    // ── Public event handler ──────────────────────────────────────────────────

    fun onAction(action: TaskDetailsAction) {
        when (action) {
            is TaskDetailsAction.TitleChanged          -> _uiState.update { it.copy(editTitle = action.value) }
            is TaskDetailsAction.DescriptionChanged    -> _uiState.update { it.copy(editDescription = action.value) }
            is TaskDetailsAction.DurationChanged       -> _uiState.update { it.copy(editDuration = action.minutes) }
            is TaskDetailsAction.PointsChanged         -> _uiState.update { it.copy(editPoints = action.points) }
            is TaskDetailsAction.MandatoryToggled      -> _uiState.update { it.copy(editMandatory = action.value) }
            is TaskDetailsAction.AllowSplittingToggled -> _uiState.update { it.copy(editAllowSplitting = action.value) }
            is TaskDetailsAction.StatusChanged         -> _uiState.update { it.copy(editStatus = action.status) }
            is TaskDetailsAction.SaveChanges           -> saveChanges()
            is TaskDetailsAction.RequestDelete         -> _uiState.update { it.copy(showDeleteConfirm = true) }
            is TaskDetailsAction.ConfirmDelete         -> deleteTask(action.cascade)
            is TaskDetailsAction.CancelDelete          -> _uiState.update { it.copy(showDeleteConfirm = false, showDeleteCascadeOption = false) }
            is TaskDetailsAction.MoveToGoal            -> moveToGoal(action.goalId)
            is TaskDetailsAction.ShowMoveGoalPicker    -> loadGoalsAndShowPicker()
            is TaskDetailsAction.DismissMoveGoalPicker -> _uiState.update { it.copy(showMoveGoalPicker = false) }
            is TaskDetailsAction.AddDependency         -> addDependency(action.dependsOnTaskId)
            is TaskDetailsAction.RemoveDependency      -> removeDependency(action.dependsOnTaskId)
            is TaskDetailsAction.ShowAddDependencyPicker -> showAddDependencyPicker()
            is TaskDetailsAction.DismissAddDependencyPicker -> _uiState.update { it.copy(showAddDependencyPicker = false) }
            is TaskDetailsAction.AddSessions           -> addSessions(action.sessions)
            is TaskDetailsAction.RequestDeleteSession  -> _uiState.update { it.copy(sessionToDelete = action.session) }
            is TaskDetailsAction.ConfirmDeleteSession  -> confirmDeleteSession()
            is TaskDetailsAction.CancelDeleteSession   -> _uiState.update { it.copy(sessionToDelete = null, isDeletingSession = false) }
            is TaskDetailsAction.ShowAddSessionSheet   -> _uiState.update { it.copy(showAddSessionSheet = true) }
            is TaskDetailsAction.DismissAddSessionSheet-> _uiState.update { it.copy(showAddSessionSheet = false) }
            is TaskDetailsAction.DismissError          -> _uiState.update { it.copy(errorMessage = null) }
            is TaskDetailsAction.Retry                 -> loadAll()
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun loadAll() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val taskDeferred       = async { getTaskUseCase(taskId) }
            val depsDeferred       = async { getDependenciesUseCase(taskId) }
            val depentsDeferred    = async { getDependentsUseCase(taskId) }
            val sessionsDeferred   = async { getTaskSessionsUseCase(taskId) }
            val goalsDeferred      = async { getGoalsUseCase() }

            val taskResult     = taskDeferred.await()
            val depsResult     = depsDeferred.await()
            val depentsResult  = depentsDeferred.await()
            val sessionsResult = sessionsDeferred.await()
            val goalsResult    = goalsDeferred.await()

            if (taskResult is Result.Success) {
                val task = taskResult.data
                val goalTasksResult = task.goalId?.let { getTasksByGoalUseCase(it) }
                val goalTasks = (goalTasksResult as? Result.Success)?.data ?: emptyList()

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        task = task,
                        editTitle = task.title,
                        editDescription = task.description ?: "",
                        editDuration = task.estimatedDurationMinutes,
                        editPoints = task.estimatedPoints,
                        editMandatory = task.mandatory,
                        editAllowSplitting = task.allowTaskSplitting,
                        editStatus = task.status,
                        dependencies = (depsResult as? Result.Success)?.data ?: state.dependencies,
                        dependents = (depentsResult as? Result.Success)?.data ?: state.dependents,
                        sessions = (sessionsResult as? Result.Success)?.data ?: state.sessions,
                        goals = (goalsResult as? Result.Success)?.data ?: state.goals,
                        goalTasks = goalTasks,
                        errorMessage = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = (taskResult as? Result.Error)?.error?.toUiText()
                            ?: UiText.StringResource(R.string.task_details_error_load_failed),
                    )
                }
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private fun saveChanges() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = updateTaskUseCase(
                taskId = taskId,
                title = state.editTitle.trim().takeIf { it.isNotBlank() },
                description = state.editDescription.trim().ifBlank { null },
                estimatedDuration = state.calculatedDurationMinutes,
                mandatory = state.editMandatory,
                estimatedPoints = state.task?.estimatedPoints ?: state.editPoints,
                allowTaskSplitting = state.editAllowSplitting,
                status = state.editStatus.name,
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { s ->
                        s.copy(
                            isSaving = false,
                            task = result.data,
                            editTitle = result.data.title,
                            editDescription = result.data.description ?: "",
                            editMandatory = result.data.mandatory,
                            editAllowSplitting = result.data.allowTaskSplitting,
                            editStatus = result.data.status,
                            editDuration = result.data.estimatedDurationMinutes,
                            editPoints = result.data.estimatedPoints,
                            successMessage = UiText.StringResource(R.string.task_details_saved),
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { s ->
                        s.copy(isSaving = false, errorMessage = result.error.toUiText())
                    }
                }
                else -> _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private fun deleteTask(cascade: Boolean) {
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            val result = deleteTaskUseCase(taskId, cascade)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDeleting = false, showDeleteConfirm = false) }
                    // Navigation back is handled by the screen observing a dedicated event.
                    // We signal "task was deleted" via a flag the screen watches.
                    _taskDeletedEvent.value = true
                }
                is Result.Error -> {
                    val hasDependents = (result.error as? AppError.Api)
                        ?.errorCode == CONFLICT_ERROR_CODE
                    _uiState.update { s ->
                        s.copy(
                            isDeleting = false,
                            showDeleteCascadeOption = hasDependents,
                            errorMessage = if (hasDependents) null else result.error.toUiText(),
                        )
                    }
                }
                else -> _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    /** One-shot event: `true` when the task was deleted, screen should navigate back. */
    private val _taskDeletedEvent = MutableStateFlow(false)
    val taskDeletedEvent: StateFlow<Boolean> = _taskDeletedEvent.asStateFlow()

    fun onTaskDeletedEventConsumed() {
        _taskDeletedEvent.value = false
    }

    // ── Move ──────────────────────────────────────────────────────────────────

    private fun loadGoalsAndShowPicker() {
        viewModelScope.launch {
            val result = getGoalsUseCase()
            if (result is Result.Success) {
                _uiState.update { it.copy(goals = result.data, showMoveGoalPicker = true) }
            } else {
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.task_details_error_load_goals)) }
            }
        }
    }

    private fun moveToGoal(goalId: String) {
        _uiState.update { it.copy(showMoveGoalPicker = false, isSaving = true) }
        viewModelScope.launch {
            val result = moveTaskUseCase(taskId, goalId)
            when (result) {
                is Result.Success -> {
                    val updatedTask = result.data
                    val goalTasksResult = updatedTask.goalId?.let { getTasksByGoalUseCase(it) }
                    val newGoalTasks = (goalTasksResult as? Result.Success)?.data ?: emptyList()
                    _uiState.update { s ->
                        s.copy(
                            isSaving = false,
                            task = updatedTask,
                            // Dependencies are same-goal — moving clears them visually
                            dependencies = emptyList(),
                            goalTasks = newGoalTasks,
                            successMessage = UiText.StringResource(R.string.task_details_moved),
                        )
                    }
                }
                is Result.Error -> _uiState.update { s ->
                    s.copy(isSaving = false, errorMessage = result.error.toUiText())
                }
                else -> _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    // ── Dependencies ──────────────────────────────────────────────────────────

    private fun showAddDependencyPicker() {
        val currentGoalId = _uiState.value.task?.goalId
        if (currentGoalId != null) {
            viewModelScope.launch {
                val result = getTasksByGoalUseCase(currentGoalId)
                if (result is Result.Success) {
                    _uiState.update { it.copy(goalTasks = result.data, showAddDependencyPicker = true) }
                } else {
                    _uiState.update { it.copy(showAddDependencyPicker = true) }
                }
            }
        } else {
            _uiState.update { it.copy(showAddDependencyPicker = true) }
        }
    }

    private fun addDependency(dependsOnTaskId: String) {
        _uiState.update { it.copy(showAddDependencyPicker = false) }
        viewModelScope.launch {
            val result = addDependencyUseCase(taskId, dependsOnTaskId)
            if (result is Result.Success) {
                // Reload dependency list so the added task appears with its full title
                val depsResult = getDependenciesUseCase(taskId)
                if (depsResult is Result.Success) {
                    _uiState.update { it.copy(dependencies = depsResult.data) }
                }
            } else if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.error.toUiText()) }
            }
        }
    }

    private fun removeDependency(dependsOnTaskId: String) {
        // Optimistic update
        _uiState.update { s ->
            s.copy(dependencies = s.dependencies.filter { it.id != dependsOnTaskId })
        }
        viewModelScope.launch {
            val result = removeDependencyUseCase(taskId, dependsOnTaskId)
            if (result is Result.Error) {
                // Revert — reload the full list
                val depsResult = getDependenciesUseCase(taskId)
                if (depsResult is Result.Success) {
                    _uiState.update { it.copy(dependencies = depsResult.data, errorMessage = result.error.toUiText()) }
                }
            }
        }
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    private fun addSessions(sessions: List<com.awan.app.core.model.SessionDraft>) {
        _uiState.update { it.copy(showAddSessionSheet = false, isSaving = true) }
        viewModelScope.launch {
            val result = addTaskSessionsUseCase(taskId, sessions)
            when (result) {
                is Result.Success -> {
                    val updatedSessionsResult = getTaskSessionsUseCase(taskId)
                    val finalSessions = (updatedSessionsResult as? Result.Success)?.data
                        ?: (uiState.value.sessions + result.data)
                    _uiState.update { s ->
                        s.copy(
                            isSaving = false,
                            sessions = finalSessions,
                            successMessage = UiText.StringResource(R.string.task_details_sessions_added),
                        )
                    }
                }
                is Result.Error -> _uiState.update { s ->
                    s.copy(isSaving = false, errorMessage = result.error.toUiText())
                }
                else -> _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun confirmDeleteSession() {
        val session = _uiState.value.sessionToDelete ?: return
        _uiState.update { it.copy(isDeletingSession = true) }
        viewModelScope.launch {
            val result = deleteSessionUseCase(session.id)
            when (result) {
                is Result.Success -> {
                    _uiState.update { s ->
                        s.copy(
                            isDeletingSession = false,
                            sessionToDelete = null,
                            sessions = s.sessions.filter { it.id != session.id },
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { s ->
                        s.copy(
                            isDeletingSession = false,
                            sessionToDelete = null,
                            errorMessage = result.error.toUiText(),
                        )
                    }
                }
                else -> _uiState.update { it.copy(isDeletingSession = false, sessionToDelete = null) }
            }
        }
    }

    private fun AppError.toUiText(): UiText = when (this) {
        is AppError.Network       -> UiText.StringResource(R.string.task_details_error_network)
        is AppError.Timeout       -> UiText.StringResource(R.string.task_details_error_network)
        is AppError.Unauthorized  -> UiText.StringResource(R.string.task_details_error_unauthorized)
        is AppError.Api           -> body?.takeIf { it.isNotBlank() }?.let { UiText.DynamicString(it) }
                                         ?: UiText.StringResource(R.string.task_details_error_generic)
        else                      -> UiText.StringResource(R.string.task_details_error_generic)
    }
}
