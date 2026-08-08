package com.awan.app.core.data.zones.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.zones.mapper.toDomain
import com.awan.app.core.data.zones.remote.SessionRemoteDataSource
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.repository.SessionRepository
import com.awan.app.core.network.dto.session.UpdateSessionRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionRemoteDataSource: SessionRemoteDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : SessionRepository {

    override suspend fun getSessionsByDate(date: LocalDate): Result<List<Session>> = withContext(ioDispatcher) {
        sessionRemoteDataSource.getSessionsByDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSessionsByRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Map<LocalDate, List<Session>>> = withContext(ioDispatcher) {
        sessionRemoteDataSource.getSessionsByRange(
            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        ).map { map ->
            map.mapKeys { LocalDate.parse(it.key) }
                .mapValues { entry -> entry.value.map { it.toDomain() } }
        }
    }

    override suspend fun getSession(sessionId: String): Result<Session> = withContext(ioDispatcher) {
        sessionRemoteDataSource.getSession(sessionId).map { it.toDomain() }
    }

    override suspend fun updateSession(
        sessionId: String,
        start: String?,
        end: String?,
        status: String?
    ): Result<Session> = withContext(ioDispatcher) {
        sessionRemoteDataSource.updateSession(
            sessionId,
            UpdateSessionRequest(start = start, end = end, status = status)
        ).map { it.toDomain() }
    }

    override suspend fun updateSessionStatus(sessionId: String, status: String): Result<Session> = withContext(ioDispatcher) {
        sessionRemoteDataSource.updateSessionStatus(sessionId, status).map { it.toDomain() }
    }

    override suspend fun lockSession(sessionId: String): Result<Session> = withContext(ioDispatcher) {
        sessionRemoteDataSource.lockSession(sessionId).map { it.toDomain() }
    }

    override suspend fun unlockSession(sessionId: String): Result<Session> = withContext(ioDispatcher) {
        sessionRemoteDataSource.unlockSession(sessionId).map { it.toDomain() }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(ioDispatcher) {
        sessionRemoteDataSource.deleteSession(sessionId)
    }
}
