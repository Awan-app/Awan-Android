package com.awan.feature.addtask.presentation

sealed interface AddTaskAction {
    data class ModeChanged(val mode: AddTaskMode) : AddTaskAction
    data class InputChanged(val input: String) : AddTaskAction
    data class DescriptionChanged(val description: String) : AddTaskAction
    data object MandatoryToggled : AddTaskAction
    data object Submit : AddTaskAction
    data object Dismiss : AddTaskAction
}
