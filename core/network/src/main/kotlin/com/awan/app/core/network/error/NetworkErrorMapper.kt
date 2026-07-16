package com.awan.app.core.network.error

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.ApiErrorResponse
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
        when (val code = code()) {
            401 -> AppError.Unauthorized
            in 500..599 -> AppError.Server(code)
            else -> {
                val bodyMessage = response()?.errorBody()?.string()?.let { rawBody ->
                    try {
                        json?.decodeFromString<ApiErrorResponse>(rawBody)?.message ?: rawBody
                    } catch (_: Exception) {
                        rawBody
                    }
                }
                AppError.Api(code, bodyMessage)
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
        } catch (e: Exception) {
            Result.Error(e.toAppError(json))
        }
    }
    return if (dispatcher != null) withContext(dispatcher) { execute() } else execute()
}
