package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.model.Category

data class EditRoutineState(
    val templateId: String? = null,
    val overrideId: String? = null,
    val date: String? = null,
    val name: String = "",
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val assignedDays: Set<DayOfWeek> = emptySet(),
    val zones: List<DailyZone> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isTodayOnly: Boolean = false,
    val error: UiText? = null,
    val validationError: UiText? = null
)
