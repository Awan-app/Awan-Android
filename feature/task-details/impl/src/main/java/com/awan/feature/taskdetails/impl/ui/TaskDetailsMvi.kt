@file:Suppress("NewApi")

package com.awan.feature.taskdetails.impl.ui

import com.awan.app.core.common.text.UiText
import com.awan.app.core.model.Goal
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskSession
import com.awan.app.core.model.TaskStatus
import java.time.LocalDate

/** All observable state for the Task Details screen. */
data class TaskDetailsUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val dependencies: List<Task> = emptyList(),
    val dependents: List<Task> = emptyList(),
    val sessions: List<TaskSession> = emptyList(),
    val goals: List<Goal> = emptyList(),

    // Editable fields (mirrored from task on load; user edits these)
    val editTitle: String = "",
    val editDescription: String = "",
    val editDuration: Int = 30,
    val editPoints: Int = 0,
    val editMandatory: Boolean = false,
    val editAllowSplitting: Boolean = false,
    val editStatus: TaskStatus = TaskStatus.SCHEDULED,

    // UI controls
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeletingSession: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showDeleteCascadeOption: Boolean = false,
    val sessionToDelete: TaskSession? = null,
    val showMoveGoalPicker: Boolean = false,
    val showAddDependencyPicker: Boolean = false,
    val showAddSessionSheet: Boolean = false,
    val errorMessage: UiText? = null,
    val successMessage: UiText? = null,
    /** All tasks in the same goal — loaded when the dependency picker is opened. */
    val goalTasks: List<Task> = emptyList(),
) {
    /** Duration computed dynamically from the sum of all scheduled sessions, or fallback to task baseline. */
    val calculatedDurationMinutes: Int
        get() = if (sessions.isNotEmpty()) {
            sessions.sumOf { session ->
                val mins = java.time.Duration.between(session.start, session.end).toMinutes().toInt()
                mins.coerceAtLeast(0)
            }
        } else {
            task?.estimatedDurationMinutes ?: editDuration
        }

    val hasUnsavedChanges: Boolean
        get() = task != null && (
            editTitle.trim() != task.title.trim() ||
            editDescription.trim() != (task.description ?: "").trim() ||
            editMandatory != task.mandatory ||
            editAllowSplitting != task.allowTaskSplitting ||
            editStatus != task.status
        )

    /** Tasks in the same goal that are not already dependencies or this task itself. */
    fun availableDependencyTasks(allGoalTasks: List<Task>): List<Task> {
        val depIds = dependencies.map { it.id }.toSet()
        return allGoalTasks.filter { it.id != task?.id && it.id !in depIds }
    }
}

/** User-driven intents for the Task Details screen. */
sealed interface TaskDetailsAction {
    data class TitleChanged(val value: String) : TaskDetailsAction
    data class DescriptionChanged(val value: String) : TaskDetailsAction
    data class DurationChanged(val minutes: Int) : TaskDetailsAction
    data class PointsChanged(val points: Int) : TaskDetailsAction
    data class MandatoryToggled(val value: Boolean) : TaskDetailsAction
    data class AllowSplittingToggled(val value: Boolean) : TaskDetailsAction
    data class StatusChanged(val status: TaskStatus) : TaskDetailsAction
    data object SaveChanges : TaskDetailsAction
    data object RequestDelete : TaskDetailsAction
    data class ConfirmDelete(val cascade: Boolean) : TaskDetailsAction
    data object CancelDelete : TaskDetailsAction
    data class MoveToGoal(val goalId: String) : TaskDetailsAction
    data object ShowMoveGoalPicker : TaskDetailsAction
    data object DismissMoveGoalPicker : TaskDetailsAction
    data class AddDependency(val dependsOnTaskId: String) : TaskDetailsAction
    data class RemoveDependency(val dependsOnTaskId: String) : TaskDetailsAction
    data object ShowAddDependencyPicker : TaskDetailsAction
    data object DismissAddDependencyPicker : TaskDetailsAction
    data class AddSessions(val sessions: List<SessionDraft>) : TaskDetailsAction
    data class RequestDeleteSession(val session: TaskSession) : TaskDetailsAction
    data object ConfirmDeleteSession : TaskDetailsAction
    data object CancelDeleteSession : TaskDetailsAction
    data object ShowAddSessionSheet : TaskDetailsAction
    data object DismissAddSessionSheet : TaskDetailsAction
    data object DismissError : TaskDetailsAction
    data object Retry : TaskDetailsAction
}
