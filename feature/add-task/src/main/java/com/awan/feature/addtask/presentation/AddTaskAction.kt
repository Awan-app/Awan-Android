package com.awan.feature.addtask.presentation

sealed interface AddTaskAction {
    data class ModeChanged(val mode: AddTaskMode) : AddTaskAction
    data class InputChanged(val input: String) : AddTaskAction
    data class DescriptionChanged(val description: String) : AddTaskAction
    data object MandatoryToggled : AddTaskAction

    data class PickerOpened(val picker: AddTaskPicker) : AddTaskAction
    data object PickerDismissed : AddTaskAction

    /** Minutes from midnight, on the day the sentence already names (today if it names none). */
    data class TimePicked(val minutesFromMidnight: Int) : AddTaskAction
    data class DurationPicked(val minutes: Int) : AddTaskAction

    data object Submit : AddTaskAction
    data object Dismiss : AddTaskAction
}
