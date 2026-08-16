package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.UpcomingSessionRow
import kotlinx.coroutines.flow.Flow

private const val UPCOMING_SESSIONS_QUERY = """
    SELECT s.id, s.taskId, t.title, s.date, s.startTime, s.endTime, s.status, s.zoneId
    FROM sessions s
    INNER JOIN tasks t ON t.id = s.taskId
    WHERE s.date >= :startDate AND s.date <= :endDate
    ORDER BY s.date ASC, s.startTime ASC
"""

@Dao
interface SessionDao {

    @Upsert
    suspend fun upsertSession(session: SessionEntity)

    @Upsert
    suspend fun upsertSessions(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY startTime ASC")
    fun observeSessionsForDate(date: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, startTime ASC")
    fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY startTime ASC")
    suspend fun getSessionsForDate(date: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionEntity?

    @Query(UPCOMING_SESSIONS_QUERY)
    fun observeUpcomingSessions(startDate: String, endDate: String): Flow<List<UpcomingSessionRow>>

    @Query(UPCOMING_SESSIONS_QUERY)
    suspend fun getUpcomingSessions(startDate: String, endDate: String): List<UpcomingSessionRow>

    @Query("DELETE FROM sessions WHERE date IN (:dates)")
    suspend fun deleteSessionsForDates(dates: List<String>)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    /**
     * Atomically replaces scheduled sessions for specified dates.
     */
    @Transaction
    suspend fun replaceSessionsForDates(dates: List<String>, sessions: List<SessionEntity>) {
        if (dates.isNotEmpty()) {
            deleteSessionsForDates(dates)
        }
        if (sessions.isNotEmpty()) {
            upsertSessions(sessions)
        }
    }
}
