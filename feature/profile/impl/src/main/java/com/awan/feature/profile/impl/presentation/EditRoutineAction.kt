package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek

sealed interface EditRoutineAction {
    data class LoadTemplate(val templateId: String?) : EditRoutineAction
    data class NameChange(val name: String) : EditRoutineAction
    data class ToggleDay(val day: DayOfWeek) : EditRoutineAction
    data class AddZone(val zone: DailyZone) : EditRoutineAction
    data class UpdateZone(val oldZone: DailyZone, val newZone: DailyZone) : EditRoutineAction
    data class DeleteZone(val zone: DailyZone) : EditRoutineAction
    data object SaveRoutine : EditRoutineAction
}