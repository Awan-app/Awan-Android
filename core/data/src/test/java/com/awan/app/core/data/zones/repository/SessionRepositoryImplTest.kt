package com.awan.app.core.data.zones.repository

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.data.zones.remote.SessionRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
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
    var sessionsToReturn = emptyList<SessionEntity>()
    var sessionToReturn: SessionEntity? = null

    override suspend fun upsertSession(session: SessionEntity) { upserted += session }
    override suspend fun upsertSessions(sessions: List<SessionEntity>) { upserted += sessions }
    override fun observeSessionsForDate(date: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override suspend fun getSessionsForDate(date: String): List<SessionEntity> = sessionsToReturn
    override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<SessionEntity> = sessionsToReturn
    override suspend fun getSession(id: String): SessionEntity? = sessionToReturn
    override suspend fun deleteSessionsForDates(dates: List<String>) {}
    override suspend fun deleteSession(id: String) { deletedIds += id }
    override suspend fun replaceSessionsForDates(dates: List<String>, sessions: List<SessionEntity>) {
        replacedDates += dates
        upserted += sessions
    }
}

private class FakeRemoteDataSource : SessionRemoteDataSource {
    var lastUpdate: Pair<String, UpdateSessionRequest>? = null
    var resultToReturn: Result<*> = Result.Success(emptyList<SessionDto>())
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun getSessionsByDate(date: String): Result<List<SessionDto>> = resultToReturn as Result<List<SessionDto>>
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun getSessionsByRange(startDate: String, endDate: String): Result<Map<String, List<SessionDto>>> = resultToReturn as Result<Map<String, List<SessionDto>>>
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun getSession(sessionId: String): Result<SessionDto> = resultToReturn as Result<SessionDto>

