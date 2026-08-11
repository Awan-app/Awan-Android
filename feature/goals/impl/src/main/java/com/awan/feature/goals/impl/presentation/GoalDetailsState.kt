package com.awan.feature.goals.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.model.Goal

data class GoalDetailsState(
    val isLoading: Boolean = true,
    val goal: Goal? = null,
    val error: UiText? = null,
    val isDeleting: Boolean = false,
    val isUpdating: Boolean = false,
    val showEditSheet: Boolean = false,
)
