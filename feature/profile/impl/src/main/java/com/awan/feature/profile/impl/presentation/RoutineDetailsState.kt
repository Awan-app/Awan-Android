package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.WeeklyTemplate

data class RoutineDetailsState(
    val template: WeeklyTemplate? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isDeleting: Boolean = false
)
