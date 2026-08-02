package com.awan.app.core.common.error

import com.awan.app.core.common.R
import com.awan.app.core.common.text.UiText

fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.StringResource(R.string.error_network)
    AppError.Timeout -> UiText.StringResource(R.string.error_timeout)
    AppError.Unauthorized -> UiText.StringResource(R.string.error_unauthorized)
    is AppError.Server -> UiText.StringResource(R.string.error_server)
    is AppError.Api -> when {
        errorCode == "OTP_LOCKED" -> UiText.StringResource(R.string.error_otp_attempts_exceeded)
        errorCode == "OTP_INVALID_CODE" -> {
            when {
                remainingAttempts == 0 -> UiText.StringResource(R.string.error_otp_attempts_exceeded)
                remainingAttempts != null && remainingAttempts > 0 -> UiText.StringResource(
                    R.string.error_invalid_otp_with_attempts,
                    remainingAttempts,
                )
                else -> UiText.StringResource(R.string.error_invalid_otp)
            }
        }
        code == 404 -> UiText.StringResource(R.string.error_email_not_found)
        code == 410 -> UiText.StringResource(R.string.error_otp_expired)
        code == 429 -> UiText.StringResource(R.string.error_too_many_requests)
        else -> body?.takeIf { it.isNotBlank() }?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.error_unknown)
    }
    AppError.Serialization -> UiText.StringResource(R.string.error_serialization)
    is AppError.Validation -> UiText.StringResource(R.string.error_validation)
    is AppError.Unknown -> UiText.StringResource(R.string.error_unknown)
}
