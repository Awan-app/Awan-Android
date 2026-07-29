package com.awan.app.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Extends read and write timeouts for AI-backed endpoints (`/ai/` in path).
 *
 * LLM calls behind the backend are inherently slower than standard CRUD, so these
 * requests get a longer window while every other endpoint keeps the default 30 s.
 */
class AiTimeoutInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return if (request.url.encodedPath.contains("/ai/")) {
            chain
                .withReadTimeout(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .withWriteTimeout(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .proceed(request)
        } else {
            chain.proceed(request)
        }
    }

    private companion object {
        const val AI_TIMEOUT_SECONDS = 90
    }
}
