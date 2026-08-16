package com.awan.app.core.network.interceptor

import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.network.api.AuthApiService
import com.awan.app.core.network.device.DeviceIdProvider
import com.awan.app.core.network.dto.auth.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val authApiServiceProvider: Provider<AuthApiService>,
    private val authTokenProvider: AuthTokenProvider,
    private val deviceIdProvider: DeviceIdProvider,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponseCount() >= 2) return null

        return runBlocking {
            refreshMutex.withLock {
                val currentToken = authTokenProvider.getAccessToken()
                val requestToken = response.request
                    .header("Authorization")
                    ?.removePrefix("Bearer ")
                    ?.trim()

                if (currentToken != null && currentToken != requestToken) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // We must refresh.
                val refreshToken = authTokenProvider.getRefreshToken()
                if (refreshToken == null) {
                    authTokenProvider.notifySessionExpired()
                    authTokenProvider.clearTokens()
                    return@runBlocking null
                }

                try {
                    val newTokens = authApiServiceProvider.get()
                        .refreshToken(
                            RefreshTokenRequest(
                                refreshToken = refreshToken,
                                deviceId = deviceIdProvider.getDeviceId(),
                            )
                        )

                    authTokenProvider.saveTokens(
                        accessToken = newTokens.accessToken,
                        refreshToken = newTokens.refreshToken,
                    )

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // Only a server rejection means the session is actually dead. IO failures,
                    // timeouts and 5xx are transient — wiping the tokens there logs the user out
                    // over a dropped connection while their refresh token is still valid.
                    if (e is HttpException && e.code() in SESSION_REJECTED_CODES) {
                        authTokenProvider.notifySessionExpired()
                        authTokenProvider.clearTokens()
                    }
                    null
                }
            }
        }
    }

    companion object {
        private val refreshMutex = Mutex()
        private val SESSION_REJECTED_CODES = setOf(
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN,
        )
    }
}

private fun Response.priorResponseCount(): Int {
    var count = 0
    var prior = priorResponse
    while (prior != null) {
        count++
        prior = prior.priorResponse
    }
    return count
}

