package com.awan.app.core.data.home.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto as TemplateDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto as TemplateOverrideResponseDto
import com.awan.app.core.network.dto.zone.ZoneDto
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.network.dto.session.SessionDto

interface HomeRemoteDataSource {
    suspend fun getZonesByDate(date: String): Result<List<ZoneDto>>
    suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>>

    suspend fun getTemplates(): Result<List<TemplateDto>>

    suspend fun getTemplateOverrides(): Result<List<TemplateOverrideResponseDto>>

    suspend fun getUserProfile(): Result<CompleteOnboardingResponse>

    suspend fun updateSession(
        sessionId: String,
        status: String? = null,
        startIso: String? = null,
        endIso: String? = null,
    ): Result<SessionDto>
}
