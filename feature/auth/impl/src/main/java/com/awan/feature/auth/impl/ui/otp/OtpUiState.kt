package com.awan.feature.auth.impl.ui.otp

import androidx.compose.runtime.Immutable

enum class OtpStatus {
    Idle,

    Verifying,

    Wrong,

    Expired,

    Locked,
}

@Immutable
data class OtpUiState(
    val email: String = "",
    val digits: List<String> = List(6) { "" },
    val status: OtpStatus = OtpStatus.Idle,
    val errorMessage: String? = null,
    val isResendEnabled: Boolean = false,
    val resendSecondsRemaining: Int = 0,
)

val OtpUiState.isComplete: Boolean
    get() = digits.all { it.isNotEmpty() }

val OtpUiState.areCellsLocked: Boolean
    get() = status == OtpStatus.Verifying || status == OtpStatus.Locked
