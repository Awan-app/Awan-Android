package com.awan.app.core.domain.home.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.UserProfileInfo
import java.time.LocalDate

import com.awan.app.core.domain.home.model.SessionStatus

import com.awan.app.core.model.SessionTaskDetail

interface HomeRepository {
    suspend fun getDaySchedule(date: LocalDate): Result<DaySchedule>
    suspend fun getUserProfile(): Result<UserProfileInfo>
    suspend fun getSessionDetail(sessionId: String): Result<SessionTaskDetail>
    suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        locked: Boolean? = null,
        startIso: String? = null,
        endIso: String? = null,
    ): Result<Unit>
    suspend fun updateSessionLock(
        sessionId: String,
        locked: Boolean,
    ): Result<Unit>
}
