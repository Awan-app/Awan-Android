package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate

data class DailyZonesState(
    val templates: List<WeeklyTemplate> = emptyList(),
    val overrides: List<TemplateOverride> = emptyList(),
    val selectedDay: DayOfWeek = DayOfWeek.MONDAY,
    val selectedDayZones: List<DailyZone> = emptyList(),
    val currentTemplate: WeeklyTemplate? = null,
    val selectedTemplateId: String? = null,
    val currentOverride: TemplateOverride? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isSaving: Boolean = false
)