package com.awan.feature.auth.impl.data.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.network.device.DeviceIdProvider
import com.awan.app.core.network.dto.LogoutRequest
import com.awan.app.core.network.dto.RequestOtpRequest
import com.awan.app.core.network.dto.VerifyOtpRequest
import com.awan.feature.auth.impl.data.remote.AuthRemoteDataSource
import com.awan.feature.auth.impl.domain.model.AuthSession
import com.awan.feature.auth.impl.domain.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val authTokenProvider: AuthTokenProvider,
    private val deviceIdProvider: DeviceIdProvider,
) : AuthRepository {

    override suspend fun requestOtp(email: String): Result<Unit> =
        remoteDataSource.requestOtp(RequestOtpRequest(email = email))

    override suspend fun verifyOtp(email: String, code: String): Result<AuthSession> {
        val result = remoteDataSource.verifyOtp(
            VerifyOtpRequest(
                email = email,
                code = code,
                deviceId = deviceIdProvider.getDeviceId(),
            )
        )

        if (result is Result.Success) {
            authTokenProvider.saveTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken,
            )
            val userDto = result.data.user
            authTokenProvider.saveUserData(
                userId = userDto?.id,
                email = userDto?.email ?: email,
            )
            authTokenProvider.setLoggedIn(true)
        }

        @Suppress("UNCHECKED_CAST")
        return when (result) {
            is Result.Success -> Result.Success(
                AuthSession(
                    accessToken = result.data.accessToken,
                    refreshToken = result.data.refreshToken,
                    expiresIn = result.data.accessTokenExpiresIn,
                    user = result.data.user?.let { userDto ->
                        User(
                            id = userDto.id,
                            email = userDto.email,
                            isNew = userDto.isNew ?: false,
                        )
                    },
                )
            )
            is Result.Error -> result as Result<AuthSession>
            Result.Loading -> result as Result<AuthSession>
        }
    }

    override suspend fun logout(): Result<Unit> {
        val accessToken = authTokenProvider.getAccessToken()

        if (accessToken != null) {
            remoteDataSource.logout(
                bearerToken = "Bearer $accessToken",
                request = LogoutRequest(deviceId = deviceIdProvider.getDeviceId()),
            )
        }

        authTokenProvider.clearTokens()

        return Result.Success(Unit)
    }

    override fun observeIsLoggedIn(): Flow<Boolean> =
        authTokenProvider.observeIsLoggedIn()
}
