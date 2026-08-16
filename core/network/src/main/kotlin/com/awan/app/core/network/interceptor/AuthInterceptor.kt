package com.awan.app.core.network.interceptor

import com.awan.app.core.datastore.auth.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
class AuthInterceptor @Inject constructor(
    private val authTokenProvider: AuthTokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (AUTH_PATHS.any { path.contains(it) }) {
            return chain.proceed(request)
        }

        val token = runBlocking { authTokenProvider.getAccessToken() }

        val authenticatedRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        val AUTH_PATHS = setOf(
            "/v1/auth/otp/request",
            "/v1/auth/otp/verify",
            "/v1/auth/firebase",
            "/v1/auth/refresh",
        )
    }
}
