package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleTask(
    val id: String,
    val title: String,
    val categoryId: String? = null,
    val defaultPoints: Int? = null,
)
