package com.awan.app.core.domain.home.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.UserProfileInfo
import java.time.LocalDate

import com.awan.app.core.domain.home.model.SessionStatus

interface HomeRepository {
    suspend fun getDaySchedule(date: LocalDate): Result<DaySchedule>
    suspend fun getUserProfile(): Result<UserProfileInfo>
    suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        startIso: String? = null,
        endIso: String? = null,
    ): Result<Unit>
}
