package com.awan.app.core.data.zones.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.data.zones.remote.SessionRemoteDataSource
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.UpdateSessionParams
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.session.UpdateSessionRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

private class FakeSessionDao : SessionDao {
    val upserted = mutableListOf<SessionEntity>()
    val deletedIds = mutableListOf<String>()
    val replacedDates = mutableListOf<List<String>>()

    override suspend fun upsertSession(session: SessionEntity) { upserted += session }
    override suspend fun upsertSessions(sessions: List<SessionEntity>) { upserted += sessions }
    override fun observeSessionsForDate(date: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override suspend fun getSessionsForDate(date: String): List<SessionEntity> = emptyList()
    override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<SessionEntity> = emptyList()
    override suspend fun getSession(id: String): SessionEntity? = null
    override suspend fun deleteSessionsForDates(dates: List<String>) {}
    override suspend fun deleteSession(id: String) { deletedIds += id }
    override suspend fun replaceSessionsForDates(dates: List<String>, sessions: List<SessionEntity>) {
        replacedDates += dates
        upserted += sessions
    }
}

private class FakeRemoteDataSource : SessionRemoteDataSource {
    var lastUpdate: Pair<String, UpdateSessionRequest>? = null
    
    override suspend fun getSessionsByDate(date: String): Result<List<SessionDto>> = Result.Success(emptyList())
    override suspend fun getSessionsByRange(startDate: String, endDate: String): Result<Map<String, List<SessionDto>>> = Result.Success(emptyMap())
    override suspend fun getSession(sessionId: String): Result<SessionDto> = Result.Error(com.awan.app.core.common.error.AppError.NotFound)
    override suspend fun updateSession(sessionId: String, request: UpdateSessionRequest): Result<SessionDto> {
        lastUpdate = sessionId to request
        return Result.Success(SessionDto(
            id = sessionId,
            start = request.start ?: "2026-08-08T10:00:00",
            end = request.end ?: "2026-08-08T11:00:00",
            status = request.status
        ))
    }
    override suspend fun updateSessionStatus(sessionId: String, status: String): Result<SessionDto> = TODO()
    override suspend fun lockSession(sessionId: String): Result<SessionDto> = TODO()
    override suspend fun unlockSession(sessionId: String): Result<SessionDto> = TODO()
    override suspend fun deleteSession(sessionId: String): Result<Unit> = Result.Success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeDao = FakeSessionDao()
    private val fakeRemote = FakeRemoteDataSource()
    private val repository = SessionRepositoryImpl(fakeRemote, fakeDao)

    @Test
    fun `updateSession calls remote and updates Room`() = runTest(testDispatcher) {
        val start = LocalDateTime.of(2026, 8, 8, 10, 0)
        val params = UpdateSessionParams(start = start, status = SessionStatus.COMPLETED)
        
        repository.updateSession("s1", params)
        
        assertEquals("s1", fakeRemote.lastUpdate?.first)
        assertEquals("2026-08-08T10:00:00", fakeRemote.lastUpdate?.second?.start)
        assertEquals("COMPLETED", fakeRemote.lastUpdate?.second?.status)
        
        assertEquals(1, fakeDao.upserted.size)
        assertEquals("s1", fakeDao.upserted.first().id)
        assertEquals("COMPLETED", fakeDao.upserted.first().status)
    }

    @Test
    fun `deleteSession calls remote and removes from Room`() = runTest(testDispatcher) {
        repository.deleteSession("s1")
        assertTrue(fakeDao.deletedIds.contains("s1"))
    }
}
