package com.awan.app.core.data.home.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.api.TemplateApiService
import com.awan.app.core.network.api.ZonesApiService
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto as TemplateDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto as TemplateOverrideResponseDto
import com.awan.app.core.network.dto.zone.ZoneDto
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

import com.awan.app.core.network.api.UserApiService
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse

import com.awan.app.core.network.api.SessionApiService
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.session.UpdateSessionRequest

class HomeRemoteDataSourceImpl @Inject constructor(
    private val zonesApiService: ZonesApiService,
    private val taskApiService: TaskApiService,
    private val templateApiService: TemplateApiService,
    private val userApiService: UserApiService,
    private val sessionApiService: SessionApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : HomeRemoteDataSource {

    override suspend fun getZonesByDate(date: String): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getEffectiveZones(date)
        }

    override suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.getTasksByDate(date)
        }

    override suspend fun getTemplates(): Result<List<TemplateDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            templateApiService.getTemplates()
        }

    override suspend fun getTemplateOverrides(): Result<List<TemplateOverrideResponseDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            templateApiService.getTemplateOverrides()
        }

    override suspend fun getUserProfile(): Result<CompleteOnboardingResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            userApiService.getUserProfile()
        }

    override suspend fun updateSession(
        sessionId: String,
        status: String?,
        startIso: String?,
        endIso: String?,
    ): Result<SessionDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.updateSession(
                sessionId = sessionId,
                request = UpdateSessionRequest(
                    start = startIso,
                    end = endIso,
                    status = status,
                ),
            )
        }
}
