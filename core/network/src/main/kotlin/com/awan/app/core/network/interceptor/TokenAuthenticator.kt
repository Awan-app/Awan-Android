package com.awan.app.core.network.interceptor

import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.network.api.AuthApiService
import com.awan.app.core.network.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
class TokenAuthenticator @Inject constructor(
    private val authApiServiceProvider: Provider<AuthApiService>,
    private val authTokenProvider: AuthTokenProvider,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Guard: give up after 2 retries (1 original + 1 after refresh).
        if (response.priorResponseCount() >= 2) return null

        return runBlocking {
            refreshMutex.withLock {
                // After acquiring the lock, check if a concurrent thread already refreshed.
                val currentToken = authTokenProvider.getAccessToken()
                val requestToken = response.request
                    .header("Authorization")
                    ?.removePrefix("Bearer ")
                    ?.trim()

                if (currentToken != null && currentToken != requestToken) {
                    // Token was refreshed by another coroutine while we waited for the lock.
                    // Re-use the new token without another network call.
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // We must refresh.
                val refreshToken = authTokenProvider.getRefreshToken()
                    ?: return@runBlocking null // No refresh token → user must log in again.

                try {
                    val newTokens = authApiServiceProvider.get()
                        .refreshToken(RefreshTokenRequest(refreshToken = refreshToken))

                    authTokenProvider.saveTokens(
                        accessToken = newTokens.accessToken,
                        refreshToken = newTokens.refreshToken,
                    )

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // Refresh failed — clear tokens so the app transitions to the login screen.
                    authTokenProvider.clearTokens()
                    null
                }
            }
        }
    }

    companion object {
        private val refreshMutex = Mutex()
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
