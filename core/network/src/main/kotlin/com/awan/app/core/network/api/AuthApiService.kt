package com.awan.app.core.network.api

import com.awan.app.core.network.dto.AuthTokensDto
import com.awan.app.core.network.dto.LoginRequest
import com.awan.app.core.network.dto.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST
interface AuthApiService {
    //TODO: add the endpoint
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): AuthTokensDto
}

