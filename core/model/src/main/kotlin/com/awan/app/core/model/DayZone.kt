package com.awan.app.core.model

import java.time.LocalTime

data class DayZone(
    val id: String,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val colorHex: String? = null,
    val category: Category? = null,
)
