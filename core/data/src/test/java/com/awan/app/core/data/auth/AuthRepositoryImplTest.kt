package com.awan.app.core.data.auth

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.auth.remote.AuthRemoteDataSource
import com.awan.app.core.data.auth.repository.AuthRepositoryImpl
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.network.device.DeviceIdProvider
import com.awan.app.core.network.dto.auth.AuthTokensDto
import com.awan.app.core.network.dto.auth.FirebaseAuthRequest
import com.awan.app.core.network.dto.auth.LogoutRequest
import com.awan.app.core.network.dto.auth.RefreshTokenRequest
import com.awan.app.core.network.dto.auth.RequestOtpRequest
import com.awan.app.core.network.dto.auth.VerifyOtpRequest
import com.awan.app.core.network.dto.auth.VerifyOtpResponse
import com.awan.app.core.network.dto.user.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: FakeAuthRemoteDataSource
    private lateinit var fakeAuthTokenProvider: FakeAuthTokenProvider
    private lateinit var fakeDeviceIdProvider: DeviceIdProvider
    private lateinit var fakeLocalDataCleaner: FakeLocalDataCleaner
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeAuthRemoteDataSource()
        fakeAuthTokenProvider = FakeAuthTokenProvider()
        fakeLocalDataCleaner = FakeLocalDataCleaner()
        fakeDeviceIdProvider = object : DeviceIdProvider {
            override fun getDeviceId(): String = "test-device-id-123"
        }
        repository = AuthRepositoryImpl(
            remoteDataSource = fakeRemoteDataSource,
            authTokenProvider = fakeAuthTokenProvider,
            deviceIdProvider = fakeDeviceIdProvider,
            localDataCleaner = fakeLocalDataCleaner,
        )
    }

    @Test
    fun `signInWithFirebase sends idToken and deviceId and saves session tokens`() = runTest {
        val response = VerifyOtpResponse(
            accessToken = "access-token-abc",
            refreshToken = "refresh-token-xyz",
            accessTokenExpiresIn = 3600L,
            user = UserDto(id = "user-1", email = "test@example.com", isNew = true),
        )
        fakeRemoteDataSource.firebaseResponse = Result.Success(response)

        val result = repository.signInWithFirebase("firebase-id-token-123")

        assertTrue(result is Result.Success)
        val session = (result as Result.Success).data
        assertEquals("access-token-abc", session.accessToken)
        assertEquals("refresh-token-xyz", session.refreshToken)
        assertEquals("user-1", session.user?.id)
        assertEquals("test@example.com", session.user?.email)
        assertTrue(session.user?.isNew == true)

        assertEquals("firebase-id-token-123", fakeRemoteDataSource.lastFirebaseAuthRequest?.idToken)
        assertEquals("test-device-id-123", fakeRemoteDataSource.lastFirebaseAuthRequest?.deviceId)
        assertEquals("access-token-abc", fakeAuthTokenProvider.savedAccessToken)
        assertEquals("refresh-token-xyz", fakeAuthTokenProvider.savedRefreshToken)
        assertEquals("user-1", fakeAuthTokenProvider.savedUserId)
        assertEquals("test@example.com", fakeAuthTokenProvider.savedEmail)
        assertTrue(fakeAuthTokenProvider.isLoggedInState)
    }

    private class FakeAuthRemoteDataSource : AuthRemoteDataSource {
        var lastFirebaseAuthRequest: FirebaseAuthRequest? = null
        var firebaseResponse: Result<VerifyOtpResponse> = Result.Success(
            VerifyOtpResponse(accessToken = "", refreshToken = "")
        )

        override suspend fun requestOtp(request: RequestOtpRequest): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyOtp(request: VerifyOtpRequest): Result<VerifyOtpResponse> = Result.Success(
            VerifyOtpResponse(accessToken = "", refreshToken = "")
        )
        override suspend fun firebaseAuth(request: FirebaseAuthRequest): Result<VerifyOtpResponse> {
            lastFirebaseAuthRequest = request
            return firebaseResponse
        }
        override suspend fun refreshToken(request: RefreshTokenRequest): Result<AuthTokensDto> = Result.Success(
            AuthTokensDto(accessToken = "", refreshToken = "")
        )
        override suspend fun logout(bearerToken: String, request: LogoutRequest): Result<Unit> = Result.Success(Unit)
    }

    private class FakeAuthTokenProvider : AuthTokenProvider {
        var savedAccessToken: String? = null
        var savedRefreshToken: String? = null
        var savedUserId: String? = null
        var savedEmail: String? = null
        var isLoggedInState: Boolean = false

        override val sessionExpired: Flow<Unit> = MutableSharedFlow()

        override suspend fun getAccessToken(): String? = savedAccessToken
        override suspend fun getRefreshToken(): String? = savedRefreshToken
        override suspend fun getUserId(): String? = savedUserId
        override suspend fun getUserEmail(): String? = savedEmail
        override fun observeIsLoggedIn(): Flow<Boolean> = MutableStateFlow(isLoggedInState)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) {
            savedAccessToken = accessToken
            savedRefreshToken = refreshToken
        }
        override suspend fun saveUserData(userId: String?, email: String?) {
            savedUserId = userId
            savedEmail = email
        }
        override suspend fun setLoggedIn(loggedIn: Boolean) {
            isLoggedInState = loggedIn
        }
        override suspend fun clearTokens() {
            savedAccessToken = null
            savedRefreshToken = null
            savedUserId = null
            isLoggedInState = false
        }
        override fun notifySessionExpired() {}
    }

    private class FakeLocalDataCleaner : com.awan.app.core.data.auth.LocalDataCleaner {
        var clearCount = 0
            private set

        override suspend fun clearAll() {
            clearCount++
        }
    }

}
