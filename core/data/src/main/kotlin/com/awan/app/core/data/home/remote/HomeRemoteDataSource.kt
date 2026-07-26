package com.awan.app.core.data.home.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.TaskWithSessionsDto
import com.awan.app.core.network.dto.TemplateDto
import com.awan.app.core.network.dto.TemplateOverrideResponseDto
import com.awan.app.core.network.dto.ZoneDto

interface HomeRemoteDataSource {
    suspend fun getZonesByDate(date: String): Result<List<ZoneDto>>
    suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>>

    suspend fun getTemplates(): Result<List<TemplateDto>>

    suspend fun getTemplateOverrides(): Result<List<TemplateOverrideResponseDto>>

    suspend fun getUserProfile(): Result<com.awan.app.core.network.dto.CompleteOnboardingResponse>

    suspend fun updateSession(
        sessionId: String,
        status: String? = null,
        startIso: String? = null,
        endIso: String? = null,
    ): Result<com.awan.app.core.network.dto.SessionDto>
}
