package com.awan.app.core.domain.calendar.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.CalendarUser
import com.awan.app.core.model.CalendarGoal
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun observeCalendar(): Flow<CalendarSnapshot?>
    suspend fun refresh(): Result<Unit>
}

data class CalendarSnapshot(
    val user: CalendarUser,
    val goals: List<CalendarGoal>,
    val templates: List<WeeklyTemplate> = emptyList(),
    val overrides: List<TemplateOverride> = emptyList(),
)
