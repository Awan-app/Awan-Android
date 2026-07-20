package com.awan.app.core.common.error

sealed class AppError {
    data object Network : AppError()
    data object Timeout : AppError()
    data object Unauthorized : AppError()
    data class Server(val code: Int) : AppError()
    data class Api(
        val code: Int,
        val body: String?,
        val remainingAttempts: Int? = null,
        val errorCode: String? = null,
    ) : AppError()
    data object Serialization : AppError()
    data class Unknown(val cause: Throwable? = null) : AppError()
}
