package com.awan.app.core.model

data class CalendarUser(
    val id: String,
    val streak: Int,
    val timezone: String,
)

data class CalendarGoal(
    val id: String,
    val title: String,
    val targetDate: String?,
    val status: String,
    val isInbox: Boolean,
    val createdAt: String? = null,
)
