package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TemplateApiService {

    @POST("v1/templates")
    suspend fun createTemplate(
        @Body request: CreateTemplateRequest,
    ): TemplateResponse
}
