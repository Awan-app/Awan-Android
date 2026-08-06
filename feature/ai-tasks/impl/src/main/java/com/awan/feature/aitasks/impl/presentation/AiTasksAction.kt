package com.awan.feature.aitasks.impl.presentation

import java.time.LocalDate

sealed interface AiTasksAction {
    /** Fired once from the Root composable; the ViewModel ignores a second call. */
    data class Load(val text: String, val note: String?, val imageUri: String?) : AiTasksAction
    data object Retry : AiTasksAction

    data class Removed(val id: Int) : AiTasksAction

    /** Puts the last removed task back where it was. Only ever one removal deep. */
    data object UndoRemove : AiTasksAction

    /** Throws away every edit and removal, back to what Awan first proposed. */
    data object ResetPlan : AiTasksAction
    data class ToggleExpanded(val id: Int) : AiTasksAction
    data class TitleChanged(val id: Int, val title: String) : AiTasksAction
    data class DescriptionChanged(val id: Int, val description: String) : AiTasksAction
    data class DurationPicked(val id: Int, val minutes: Int) : AiTasksAction
    data class CategoryPicked(val id: Int, val categoryId: String?) : AiTasksAction
    data class MandatoryToggled(val id: Int) : AiTasksAction

    /** Opens the date step of the session picker for this task/session. */
    data class SessionTapped(val id: Int, val sessionIndex: Int) : AiTasksAction
    data class SessionRemoved(val id: Int, val sessionIndex: Int) : AiTasksAction

    /** Appends a new, user-owned session and immediately opens the picker to set its time. */
    data class SessionAdded(val id: Int) : AiTasksAction
    data class SessionDatePicked(val date: LocalDate) : AiTasksAction
    data class SessionTimePicked(val minutesFromMidnight: Int) : AiTasksAction
    data object SessionPickerDismissed : AiTasksAction

    data object Accept : AiTasksAction

    data object BackRequested : AiTasksAction
    data object DiscardConfirmed : AiTasksAction
    data object DiscardCancelled : AiTasksAction
}
