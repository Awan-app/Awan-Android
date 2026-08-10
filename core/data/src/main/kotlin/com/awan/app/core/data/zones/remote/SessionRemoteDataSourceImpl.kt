package com.awan.app.core.data.zones.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.SessionApiService
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.session.UpdateSessionRequest
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SessionRemoteDataSourceImpl @Inject constructor(
    private val sessionApiService: SessionApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : SessionRemoteDataSource {

    override suspend fun getSessionsByDate(date: String): Result<List<SessionDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.getSessionsByDate(date)
        }

    override suspend fun getSessionsByRange(
        startDate: String,
        endDate: String
    ): Result<Map<String, List<SessionDto>>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.getSessionsByRange(startDate, endDate)
        }

    override suspend fun getSession(sessionId: String): Result<SessionDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.getSession(sessionId)
        }

    override suspend fun updateSession(
        sessionId: String,
        request: UpdateSessionRequest
    ): Result<SessionDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.updateSession(sessionId, request)
        }

    override suspend fun lockSession(sessionId: String): Result<SessionDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.lockSession(sessionId)
        }

    override suspend fun unlockSession(sessionId: String): Result<SessionDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.unlockSession(sessionId)
        }

    override suspend fun deleteSession(sessionId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            sessionApiService.deleteSession(sessionId)
        }
}
