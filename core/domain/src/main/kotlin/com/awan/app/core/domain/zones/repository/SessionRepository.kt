package com.awan.app.core.domain.zones.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.UpdateSessionParams
import java.time.LocalDate

interface SessionRepository {
    suspend fun getSessionsByDate(date: LocalDate): Result<List<Session>>
    suspend fun getSessionsByRange(startDate: LocalDate, endDate: LocalDate): Result<Map<LocalDate, List<Session>>>
    suspend fun getSession(sessionId: String): Result<Session>
    suspend fun updateSession(sessionId: String, params: UpdateSessionParams): Result<Session>
    suspend fun updateSessionStatus(sessionId: String, status: SessionStatus): Result<Session>
    suspend fun lockSession(sessionId: String): Result<Session>
    suspend fun unlockSession(sessionId: String): Result<Session>
    suspend fun deleteSession(sessionId: String): Result<Unit>
}
