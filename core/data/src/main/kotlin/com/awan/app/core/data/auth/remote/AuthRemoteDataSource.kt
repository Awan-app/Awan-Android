package com.awan.app.core.data.auth.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.auth.AuthTokensDto
import com.awan.app.core.network.dto.auth.LogoutRequest
import com.awan.app.core.network.dto.auth.RefreshTokenRequest
import com.awan.app.core.network.dto.auth.RequestOtpRequest
import com.awan.app.core.network.dto.auth.VerifyOtpRequest
import com.awan.app.core.network.dto.auth.VerifyOtpResponse

interface AuthRemoteDataSource {
    suspend fun requestOtp(request: RequestOtpRequest): Result<Unit>
    suspend fun verifyOtp(request: VerifyOtpRequest): Result<VerifyOtpResponse>
    suspend fun refreshToken(request: RefreshTokenRequest): Result<AuthTokensDto>
    suspend fun logout(bearerToken: String, request: LogoutRequest): Result<Unit>
}
