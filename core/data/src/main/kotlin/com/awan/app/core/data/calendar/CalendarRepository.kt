package com.awan.app.core.data.calendar

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.CalendarUser
import com.awan.app.core.model.Goal
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun observeCalendar(): Flow<CalendarSnapshot?>
    suspend fun refresh(): Result<Unit>
}

data class CalendarSnapshot(
    val user: CalendarUser,
    val goals: List<Goal>,
)
