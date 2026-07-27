package com.awan.app.core.domain.home.model

data class DayZone(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val color: String?,
)
