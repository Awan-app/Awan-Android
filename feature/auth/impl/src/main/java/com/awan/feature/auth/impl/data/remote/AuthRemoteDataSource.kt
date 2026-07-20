package com.awan.feature.auth.impl.data.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.LogoutRequest
import com.awan.app.core.network.dto.RequestOtpRequest
import com.awan.app.core.network.dto.VerifyOtpRequest
import com.awan.app.core.network.dto.VerifyOtpResponse

interface AuthRemoteDataSource {
    suspend fun requestOtp(request: RequestOtpRequest): Result<Unit>
    suspend fun verifyOtp(request: VerifyOtpRequest): Result<VerifyOtpResponse>
    suspend fun logout(bearerToken: String, request: LogoutRequest): Result<Unit>
}
