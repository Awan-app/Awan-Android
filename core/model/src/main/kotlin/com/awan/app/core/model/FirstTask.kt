package com.awan.app.core.model

data class FirstTask(
    val id: String,
    val title: String,
    val zoneId: String,
    val startMinutes: Int,
    val durationMinutes: Int,
)
