package com.awan.app.core.domain.zones.model

import com.awan.app.core.model.Category

/** [categoryId] is required by the backend on every zone write; a zone without one cannot be saved. */
data class DailyZone(
    val id: String?,
    val name: String,
    val startTime: String,
    val endTime: String,
    val color: String,
    val templateId: String? = null,
    val templateOverrideId: String? = null,
    val categoryId: String? = null,
    val category: Category? = null
)

data class WeeklyTemplate(
    val id: String,
    val name: String,
    val daysOfWeek: List<DayOfWeek>,
    val zones: List<DailyZone>,
    val category: Category? = null
)

data class TemplateOverride(
    val id: String,
    val name: String? = null,
    val dateOfDay: String,
    val zones: List<DailyZone>,
    val category: Category? = null
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
