package com.awan.app.core.network.error

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.common.ApiErrorResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toAppError(json: Json? = null): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout
    is UnknownHostException -> AppError.Network
    is IOException -> AppError.Network
    is HttpException -> {
        val code = code()
        val rawBody = response()?.errorBody()?.string()
        var bodyMessage: String? = rawBody
        var remainingAttempts: Int? = null
        var retryAfterSeconds: Int? = null
        var errorCode: String? = null

        if (!rawBody.isNullOrBlank() && json != null) {
            try {
                val parsed = json.decodeFromString<ApiErrorResponse>(rawBody)
                bodyMessage = parsed.message ?: rawBody
                remainingAttempts = parsed.info?.remainingAttempts
                retryAfterSeconds = parsed.info?.retryAfterSeconds
                errorCode = parsed.errorCode ?: parsed.code
            } catch (_: Exception) {
            }
        }

        if (errorCode != null) {
            AppError.Api(
                code = code,
                body = bodyMessage,
                remainingAttempts = remainingAttempts,
                retryAfterSeconds = retryAfterSeconds,
                errorCode = errorCode,
            )
        } else {
            when (code) {
                401 -> AppError.Unauthorized
                in 500..599 -> AppError.Server(code)
                else -> AppError.Api(
                    code = code,
                    body = bodyMessage,
                    remainingAttempts = remainingAttempts,
                    retryAfterSeconds = retryAfterSeconds,
                    errorCode = errorCode,
                )
            }
        }
    }
    is SerializationException -> AppError.Serialization
    else -> AppError.Unknown(this)
}

suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher? = null,
    json: Json? = null,
    call: suspend () -> T,
): Result<T> {
    val execute: suspend () -> Result<T> = {
        try {
            Result.Success(call())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toAppError(json))
        }
    }

    return if (dispatcher != null) {
        withContext(dispatcher) { execute() }
    } else {
        execute()
    }
}
