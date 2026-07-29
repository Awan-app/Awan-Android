package com.awan.feature.profile.impl.presentation

sealed interface RoutineDetailsAction {
    data class LoadTemplate(val templateId: String) : RoutineDetailsAction
    data class DeleteRoutine(val templateId: String) : RoutineDetailsAction
}
