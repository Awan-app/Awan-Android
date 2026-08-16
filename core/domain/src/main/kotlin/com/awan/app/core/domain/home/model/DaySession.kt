package com.awan.app.core.domain.home.model

import com.awan.app.core.model.SessionStatus

data class DaySession(
    val id: String,
    val taskId: String,
    val taskTitle: String,
    val zoneId: String?,
    val startMinutes: Int,
    val durationMinutes: Int,
    val status: SessionStatus,
    val locked: Boolean,
    val points: Int?,
    val categoryId: String?,
    val categoryName: String?,
)
