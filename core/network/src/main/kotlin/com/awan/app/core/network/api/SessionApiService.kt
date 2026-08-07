package com.awan.app.core.network.api

import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.session.UpdateSessionRequest
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

import retrofit2.http.GET
import retrofit2.http.PATCH

interface SessionApiService {

    @GET("v1/sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String,
    ): SessionDto

    @PUT("v1/sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: String,
        @Body request: UpdateSessionRequest,
    ): SessionDto

    @PATCH("v1/sessions/{sessionId}/lock")
    suspend fun lockSession(
        @Path("sessionId") sessionId: String,
    ): SessionDto

    @PATCH("v1/sessions/{sessionId}/unlock")
    suspend fun unlockSession(
        @Path("sessionId") sessionId: String,
    ): SessionDto
}
