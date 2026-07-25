package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.zones.model.DailyZone

sealed interface DayDetailsAction {
    data class LoadDayDetails(val date: String) : DayDetailsAction
    data class CustomizeDay(val zones: List<DailyZone>) : DayDetailsAction
    data class UpdateOverride(val overrideId: String, val zones: List<DailyZone>) : DayDetailsAction
    data class ResetToWeeklyRoutine(val overrideId: String) : DayDetailsAction
}