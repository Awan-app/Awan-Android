package com.awan.app.core.data.template.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.zone.CreateTemplateRequest
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto as TemplateResponse

interface TemplateRemoteDataSource {
    suspend fun createTemplate(request: CreateTemplateRequest): Result<TemplateResponse>
}
