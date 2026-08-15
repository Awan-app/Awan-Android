package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek

sealed interface EditRoutineAction {
    data class LoadTemplate(val templateId: String?, val overrideId: String? = null, val date: String? = null) : EditRoutineAction
    data class NameChange(val name: String) : EditRoutineAction
    data class ToggleDay(val day: DayOfWeek) : EditRoutineAction
    data class AddZone(val zone: DailyZone) : EditRoutineAction
    data class UpdateZone(val oldZone: DailyZone, val newZone: DailyZone) : EditRoutineAction
    data class DeleteZone(val zone: DailyZone) : EditRoutineAction
    data class ReorderZones(val from: Int, val to: Int) : EditRoutineAction
    data object SaveRoutine : EditRoutineAction
    data object DeleteRoutine : EditRoutineAction
    data class CreateCategory(val name: String) : EditRoutineAction
    data class ToggleTodayOnly(val isTodayOnly: Boolean) : EditRoutineAction
    data class DateChange(val date: String) : EditRoutineAction
}
