package com.awan.app.core.data.zones.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.data.zones.mapper.toDomain
import com.awan.app.core.data.zones.mapper.toEntity
import com.awan.app.core.data.zones.remote.SessionRemoteDataSource
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.repository.SessionRepository
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.UpdateSessionParams
import com.awan.app.core.network.dto.session.UpdateSessionRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionRemoteDataSource: SessionRemoteDataSource,
    private val sessionDao: SessionDao,
) : SessionRepository {

    private val sessionDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override suspend fun getSessionsByDate(date: LocalDate): Result<List<Session>> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val result = sessionRemoteDataSource.getSessionsByDate(dateStr)
        if (result is Result.Success) {
            sessionDao.replaceSessionsForDates(listOf(dateStr), result.data.map { it.toEntity() })
        }
        return result.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSessionsByRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Map<LocalDate, List<Session>>> {
        val result = sessionRemoteDataSource.getSessionsByRange(
            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        if (result is Result.Success) {
            val allEntities = result.data.values.flatten().map { it.toEntity() }
            val dates = result.data.keys.toList()
            sessionDao.replaceSessionsForDates(dates, allEntities)
        }
        return result.map { map ->
            map.mapKeys { LocalDate.parse(it.key) }
                .mapValues { entry -> entry.value.map { it.toDomain() } }
        }
    }

    override suspend fun getSession(sessionId: String): Result<Session> =
        sessionRemoteDataSource.getSession(sessionId).map { it.toDomain() }

    override suspend fun updateSession(
        sessionId: String,
        params: UpdateSessionParams
    ): Result<Session> {
        val result = sessionRemoteDataSource.updateSession(
            sessionId,
            UpdateSessionRequest(
                start = params.start?.format(sessionDateTimeFormatter),
                end = params.end?.format(sessionDateTimeFormatter),
                status = params.status?.name
            )
        )
        if (result is Result.Success) {
            sessionDao.upsertSession(result.data.toEntity())
        }
        return result.map { it.toDomain() }
    }

    override suspend fun updateSessionStatus(sessionId: String, status: SessionStatus): Result<Session> {
        val result = sessionRemoteDataSource.updateSessionStatus(sessionId, status.name)
        if (result is Result.Success) {
            sessionDao.upsertSession(result.data.toEntity())
        }
        return result.map { it.toDomain() }
    }

    override suspend fun lockSession(sessionId: String): Result<Session> {
        val result = sessionRemoteDataSource.lockSession(sessionId)
        if (result is Result.Success) {
            sessionDao.upsertSession(result.data.toEntity())
        }
        return result.map { it.toDomain() }
    }

    override suspend fun unlockSession(sessionId: String): Result<Session> {
        val result = sessionRemoteDataSource.unlockSession(sessionId)
        if (result is Result.Success) {
            sessionDao.upsertSession(result.data.toEntity())
        }
        return result.map { it.toDomain() }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        val result = sessionRemoteDataSource.deleteSession(sessionId)
        if (result is Result.Success) {
            sessionDao.deleteSession(sessionId)
        }
        return result
    }
}
