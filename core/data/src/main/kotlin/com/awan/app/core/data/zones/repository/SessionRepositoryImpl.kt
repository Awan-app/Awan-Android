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

        return when (val result = sessionRemoteDataSource.getSessionsByDate(dateStr)) {
            is Result.Success -> {
                sessionDao.replaceSessionsForDates(
                    listOf(dateStr),
                    result.data.map { it.toEntity() }
                )

                Result.Success(result.data.map { it.toDomain() })
            }

            is Result.Error -> {
                val cached = sessionDao.getSessionsForDate(dateStr)

                if (cached.isNotEmpty()) {
                    Result.Success(cached.map { it.toDomain() })
                } else {
                    result
                }
            }

            Result.Loading -> Result.Loading
        }
    }

    override suspend fun getSessionsByRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Map<LocalDate, List<Session>>> {
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return when (val result = sessionRemoteDataSource.getSessionsByRange(startStr, endStr)) {
            is Result.Success -> {
                val allEntities = result.data.values
                    .flatten()
                    .map { it.toEntity() }

                val dates = generateSequence(startDate) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(endDate) }
                    .map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
                    .toList()

                sessionDao.replaceSessionsForDates(
                    dates,
                    allEntities
                )

                Result.Success(
                    result.data
                        .mapKeys { LocalDate.parse(it.key) }
                        .mapValues { entry ->
                            entry.value.map { it.toDomain() }
                        }
                )
            }

            is Result.Error -> {
                val cached = sessionDao.getSessionsForDateRange(
                    startStr,
                    endStr
                )

                if (cached.isNotEmpty()) {
                    val grouped = cached
                        .groupBy { LocalDate.parse(it.date) }
                        .mapValues { entry ->
                            entry.value.map { it.toDomain() }
                        }

                    Result.Success(grouped)
                } else {
                    result
                }
            }

            Result.Loading -> Result.Loading
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
                locked = params.locked
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
