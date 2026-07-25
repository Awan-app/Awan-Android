package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.DailyZone

data class DayDetailsState(
    val date: String = "",
    val effectiveZones: List<DailyZone> = emptyList(),
    val isOverride: Boolean = false,
    val overrideId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiText? = null
)