package com.awan.app.core.data.template.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateResponse

interface TemplateRemoteDataSource {
    suspend fun createTemplate(request: CreateTemplateRequest): Result<TemplateResponse>
}
