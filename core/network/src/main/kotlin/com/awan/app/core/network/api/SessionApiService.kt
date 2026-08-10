package com.awan.app.core.network.api

import com.awan.app.core.network.dto.session.CompleteSessionResponse
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.session.UpdateSessionRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SessionApiService {

    @GET("v1/sessions/date/{date}")
    suspend fun getSessionsByDate(
        @Path("date") date: String
    ): List<SessionDto>

    @GET("v1/sessions/range")
    suspend fun getSessionsByRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Map<String, List<SessionDto>>

    @GET("v1/sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String
    ): SessionDto

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

    @PATCH("v1/sessions/{sessionId}/lock")
    suspend fun lockSession(
        @Path("sessionId") sessionId: String
    ): SessionDto

    @PATCH("v1/sessions/{sessionId}/unlock")
    suspend fun unlockSession(
        @Path("sessionId") sessionId: String
    ): SessionDto

    @DELETE("v1/sessions/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: String,
    )
}