    override suspend fun updateSession(sessionId: String, request: UpdateSessionRequest): Result<SessionDto> {
        lastUpdate = sessionId to request
        return Result.Success(SessionDto(
            id = sessionId,
            start = request.start ?: "2026-08-08T10:00:00",
            end = request.end ?: "2026-08-08T11:00:00",
            status = null,
            locked = request.locked ?: false
        ))
    }
    override suspend fun lockSession(sessionId: String): Result<SessionDto> = TODO()
    override suspend fun unlockSession(sessionId: String): Result<SessionDto> = TODO()
    override suspend fun deleteSession(sessionId: String): Result<Unit> = Result.Success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeDao = FakeSessionDao()
    private val fakeRemote = FakeRemoteDataSource()
    private val connectivityMonitor = object : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(true)
        override fun isCurrentlyOnline(): Boolean = true
    }
    private val repository = SessionRepositoryImpl(fakeRemote, fakeDao, connectivityMonitor)

    @Test
    fun `getSessionsByDate returns remote sessions and persists them on success`() = runTest(testDispatcher) {
        val date = LocalDate.of(2026, 8, 8)
        val remoteSessions = listOf(
            SessionDto("s1", "2026-08-08T10:00:00", "2026-08-08T11:00:00", "SCHEDULED")
        )
        fakeRemote.resultToReturn = Result.Success(remoteSessions)

        val result = repository.getSessionsByDate(date)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
        assertEquals("s1", result.data.first().id)
        
        assertEquals(1, fakeDao.upserted.size)
        assertEquals("s1", fakeDao.upserted.first().id)
        assertTrue(fakeDao.replacedDates.contains(listOf("2026-08-08")))
    }

    @Test
    fun `getSessionsByDate returns cached sessions on remote failure`() = runTest(testDispatcher) {
        val date = LocalDate.of(2026, 8, 8)
        fakeRemote.resultToReturn = Result.Error(AppError.Network)
        fakeDao.sessionsToReturn = listOf(
            SessionEntity("s1", "t1", null, "2026-08-08", "10:00:00", "11:00:00", "SCHEDULED", false)
        )

        val result = repository.getSessionsByDate(date)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
        assertEquals("s1", result.data.first().id)
    }

    @Test
    fun `getSessionsByDate returns error on remote failure and empty cache`() = runTest(testDispatcher) {
        val date = LocalDate.of(2026, 8, 8)
        fakeRemote.resultToReturn = Result.Error(AppError.Network)
        fakeDao.sessionsToReturn = emptyList()

        val result = repository.getSessionsByDate(date)

        assertTrue(result is Result.Error)
        assertEquals(AppError.Network, (result as Result.Error).error)
    }

    @Test
    fun `getSessionsByRange returns remote sessions and persists them on success`() = runTest(testDispatcher) {
        val start = LocalDate.of(2026, 8, 8)
        val end = LocalDate.of(2026, 8, 9)
        val remoteSessions = mapOf(
            "2026-08-08" to listOf(SessionDto("s1", "2026-08-08T10:00:00", "2026-08-08T11:00:00", "SCHEDULED")),
            "2026-08-09" to listOf(SessionDto("s2", "2026-08-09T10:00:00", "2026-08-09T11:00:00", "SCHEDULED"))
        )
        fakeRemote.resultToReturn = Result.Success(remoteSessions)

        val result = repository.getSessionsByRange(start, end)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.size)
        assertEquals(1, data[start]?.size)
        assertEquals(1, data[end]?.size)
        
        assertEquals(2, fakeDao.upserted.size)
        assertTrue(fakeDao.replacedDates.first().contains("2026-08-08"))
        assertTrue(fakeDao.replacedDates.first().contains("2026-08-09"))
    }

    @Test
    fun `getSessionsByRange returns cached sessions on remote failure`() = runTest(testDispatcher) {
        val start = LocalDate.of(2026, 8, 8)
        val end = LocalDate.of(2026, 8, 9)
        fakeRemote.resultToReturn = Result.Error(AppError.Network)
        fakeDao.sessionsToReturn = listOf(
            SessionEntity("s1", "t1", null, "2026-08-08", "10:00:00", "11:00:00", "SCHEDULED", false),
            SessionEntity("s2", "t2", null, "2026-08-09", "10:00:00", "11:00:00", "SCHEDULED", false)
        )

        val result = repository.getSessionsByRange(start, end)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.size)
        assertEquals("s1", data[start]?.first()?.id)
        assertEquals("s2", data[end]?.first()?.id)
    }

    @Test
    fun `getSession returns remote session and persists it on success`() = runTest(testDispatcher) {
        val sessionId = "s1"
        val remoteSession = SessionDto(sessionId, "2026-08-08T10:00:00", "2026-08-08T11:00:00", "SCHEDULED")
        fakeRemote.resultToReturn = Result.Success(remoteSession)

        val result = repository.getSession(sessionId)

        assertTrue(result is Result.Success)
        assertEquals(sessionId, (result as Result.Success).data.id)
        assertEquals(1, fakeDao.upserted.size)
        assertEquals(sessionId, fakeDao.upserted.first().id)
    }

    @Test
    fun `getSession returns cached session on remote failure`() = runTest(testDispatcher) {
        val sessionId = "s1"
        fakeRemote.resultToReturn = Result.Error(AppError.Network)
        val cached = SessionEntity(sessionId, "t1", "z1", "2026-08-08", "10:00:00", "11:00:00", "SCHEDULED", false)
        fakeDao.sessionToReturn = cached

        val result = repository.getSession(sessionId)

        assertTrue(result is Result.Success)
        assertEquals(sessionId, (result as Result.Success).data.id)
    }

    @Test
    fun `updateSession calls remote and updates Room`() = runTest(testDispatcher) {
        val start = LocalDateTime.of(2026, 8, 8, 10, 0)
        val params = UpdateSessionParams(start = start, locked = true)
        
        repository.updateSession("s1", params)
        
        assertEquals("s1", fakeRemote.lastUpdate?.first)
        assertEquals("2026-08-08T10:00:00", fakeRemote.lastUpdate?.second?.start)
        assertEquals(true, fakeRemote.lastUpdate?.second?.locked)
        
        assertEquals(1, fakeDao.upserted.size)
        assertEquals("s1", fakeDao.upserted.first().id)
        assertTrue(fakeDao.upserted.first().locked)
    }

    @Test
    fun `deleteSession calls remote and removes from Room`() = runTest(testDispatcher) {
        repository.deleteSession("s1")
        assertTrue(fakeDao.deletedIds.contains("s1"))
    }
}
