package com.awan.app.core.domain.home.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.UserProfileInfo
import java.time.LocalDate

interface HomeRepository {
    suspend fun getDaySchedule(date: LocalDate): Result<DaySchedule>
    suspend fun getUserProfile(): Result<UserProfileInfo>

    /**
     * Marks a session done. The returned reward is empty on a repeat completion — a session only
     * ever pays out the first time.
     */
    suspend fun completeSession(sessionId: String): Result<SessionReward>

    /** Undoes a completion. Awarded points are not clawed back, so there is nothing to return. */
    suspend fun uncompleteSession(sessionId: String): Result<Unit>

    suspend fun cancelSession(sessionId: String): Result<Unit>

    /** Moves a session in time. The one path that still uses the generic update endpoint. */
    suspend fun moveSession(sessionId: String, startIso: String, endIso: String): Result<Unit>
}
