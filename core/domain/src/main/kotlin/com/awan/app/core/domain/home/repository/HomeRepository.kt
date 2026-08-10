package com.awan.app.core.domain.home.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.UserProfileInfo
import com.awan.app.core.model.SessionTaskDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getDaySchedule(date: LocalDate): Flow<Result<DaySchedule>>

    /**
     * Pulls one day's sessions into Room, replacing what is there for that date. [getDaySchedule]
     * renders the cached day immediately; this is what makes another device's edits show up, and
     * it is the only way a day outside the background sync's week gets fetched at all.
     */
    suspend fun refreshSchedule(date: LocalDate): Result<Unit>
    suspend fun getUserProfile(): Result<UserProfileInfo>
    suspend fun getSessionDetail(sessionId: String): Result<SessionTaskDetail>

    /**
     * Marks a session done. The returned reward is empty on a repeat completion — a session only
     * ever pays out the first time.
     */
    suspend fun completeSession(sessionId: String): Result<SessionReward>

    /** Undoes a completion. Awarded points are not clawed back, so there is nothing to return. */
    suspend fun uncompleteSession(sessionId: String): Result<Unit>

    suspend fun cancelSession(sessionId: String): Result<Unit>

    /**
     * Moves a session in time. The one path that still uses the generic update endpoint, and the
     * only one callers need for reordering or re-timing — status is never touched here.
     */
    suspend fun moveSession(sessionId: String, startIso: String, endIso: String): Result<Unit>

    suspend fun updateSessionLock(
        sessionId: String,
        locked: Boolean,
    ): Result<Unit>

    suspend fun updateTaskDetails(
        taskId: String,
        title: String? = null,
        description: String? = null,
        estimatedDuration: Int? = null,
        estimatedPoints: Int? = null,
        mandatory: Boolean? = null,
        allowTaskSplitting: Boolean? = null,
    ): Result<Unit>

    suspend fun deleteSession(sessionId: String): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
}
