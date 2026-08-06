package com.awan.app.core.data.auth.repository

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.auth.remote.AuthRemoteDataSource
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.model.User
import com.awan.app.core.domain.auth.repository.AuthRepository
import com.awan.app.core.network.device.DeviceIdProvider
import com.awan.app.core.network.dto.auth.LogoutRequest
import com.awan.app.core.network.dto.auth.RefreshTokenRequest
import com.awan.app.core.network.dto.auth.RequestOtpRequest
import com.awan.app.core.network.dto.auth.VerifyOtpRequest
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
                            accessToken = result.data.accessToken,
                            refreshToken = result.data.refreshToken,
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

    override fun observeSessionExpired(): Flow<Unit> =
        authTokenProvider.sessionExpired

    override suspend fun getLastUsedEmail(): String? = authTokenProvider.getUserEmail()

    // The email deliberately outlives clearTokens(), so it must not count towards "is there a
    // session" — otherwise a logged-out user reads back as signed in. Use getLastUsedEmail() to
    // reach the surviving email.
    override suspend fun getUser(): User? {
        val userId = authTokenProvider.getUserId()
        val accessToken = authTokenProvider.getAccessToken()
        val refreshToken = authTokenProvider.getRefreshToken()

        if (userId == null && accessToken == null && refreshToken == null) {
            return null
        }

        return User(
            id = userId,
            email = authTokenProvider.getUserEmail(),
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    override suspend fun refreshUserData(): Result<User> {
        val refreshToken = authTokenProvider.getRefreshToken()
            ?: return Result.Error(AppError.Unauthorized)

        val result = remoteDataSource.refreshToken(
            RefreshTokenRequest(
                refreshToken = refreshToken,
                deviceId = deviceIdProvider.getDeviceId(),
            )
        )

        return when (result) {
            is Result.Success -> {
                authTokenProvider.saveTokens(
                    accessToken = result.data.accessToken,
                    refreshToken = result.data.refreshToken,
                )
                val user = getUser() ?: User(
                    id = authTokenProvider.getUserId(),
                    email = authTokenProvider.getUserEmail(),
                    accessToken = result.data.accessToken,
                    refreshToken = result.data.refreshToken,
                )
                Result.Success(user)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }
}
