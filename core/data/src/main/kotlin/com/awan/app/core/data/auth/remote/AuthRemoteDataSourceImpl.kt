package com.awan.app.core.data.auth.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.AuthApiService
import com.awan.app.core.network.dto.auth.AuthTokensDto
import com.awan.app.core.network.dto.auth.FirebaseAuthRequest
import com.awan.app.core.network.dto.auth.LogoutRequest
import com.awan.app.core.network.dto.auth.RefreshTokenRequest
import com.awan.app.core.network.dto.auth.RequestOtpRequest
import com.awan.app.core.network.dto.auth.VerifyOtpRequest
import com.awan.app.core.network.dto.auth.VerifyOtpResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthRemoteDataSource {

    override suspend fun requestOtp(request: RequestOtpRequest): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            authApiService.requestOtp(request)
        }

    override suspend fun verifyOtp(request: VerifyOtpRequest): Result<VerifyOtpResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            authApiService.verifyOtp(request)
        }

    override suspend fun firebaseAuth(request: FirebaseAuthRequest): Result<VerifyOtpResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            authApiService.firebaseAuth(request)
        }

    override suspend fun refreshToken(request: RefreshTokenRequest): Result<AuthTokensDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            authApiService.refreshToken(request)
        }

    override suspend fun logout(bearerToken: String, request: LogoutRequest): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            authApiService.logout(bearerToken, request)
        }
}
