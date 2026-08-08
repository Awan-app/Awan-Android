package com.awan.app.core.network.api

import com.awan.app.core.network.dto.session.CompleteSessionResponse
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.session.UpdateSessionRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SessionApiService {

    /**
     * Moves a session in time. Status is deliberately absent — editing a session only ever changes
     * its times, and the dedicated endpoints below are the only way it changes state.
     */
    @PUT("v1/sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: String,
        @Body request: UpdateSessionRequest,
    ): SessionDto

    /** The only endpoint that pays out: points and streak, and only on the first completion. */
    @POST("v1/sessions/{sessionId}/complete")
    suspend fun completeSession(@Path("sessionId") sessionId: String): CompleteSessionResponse

    /** Undoes a completion. Awarded points are not taken back, so nothing is returned to celebrate. */
    @POST("v1/sessions/{sessionId}/uncomplete")
    suspend fun uncompleteSession(@Path("sessionId") sessionId: String): SessionDto

    @POST("v1/sessions/{sessionId}/cancel")
    suspend fun cancelSession(@Path("sessionId") sessionId: String): SessionDto
}
