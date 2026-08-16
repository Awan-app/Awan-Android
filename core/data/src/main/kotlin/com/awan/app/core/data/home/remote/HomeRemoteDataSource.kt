package com.awan.app.core.data.home.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto as TemplateDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto as TemplateOverrideResponseDto
import com.awan.app.core.network.dto.zone.ZoneDto
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.network.dto.session.CompleteSessionResponse
import com.awan.app.core.network.dto.session.SessionDto

import com.awan.app.core.network.dto.task.TaskInfoResponse

interface HomeRemoteDataSource {
    suspend fun getZonesByDate(date: String): Result<List<ZoneDto>>
    suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>>

    suspend fun getTemplates(): Result<List<TemplateDto>>

    suspend fun getTemplateOverrides(): Result<List<TemplateOverrideResponseDto>>

    suspend fun getUserProfile(): Result<CompleteOnboardingResponse>

    suspend fun completeSession(sessionId: String): Result<CompleteSessionResponse>

    suspend fun uncompleteSession(sessionId: String): Result<SessionDto>

    suspend fun cancelSession(sessionId: String): Result<SessionDto>

    suspend fun moveSession(sessionId: String, startIso: String, endIso: String): Result<SessionDto>

    suspend fun getSession(sessionId: String): Result<SessionDto>

    suspend fun getTask(taskId: String): Result<TaskInfoResponse>

    suspend fun lockSession(sessionId: String): Result<SessionDto>

    suspend fun unlockSession(sessionId: String): Result<SessionDto>

    suspend fun updateTask(
        taskId: String,
        request: com.awan.app.core.network.dto.task.TaskUpdateRequest,
    ): Result<TaskInfoResponse>

    suspend fun deleteSession(sessionId: String): Result<Unit>

    suspend fun deleteTask(taskId: String, cascade: Boolean = true): Result<Unit>
}
