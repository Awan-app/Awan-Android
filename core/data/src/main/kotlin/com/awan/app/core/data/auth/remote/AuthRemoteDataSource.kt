package com.awan.app.core.data.auth.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.AuthTokensDto
import com.awan.app.core.network.dto.LogoutRequest
import com.awan.app.core.network.dto.RefreshTokenRequest
import com.awan.app.core.network.dto.RequestOtpRequest
import com.awan.app.core.network.dto.VerifyOtpRequest
import com.awan.app.core.network.dto.VerifyOtpResponse

interface AuthRemoteDataSource {
    suspend fun requestOtp(request: RequestOtpRequest): Result<Unit>
    suspend fun verifyOtp(request: VerifyOtpRequest): Result<VerifyOtpResponse>
    suspend fun refreshToken(request: RefreshTokenRequest): Result<AuthTokensDto>
    suspend fun logout(bearerToken: String, request: LogoutRequest): Result<Unit>
}
