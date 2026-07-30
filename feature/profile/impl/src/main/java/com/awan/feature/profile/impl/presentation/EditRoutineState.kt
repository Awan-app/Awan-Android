package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek

data class EditRoutineState(
    val templateId: String? = null,
    val name: String = "",
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val assignedDays: Set<DayOfWeek> = emptySet(),
    val zones: List<DailyZone> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val validationError: UiText? = null
)