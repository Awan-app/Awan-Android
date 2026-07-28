package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleCategory(
    val id: String,
    val name: String,
    val icon: String = "",
    val categoryStyle: TaskCategory,
)
