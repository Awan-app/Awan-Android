package com.awan.app.core.data.zones.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.session.UpdateSessionRequest

interface SessionRemoteDataSource {
    suspend fun getSessionsByDate(date: String): Result<List<SessionDto>>
    suspend fun getSessionsByRange(startDate: String, endDate: String): Result<Map<String, List<SessionDto>>>
    suspend fun getSession(sessionId: String): Result<SessionDto>
    suspend fun updateSession(sessionId: String, request: UpdateSessionRequest): Result<SessionDto>
    suspend fun lockSession(sessionId: String): Result<SessionDto>
    suspend fun unlockSession(sessionId: String): Result<SessionDto>
    suspend fun deleteSession(sessionId: String): Result<Unit>
}
