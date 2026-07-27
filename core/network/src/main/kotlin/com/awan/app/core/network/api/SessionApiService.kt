package com.awan.app.core.network.api

import com.awan.app.core.network.dto.SessionDto
import com.awan.app.core.network.dto.UpdateSessionRequest
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface SessionApiService {

    @PUT("v1/sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: String,
        @Body request: UpdateSessionRequest,
    ): SessionDto
}
