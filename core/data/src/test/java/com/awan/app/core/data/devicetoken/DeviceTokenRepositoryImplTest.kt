package com.awan.app.core.data.devicetoken

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.devicetoken.remote.DeviceTokenRemoteDataSource
import com.awan.app.core.data.devicetoken.repository.DeviceTokenRepositoryImpl
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.network.device.DeviceIdProvider
import com.awan.app.core.network.dto.devicetoken.DeviceTokenResponse
import com.awan.app.core.network.dto.devicetoken.RegisterDeviceTokenRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceTokenRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: FakeDeviceTokenRemoteDataSource
    private lateinit var fakeAuthTokenProvider: FakeAuthTokenProvider
    private lateinit var fakeDeviceIdProvider: DeviceIdProvider
    private lateinit var repository: DeviceTokenRepositoryImpl

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeDeviceTokenRemoteDataSource()
        fakeAuthTokenProvider = FakeAuthTokenProvider()
        fakeDeviceIdProvider = object : DeviceIdProvider {
            override fun getDeviceId(): String = "test-device-id-123"
        }
        repository = DeviceTokenRepositoryImpl(
            remoteDataSource = fakeRemoteDataSource,
            deviceIdProvider = fakeDeviceIdProvider,
            authTokenProvider = fakeAuthTokenProvider,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `registerDeviceToken saves token and calls remote data source`() = runTest {
        val fcmToken = "test-fcm-token-456"

        val result = repository.registerDeviceToken(fcmToken)

        assertTrue(result is Result.Success)
        assertEquals(fcmToken, fakeAuthTokenProvider.getFcmToken())
        assertEquals("test-device-id-123", fakeRemoteDataSource.registeredRequest?.deviceId)
        assertEquals(fcmToken, fakeRemoteDataSource.registeredRequest?.fcmToken)
    }

    @Test
    fun `removeDeviceToken calls remote data source with deviceId`() = runTest {
        val result = repository.removeDeviceToken()

        assertTrue(result is Result.Success)
        assertEquals("test-device-id-123", fakeRemoteDataSource.removedDeviceId)
    }
}

private class FakeDeviceTokenRemoteDataSource : DeviceTokenRemoteDataSource {
    var registeredRequest: RegisterDeviceTokenRequest? = null
    var removedDeviceId: String? = null

    override suspend fun registerDeviceToken(request: RegisterDeviceTokenRequest): Result<DeviceTokenResponse> {
        registeredRequest = request
        return Result.Success(
            DeviceTokenResponse(
                id = "token-row-id-1",
                deviceId = request.deviceId,
                deviceType = request.deviceType,
                createdAt = "2026-08-11T00:00:00Z",
                updatedAt = "2026-08-11T00:00:00Z",
            )
        )
    }

    override suspend fun removeDeviceToken(deviceId: String): Result<Unit> {
        removedDeviceId = deviceId
        return Result.Success(Unit)
    }
}

private class FakeAuthTokenProvider : AuthTokenProvider {
    private var fcmToken: String? = null

    override suspend fun getAccessToken(): String? = "access-token"
    override suspend fun getRefreshToken(): String? = "refresh-token"
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUserData(userId: String?, email: String?) {}
    override suspend fun getUserId(): String? = "user-123"
    override suspend fun getUserEmail(): String? = "user@test.com"
    override suspend fun clearTokens() {}
    override fun observeIsLoggedIn(): Flow<Boolean> = emptyFlow()
    override suspend fun setLoggedIn(loggedIn: Boolean) {}
    override suspend fun saveFcmToken(token: String) {
        fcmToken = token
    }
    override suspend fun getFcmToken(): String? = fcmToken
    override val sessionExpired: Flow<Unit> = emptyFlow()
    override fun notifySessionExpired() {}
}
