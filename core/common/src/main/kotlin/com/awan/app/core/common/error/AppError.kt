package com.awan.app.core.common.error

sealed class AppError {
    data object Network : AppError()
    data object Timeout : AppError()
    data object Unauthorized : AppError()
    data object NotFound : AppError()
    data class Server(val code: Int) : AppError()
    data class Api(
        val code: Int,
        val body: String?,
        val remainingAttempts: Int? = null,
        val retryAfterSeconds: Int? = null,
        val errorCode: String? = null,
    ) : AppError()
    data object Serialization : AppError()

    /** Input rejected before it left the device — never comes back from a data source. */
    data class Validation(val reason: ValidationReason) : AppError()
    data class Unknown(val cause: Throwable? = null) : AppError()
}

enum class ValidationReason {
    TEXT_BLANK,
    TEXT_TOO_LONG,
    IMAGE_TOO_LARGE,
    IMAGE_TYPE_UNSUPPORTED,
}
