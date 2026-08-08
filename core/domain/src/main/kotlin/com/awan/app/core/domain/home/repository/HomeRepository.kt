package com.awan.app.core.domain.home.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.SessionStatus
import com.awan.app.core.domain.home.model.UserProfileInfo
import com.awan.app.core.model.SessionTaskDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getDaySchedule(date: LocalDate): Flow<Result<DaySchedule>>
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
