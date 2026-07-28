package com.awan.app.core.data.template.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TemplateApiService
import com.awan.app.core.network.dto.zone.CreateTemplateRequest
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto as TemplateResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class TemplateRemoteDataSourceImpl @Inject constructor(
    private val templateApiService: TemplateApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TemplateRemoteDataSource {

    override suspend fun createTemplate(request: CreateTemplateRequest): Result<TemplateResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            templateApiService.createTemplate(request)
        }
}
