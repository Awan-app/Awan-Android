package com.awan.app.core.model

data class DailyZone(
    val id: String?,
    val name: String,
    val startTime: String,
    val endTime: String,
    val color: String,
    val templateId: String? = null,
    val templateOverrideId: String? = null
)

data class WeeklyTemplate(
    val id: String,
    val name: String,
    val daysOfWeek: List<DayOfWeek>,
    val zones: List<DailyZone>
)

data class TemplateOverride(
    val id: String,
    val dateOfDay: String,
    val zones: List<DailyZone>
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
