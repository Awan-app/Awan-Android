package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek

sealed interface DailyZonesAction {
    data object LoadData : DailyZonesAction
    data class SelectDay(val day: DayOfWeek) : DailyZonesAction
    data class SelectTemplate(val templateId: String) : DailyZonesAction
    data class DeleteTemplate(val templateId: String) : DailyZonesAction
    data class ReorderZones(val fromIndex: Int, val toIndex: Int) : DailyZonesAction
    data class AddZone(val zone: DailyZone) : DailyZonesAction
    data class UpdateZone(val zone: DailyZone) : DailyZonesAction
    data class DeleteZone(val zone: DailyZone) : DailyZonesAction
    data object CustomizeDay : DailyZonesAction
    data object ResetDay : DailyZonesAction
    data object ClearError : DailyZonesAction
}