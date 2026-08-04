package com.awan.feature.addtask.presentation

import java.time.LocalDate

sealed interface AddTaskAction {
    data class ModeChanged(val mode: AddTaskMode) : AddTaskAction
    data class InputChanged(val input: String) : AddTaskAction
    data class DescriptionChanged(val description: String) : AddTaskAction
    data object MandatoryToggled : AddTaskAction

    data class PickerOpened(val picker: AddTaskPicker) : AddTaskAction
    data object PickerDismissed : AddTaskAction

    /** Step one of scheduling: parks the day and advances to the clock. */
    data class DatePicked(val date: LocalDate) : AddTaskAction

    /** Minutes from midnight, on the day just picked — or the one the sentence already names. */
    data class TimePicked(val minutesFromMidnight: Int) : AddTaskAction
    data class DurationPicked(val minutes: Int) : AddTaskAction
    data class CategoryPicked(val categoryName: String) : AddTaskAction

    data object AiToggled : AddTaskAction

    /** Keeps Awan's task and asks the engine to place it. */
    data object ScheduleWithAi : AddTaskAction

    /** Reveals the when chip; confirming then replaces Awan's task with a scheduled one. */
    data object ScheduleManually : AddTaskAction

    data object Submit : AddTaskAction

    data class GoalOptionSelected(val option: String) : AddTaskAction
    data object AcceptGoalProposal : AddTaskAction

    /** A dismiss the user asked for. Becomes [Dismiss] only once nothing would be lost. */
    data object DismissRequested : AddTaskAction
    data object DiscardConfirmed : AddTaskAction
    data object DiscardCancelled : AddTaskAction
    data object Dismiss : AddTaskAction
    data class SetMicPermissionRequested(val requested: Boolean) : AddTaskAction
}
