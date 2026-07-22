package com.awan.app.core.network.api

import com.awan.app.core.network.dto.AuthTokensDto
import com.awan.app.core.network.dto.LogoutRequest
import com.awan.app.core.network.dto.RefreshTokenRequest
import com.awan.app.core.network.dto.RequestOtpRequest
import com.awan.app.core.network.dto.VerifyOtpRequest
import com.awan.app.core.network.dto.VerifyOtpResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("v1/auth/otp/request")
    suspend fun requestOtp(@Body request: RequestOtpRequest)

    @POST("v1/auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse

    @POST("v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): AuthTokensDto

    @POST("v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String,
        @Body request: LogoutRequest,
    )
}
